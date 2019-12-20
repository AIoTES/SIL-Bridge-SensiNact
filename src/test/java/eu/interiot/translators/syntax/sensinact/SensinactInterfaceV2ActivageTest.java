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
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SensinactInterfaceV2ActivageTest {

    private SensinactConfig config;
    private SensinactAPI sensinact;
    private AtomicInteger counter;

    @Before
    public void before() throws Exception {
        counter = new AtomicInteger();
        config = new SensinactConfig();
        config.setHost("localhost");
        config.setHttpPort("8082");
        config.setProtocol("http");
        config.setMaxDeviceNumber("-1");
        config.setVersion("v2");
        System.out.print("\n creating sensiNact... ");
        sensinact = SensinactFactory.createInstance(config);
        System.out.println("created");
        System.out.print("\n connecting sensiNact... ");
        sensinact.connect();
        System.out.println("connected");
    }

    @After
    public void after() {
        sensinact.disconnect();
    }

    @Test
    public void websocketConnectionTest() {
        System.out.println("\nTesting websocket connection...");
        try {
            sensinact.setListener(new SensinactModelRecoverListener() {
                @Override
                public void notify(String provider, String service, String resource, String type, String value, String timestamp) {
                    System.out.println(
                            String.format(
                                    " ... received notification from %s/%s/%s: type=%s, value=%s, timestamp=%s",
                                    provider,
                                    service,
                                    resource,
                                    type,
                                    value,
                                    timestamp
                            )
                    );
                    counter.getAndAdd(1);
                }
            });
            Thread.sleep(15000);
            System.out.println(
                    String.format(
                            "... received %s notifications",
                            counter.get()
                    )
            );
            Assert.assertTrue("failed to receive any notification", counter.get() > 0);
        } catch (Exception ex) {
            Assert.fail("unexpected exception " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void deviceCreation() {
        System.out.println("\nTesting create device...");
        doCreateProvider("temporaryProvider");
        Boolean devicePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getProvider().equals("temporaryProvider")
        ).toArray().length > 0;
        System.out.println(devicePresent?"device present":"device absent");
        Assert.assertTrue("failed to create device", devicePresent );
    }

    @Test
    public void serviceCreation() {
        System.out.println("\nTesting create service...");
        doCreateService("temporaryProvider", "temporaryService");
        Boolean devicePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getProvider().equals("temporaryProvider")
        ).toArray().length > 0;
        System.out.println(devicePresent?"device present":"device absent");
        Boolean servicePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getService().equals("temporaryService")
        ).toArray().length > 0;
        System.out.println(servicePresent?"service present":"service absent");
        Assert.assertTrue("failed to create service", devicePresent && servicePresent);
    }

    @Test
    public void resourceCreation() {
        System.out.println("\nTesting create resource...");
        doCreateResource("temporaryProvider", "temporaryService", "temporaryResource", "temporaryType", "OK");
        Boolean devicePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getProvider().equals("temporaryProvider")
        ).toArray().length > 0;
        System.out.println(devicePresent?"device present":"device absent");
        Boolean servicePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getService().equals("temporaryService")
        ).toArray().length > 0;
        System.out.println(servicePresent?"service present":"service absent");
        Boolean resourcePresent = sensinact.listDevices().stream().filter(
            sNAResource -> sNAResource.getResource().equals("temporaryResource")
        ).toArray().length > 0;
        System.out.println(resourcePresent?"resource present":"resource absent");
        Assert.assertTrue("failed to create resource", devicePresent && servicePresent && resourcePresent);
    }
    
    private void doCreateProvider(final String provider) {
        doCreateResource("temporaryProvider", null, null, null, null);
    }
    
    private void doCreateService(final String provider, final String service) {
        doCreateResource("temporaryProvider","temporaryService", null, null, null);
    }

    private void doCreateResource(final String provider, final String service, final String resource, final String type, final String value) {
        try {
            sensinact.createDevice(provider, service, resource, type, value);
            System.out.println(
                    String.format(
                            "... created %s/%s/%s resource",
                            provider, service, resource
                    )
            );
            Thread.sleep(1000);
            List<SNAResource> listDevices = sensinact.listDevices();
            for (SNAResource snaResource : listDevices) {
                if (snaResource.getProvider().equals("temporaryProvider")) {
                    System.out.println(
                            String.format(
                                    " ...in device list, found %s",
                                    snaResource
                            )
                    );
                }
            }
        } catch (Exception ex) {
            Assert.fail("unexpected exception " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void resourceRemoval() {
        System.out.println("\nTesting remove resource...");
        try {
            doCreateResource("temporaryProvider", "temporaryService", "temporaryResource", "temporaryType", "OK");
            sensinact.removeDevice("temporaryProvider", "temporaryService", "temporaryResource");
            //sensinact.removeDevice("temporaryProvider","temporaryService","temporaryResource");
            System.out.println(
                    String.format(
                            "... removed temporaryProvider/temporaryService/temporaryResource resource"
                    )
            );
            Thread.sleep(1000);
           
            Boolean devicePresent = sensinact.listDevices().stream().filter(
                sNAResource -> sNAResource.getProvider().equals("temporaryProvider")
            ).toArray().length > 0;
            System.out.println(devicePresent?"device present":"device absent");
            Boolean servicePresent = sensinact.listDevices().stream().filter(
                sNAResource -> sNAResource.getService().equals("temporaryService")
            ).toArray().length > 0;
            System.out.println(servicePresent?"service present":"service absent");
            Boolean resourcePresent = sensinact.listDevices().stream().filter(
                sNAResource -> sNAResource.getResource().equals("temporaryResource")
            ).toArray().length > 0;
            System.out.println(resourcePresent?"resource present":"resource absent");
            Assert.assertTrue("failed to remove resource", !devicePresent && !servicePresent && !resourcePresent);
        } catch (Exception ex) {
            Assert.fail("unexpected exception " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void providerRemoval() {
        System.out.println("\nTesting remove device...");
        try {
            doCreateProvider("temporaryProvider");
            sensinact.removeDevice("temporaryProvider", null, null);
            System.out.println(
                    String.format(
                            "... removed temporaryProvider device"
                    )
            );
            Thread.sleep(1000);
            Boolean devicePresent = sensinact.listDevices().stream().filter(sNAResource -> sNAResource.getProvider().equals("temporaryProvider")).toArray().length > 0;
            System.out.println(devicePresent?"device present":"device absent");
            Assert.assertTrue("failed to remove provider", !devicePresent);
        } catch (Exception ex) {
            Assert.fail("unexpected exception " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void listDevice() {
        System.out.println("\nTesting list devices...");
        List<SNAResource> listDevices = sensinact.listDevices();
        for (SNAResource snaResource : listDevices) {
            System.out.println(
                String.format(
                        " ...found %s",
                        snaResource
                )
            );
        }
        final int size = sensinact.listDevices().size();
        System.out.println(
                String.format(
                        "found %s devices",
                        size
                )
        );
        Assert.assertTrue("unexpected empty list of devices", size > 0);
    }
}