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

/**
 * This implementation of Sensinact API refers to the Sensinact Eclipse project version, which is accessible via Eclipse.org
 */
public class SensinactCommunicationBridgeV2 implements SensinactAPI {

    private final Logger LOG = LoggerFactory.getLogger(SensinactCommunicationBridgeV2.class);
    private SensinactConfig config;
    private SensinactWebSocketConnectionManager connectionWebSocket;
    private SensinactModelRecoverListener listener;
    private SensinactWebSocketConnectionManager deviceCreationEndPoint;

    @Override
    public SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        LOG.debug("Subscription V2");
        return null;
    }

    @Override
    public UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception {
        LOG.debug("Unsubscribe V2");
        return null;
    }

    @Override
    public void createDevice(String provider, String service, String resource,String value) throws Exception {
        Session session=deviceCreationEndPoint.getSession().get();
        try {
            final String payload= ProviderJSONPayload.builder().provider(provider).service(service).resource(resource).value(value).build().toString();
            LOG.debug("Sending device creation/update message {} to sensinact",payload);
            session.getRemote().sendString(payload);
        }catch (Exception e){
            LOG.error("Failed to create device {}/{}/{}/{}",provider,service,resource,value,e);
        }

    }

    @Override
    public void removeDevice(String provider, String service, String resource) throws Exception {
        Session session=deviceCreationEndPoint.getSession().get();
        try {
            final String payload=ProviderJSONPayload.builder().provider(provider).service(service).resource(resource).type(ProviderJSONPayload.TYPE.Goodbye).build().toString();
            LOG.debug("Sending device creation/update message {} to sensinact",payload);
            session.getRemote().sendString(payload);
        }catch (Exception e){
            LOG.error("Failed to remove device {}/{}/{}",provider,service,resource,e);
        }

    }

    @Override
    public List<SNAResource> listDevices() {

        List<SNAResource> snaResourceList=new ArrayList<>();

        String URL = "http://%s:%s/jsonpath:sensinact?jsonpath=$.*";
        try {
            LOG.debug("Starting executing.." + Thread.currentThread().getName());

            HTTP httpcall = new HTTP();
            httpcall.setMethod("GET");
            try {

                String result = httpcall.submit(String.format(URL, config.getHost(), config.getHttpPort()));
                JsonObject jsonPathPayload = (JsonObject) new JsonParser().parse(result);

                JsonArray jsProvider=jsonPathPayload .getAsJsonArray("providers");

                LOG.debug("Total providers:" + jsProvider.size());
                for (Iterator itProvider = jsProvider.iterator(); itProvider.hasNext(); ) {
                    JsonObject ob = (JsonObject) itProvider.next();
                    String providerName = ob.get("name").getAsString();
                    LOG.debug("Provider:" + providerName);
                    JsonArray jsServices = ob.getAsJsonArray("services");

                    for (Iterator itService = jsServices.iterator(); itService.hasNext(); ) {
                        JsonObject obService = (JsonObject) itService.next();
                        String serviceName = obService.get("name").getAsString();
                        LOG.debug("Service:" + serviceName);
                        JsonArray jsResources = obService.getAsJsonArray("resources");

                        for (Iterator itResource = jsResources.iterator(); itResource.hasNext(); ) {
                            JsonObject obResource = (JsonObject) itResource.next();
                            String resourceName = obResource.get("name").getAsString();

                            snaResourceList.add(new SNAResource(providerName,serviceName,resourceName));

                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Failed parser jsonpath response",e);
            }
        } catch (Exception e) {
            LOG.debug("Failed to execute HTTP call",e);
        }
        return snaResourceList;
    }

    @Override
    public void act(String provider, String service, String resource) throws Exception {
        HTTP httpcall=new HTTP();
        httpcall.setMethod("POST");
        String URL=String.format("http://%s:%s/sensinact/providers/%s/services/%s/resources/%s/ACT",config.getHost(),config.getHttpPort(),provider,service,resource);
        httpcall.submit(URL);
    }

    @Override
    public void setConfig(SensinactConfig config) {
        this.config=config;
    }

    @Override
    public void setListener(SensinactModelRecoverListener listener) {
        this.listener=listener;
    }

    private void connectWebsocket(){
        String protocol="";
        if(config.getProtocol()==null){
            protocol="ws";
        }else {
            if(config.getProtocol().equals("https")){
                protocol="wss";
            }else if(config.getProtocol().equals("http")){
                protocol="ws";
            }
        }

        final String sensinactWebSocketURL = String.format("%s://%s:%d/ws",protocol,  config.getHost(), config.getHttpPort()).toString();
        connectionWebSocket=new SensinactWebSocketConnectionManager(sensinactWebSocketURL, new WebSocketModelRecoverListener() {
            @Override
            public void notify(String content) {
                try {
                    LOG.info("Message received before json parser:{}",content);
                    JsonObject payloadJson= (JsonObject) new JsonParser().parse(content);

                    JsonObject messagesJson=payloadJson.getAsJsonObject().getAsJsonArray("messages").get(0).getAsJsonObject();
                    String value=messagesJson.get("notification").getAsJsonObject().get("value").getAsString();
                    String[] updateMessage=messagesJson.get("uri").getAsString().split("/");
                    if(listener!=null){
                        listener.notify(updateMessage[1],updateMessage[2],updateMessage[3],value);
                    }
                }catch(Throwable e){
                    LOG.error("Failed to deliver message",e);
                }
            }
        });
        connectionWebSocket.setTriggerAfterConnection(new WebSocketTrigger() {
            @Override
            public void execute() throws Exception {
                connectionWebSocket.getSession().get().getRemote().sendString("{\"uri\":\"sensinact/SUBSCRIBE\",\"rid\":\"webapp\",\"parameters\":[{\"name\":\"sender\",\"type\":\"string\",\"value\":\"/.*\"},{\"name\":\"pattern\",\"type\":\"boolean\",\"value\":true},{\"name\":\"complement\",\"type\":\"boolean\",\"value\":false},{\"name\":\"types\",\"type\":\"array\",\"value\":[\"UPDATE\",\"LIFECYCLE\"]}]}");
            }
        });

        final String deviceCreationURL = String.format("%s://%s:%d/androidws",protocol,  config.getHost(), config.getHttpPort()).toString();

        deviceCreationEndPoint=new SensinactWebSocketConnectionManager(deviceCreationURL);

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
