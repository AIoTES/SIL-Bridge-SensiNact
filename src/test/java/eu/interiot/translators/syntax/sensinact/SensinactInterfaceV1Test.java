package eu.interiot.translators.syntax.sensinact;

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.http.model.exception.InvalidConfigurationValueException;
import eu.interiot.intermw.bridge.sensinact.v1.SensinactCommunicationBridgeV1;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
            public void notify(String provider, String service, String resource, String value) {
                counter.getAndAdd(1);
            }
        });
        Thread.sleep(3000);
        Assert.assertTrue(counter.get()>0);
    }

    @Test(expected = Exception.class)
    public void createDevice() throws Exception {
        sensinact.createDevice("","","","");
    }

    @Test(expected = Exception.class,timeout = 20000)
    public void removeDevice() throws Exception {
        sensinact.removeDevice("","","");
    }

    @Test(timeout = 20000)
    public void listDevice(){

        sensinact.listDevices().forEach((dev)->{
            System.out.println(String.format("%s/%s/%s found",dev.getProvider(),dev.getService(),dev.getResource()));
        });

        Assert.assertTrue(sensinact.listDevices().size()>0);
    }

}
