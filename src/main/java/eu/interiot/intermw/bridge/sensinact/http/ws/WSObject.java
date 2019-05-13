package eu.interiot.intermw.bridge.sensinact.http.ws;

import eu.interiot.intermw.bridge.sensinact.fetcher.WebSocketModelRecoverListener;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@WebSocket
public class WSObject {

    private final Logger LOG = LoggerFactory.getLogger(WSObject.class);
    private final CountDownLatch closeLatch;
    private WebSocketModelRecoverListener listener;
    private SNAWebSocketConnectionListener connectionListener;
    private Session session;
    private boolean isConnected=false;

    public WSObject(WebSocketModelRecoverListener listener){
        this();
        this.listener=listener;
    }

    public WSObject()
    {
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException
    {
        return this.closeLatch.await(duration,unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        LOG.warn("Connection closed: {} - {}",statusCode,reason);
        this.session = null;
        this.isConnected=false;
        this.closeLatch.countDown(); // trigger latch
        if(getConnectionListener()!=null){
            try {
                getConnectionListener().disconnected();
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        LOG.info("Websocket connected, session {}",session);
        isConnected=true;
        this.session = session;
        try
        {
            /*
            Future<Void> fut;
            fut = session.getRemote().sendStringByFuture("Hello");
            fut.get(2, TimeUnit.SECONDS);

            fut = session.getRemote().sendStringByFuture("Thanks for the conversation.");
            fut.get(2,TimeUnit.SECONDS);

            //session.close(StatusCode.NORMAL,"I'm done");
            */
            if(getConnectionListener()!=null){
                try {
                    getConnectionListener().connected();
                }catch(Exception e){
                    LOG.error("Fail to notify websocket connection listener",e);
                }
            }

        }
        catch (Throwable t)
        {
            LOG.error("Fail to connect websocket",t);
        }
    }

    @OnWebSocketMessage
    public void onMessage(String msg)
    {
        LOG.info("Message Received via WebSocket, content: {}",msg);

        try {
           if(listener!=null){
               listener.notify(msg);
           }else {
               LOG.warn("No listener configured");
           }

        }catch(Exception e){
            LOG.error("Failed to deliver the message",e);
        }

    }

    public SNAWebSocketConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(SNAWebSocketConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
