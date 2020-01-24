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

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SensinactInterfaceV1Test {

    private static SensinactConfig config;
    private SensinactAPI sensinact;
    private AtomicInteger counter;

    @Before
    public void before() throws Exception {
        counter=new AtomicInteger();
        config=new SensinactConfig();
        config.setHost("sensinact.ddns.net");
        config.setHttpPort("80");
        config.setVersion("v1");
        config.setWebSocketPort("8092");
        config.setProtocol("http");
        config.setMaxDeviceNumber("30");
        sensinact=SensinactFactory.createInstance(config);
        sensinact.connect();
    }

    @After
    public void after(){
        sensinact.disconnect();
    }

    @Test(timeout = 20000)
    public void websocketConnectionTest() throws Exception {
        sensinact.setListener(new SensinactModelRecoverListener() {
            @Override
            public void notify(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {
                counter.getAndAdd(1);
            }
        });
        Thread.sleep(3000);
        Assert.assertTrue(counter.get()>0);
    }

    @Test(expected = Exception.class)
    public void createDevice() throws Exception {
        sensinact.updateResource("","","","","");
    }

    @Test(expected = Exception.class,timeout = 20000)
    public void removeDevice() throws Exception {
        sensinact.removeResource("","","");
    }

    @Test(timeout = 20000)
    public void listDevice(){

        sensinact.listResources().forEach((dev)->{
            System.out.println(String.format("%s/%s/%s found",dev.getProvider(),dev.getService(),dev.getResource()));
        });

        Assert.assertTrue(sensinact.listResources().size()>0);
    }

}
