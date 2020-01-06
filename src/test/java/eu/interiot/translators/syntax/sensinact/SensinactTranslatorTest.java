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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.ontology.SNAOntologyAggregator;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import static eu.interiot.translators.syntax.sensinact.SensinactTranslator.snA;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
            String createObservationMessage = createObservationMessage(model);
            System.out.println(createObservationMessage);
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
            //subscribe to all resources
            sensinact.setListener((String provider, String service, String resource, String type, String value, String timestamp) -> {
                if ()
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
                try {
                    System.out.print("\nobservation message= ");
                    final Model model = aggregator.createModel(provider, service, resource, type, value ,timestamp);
                    String observationMessage = createObservationMessage(model);
                    System.out.println(observationMessage);
                } catch (Exception ex) {
                    System.out.println(" -failed: " + ex.getMessage());
                }
                counter.getAndAdd(1);
            });
            System.out.print("\n connecting sensiNact... ");
            sensinact.connect();
            System.out.println("connected");

            final int size = sensinact.listDevices().size();
            System.out.println(
                    String.format(
                            "found %s devices",
                            size
                    )
            );
            Assert.assertTrue("unexpected empty list of devices", size > 0);
            Thread.sleep(300000);
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

    private Model createModel(final String observation) {
        // convert observation from N3 format into a MessagePayload object
        Model m = ModelFactory.createDefaultModel();
        InputStream inStream = new ByteArrayInputStream(observation.getBytes());
        RDFDataMgr.read(m, inStream, Lang.N3);
        return m;
    }

    private String createObservationMessage(Model model) throws IOException{
        Message callbackMessage = new Message();
        // Metadata
        PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
        metadata.initializeMetadata();
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
        metadata.setSenderPlatformId(new EntityID(snA));
//        metadata.setConversationId(conversationId);
        callbackMessage.setMetadata(metadata);
       
        //Finish creating the message
        MessagePayload messagePayload = new MessagePayload(model);
        callbackMessage.setPayload(messagePayload); 
       
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonMessage = (ObjectNode) mapper.readTree(callbackMessage.serializeToJSONLD());
//        ObjectNode context = (ObjectNode) jsonMessage.get("@context");
        // TODO: to use @vocab instead of msg, replace also all "msg:XXX" tags by "InterIoT:message/XXX"
//        context.remove("msg");
//        context.put("@vocab", "http://inter-iot.eu/message/");
//        jsonMessage.set("@context", context);
        return jsonMessage.toString();
       
    }
}

