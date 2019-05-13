package eu.interiot.intermw.bridge.sensinact.http.ws;

import eu.interiot.intermw.bridge.sensinact.fetcher.WebSocketModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.v1.SensinactCommunicationBridgeV1;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Future;

public class SensinactWebSocketConnectionManager {

    private final Logger LOG = LoggerFactory.getLogger(SensinactCommunicationBridgeV1.class);
    private WebSocketModelRecoverListener listener;
    private String sensinactWebSocketURL;// format: "ws://sensinact.ddns.net:8092";
    private WSObject socket;
    private WebSocketClient client;
    Future<Session> session;
    private WebSocketTrigger triggerAfterConnection;
    private boolean voluntarilyDisconnect=false;

    public SensinactWebSocketConnectionManager(String webSocketConnectionURL) {
        this.sensinactWebSocketURL = webSocketConnectionURL;
    }

    public SensinactWebSocketConnectionManager(String webSocketConnectionURL, WebSocketModelRecoverListener listeners) {
        this.sensinactWebSocketURL = webSocketConnectionURL;
        this.listener = listeners;
    }

    public void disconnect(){
        voluntarilyDisconnect=true;
        try {
            client.stop();
        } catch (Exception e) {
            LOG.warn("Failed to stop WebSocket client",e);
        }

        try {
            session.get().disconnect();
        } catch (Exception e) {
            LOG.warn("Failed to stop WebSocket client",e);
        }
    }

    public void connect() {
        voluntarilyDisconnect=false;
        client = new WebSocketClient();

        socket = new WSObject(listener);

        socket.setConnectionListener(new SNAWebSocketConnectionListener() {
            @Override
            public void disconnected() {
                //Case the connection is lost, try to re-establish
                if(!voluntarilyDisconnect){
                    SensinactWebSocketConnectionManager.this.connect();
                }
            }

            @Override
            public void connected() {
                //Nothing to do here
            }
        });

        Runnable t1 = () -> {
                try {
                    client.start();
                    URI echoUri = new URI(sensinactWebSocketURL);
                    ClientUpgradeRequest request = new ClientUpgradeRequest();
                    while (!socket.isConnected()) {
                        session=client.connect(socket, echoUri, request);
                        LOG.debug("Connecting to : {}", echoUri);
                        Thread.sleep(5000);
                    }

                    LOG.debug("Connected to : {}", echoUri);

                    if(triggerAfterConnection!=null){
                        try {
                            triggerAfterConnection.execute();
                        }catch(Exception e){
                            LOG.error("Failed to call websocket trigger",e);
                        }

                    }

                } catch (Throwable t) {
                    LOG.error("Failed to connect to websocket",t);
                }
        };

        Thread runThread=new Thread(t1);

        runThread.start();

        try {
            runThread.join();
        } catch (InterruptedException e) {
            LOG.error("Failed to join ws connection thread",e);
        }

    }

    public Future<Session> getSession(){
        return session;
    }

    public void setTriggerAfterConnection(WebSocketTrigger triggerAfterConnection) {
        this.triggerAfterConnection = triggerAfterConnection;
    }

    public void setMessageListener(WebSocketModelRecoverListener listener) {
        this.listener = listener;
    }
}
