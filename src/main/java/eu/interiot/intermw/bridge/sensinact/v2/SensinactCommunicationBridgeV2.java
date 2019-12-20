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
package eu.interiot.intermw.bridge.sensinact.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.fetcher.WebSocketModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.HTTP;
import eu.interiot.intermw.bridge.sensinact.http.model.ProviderJSONPayload;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.http.ws.WebSocketTrigger;
import eu.interiot.intermw.bridge.sensinact.http.ws.SensinactWebSocketConnectionManager;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.intermw.bridge.sensinact.wrapper.SubscriptionResponse;
import eu.interiot.intermw.bridge.sensinact.wrapper.UnsubscriptionResponse;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * This implementation of Sensinact API refers to the Sensinact Eclipse project
 * version, which is accessible via Eclipse.org
 */
public class SensinactCommunicationBridgeV2 implements SensinactAPI {

    private final Logger LOG = LoggerFactory.getLogger(SensinactCommunicationBridgeV2.class);
    private SensinactConfig config;
    private SensinactWebSocketConnectionManager connectionWebSocket;
    private SensinactModelRecoverListener listener;
    private SensinactWebSocketConnectionManager deviceCreationEndPoint;
    private static final String AHA_SERVICE_NAME = "aha";
    private static final String FUNCTION_TYPE_RESOURCE_NAME = "function-type";
    private final Map<String, SNAResource> subscribedResources;
    private static final String RESOURCE_KEY_PATTERN = "%s/%s/%s";

    public SensinactCommunicationBridgeV2() {
        subscribedResources = new HashMap<String, SNAResource>();
    }
    
