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
package eu.interiot.intermw.bridge.sensinact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.interiot.intermw.bridge.BridgeConfiguration;
import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.ontology.SNAOntologyAggregator;
import eu.interiot.intermw.bridge.sensinact.wrapper.ConversationMapper;
import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.requests.UnsubscribeReq;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import eu.interiot.message.payload.types.IoTDevicePayload;
import static eu.interiot.translators.syntax.sensinact.SensinactTranslator.snA;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "http://sensinact.ddns.net/sensinact")
public class SensiNactBridge extends AbstractBridge {

    private static final Logger LOG = LoggerFactory.getLogger(SensiNactBridge.class);
    private final SNAOntologyAggregator ontologyAggregator = new SNAOntologyAggregator();
    private final SensinactAPI sensinact;
    private final ConversationMapper conversation = new ConversationMapper();

    public SensiNactBridge(BridgeConfiguration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        LOG.debug("SensiNactBridge is initializing...");
        Properties properties = configuration.getProperties();
        try {
            String sensinactHost = properties.getProperty("sensinact.host", "127.0.0.1");
            String sensinactWebSocketPort = properties.getProperty("sensinact.websocket.port", "8092");
            String sensinactHttpPort = properties.getProperty("sensinact.http.port", "80");
            String sensinactVersion = properties.getProperty("sensinact.version", "v1");
            String sensinactProtocol = properties.getProperty("sensinact.protocol", "http");
            String sensinactMaxDeviceNumber = properties.getProperty("sensinact.provider.maxNumber", "100");

            SensinactConfig config = new SensinactConfig();
            config.setHost(sensinactHost);
            config.setProtocol(sensinactProtocol);
            config.setVersion(sensinactVersion);
            config.setHttpPort(sensinactHttpPort);
            config.setWebSocketPort(sensinactWebSocketPort);
            config.setMaxDeviceNumber(sensinactMaxDeviceNumber);
            sensinact = SensinactFactory.createInstance(config);
            LOG.info("SensiNactBridge has been initialized successfully.");

        } catch (Exception e) {
            throw new BridgeException("Failed to read SensiNact bridge configuration: " + e.getMessage());
        }

    }

    @Override
    public Message registerPlatform(Message message) throws Exception {

        LOG.info("Registering platform..");

        //There is not a particular task to be performed in register for sensinact
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        sensinact.setListener(new SensinactModelRecoverListener() {
            @Override
            public void notify(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {

                try {
                    LOG.info("notified of an update of {}/{}/{} type={}, value={}, timestamp={}, metadata={}",
                            provider, service, resource,
                            type, value, timestamp, metadata
                    );

                    //No reason for keeping track of the whole ontology
                    //ontologyAggregator.updateOntologyWith(provider,service,resource,value);
                    final String deviceId = String.format("%s/%s/%s", provider, service, resource);

                    for (String conversationId : conversation.subscriptionsGet(deviceId)) {

                        LOG.info("Sending notification to conversation id {} on device {}", conversationId, deviceId);

                        PlatformMessageMetadata platformMetadata = new MessageMetadata().asPlatformMessageMetadata();
                        platformMetadata.initializeMetadata();
                        platformMetadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
                        platformMetadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);

                        platformMetadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
                        platformMetadata.setConversationId(conversationId);
                        Model model = 
                                ontologyAggregator.transformOntology(
                                    provider, service, resource,
                                    type, value, timestamp, metadata
                                );
//                        final Model model2 = 
//                                ontologyAggregator.createModel(
//                                        provider, service, resource, 
//                                        type, value, timestamp, metadata
//                                );
//                        Message observationMessage2 = SensiNactBridge.createObservationMessage(model2);
//                        log.info("observation message2:\n {}", SensiNactBridge.toString(observationMessage2));
                        MessagePayload messagePayload = 
                                new MessagePayload(model);
                        Message observationMessage = new Message();
                        observationMessage.setMetadata(platformMetadata);
                        observationMessage.setPayload(messagePayload);

                        try {
                            LOG.info("Sending following observation message to intermw {}", observationMessage.serializeToJSONLD());
                            publisher.publish(observationMessage);
                        } catch (BrokerException e) {
                            LOG.error("Failed to publish message from the conversation id:" + conversationId);
                        }

                    }

                } catch (Exception e) {
                    LOG.error("Failed to register platform", e);
                }

            }
        });

        sensinact.connect();

        return responseMessage;
    }

