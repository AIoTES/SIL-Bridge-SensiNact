/**
 * /**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union's Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2017-2018, by : - Università degli Studi della Calabria
 * <p>
 * <p>
 * For more information, contact: - @author
 * <a href="mailto:g.caliciuri@dimes.unical.it">Giuseppe Caliciuri</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * <p>
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */
package eu.interiot.intermw.bridge.sensinact.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.fetcher.WebSocketModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.HTTP;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.http.ws.SensinactWebSocketConnectionManager;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.intermw.bridge.sensinact.wrapper.SubscriptionResponse;
import eu.interiot.intermw.bridge.sensinact.wrapper.UnsubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This Sensinact version is the previous Eclipse version, meaning that the only
 * the current deployment made in Japan is compatible
 */
public class SensinactCommunicationBridgeV1 implements SensinactAPI {

    protected ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private final Logger LOG = LoggerFactory.getLogger(SensinactCommunicationBridgeV1.class);
    private static final String URL = "http://%s:%s/sensinact/providers/filter?jsonpath=$..providers.%s"; //http://%s:%s/sensinact/providers/filter?jsonpath=$..providers.[0:100]
    private SensinactConfig config;
    private SensinactModelRecoverListener listener;
    private SensinactSubscriptionManagerV1 sensinactSubscriptionManager;
    private SensinactWebSocketConnectionManager connectionWebSocket;
    private static final String AHA_SERVICE_NAME = "aha";
    private static final String FUNCTION_TYPE_RESOURCE_NAME = "function-type";
    private static final String NOT_A_FUNCTION = "not-a-function";

    private void startCommunication() {
        // First polling
        //service.scheduleAtFixedRate(new SensinactJSONModelFetcherTask(),0L,5000L, TimeUnit.MILLISECONDS);
        connectionWebSocket = new SensinactWebSocketConnectionManager(String.format("ws://%s:%d", config.getHost(), config.getWebsocketPort()).toString(), new WebSocketModelRecoverListener() {
            @Override
            public void notify(String content) {
                try {
                    JsonObject payloadJson = (JsonObject) new JsonParser().parse(content);
                    String type = SNAResource.DEFAULT_TYPE;
                    String[] updateMessage = payloadJson.get("uri").getAsString().split("/");
                    String value = payloadJson.getAsJsonObject("notification").get("value").getAsString();
                    String timestamp = payloadJson.getAsJsonObject("notification").get("timestamp").getAsString();
                    if (listener != null) {
                        listener.notify(updateMessage[1], updateMessage[2], updateMessage[3], type, value, timestamp, Collections.EMPTY_MAP);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to deliver message", e);
                }

            }
        });
        sensinactSubscriptionManager = new SensinactSubscriptionManagerV1(config);
        connectionWebSocket.connect();
    }

    public void stopCommunication() {
        try {
            LOG.info("shutting down schedule service");
            service.shutdown();
        } catch (Exception e) {
            LOG.error("Failed", e);
        }

        try {
            LOG.info("shutting down websocket connection");
            connectionWebSocket.disconnect();
        } catch (Exception e) {
            LOG.error("Failed", e);
        }

    }

    @Override
    public SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        return sensinactSubscriptionManager.subscribe(userId, provider, service, resource, callback);
    }

