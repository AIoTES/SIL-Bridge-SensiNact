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

import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import eu.interiot.message.payload.types.SoftwarePlatformPayload;
import eu.interiot.translators.syntax.SyntacticTranslator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import org.apache.jena.rdf.model.*;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * INTER-IoT; Interoperability of IoT Platforms. INTER-IoT is a R&amp;D project
 * which has received funding from the European Union's Horizon 2020 research
 * and innovation programme under grant agreement No 687283. Copyright (C)
 * 2016-2018, by (Author's company of this file):
 *
 * For more information, contact: - @author
 * <a href="mailto:sensinact@cea.fr"></a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * This code is licensed under the EPL license, available at the root
 * application directory
 */
public class SensinactTranslator extends SyntacticTranslator<String> {

    public static String sensinactbaseURI = "http://example.com/syntax/Sensinact#";
    public static String sensinactformatName = "Sensinact Data Format Name";
    public static final String snA = "http://sna.com/model#";
    private Resource attributeType;

    public SensinactTranslator() {
        super(sensinactbaseURI, sensinactformatName);
        System.out.println("Sintactic translator was called");
    }

    @Override
    public Model toJenaModel(String s) throws Exception {

        Model model = ModelFactory.createDefaultModel();

        Resource root = model.createResource(snA + "root");
        Property localeProperty = model.createProperty(snA + "value");
        Resource localeResource = model.createResource(snA + "locale");
        model.add(localeResource, localeProperty, s);
        return model;
    }

    protected static Message translate(final String observation, final String conversationId) {
        // create message metadata
        PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
        metadata.initializeMetadata();
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
        metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
        metadata.setSenderPlatformId(new EntityID(snA));
        metadata.setConversationId(conversationId);

        // convert observation from N3 format into a MessagePayload object
        Model m = ModelFactory.createDefaultModel();
        InputStream inStream = new ByteArrayInputStream(observation.getBytes());
        RDFDataMgr.read(m, inStream, Lang.N3);
        MessagePayload messagePayload = new MessagePayload(m);

        Message observationMessage = new Message();
        observationMessage.setMetadata(metadata);
        observationMessage.setPayload(messagePayload);
        return observationMessage;
    }

    public static String getSenderPlatformId(final Message message) {
        // parse message metadata
        MessageMetadata metadata = message.getMetadata();
        PlatformMessageMetadata platformMetadata = metadata.asPlatformMessageMetadata();
        Optional<EntityID> senderPlatformId = platformMetadata.getSenderPlatformId();
        return senderPlatformId.toString();
    }

    public static Set<String> getReceivingPlatformIds(final Message message) {
        // parse message metadata
        MessageMetadata metadata = message.getMetadata();
        PlatformMessageMetadata platformMetadata = metadata.asPlatformMessageMetadata();
        Set<EntityID> receivingPlatformIDs = platformMetadata.getReceivingPlatformIDs();
        Set<String> receivingPlatforms = new HashSet<String>();
        for (EntityID receivingPlatformID : receivingPlatformIDs) {
            receivingPlatforms.add(receivingPlatformID.toString());
        }
        return receivingPlatforms;
    }

    public static Set<SoftwarePlatform> getRegisteredPlatforms(final Message platformRegistrationMessage) {
        Set<SoftwarePlatform> registeredPlatforms = new HashSet<SoftwarePlatform>();
        // parse message metadata
        MessagePayload messagePayload = platformRegistrationMessage.getPayload();
        SoftwarePlatformPayload softwarePlatformPayload = messagePayload.asGOIoTPPayload().asSoftwarePlatformPayload();
        Set<EntityID> softwarePlatforms = softwarePlatformPayload.getSoftwarePlatforms();
        for (EntityID softwarePlatform : softwarePlatforms) {
            Optional<String> baseEndpoint = softwarePlatformPayload.getHasBaseEndpoint(softwarePlatform);
            Optional<String> name = softwarePlatformPayload.getHasName(softwarePlatform);
            registeredPlatforms.add(new SoftwarePlatform(softwarePlatform, baseEndpoint, name));
        }
        return registeredPlatforms;
    }

    public static class SoftwarePlatform {
        public final String id;
        public final String baseEndpoint;
        public final String name;
        
        public SoftwarePlatform(final EntityID id, final Optional<String> baseEndpoint, final Optional<String> name) {
            this.id = id.toString();
            this.baseEndpoint = baseEndpoint.get();
            this.name = name.get();
        }
        
        @Override
        public String toString() {
            return String.format("sensiNact(%s, %s, %s)", id, baseEndpoint, name);
        }
    }

    @Override
    public String toFormatX(Model model) throws Exception {
        Iterator it = model.listObjects();
        while (it.hasNext()) {
            RDFNode node = (RDFNode) it.next();
            System.out.println("Node --->" + node);
            System.out.println("-->" + node.asResource());
        }
        return model.getResource(snA + "locale").getURI();
    }

}

