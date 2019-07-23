package eu.interiot.translators.syntax.sensinact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.ontology.SNAOntologyAggregator;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SensinactOntologyTest {

    SNAOntologyAggregator aggregator;

    @Before
    public void config() {
        aggregator = new SNAOntologyAggregator();
    }

    @Test
    public void save() throws Exception {
        System.out.println("\nRecording ontology...");
        AtomicInteger counter = new AtomicInteger();
        SensinactConfig sc = new SensinactConfig();
        sc.setVersion("v2");
        sc.setProtocol("http");
        sc.setHost("localhost");
        sc.setHttpPort("8082");
        sc.setMaxDeviceNumber("-1");

        SensinactAPI sensinact = SensinactFactory.createInstance(sc);

        sensinact.setListener((provider, service, resource, value) -> {
            aggregator.updateOntologyWith(provider, service, resource, value);
            System.out.println(
                    String.format(
                            " ... received notification from %s/%s/%s: %s",
                            provider,
                            service,
                            resource,
                            value
                    )
            );
            counter.getAndAdd(1);
        });
        System.out.print("\n connecting sensiNact... ");
        sensinact.connect();
        System.out.println("connected");

        System.out.println("\n listing devices... ");
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
        Thread.sleep(300000);
        System.out.println(
                String.format(
                        "... received %s notifications for ontology creation",
                        counter.get()
                )
        );
        aggregator.printOntology(SNAOntologyAggregator.JenaWriterType.n3);

        aggregator.saveOntology("/nobackup/SNAOntologyWithData.owl", SNAOntologyAggregator.JenaWriterType.rdf);
    }


    @Test
    public void aggregatorResourceCountTest() throws IOException {

        aggregator.updateOntologyWith("p1", "s1fromp1", "r1froms1fromp1", "value1");
        aggregator.updateOntologyWith("p1", "s2fromp1", "r1froms2fromp1", "value2");
        aggregator.updateOntologyWith("p1", "s2fromp1", "r2froms2fromp1", "value3");
        aggregator.updateOntologyWith("p2", "s2", "r2", "value4");
        Assert.assertTrue(aggregator.getResourceList().size() == 4);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource) -> resource.getProvider().equals("p1") && resource.getService().equals("s2fromp1") && resource.getResource().contains("froms2fromp1")).toArray().length == 2);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource) -> resource.getProvider().equals("p1")).toArray().length == 3);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource) -> resource.getProvider().equals("p2")).toArray().length == 1);

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
