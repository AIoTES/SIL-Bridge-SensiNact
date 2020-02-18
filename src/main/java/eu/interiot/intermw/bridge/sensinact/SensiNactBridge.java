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
import eu.interiot.translators.syntax.sensinact.SensinactTranslator;
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
    private final Map<String, SensinactAPI> sensinactMap;
    private final ConversationMapper conversation = new ConversationMapper();
    private final SensinactAPI defaultSensinact;
    private static final String DEFAULT_PLATFORM_NAME = "default sensiNact";

    public SensiNactBridge(BridgeConfiguration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        LOG.debug("SensiNactBridge is initializing...");
        sensinactMap = new HashMap<String, SensinactAPI>();
        Properties properties = configuration.getProperties();
        try {
            String sensinactHost = properties.getProperty("sensinact.host", "127.0.0.1");
            String sensinactHttpPort = properties.getProperty("sensinact.http.port", "80");
            String sensinactVersion = properties.getProperty("sensinact.version", "v2");
            String sensinactProtocol = properties.getProperty("sensinact.protocol", "http");
            String sensinactMaxDeviceNumber = properties.getProperty("sensinact.provider.maxNumber", "100");
            String sensinactName = properties.getProperty("sensinact.name", DEFAULT_PLATFORM_NAME);

            SensinactConfig config = new SensinactConfig();
            config.setHost(sensinactHost);
            config.setProtocol(sensinactProtocol);
            config.setVersion(sensinactVersion);
            config.setHttpPort(sensinactHttpPort);
            config.setMaxDeviceNumber(sensinactMaxDeviceNumber);
            config.setName(sensinactName);
            defaultSensinact = SensinactFactory.createInstance(config);
            sensinactMap.put("default", defaultSensinact);
            LOG.info("SensiNactBridge has been initialized successfully with default sensiNact instance {}.", defaultSensinact);

        } catch (Exception e) {
            throw new BridgeException("Failed to read SensiNact bridge configuration: " + e.getMessage());
        }

    }

    private SensinactAPI getReceivingSensinact(Message message) throws SensinactInstanceNotFoundException, IOException {
        SensinactAPI sensinact;
        Set<String> receivingPlatformIds = SensinactTranslator.getReceivingPlatformIds(message);
        if (receivingPlatformIds.size() > 1) {
            LOG.warn("unexpected more than one ({}) receiving sensiNact platforms in message: {}", receivingPlatformIds);
        } else if (receivingPlatformIds.size() < 1) {
            LOG.error("not found any receiving sensiNact platform in message");
            throw new IOException("not found any receiving sensiNact platform in message");
        }
        String receivingPlatformId = receivingPlatformIds.iterator().next();
        if (receivingPlatformId != null && sensinactMap.containsKey(receivingPlatformId)) {
            sensinact = sensinactMap.get(receivingPlatformId);
        } else {
            throw new SensinactInstanceNotFoundException(receivingPlatformId);
        }
        return sensinact;
        
    }
    
    private SensinactAPI getSenderSensinact(Message message) throws SensinactInstanceNotFoundException, IOException {
        SensinactAPI sensinact;
        String senderPlatformId = SensinactTranslator.getSenderPlatformId(message);
        if (senderPlatformId != null && sensinactMap.containsKey(senderPlatformId)) {
            sensinact = sensinactMap.get(senderPlatformId);
        } else {
            throw new SensinactInstanceNotFoundException(senderPlatformId);
        }
        return sensinact;
        
    }
    
    private SensinactAPI getRegisteredSensinact(Message registrationMessage) throws SensinactInstanceNotFoundException, IOException {
        final String jsonMessage = registrationMessage.serializeToJSONLD();
        LOG.debug("searching for sensiNact info in message:\n{}", jsonMessage);
        SensinactAPI sensinact;
        Set<SensinactTranslator.SoftwarePlatform> registeredPlatforms = SensinactTranslator.getRegisteredPlatforms(registrationMessage);
        if (registeredPlatforms.size() > 1) {
            LOG.warn("unexpected more than one ({}) registered sensiNact platforms in registration message: {}", registeredPlatforms);
        } else if (registeredPlatforms.size() < 1) {
            LOG.error("not found any registered sensiNact platform in registration message");
            throw new IOException("not found any registered sensiNact platform in registration message");
        }
        SensinactTranslator.SoftwarePlatform sensinactPlatform = registeredPlatforms.iterator().next();
        if (sensinactPlatform.id != null && sensinactMap.containsKey(sensinactPlatform.id)) {
            sensinact = sensinactMap.get(sensinactPlatform.id);
        } else {
            throw new SensinactInstanceNotFoundException(sensinactPlatform.id, sensinactPlatform.baseEndpoint);
        }
        return sensinact;
    }

    @Override
    public Message registerPlatform(Message registrationMessage) throws Exception {

        LOG.info("Registering sensiNact platform...");
        SensinactAPI sensinact;
        try {
            sensinact = getRegisteredSensinact(registrationMessage);
            throw new Exception("already registered platform " + sensinact.getName());
        } catch (SensinactInstanceNotFoundException e) {
            String sensinactInstanceName = e.getSensinactInstanceName();
            String baseEndpoint = e.getBaseEndpoint();
            SensinactConfig config = new SensinactConfig(sensinactInstanceName, baseEndpoint);
            sensinact = SensinactFactory.createInstance(config);            
            sensinactMap.put(sensinactInstanceName, sensinact);
            Message responseMessage = createResponseMessage(registrationMessage);
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
                            MessagePayload messagePayload = 
                                    new MessagePayload(model);
                            Message observationMessage = new Message();
                            observationMessage.setMetadata(platformMetadata);
                            observationMessage.setPayload(messagePayload);
                            try {
                                LOG.info("Sending following observation message to intermw {}", observationMessage.serializeToJSONLD());
                                publisher.publish(observationMessage);
                            } catch (BrokerException e) {
                                LOG.error("Failed to publish message from the conversation id {}: {}", conversationId, e.getMessage());
                                LOG.debug("Failed to publish message from the conversation id {}: {}", conversationId, e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to register sensiNact platform: {}", e.getMessage());
                        LOG.debug("Failed to register sensiNact platform: {}", e.getMessage(), e);
                    }
                }
            });
            sensinact.connect();
            LOG.info("Successfully registered sensiNact platform {}", sensinact);
            return responseMessage;
        } catch (Exception e) {
            LOG.error("Failed to register sensiNact platform: {}", e.getMessage());
            LOG.debug("Failed to register sensiNact platform: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Message updatePlatform(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        return responseMessage;
    }

    @Override
    public Message unregisterPlatform(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        LOG.debug("Unregistering sensiNact platform {}...", platform.getPlatformId());
        try {
            final SensinactAPI sensinact = getRegisteredSensinact(message);
            String sensinactName = sensinact.getName();
            responseMessage.getMetadata().setStatus("OK");
            sensinact.disconnect();
            sensinactMap.remove(sensinactName);
            LOG.info("Successfully unregistered sensiNact platform {}...", sensinact.getName());
        } catch (Exception e) {
            LOG.error("Failed to unregister platform {}: {}", platform.getPlatformId(), e.getMessage());
            LOG.debug("Failed to unregister platform {}", platform.getPlatformId(), e);
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
       
    private static List<String> convertIoTDeviceID(List<String> deviceIds) {
        List<String> neoDeviceIds = new ArrayList<>();
        for (String deviceId : deviceIds) {
            neoDeviceIds.add(deviceId.replaceFirst("http://sensinact.ddns.net/", ""));
        }
        return neoDeviceIds;
    }

    @Override
    public Message subscribe(Message message) throws Exception {
        LOG.debug("Received subscription message:...\n{}", message.serializeToJSONLD());
        /**
         * @TODO map device and conversation use conversation id and map with
         * the devices message.getMetadata().getConversationId()
         * message.getPayloadAsGOIoTPPayload().asIoTDevicePayload().getIoTDevices()
         */
        
        Message responseMessage = createResponseMessage(message);
        final List<String> deviceIds = extractDeviceIds(message);
        if (deviceIds.isEmpty()) {
            throw new Exception("no more device to subscribe to");
        }
        try {
            LOG.debug("Subscribing to new devices {} from {}...", deviceIds, platform.getPlatformId());
            final SensinactAPI sensinact = getReceivingSensinact(message);
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
            final SensinactAPI sensinact = getReceivingSensinact(message);

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

    private void snaCreateUpdateDevice(SensinactAPI sensinact, SNAResource snaResource) {

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

        final SensinactAPI sensinact = getReceivingSensinact(message);
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach(new Consumer<SNAResource>() {
            @Override
            public void accept(SNAResource snaResource) {
                snaCreateUpdateDevice(sensinact, snaResource);
            }
        });

        return responseMessage;
    }

    @Override
    public Message platformUpdateDevices(Message message) throws Exception {

        final SensinactAPI sensinact = getReceivingSensinact(message);
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());
        List<SNAResource> resourceList = localAggregator.getResourceList();
        if (resourceList.isEmpty()) {
            throw new Exception("no more device to update");
        }
        resourceList.forEach((snaResource) -> {
            snaCreateUpdateDevice(sensinact, snaResource);
        });
        return responseMessage;
    }

    @Override
    public Message platformDeleteDevices(Message message) throws Exception {

        final SensinactAPI sensinact = getReceivingSensinact(message);
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

        final SensinactAPI sensinact = getReceivingSensinact(message);
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