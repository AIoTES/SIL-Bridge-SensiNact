package eu.interiot.intermw.bridge.sensinact.ontology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import org.apache.jena.ontology.OntModel;
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
        testTransformOntology("provider", "service", "resource", "value");
    }

    private void testTransformOntology(final String provider, final String service, final String resource, final String value) {
        SNAOntologyAggregator aggregator = new SNAOntologyAggregator();
        OntModel model = aggregator.transformOntology(provider, service, resource, value);
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