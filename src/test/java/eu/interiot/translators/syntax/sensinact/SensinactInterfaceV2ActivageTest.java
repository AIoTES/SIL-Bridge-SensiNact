package eu.interiot.translators.syntax.sensinact;

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SensinactInterfaceV2ActivageTest {

    private SensinactConfig config;
    private SensinactAPI sensinact;
    private AtomicInteger counter;

    @Before
    public void before() throws Exception {
        counter=new AtomicInteger();
        config=new SensinactConfig();
        config.setHost("http://193.48.18.250:8080");
        config.setHttpPort("8080");
        config.setProtocol("http");
        config.setMaxDeviceNumber("-1");
        config.setVersion("v2");
        sensinact=SensinactFactory.createInstance(config);
        sensinact.connect();
    }

    @After
    public void after(){
        sensinact.disconnect();
    }

    @Test
    public void websocketConnectionTest() throws InterruptedException {
        sensinact.setListener(new SensinactModelRecoverListener() {
            @Override
            public void notify(String provider, String service, String resource, String value) {
                counter.getAndAdd(1);
            }
        });
        Thread.sleep(3000);
        Assert.assertTrue(counter.get()>0);
    }

    @Test
    public void deviceCreation() throws Exception {
        sensinact.createDevice("temporaryProvider","temporaryService","temporaryResource","OK");
        Thread.sleep(1000);
        Boolean devicePresent=sensinact.listDevices().stream().filter(sNAResource->sNAResource.getProvider().equals("temporaryProvider")).toArray().length>0;
        Boolean servicePresent=sensinact.listDevices().stream().filter(sNAResource->sNAResource.getProvider().equals("temporaryProvider")&&sNAResource.getService().equals("temporaryService")).toArray().length>0;
        Boolean resourceNotPresent=sensinact.listDevices().stream().filter(sNAResource->sNAResource.getProvider().equals("temporaryResource")).toArray().length==0;
        Assert.assertTrue(devicePresent&&servicePresent&&resourceNotPresent);
    }

    @Test
    @Ignore
    public void resourceRemoval() throws Exception {
        deviceCreation();
        sensinact.removeDevice("temporaryProvider","temporaryService","temporaryResource");
        //sensinact.removeDevice("temporaryProvider","temporaryService","temporaryResource");
        Thread.sleep(1000);

        Stream<SNAResource> temporaryProviderStream=sensinact.listDevices().stream().filter(sNAResource->sNAResource.getProvider().equals("temporaryProvider"));

        Boolean devicePresent=temporaryProviderStream.toArray().length>0;
        Stream<SNAResource> temporaryServiceStream=temporaryProviderStream.filter(sNAResource->sNAResource.getService().equals("temporaryService"));
        Boolean servicePresent=temporaryServiceStream.toArray().length==0;
        Boolean resourceNotPresent=temporaryProviderStream.filter(sNAResource->sNAResource.getService().equals("temporaryService")).toArray().length==0;
        Assert.assertTrue(devicePresent&&servicePresent&&resourceNotPresent);
    }

    @Test
    public void providerRemoval() throws Exception {
        deviceCreation();
        sensinact.removeDevice("temporaryProvider",null,null);
        Thread.sleep(1000);
        Boolean deviceNotPresent=sensinact.listDevices().stream().filter(sNAResource->sNAResource.getProvider().equals("temporaryProvider")).toArray().length==0;
        Assert.assertTrue(deviceNotPresent);
    }

    @Test
    public void listDevice(){

        sensinact.listDevices().forEach((dev)->{
            System.out.println(String.format("%s/%s/%s found",dev.getProvider(),dev.getService(),dev.getResource()));
        });

        Assert.assertTrue(sensinact.listDevices().size()>0);
    }


}