    @Override
    public UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        return sensinactSubscriptionManager.unsubscribe(userId, provider, service, resource, callback);
    }

    @Override
    public void createDevice(String provider, String service, String resource, String type, String value) throws Exception {
        throw new Exception("Create device method is not supported for sensinact V1");
    }

    @Override
    public void removeDevice(String provider, String service, String resource) throws Exception {
        throw new Exception("Remove device method is not supported for sensinact V1");
    }

    @Override
    public List<SNAResource> listDevices() {
        String functionType;
        List<SNAResource> providerResourceList = new ArrayList<>();
        List<SNAResource> deviceList = new ArrayList<>();
        try {
            LOG.info("Starting executing.." + Thread.currentThread().getName());

            HTTP httpcall = new HTTP();
            httpcall.setMethod("GET");
            try {
                String deviceLimit = "*";
                if (config.getMaxDeviceNumber() != null && config.getMaxDeviceNumber() > -1) {
                    deviceLimit = String.format("[0:%d]", config.getMaxDeviceNumber());
                }
                String result = httpcall.submit(String.format(URL, config.getHost(), config.getHttpPort(), deviceLimit));
                JsonArray jsProvider = (JsonArray) new JsonParser().parse(result);
                LOG.info("Total providers:" + jsProvider.size());
                for (Iterator itProvider = jsProvider.iterator(); itProvider.hasNext();) {
                    providerResourceList.clear();
                    functionType = NOT_A_FUNCTION;
                    JsonObject ob = (JsonObject) itProvider.next();
                    String providerName = ob.get("name").getAsString();
                    LOG.info("Provider:" + providerName);
                    JsonArray jsServices = ob.getAsJsonArray("services");

                    for (Iterator itService = jsServices.iterator(); itService.hasNext();) {
                        JsonObject obService = (JsonObject) itService.next();
                        String serviceName = obService.get("name").getAsString();
                        LOG.info("Service:" + serviceName);
                        JsonArray jsResources = obService.getAsJsonArray("resources");

                        for (Iterator itResource = jsResources.iterator(); itResource.hasNext();) {
                            JsonObject obResource = (JsonObject) itResource.next();
                            String resourceName = obResource.get("name").getAsString();
                            if (serviceName.equals(AHA_SERVICE_NAME)
                                    && resourceName.equals(FUNCTION_TYPE_RESOURCE_NAME)) {
                                functionType = resourceName;
                            }
                            providerResourceList.add(new SNAResource(providerName, serviceName, resourceName, functionType));
                        }
                    }
                    for (SNAResource resource : providerResourceList) {
                        resource.setType(functionType);
                    }
                    deviceList.addAll(providerResourceList);
                }
                LOG.info(result);
            } catch (Exception e) {
                LOG.debug("Failed parser jsonpath response", e);
            }
            LOG.info("Finished executing");
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call", e);
        }

        return deviceList;
    }

    @Override
    public void act(String provider, String service, String resource) throws Exception {
        HTTP httpcall = new HTTP();
        httpcall.setMethod("POST");
        String URL = String.format("http://%s:%s/sensinact/providers/%s/services/%s/resources/%s/ACT", config.getHost(), config.getHttpPort(), provider, service, resource);
        httpcall.submit(URL);
    }

    @Override
    public void setConfig(SensinactConfig config) {
        this.config = config;
    }

    @Override
    public void setListener(SensinactModelRecoverListener listener) {
        this.listener = listener;
    }

    @Override
    public void connect() {
        if (config == null) {
            LOG.error("The configuration bridge is empty, setup the connect information before start service");
            return;
        }

        if (listener == null) {
            LOG.warn("There is no listener for the notification");
        }

        startCommunication();
    }

    @Override
    public void disconnect() {
        stopCommunication();
    }

    /**
     * This task fetchs the current JSON model of all sensinact
     * provider/service/resource hierarchy
     */
    class SensinactJSONModelFetcherTask implements Runnable {

        @Override
        public void run() {
            final long timestamp = System.currentTimeMillis();
            for (SNAResource resource : SensinactCommunicationBridgeV1.this.listDevices()) {
                if (SensinactCommunicationBridgeV1.this.listener != null) {
                    SensinactCommunicationBridgeV1.this.listener.notify(
                            resource.getProvider(), 
                            resource.getService(), 
                            resource.getResource(), 
                            resource.getType(), 
                            null, 
                            String.valueOf(timestamp),
                            resource.getMetadata()
                    );
                }
            }

        }
    }

    public SensinactModelRecoverListener getListener() {
        return listener;
    }
}