    @Override
    public SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        LOG.debug("Subscribing V2...");
        final String key = String.format(RESOURCE_KEY_PATTERN, provider, service, resource);
        String type = "undefined";
        try {
            final SNAResource snaResource = getResource(provider, service, resource);
            type = snaResource.getType();
            subscribedResources.put(key, snaResource);
            LOG.info("successfully subscribed to {}", snaResource);
        } catch (ResourceNotFoundException e) {
            LOG.error("failed to subscribe to {}/{}/{}: {}", provider, service, resource, e.getMessage());
            return new SubscriptionResponse(key);
        }
        return new SubscriptionResponse(key, type);
    }

    @Override
    public UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        LOG.debug("Unsubscribing V2...");
        final String key = String.format(RESOURCE_KEY_PATTERN, provider, service, resource);
        String type = "undefined";
        final SNAResource snaResource = subscribedResources.get(key);
        final UnsubscriptionResponse response;
        if (snaResource != null) {
            type = snaResource.getType();
            response = new UnsubscriptionResponse(key, type);
            LOG.info("successfully unsubscribed to {}", snaResource);
        } else {
            LOG.error("failed to unsubscribe to {}/{}/{}: {}", provider, service, resource);
            return new UnsubscriptionResponse(key);
        }
        return response;
    }

    @Override
    public void createDevice(String provider, String service, String resource, String type, String value) throws Exception {
        Session session = deviceCreationEndPoint.getSession().get();
        try {
            final String payload = ProviderJSONPayload.builder().provider(provider).service(service).resource(resource).type(type).value(value).build().toString();
            LOG.debug("Sending device creation/update message {} to sensinact", payload);
            session.getRemote().sendString(payload);
        } catch (Exception e) {
            LOG.error("Failed to create device {}/{}/{}/{}", provider, service, resource, value, e);
        }

    }

    @Override
    public void removeDevice(String provider, String service, String resource) throws Exception {
        Session session = deviceCreationEndPoint.getSession().get();
        try {
            final String payload = ProviderJSONPayload.builder().provider(provider).service(service).resource(resource).type(ProviderJSONPayload.TYPE.Goodbye).build().toString();
            LOG.debug("Sending device creation/update message {} to sensinact", payload);
            session.getRemote().sendString(payload);
        } catch (Exception e) {
            LOG.error("Failed to remove device {}/{}/{}", provider, service, resource, e);
        }

    }

    @Override
    public List<SNAResource> listDevices() {
        String functionType;
        List<SNAResource> snaResourceList = new ArrayList<>();
        List<SNAResource> providerResourceList = new ArrayList<>();
        String URL = "http://%s:%s/jsonpath:sensinact?jsonpath=$.*";
        try {
            LOG.debug("Starting executing.." + Thread.currentThread().getName());

            HTTP httpcall = new HTTP();
            httpcall.setMethod("GET");
            try {

                String result = httpcall.submit(String.format(URL, config.getHost(), config.getHttpPort()));
                JsonObject jsonPathPayload = (JsonObject) new JsonParser().parse(result);

                JsonArray jsProvider = jsonPathPayload.getAsJsonArray("providers");

                LOG.debug("Total providers:" + jsProvider.size());
                for (Iterator itProvider = jsProvider.iterator(); itProvider.hasNext();) {
                    providerResourceList.clear();
                    functionType = SNAResource.DEFAULT_TYPE;
                    JsonObject ob = (JsonObject) itProvider.next();
                    String providerName = ob.get("name").getAsString();
                    LOG.debug("Provider:" + providerName);
                    JsonArray jsServices = ob.getAsJsonArray("services");

                    for (Iterator itService = jsServices.iterator(); itService.hasNext();) {
                        JsonObject obService = (JsonObject) itService.next();
                        String serviceName = obService.get("name").getAsString();
                        LOG.debug("Service:" + serviceName);
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
                    snaResourceList.addAll(providerResourceList);
                }
            } catch (Exception e) {
                LOG.debug("Failed parser jsonpath response", e);
            }
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call", e);
        }
        return snaResourceList;
    }


    private SNAResource getResource(final String providerId, final String serviceId, final String resourceId) throws ResourceNotFoundException {
        String functionType;
        SNAResource resource = null;
        List<SNAResource> providerResourceList = new ArrayList<>();
        final String URL = "http://%s:%s/jsonpath:sensinact?jsonpath=$['providers'][?(@['name'] == '%s')]";
        try {
            LOG.debug("Starting executing.." + Thread.currentThread().getName());

            HTTP httpcall = new HTTP();
            httpcall.setMethod("GET");
            try {

                final String result = httpcall.submit(String.format(URL, config.getHost(), config.getHttpPort(), providerId));
                final JsonObject jsonPathPayload = (JsonObject) new JsonParser().parse(result);
                providerResourceList.clear();
                functionType = SNAResource.DEFAULT_TYPE;
                JsonObject ob = jsonPathPayload;
                String providerName = ob.get("name").getAsString();
                LOG.debug("Provider:" + providerName);
                JsonArray jsServices = ob.getAsJsonArray("services");

                for (Iterator itService = jsServices.iterator(); itService.hasNext();) {
                    JsonObject obService = (JsonObject) itService.next();
                    String serviceName = obService.get("name").getAsString();
                    LOG.debug("Service:" + serviceName);
                    JsonArray jsResources = obService.getAsJsonArray("resources");

                    for (Iterator itResource = jsResources.iterator(); itResource.hasNext();) {
                        JsonObject obResource = (JsonObject) itResource.next();
                        String resourceName = obResource.get("name").getAsString();
                        if (serviceName.equals(AHA_SERVICE_NAME) 
                        && resourceName.equals(FUNCTION_TYPE_RESOURCE_NAME)) {
                            functionType = resourceName;
                        }
                        if (serviceName.equals(serviceId) 
                        && resourceName.equals(resourceId)) {
                            resource = new SNAResource(providerName, serviceName, resourceName, functionType);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Failed parser jsonpath response", e);
            }
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call", e);
        }
        if (resource == null) {
            throw new ResourceNotFoundException(providerId, serviceId, resourceId);
        }
        return resource;
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

    private void connectWebsocket() {
        String protocol = "";
        if (config.getProtocol() == null) {
            protocol = "ws";
        } else {
            if (config.getProtocol().equals("https")) {
                protocol = "wss";
            } else if (config.getProtocol().equals("http")) {
                protocol = "ws";
            }
        }

        final String sensinactWebSocketURL = String.format("%s://%s:%d/ws", protocol, config.getHost(), config.getHttpPort()).toString();
        connectionWebSocket = new SensinactWebSocketConnectionManager(sensinactWebSocketURL, new WebSocketModelRecoverListener() {
            @Override
            public void notify(String content) {
                try {
                    LOG.info("Message received before json parser:{}", content);
                    JsonObject payloadJson = (JsonObject) new JsonParser().parse(content);
                    JsonObject messagesJson = payloadJson.getAsJsonObject().getAsJsonArray("messages").get(0).getAsJsonObject();
                    String value = messagesJson.get("notification").getAsJsonObject().get("value").getAsString();
                    String timestamp = messagesJson.get("notification").getAsJsonObject().get("timestamp").getAsString();
                    String resourceURI = messagesJson.get("uri").getAsString();
                    SNAResource snaResource = subscribedResources.get(resourceURI);
                    if (snaResource== null) {
                        snaResource = new SNAResource(resourceURI, value);
                        snaResource.setValue(value);
                        LOG.warn("notifying with unsubscribed %s: undefined type", snaResource, timestamp);
                    }
                    if (listener != null) {
                        final String providerId = snaResource.getProvider();
                        final String serviceId = snaResource.getService();
                        final String resourceId = snaResource.getResource();
                        final String type = snaResource.getType();
                        listener.notify(providerId, serviceId, resourceId, type, value, timestamp);
                        LOG.info("successfully notified %s with %s at %s", listener, snaResource, timestamp);
                    } else {
                        LOG.error("failed to notify with %s at %s: unexpected null listener", snaResource, timestamp);
                    }
                } catch (Throwable e) {
                    LOG.error("Failed to deliver message", e);
                }
            }
        });
        connectionWebSocket.setTriggerAfterConnection(new WebSocketTrigger() {
            @Override
            public void execute() throws Exception {
                connectionWebSocket.getSession().get().getRemote().sendString("{\"uri\":\"sensinact/SUBSCRIBE\",\"rid\":\"webapp\",\"parameters\":[{\"name\":\"sender\",\"type\":\"string\",\"value\":\"/.*\"},{\"name\":\"pattern\",\"type\":\"boolean\",\"value\":true},{\"name\":\"complement\",\"type\":\"boolean\",\"value\":false},{\"name\":\"types\",\"type\":\"array\",\"value\":[\"UPDATE\",\"LIFECYCLE\"]}]}");
            }
        });

        final String deviceCreationURL = String.format("%s://%s:%d/androidws", protocol, config.getHost(), config.getHttpPort()).toString();

        deviceCreationEndPoint = new SensinactWebSocketConnectionManager(deviceCreationURL);

        connectionWebSocket.connect();
        deviceCreationEndPoint.connect();

    }

    @Override
    public void connect() {
        connectWebsocket();
    }

    @Override
    public void disconnect() {
        connectionWebSocket.disconnect();
        deviceCreationEndPoint.disconnect();
    }
}
