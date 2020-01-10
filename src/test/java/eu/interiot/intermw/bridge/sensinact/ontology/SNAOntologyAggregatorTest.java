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
package eu.interiot.intermw.bridge.sensinact.ontology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import java.io.IOException;
import org.apache.jena.rdf.model.Model;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Stéphane Bergeon <stephane.bergeon@cea.fr
 */
public class SNAOntologyAggregatorTest {

    public SNAOntologyAggregatorTest() {
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
    public void testTransformOntology() {
        testTransformOntology("provider", "service", "resource", "type", "value", "timestamp");
    }

    private void testTransformOntology(final String provider, final String service, final String resource, final String type, final String value, final String timestamp) {
        SNAOntologyAggregator aggregator = new SNAOntologyAggregator();
        Model model = aggregator.transformOntology(provider, service, resource, type, value ,timestamp);
        assertNotNull("unexpected null model", model);
        try {
            String observationMessage = createObservationMessage(model);
            assertNotNull("unexpected null observationMessage", observationMessage);
            System.out.println("translated message = " + observationMessage);
            assertNotSame("unexpected empty message", "", observationMessage);
        } catch (IOException ex) {
            fail("unexpected error: "+ ex.getMessage());
        }
    }

    private String createObservationMessage(Model model) throws IOException{
        Message callbackMessage = new Message();
        // Metadata
        PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
        metadata.initializeMetadata();
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
//        metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
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