package eu.interiot.translators.syntax.sensinact;

import eu.interiot.intermw.bridge.sensinact.http.model.exception.InvalidConfigurationValueException;
import eu.interiot.intermw.bridge.sensinact.http.ws.SensinactWebSocketConnectionManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SensinactWebSocketAbstractionTest {

    SensinactWebSocketConnectionManager snaWsConnection;

    @Before
    public void config() throws InvalidConfigurationValueException {
        snaWsConnection=new SensinactWebSocketConnectionManager("ws://sensinact.ddns.net:8092");
    }

    @After
    public void disconfig(){
        snaWsConnection.disconnect();
    }

    @Test
    public void wsMessageReceived() throws InterruptedException {
        AtomicInteger counter=new AtomicInteger(0);
        snaWsConnection.setMessageListener((content)-> {
                counter.getAndAdd(1);
                System.out.println(String.format("Message Received %s",content));
        });
        snaWsConnection.connect();
        Thread.sleep(5000);
        Assert.assertTrue(counter.get()>0);
    }

    Boolean result=false;

    @Test
    public void wsTriggerConnectionListener() throws InterruptedException {

        snaWsConnection.setTriggerAfterConnection(()-> {
                result=true;

        });

        snaWsConnection.connect();

        Thread.currentThread().sleep(6000);

        Assert.assertTrue(result);
    }


}