    @Override
    public Message updatePlatform(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        return responseMessage;
    }

    @Override
    public Message unregisterPlatform(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        try {

            LOG.debug("Unregistering platform {}...", platform.getPlatformId());

            responseMessage.getMetadata().setStatus("OK");

            sensinact.disconnect();

        } catch (Exception e) {
            LOG.debug("Failed to register platform {}", platform.getPlatformId(), e);
            responseMessage.getMetadata().addMessageType(URIManagerMessageMetadata.MessageTypesEnum.ERROR);
            responseMessage.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
            responseMessage.getMetadata().setStatus("NOK");
        } finally {
            return responseMessage;
        }

    }

    private List<String> extractDeviceIds(Message message) {
        IoTDevicePayload ioTDevicePayload = message.getPayloadAsGOIoTPPayload().asIoTDevicePayload();
        Set<EntityID> deviceEntityIds = ioTDevicePayload.getIoTDevices();
        List<String> deviceIds = new ArrayList<>();
        for (EntityID deviceEntityId : deviceEntityIds) {
            deviceIds.add(deviceEntityId.toString());
        }
        return deviceIds;
    }

    private void createObservationsListener(String conversationId, URL convCallbackUrl) {
        LOG.debug("Creating callback listener for conversation {} listening at {}...", conversationId, convCallbackUrl);
        Spark.post(conversationId, (request, response) -> {
            LOG.debug("New observation received from the platform.");
            try {
                // create message metadata
                PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
                metadata.initializeMetadata();
                metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
                metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
                metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
                metadata.setConversationId(conversationId);

                // convert observation from N3 format into a MessagePayload object
                String observationN3 = request.body();
                Model m = ModelFactory.createDefaultModel();
                InputStream inStream = new ByteArrayInputStream(observationN3.getBytes());
                RDFDataMgr.read(m, inStream, Lang.N3);
                MessagePayload messagePayload = new MessagePayload(m);

                Message observationMessage = new Message();
                observationMessage.setMetadata(metadata);
                observationMessage.setPayload(messagePayload);

                publisher.publish(observationMessage);
                LOG.debug("Observation message {} has been published upstream through Inter MW.", observationMessage.getMetadata().getMessageID().get());

                response.status(204);
                return "";

            } catch (Exception e) {
                LOG.debug("Failed to handle observation with conversationId " + conversationId + ": " + e.getMessage(), e);
                response.status(400);
                return "Failed to handle observation: " + e.getMessage();
            }
        });
    }

