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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import java.text.MessageFormat;
import java.text.ParseException;
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
    private final UserSubscriptionManager userSubscriptions;
    private static final String RESOURCE_PATH_PATTERN = "/%s/%s/%s/value";
    private static final String GET_ALL_PROVIDERS_DETAILS_REQUEST_PATTERN = 
        "http://%s:%s/jsonpath:sensinact?jsonpath=$.*";
    private static final String GET_ONE_PROVIDER_DETAILS_REQUEST_PATTERN = 
        "http://%s:%s/jsonpath:sensinact?jsonpath=[?(@['name'] == '%s')]";
    private static final String GET_RESOURCE_REQUEST_PATTERN = 
        "http://%s:%s/sensinact/%s/%s/%s";
    private static final String GET_RESOURCE_VALUE_REQUEST_PATTERN = 
        GET_RESOURCE_REQUEST_PATTERN + "/GET";
    private static final String ACT_RESOURCE_REQUEST_PATTERN =
        "http://%s:%s/sensinact/providers/%s/services/%s/resources/%s/ACT";
    private static final String LIFECYCLE_SUBSCRIBE_REQUEST = 
        "{\"uri\":\"sensinact/SUBSCRIBE\",\"rid\":\"webapp\",\"parameters\":[{\"name\":\"sender\",\"type\":\"string\",\"value\":\"/.*\"},{\"name\":\"pattern\",\"type\":\"boolean\",\"value\":true},{\"name\":\"complement\",\"type\":\"boolean\",\"value\":false},{\"name\":\"types\",\"type\":\"array\",\"value\":[\"UPDATE\",\"LIFECYCLE\"]}]}";
    
    public SensinactCommunicationBridgeV2() {
        subscribedResources = new HashMap<String, SNAResource>();
        userSubscriptions = new UserSubscriptionManager();
    }
    
    @Override
    public SubscriptionResponse subscribe(String resourceUri) throws Exception {
        try {
            Object[] uri = RESOURCE_URI_FORMAT.parse(resourceUri);
            SubscriptionResponse subscribe = subscribe(uri[0].toString(), uri[1].toString(), uri[2].toString());
            return subscribe;
        } catch (ParseException e) {
            LOG.error("unable to subscribe to resourceUri: not a valid uri: {}", e.getMessage());
            throw e;
        }
    }
        
    @Override
    public SubscriptionResponse subscribe(String provider, String service, String resource) throws Exception {
        return subscribe(UNKNOWN_USER, provider, service, resource, NO_CALLBACK);
    }

    @Override
    public SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        final String resourcePath = String.format(RESOURCE_PATH_PATTERN, provider, service, resource);
        LOG.debug(String.format("Subscribing to %s for user %s...", resourcePath, userId));
        String type;
        SubscriptionResponse response;
        SNAResource alreadySubscribedResource = subscribedResources.get(resourcePath);
        if (alreadySubscribedResource != null) {
            LOG.info("have already subscribed to {} before...", resourcePath);
            if (userSubscriptions.hasAlreadySubscribedTo(userId, resourcePath)) {
                response = userSubscriptions.getSubscriptionResponse(userId, resourcePath);
            } else {
                response = userSubscriptions.putSubscriptionResponse(userId, alreadySubscribedResource);
            }
        } else {
            try {
                final SNAResource snaResource = getResource(provider, service, resource);
                type = snaResource.getType();
                LOG.info("successfully subscribed to {}", snaResource);
                response = new SubscriptionResponse(snaResource);
                subscribedResources.put(resourcePath, snaResource);
                userSubscriptions.putSubscriptionResponse(userId, snaResource);
            } catch (ResourceNotFoundException e) {
                LOG.error("failed to subscribe to {}/{}/{}: {}", provider, service, resource, e.getMessage());
                response = new SubscriptionResponse(resourcePath);
            }
        }
        return response;
    }

    @Override
    public UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        final String resourcePath = String.format(RESOURCE_PATH_PATTERN, provider, service, resource);
        LOG.debug(String.format("Unsubscribing to %s for user %s...", resourcePath, userId));
        String type;
        final SNAResource subscribedResource = subscribedResources.get(resourcePath);
        final UnsubscriptionResponse response;
        if (subscribedResource != null) {
            userSubscriptions.removeSubscriptionResponse(userId, subscribedResource);
            type = subscribedResource.getType();
            response = new UnsubscriptionResponse(resourcePath, type);
            LOG.info("successfully unsubscribed to {}", subscribedResource);
        } else {
            LOG.error("failed to unsubscribe to {}", resourcePath);
            return new UnsubscriptionResponse(resourcePath);
        }
        return response;
    }

    @Override
    public void updateResource(String provider, String service, String resource, String type, String value, Map<String, String> metadata) throws Exception {
        Session session = deviceCreationEndPoint.getSession().get();
        try {
            final String payload = ProviderJSONPayload.builder().provider(provider).service(service).resource(resource).type(type).value(value).metadata(metadata).build().toString();
            LOG.debug("Sending device creation/update message {} to sensinact", payload);
            session.getRemote().sendString(payload);
        } catch (Exception e) {
            LOG.error("Failed to create device {}/{}/{}/{}", provider, service, resource, value, e);
        }

    }

    @Override
    public void removeResource(String provider, String service, String resource) throws Exception {
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
    public List<SNAResource> listResources() {
        String functionType;
        List<SNAResource> snaResourceList = new ArrayList<>();
        List<SNAResource> providerResourceList = new ArrayList<>();
        try {
            LOG.debug("listing resources..." + Thread.currentThread().getName());

            HTTP httpcall = new HTTP();
            httpcall.setMethod("GET");
            try {
                String result = httpcall.submit(
                        String.format(GET_ALL_PROVIDERS_DETAILS_REQUEST_PATTERN, config.getHost(), config.getHttpPort()));
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
                                String resourceValue = 
                                        getResourceValue(providerName, serviceName, resourceName, SNAResource.DEFAULT_TYPE);
                                functionType = resourceValue;
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


    private String getResourceValue(final String providerId, final String serviceId, final String resourceId, final String defaultValue) {
        HTTP httpcall = new HTTP();
        httpcall.setMethod("GET");
        JsonObject jsonPathPayload;
        String value = defaultValue;
        try {
            String result = httpcall.submit(
                String.format(
                    GET_RESOURCE_VALUE_REQUEST_PATTERN, 
                    config.getHost(), 
                    config.getHttpPort(),
                    providerId,
                    serviceId,
                    resourceId
                )
            );
            jsonPathPayload = (JsonObject) new JsonParser().parse(result);
            JsonObject response = jsonPathPayload.getAsJsonObject("response");
            value = response.get("value").getAsString();
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call", e);
        }
        return value;
    }

    private Map<String, String> getResourceMetadata(final String providerId, final String serviceId, final String resourceId) {
        final Map<String, String> metadataMap = new HashMap<String, String>();
        HTTP httpcall = new HTTP();
        httpcall.setMethod("GET");
        JsonObject jsonPathPayload;
        String value = "";
        try {
            String result = httpcall.submit(
                String.format(
                    GET_RESOURCE_REQUEST_PATTERN, 
                    config.getHost(), 
                    config.getHttpPort(),
                    providerId,
                    serviceId,
                    resourceId
                )
            );
            jsonPathPayload = (JsonObject) new JsonParser().parse(result);
            JsonObject response = jsonPathPayload.getAsJsonObject("response");
            JsonArray attributes = response.getAsJsonArray("attributes");
            JsonArray metadata = attributes.get(0).getAsJsonObject().getAsJsonArray("metadata");
            String propertyId, propertyValue;
            for (JsonElement element : metadata) {
                JsonObject asJsonObject = element.getAsJsonObject();
                JsonPrimitive asJsonPrimitive = asJsonObject.getAsJsonPrimitive("name");
                propertyId = asJsonPrimitive.getAsString();
                JsonPrimitive asJsonObjectValue = asJsonObject.getAsJsonPrimitive("value");
                propertyValue = asJsonObjectValue.getAsString();
                metadataMap.put(propertyId, propertyValue);
            }
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call", e);
        }
        return metadataMap;
    }

    private SNAResource getResource(final String providerId, final String serviceId, final String resourceId) throws ResourceNotFoundException {
        SNAResource resource = null;
        String functionType = 
            getResourceValue(providerId, AHA_SERVICE_NAME, FUNCTION_TYPE_RESOURCE_NAME, SNAResource.DEFAULT_TYPE);
        Map<String, String> metadata = 
            getResourceMetadata(providerId, serviceId, resourceId);
        resource = new SNAResource(providerId, serviceId, resourceId, functionType, metadata);
        return resource;
    }

    @Override
    public void act(String provider, String service, String resource) throws Exception {
        HTTP httpcall = new HTTP();
        httpcall.setMethod("POST");
        String URL = String.format(
            ACT_RESOURCE_REQUEST_PATTERN,
            config.getHost(), 
            config.getHttpPort(), 
            provider, 
            service, 
            resource
        );
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
                    System.out.println(String.format("Message received before json parser: %s", content));
                    JsonObject payloadJson = (JsonObject) new JsonParser().parse(content);
                    JsonObject messagesJson = payloadJson.getAsJsonObject().getAsJsonArray("messages").get(0).getAsJsonObject();
                    String value = messagesJson.get("notification").getAsJsonObject().get("value").getAsString();
                    String timestamp = messagesJson.get("notification").getAsJsonObject().get("timestamp").getAsString();
                    String resourceURI = messagesJson.get("uri").getAsString();
                    //TODO try subscription !
                    SNAResource snaResource = subscribedResources.get(resourceURI);
                    if (snaResource == null) {
                        snaResource = new SNAResource(resourceURI, value);
                        snaResource.setValue(value);
                        LOG.warn("notifying at {} with unsubscribed {}: undefined type", timestamp, snaResource);
                    } else {
                        snaResource.setValue(value);
                        LOG.info("notifying at {} with subscribed {}", timestamp, snaResource);
                    }
                    if (listener != null) {
                        final String providerId = snaResource.getProvider();
                        final String serviceId = snaResource.getService();
                        final String resourceId = snaResource.getResource();
                        final String type = snaResource.getType();
                        final Map<String, String> metadata = snaResource.getMetadata();
                        listener.notify(providerId, serviceId, resourceId, type, value, timestamp, metadata);
                        LOG.info("successfully notified {} with {} at {}", listener, snaResource, timestamp);
                    } else {
                        LOG.error("failed to notify with {} at {}: unexpected null listener", snaResource, timestamp);
                    }
                } catch (Throwable e) {
                    LOG.error("Failed to deliver message: {}", e.getMessage());
                }
            }
        });
        connectionWebSocket.setTriggerAfterConnection(new WebSocketTrigger() {
            @Override
            public void execute() throws Exception {
                connectionWebSocket.getSession().get().getRemote().sendString(LIFECYCLE_SUBSCRIBE_REQUEST);
            }
        });

        final String deviceCreationURL = String.format("%s://%s:%d/androidws", protocol, config.getHost(), config.getHttpPort());

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
    
    private static class UserSubscriptionManager extends HashMap<String, Map<String, SubscriptionResponse>> {
                
        private UserSubscriptionManager() {
            super();
        }
        
        private boolean hasAlreadySubscribedTo(final String user, final String resourcePath) {
            boolean isAlreadySubscribed = false;
            if (this.containsKey(user)) {
                final Map<String, SubscriptionResponse> userSubscriptions = get(user);
                if (userSubscriptions != null) {
                    isAlreadySubscribed = userSubscriptions.containsKey(resourcePath);
                } else {
                    isAlreadySubscribed = false;
                }
            } else {
                isAlreadySubscribed = false;
            }
            return isAlreadySubscribed;
        }
        
        private SubscriptionResponse getSubscriptionResponse(final String user, final String resourcePath) {
            SubscriptionResponse subscriptionResponse = null;
            if (this.containsKey(user)) {
                final Map<String, SubscriptionResponse> userSubscriptions = get(user);
                if (userSubscriptions != null) {
                    subscriptionResponse = userSubscriptions.get(resourcePath);
                } else {
                    subscriptionResponse = null;
                }
            } else {
                subscriptionResponse = null;
            }
            return subscriptionResponse;
        }
        
        private SubscriptionResponse putSubscriptionResponse(final String user, final SNAResource resource) throws AlreadySubscribedException {
            SubscriptionResponse response = null;
            Map<String, SubscriptionResponse> userSubscriptions = get(user);
            if (userSubscriptions == null) {
                userSubscriptions = new HashMap<String, SubscriptionResponse>();
                put(user, userSubscriptions);
            }
            final String key = String.format(
                    RESOURCE_PATH_PATTERN, 
                    resource.getProvider(), 
                    resource.getService(), 
                    resource.getResource()
            );
            if (userSubscriptions.containsKey(key)) {
                throw new AlreadySubscribedException(resource);
            }
            response = new SubscriptionResponse(resource);
            response = userSubscriptions.put(key, response);
            return response;
        }
        
        private SubscriptionResponse removeSubscriptionResponse(final String user, final SNAResource resource) {
            SubscriptionResponse isSubscribed = null;
            final String key = String.format(
                    RESOURCE_PATH_PATTERN, 
                    resource.getProvider(), 
                    resource.getService(), 
                    resource.getResource()
            );
            Map<String, SubscriptionResponse> userSubscriptions = get(user);
            if (userSubscriptions != null) {
                isSubscribed = userSubscriptions.remove(key);
            }
            return isSubscribed;
        }
    }
}
