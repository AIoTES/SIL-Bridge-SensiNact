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

import eu.interiot.intermw.bridge.sensinact.SensiNactBridge;
import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.ontology.SNAOntologyAggregator;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.intermw.bridge.sensinact.wrapper.SubscriptionResponse;
import eu.interiot.message.Message;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Stéphane Bergeon <stephane.bergeon@cea.fr
 */
public class SensinactTranslatorTest {
   
    private static final long TEST_DURATION = 900000;
    
    public SensinactTranslatorTest() {
    }
   
    @BeforeClass
    public static void setUpClass() {
    }
   
    @AfterClass
    public static void tearDownClass() {
    }
   
    @Before
    public void setUp() {
    }
   
    @After
    public void tearDown() {
    }

    @Test
    public void testToJena_1() {
        testToJena("message");
    }

    private void testToJena(final String message) {
        try {
            SensinactTranslator translator = new SensinactTranslator();
            Model model = translator.toJenaModel(message);
            assertNotNull("unexpected null translated model", model);
            String translateddMessage = translator.printJenaModel(model, Lang.N3);
            assertNotNull("unexpected null translated message", translateddMessage);
            Message observationMessage = SensiNactBridge.createObservationMessage(model);
            System.out.println(SensiNactBridge.toString(observationMessage));
        } catch (Exception ex) {
            fail("unexpected error " + ex.getMessage());
        }
    }
   
    @Test
    public void testTranslate_1() {
        testTranslate("message");
    }

    @Test
    public void translate() {
        System.out.println("\nTranslating...");
        AtomicInteger counter = new AtomicInteger();
        SensinactConfig sc = new SensinactConfig();
        try {
            sc.setVersion("v2");
            sc.setProtocol("http");
            sc.setHost("localhost");
            sc.setHttpPort("8082");
            sc.setMaxDeviceNumber("-1");
            SNAOntologyAggregator aggregator = new SNAOntologyAggregator();

            SensinactAPI sensinact = SensinactFactory.createInstance(sc);
            SensinactModelRecoverListener listener = 
                new TestSensinactModelRecoverListener(sensinact, aggregator, counter);
            sensinact.setListener(listener);
            System.out.print("\n connecting sensiNact... ");
            sensinact.connect();
            System.out.println("connected");

            final int size = sensinact.listResources().size();
            System.out.println(
                String.format(
                    "found %s devices",
                    size
                )
            );
            Assert.assertTrue("unexpected empty list of devices", size > 0);
            Thread.sleep(TEST_DURATION);
            System.out.println(
                String.format(
                    "... received %s notifications for translation",
                    counter.get()
                )
            );
        } catch (Exception ex) {
            fail("unexpected error " + ex.getMessage());
        }
    }

    private void testTranslate(final String message) {
        System.out.println("\nTranslating " + message);
        try {
            SensinactTranslator translator = new SensinactTranslator();
            Message translatedMessage = translator.translate(message, "conversation");
            assertNotNull("unexpected null translated message", translatedMessage);
            String jsonMessage = translatedMessage.serializeToJSONLD();
            System.out.println(jsonMessage);
        } catch (Exception ex) {
            fail("unexpected error " + ex.getMessage());
        }
    }

    private class TestSensinactModelRecoverListener implements SensinactModelRecoverListener {

        private final AtomicInteger counter;
        private final SNAOntologyAggregator aggregator;
        private final SensinactAPI sensinact;
        private final Set<String> subscribedResources;

        public TestSensinactModelRecoverListener(final SensinactAPI sensinact, final SNAOntologyAggregator aggregator, final AtomicInteger counter) {
            this.counter = counter;
            this.aggregator = aggregator;
            this.sensinact = sensinact;
            this.subscribedResources = new HashSet<String>();
        }

        @Override
        public void notify(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {
            final String resourcePath = String.format("%s/%s/%s", provider, service, resource); 
            System.out.println(
                String.format(
                    " ... received notification from %s: type=%s, value=%s, timestamp=%s, metadata=%s",
                    resourcePath,
                    type,
                    value,
                    timestamp,
                    metadata
                )
            );
            try {
                SubscriptionResponse subscriptionResponse = sensinact.subscribe(resourcePath);
                type = subscriptionResponse.getType();
                System.out.print(String.format("subscribed to %s", resourcePath));
            } catch (Exception ex) {
                System.out.println(String.format("failed to subscribe to %s: %s", resourcePath, ex.getMessage()));
            }
            try {
                System.out.println("\nobservation message= ");
                final Model model = aggregator.createModel(provider, service, resource, type, value, timestamp, metadata);
                Message observationMessage = SensiNactBridge.createObservationMessage(model);
                System.out.println(SensiNactBridge.toString(observationMessage));
                List<SNAResource> resourceList = aggregator.getResourceList();
                System.out.println("found resources: " + resourceList);
            } catch (Exception ex) {
                System.out.println(" -failed: " + ex.getMessage());
            }
            counter.getAndAdd(1);
        }
    }
}
    