    public static String toString(final Message message) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonMessage = (ObjectNode) mapper.readTree(message.serializeToJSONLD());
        return jsonMessage.toString();
    }

    public static Message createObservationMessage(Model model) throws IOException{
        Message observationMessage = new Message();
        // Metadata
        PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
        metadata.initializeMetadata();
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
        metadata.setSenderPlatformId(new EntityID(snA));
        observationMessage.setMetadata(metadata);
       
        //Finish creating the message
        MessagePayload messagePayload = new MessagePayload(model);
        observationMessage.setPayload(messagePayload); 
        return observationMessage;
    }
       
    private List<String> convertIoTDeviceID(List<String> deviceIds) {

        List<String> neoDeviceIds = new ArrayList<>();

        for (String deviceId : deviceIds) {
            neoDeviceIds.add(deviceId.replaceFirst("http://sensinact.ddns.net/", ""));
        }

        return neoDeviceIds;

    }

    @Override
    public Message subscribe(Message message) throws Exception {
        /**
         * @TODO map device and conversation use conversation id and map with
         * the devices message.getMetadata().getConversationId()
         * message.getPayloadAsGOIoTPPayload().asIoTDevicePayload().getIoTDevices()
         */

        Message responseMessage = createResponseMessage(message);
        final List<String> deviceIds = extractDeviceIds(message);
        LOG.debug("Received subscription message:...\n{}", message.serializeToJSONLD());
        try {

            LOG.debug("Subscribing to new devices {} from {}...", deviceIds, platform.getPlatformId());

            responseMessage.getMetadata().setStatus("OK");

            final String conversationId = message.getMetadata().getConversationId().orElse(null);

            if (conversationId != null) {
                final List<String> convertedIoTDeviceIDs = convertIoTDeviceID(deviceIds);
                LOG.debug("subscribing devices {}...", convertedIoTDeviceIDs);
                conversation.subscriptionsPut(convertedIoTDeviceIDs, conversationId);
                for (String resourceUri : convertedIoTDeviceIDs) {
                    sensinact.subscribe(resourceUri);
                }
                LOG.debug("subscribed devices {} : conversation {}...", convertedIoTDeviceIDs, conversationId);
            } else {
                throw new NullPointerException("unexpected null conversation id");
            }

        } catch (Exception e) {
            LOG.error("Failed to subscribe devices {} for platform {}: {}", deviceIds, platform.getPlatformId(), e.getMessage());
            LOG.debug(e.getMessage(), e);
            responseMessage.getMetadata().addMessageType(URIManagerMessageMetadata.MessageTypesEnum.ERROR);
            responseMessage.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
            responseMessage.getMetadata().setStatus("NOK");
        }

        return responseMessage;

    }

    @Override
    public Message unsubscribe(Message message) throws Exception {
        UnsubscribeReq req = new UnsubscribeReq(message);
        String conversationId = req.getConversationId();

        Message responseMessage = createResponseMessage(message);
        try {
            conversation.removeConversation(conversationId);
            responseMessage.getMetadata().setStatus("OK");
        } catch (Exception e) {
            LOG.error("Failed to unsubscribe device list", e);
            responseMessage.getMetadata().setStatus("KO");
        }

        return responseMessage;

    }

    @Override
    public Message query(Message message) throws Exception {
        return listDevices(message);
    }

    @Override
    public Message listDevices(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        try {

            responseMessage.getMetadata().setStatus("OK");

            SNAOntologyAggregator soa = new SNAOntologyAggregator();

            sensinact.listResources().forEach((snaResource -> {
                soa.updateOntologyWith(snaResource);
            }));

            MessagePayload messagePayload = new MessagePayload(soa.getOntModel());
            responseMessage.setPayload(messagePayload);

        } catch (Exception e) {
            LOG.error("Failed to fetch device list", e);
            responseMessage.getMetadata().setStatus("KO");
        }

        return responseMessage;
    }

    private void snaCreateUpdateDevice(SNAResource snaResource) {

        try {
            LOG.info("Creating/Updating Sensinact device {}...", 
                    snaResource);
            sensinact.updateResource(
                    snaResource.getProvider(),
                    snaResource.getService(),
                    snaResource.getResource(),
                    snaResource.getType(),
                    snaResource.getValue(),
                    snaResource.getMetadata()
            );
            LOG.info("Sensinact device {} created/updated", 
                    snaResource);
        } catch (Exception e) {
            LOG.error("Failed to create/update Sensinact device {}", 
                    snaResource);
        }

    }

    @Override
    public Message platformCreateDevices(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach(new Consumer<SNAResource>() {
            @Override
            public void accept(SNAResource snaResource) {
                snaCreateUpdateDevice(snaResource);
            }
        });

        return responseMessage;
    }

    @Override
    public Message platformUpdateDevices(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach((snaResource) -> {
            snaCreateUpdateDevice(snaResource);
        });
        return responseMessage;
    }

    @Override
    public Message platformDeleteDevices(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach((snaResource) -> {
            try {
                LOG.info("Removing Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
                sensinact.removeResource(snaResource.getProvider(), snaResource.getService(), snaResource.getResource());
                LOG.info("Sensinact device {}/{}/{}/{} removed", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            } catch (Exception e) {
                LOG.error("Failed to remove Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            }
        });
        return responseMessage;
    }

    @Override
    public Message observe(Message message) throws Exception {
        String jsonMessage = message.serializeToJSONLD();
        LOG.debug("observed message: {}", jsonMessage);
        return platformUpdateDevices(message);
    }

    @Override
    public Message actuate(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach((snaResource) -> {
            try {
                LOG.info("ACT Sensinact device {}/{}/{}/{}..", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
                sensinact.act(snaResource.getProvider(), snaResource.getService(), snaResource.getResource());
                LOG.info("Sensinact device {}/{}/{}/{} ACT sent", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            } catch (Exception e) {
                LOG.error("Failed to send ACT command to Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            }
        });

        LOG.warn("Devices act not yet supported");
        return responseMessage;
    }

    @Override
    public Message error(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");
        LOG.warn("Error message received from the framework {}", message.getPayload().toString());
        return responseMessage;
    }

    @Override
    public Message unrecognized(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");
        LOG.warn("Unrecognize signal received from the framework {}", message.getPayload().toString());
        return responseMessage;
    }
}