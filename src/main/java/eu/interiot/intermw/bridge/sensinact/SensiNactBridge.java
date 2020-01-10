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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "http://sensinact.ddns.net/sensinact")
public class SensiNactBridge extends AbstractBridge {

    private final Logger log = LoggerFactory.getLogger(SensiNactBridge.class);
    private final SNAOntologyAggregator ontologyAggregator = new SNAOntologyAggregator();
    private SensinactAPI sensinact;
    private final ConversationMapper conversation = new ConversationMapper();

    public SensiNactBridge(BridgeConfiguration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        log.debug("SensiNactBridge is initializing...");
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
            log.info("SensiNactBridge has been initialized successfully.");

        } catch (Exception e) {
            throw new BridgeException("Failed to read SensiNact bridge configuration: " + e.getMessage());
        }

    }

    @Override
    public Message registerPlatform(Message message) throws Exception {

        log.info("Registering platform..");

        //There is not a particular task to be performed in register for sensinact
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        sensinact.setListener(new SensinactModelRecoverListener() {
            @Override
            public void notify(String provider, String service, String resource, String type, String value, String timestamp) {

                try {
                    log.info("notified of an update of {]/{}/{} type={}, value={} timestamp={}",
                            provider, service, resource,
                            type, value, timestamp
                    );

                    //No reason for keeping track of the whole ontology
                    //ontologyAggregator.updateOntologyWith(provider,service,resource,value);
                    final String deviceId = String.format("%s/%s/%s", provider, service, resource);

                    for (String conversationId : conversation.subscriptionsGet(deviceId)) {

                        log.info("Sending notification to conversation id {} on device {}", conversationId, deviceId);

                        PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
                        metadata.initializeMetadata();
                        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
                        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);

                        metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
                        metadata.setConversationId(conversationId);
                        Model model = 
                                ontologyAggregator.transformOntology(
                                    provider, service, resource,
                                    type, value, timestamp
                                );
                        MessagePayload messagePayload = 
                                new MessagePayload(model);
                        Message observationMessage = new Message();
                        observationMessage.setMetadata(metadata);
                        observationMessage.setPayload(messagePayload);

                        try {
                            log.info("Sending following observation message to intermw {}", observationMessage.serializeToJSONLD());
                            publisher.publish(observationMessage);
                        } catch (BrokerException e) {
                            log.error("Failed to publish message from the conversation id:" + conversationId);
                        }

                    }

                } catch (Exception e) {
                    log.error("Failed to register platform", e);
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

            log.debug("Unregistering platform {}...", platform.getPlatformId());

            responseMessage.getMetadata().setStatus("OK");

            sensinact.disconnect();

        } catch (Exception e) {
            log.debug("Failed to register platform {}", platform.getPlatformId(), e);
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
        log.debug("Creating callback listener for conversation {} listening at {}...", conversationId, convCallbackUrl);
        Spark.post(conversationId, (request, response) -> {
            log.debug("New observation received from the platform.");
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
                log.debug("Observation message {} has been published upstream through Inter MW.", observationMessage.getMetadata().getMessageID().get());

                response.status(204);
                return "";

            } catch (Exception e) {
                log.debug("Failed to handle observation with conversationId " + conversationId + ": " + e.getMessage(), e);
                response.status(400);
                return "Failed to handle observation: " + e.getMessage();
            }
        });
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
        try {

            log.debug("Subscribing to new devices {} from {}...", deviceIds, platform.getPlatformId());

            responseMessage.getMetadata().setStatus("OK");

            final String conversationId = message.getMetadata().getConversationId().orElse(null);

            if (conversationId != null) {
                final List<String> convertedIoTDeviceIDs = convertIoTDeviceID(deviceIds);
                log.debug("subscribing devices {}...", convertedIoTDeviceIDs);
                conversation.subscriptionsPut(convertedIoTDeviceIDs, conversationId);
                log.debug("subscribed devices {} : conversation {}...", convertedIoTDeviceIDs, conversationId);
            } else {
                throw new NullPointerException("unexpected null conversation id");
            }

        } catch (Exception e) {
            log.error("Failed to subscribe devices {} for platform {}: {}", deviceIds, platform.getPlatformId(), e.getMessage());
            log.debug(e.getMessage(), e);
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
            log.error("Failed to unsubscribe device list", e);
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

            sensinact.listDevices().forEach((snaResource -> {
                soa.updateOntologyWith(snaResource);
            }));

            MessagePayload messagePayload = new MessagePayload(soa.getOntModel());
            responseMessage.setPayload(messagePayload);

        } catch (Exception e) {
            log.error("Failed to fetch device list", e);
            responseMessage.getMetadata().setStatus("KO");
        }

        return responseMessage;
    }

    private void snaCreateUpdateDevice(SNAResource snaResource) {

        try {
            log.info("Creating/Updating Sensinact device {}/{}/{}/{}...", 
                    snaResource.getProvider(), 
                    snaResource.getService(), 
                    snaResource.getResource(), 
                    snaResource.getValue());
            sensinact.createDevice(
                    snaResource.getProvider(),
                    snaResource.getService(),
                    snaResource.getResource(),
                    snaResource.getType(),
                    snaResource.getValue()
            );
            log.info("Sensinact device {}/{}/{}/{} created/updated", 
                    snaResource.getProvider(), 
                    snaResource.getService(), 
                    snaResource.getResource(), 
                    snaResource.getValue());
        } catch (Exception e) {
            log.error("Failed to create/update Sensinact device {}/{}/{}/{}", 
                    snaResource.getProvider(), 
                    snaResource.getService(), 
                    snaResource.getResource(), 
                    snaResource.getValue());
        }

    }

    @Override
    public Message platformCreateDevices(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach((snaResource) -> {
            snaCreateUpdateDevice(snaResource);
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
                log.info("Removing Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
                sensinact.removeDevice(snaResource.getProvider(), snaResource.getService(), snaResource.getResource());
                log.info("Sensinact device {}/{}/{}/{} removed", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            } catch (Exception e) {
                log.error("Failed to remove Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            }
        });
        return responseMessage;
    }

    @Override
    public Message observe(Message message) throws Exception {
        String jsonMessage = message.serializeToJSONLD();
        log.debug("observed message: {}", jsonMessage);
        return platformUpdateDevices(message);
    }

    @Override
    public Message actuate(Message message) throws Exception {

        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");

        SNAOntologyAggregator localAggregator = new SNAOntologyAggregator(message.getPayload().getJenaModel());

        localAggregator.getResourceList().forEach((snaResource) -> {
            try {
                log.info("ACT Sensinact device {}/{}/{}/{}..", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
                sensinact.act(snaResource.getProvider(), snaResource.getService(), snaResource.getResource());
                log.info("Sensinact device {}/{}/{}/{} ACT sent", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            } catch (Exception e) {
                log.error("Failed to send ACT command to Sensinact device {}/{}/{}/{}", snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue());
            }
        });

        log.warn("Devices act not yet supported");
        return responseMessage;
    }

    @Override
    public Message error(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");
        log.warn("Error message received from the framework {}", message.getPayload().toString());
        return responseMessage;
    }

    @Override
    public Message unrecognized(Message message) throws Exception {
        Message responseMessage = createResponseMessage(message);
        responseMessage.getMetadata().setStatus("OK");
        log.warn("Unrecognize signal received from the framework {}", message.getPayload().toString());
        return responseMessage;
    }

}
