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
