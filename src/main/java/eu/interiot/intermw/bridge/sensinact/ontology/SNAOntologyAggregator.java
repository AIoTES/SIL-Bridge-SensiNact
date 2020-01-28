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

import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;

import java.io.*;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This classes loads Sensinact Ontology and create individuals on that ontology
 * according to information sent to updateOntology
 */
public class SNAOntologyAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(SNAOntologyAggregator.class);
    
    private static final DateTimeFormatter DATE_TIMESTAMP_FORMATTER
            = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final OntModel EMPTY_MODEL = ModelFactory.createOntologyModel();

    public enum JenaWriterType {
        rdf("RDF/XML"),
        n3("N3"),
        rdfjson("RDF/JSON"),
        ttl("TTL"),
        jsonld("JSON-LD"),;

        JenaWriterType(String name) {
            this.name = name;
        }
        String name;
    }

    private OntModel model;
    private static final String SNA_ONTOLOGY_PREFIX = "http://sensinact.com#";
    private static final String SNA_ONTOLOGY_FILE_PATH = "/ontology/SNAOntology.owl";
    private static final String SNA_ONTOLOGY_FILE_PATH_PATTERN = "/ontology/%s/SNAOntology.owl";
    private static final Properties SNA2AIOTES_KPI = new Properties();
    static {
        try {
            SNA2AIOTES_KPI.load(SNAOntologyAggregator.class.getResourceAsStream("sna2aiotes-kpi.properties"));
        } catch (IOException ex) {
            LOG.error("failed to load sensiNact to AIOTES mapping sna2aiotes-kpi.properties");
        }
    };

    public SNAOntologyAggregator(Model rdfmodel) {
        model = ModelFactory.createOntologyModel();
        model.add(rdfmodel);
    }

    public SNAOntologyAggregator(String filepath, String format) throws FileNotFoundException {
        model = ModelFactory.createOntologyModel();
        InputStream is = new FileInputStream(filepath);
        model.read(is, null, format);

    }

    public SNAOntologyAggregator(JenaWriterType lang) {
        model = ModelFactory.createOntologyModel();
        InputStream is = this.getClass().getResourceAsStream(SNA_ONTOLOGY_FILE_PATH);
        model.read(is, null, lang.name);
    }

    public SNAOntologyAggregator() {
        this(JenaWriterType.rdf);
    }

    public OntClass getOntologyClass(String clazz) {
        return model.getOntClass(SNA_ONTOLOGY_PREFIX + clazz);
    }

    public Property getObjectProperty(String property) {
        return model.getProperty(SNA_ONTOLOGY_PREFIX + property);
    }

    public DatatypeProperty getDataProperty(String property) {
        return model.getDatatypeProperty(SNA_ONTOLOGY_PREFIX + property);
    }

    public Model transformOntology(String provider, String service, String resource, String type, String value, String timestamp) {
        final OntModel isolatedModel = ModelFactory.createOntologyModel();
        String ontologyFilePath;
        InputStream is;
        ontologyFilePath = SNA_ONTOLOGY_FILE_PATH;
        is = this.getClass().getResourceAsStream(ontologyFilePath);
        RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
//        ontologyFilePath = String.format(SNA_ONTOLOGY_FILE_PATH_PATTERN, type);
//        is = this.getClass().getResourceAsStream(ontologyFilePath);
//        if (is != null) {
//            RDFDataMgr..read(isolatedModel, is, Lang.RDFXML);
//        }
        updateOntologyWith(provider, service, resource, type, value, timestamp, isolatedModel);
        this.model = isolatedModel;
        final Model minimalModel = isolatedModel.difference(EMPTY_MODEL);
        return minimalModel;
    }

    public Model transformOntology(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {
        final OntModel isolatedModel = ModelFactory.createOntologyModel();
        String ontologyFilePath;
        InputStream is;
        ontologyFilePath = SNA_ONTOLOGY_FILE_PATH;
        is = this.getClass().getResourceAsStream(ontologyFilePath);
        RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
//        ontologyFilePath = String.format(SNA_ONTOLOGY_FILE_PATH_PATTERN, type);
//        is = this.getClass().getResourceAsStream(ontologyFilePath);
//        if (is != null) {
//            RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
//        }
        updateOntologyWith(provider, service, resource, type, value, timestamp, metadata, isolatedModel);
        this.model = isolatedModel;
        final Model minimalModel = isolatedModel.difference(EMPTY_MODEL);
        return minimalModel;
    }

    public Model createModel(String provider, String service, String resource, String type, String value, String timestamp) {
        final Model isolatedModel = transformOntology(provider, service, resource, type, value, timestamp);
        return isolatedModel;
    }

    public Model createModel(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {
        final Model isolatedModel = transformOntology(provider, service, resource, type, value, timestamp, metadata);
        return isolatedModel;
    }

    public void updateOntologyWith(String provider, String service, String resource, String type, String value, String timestamp) {
        updateOntologyWith(provider, service, resource, type, value, timestamp, getOntModel());
    }

    public void updateOntologyWith(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata) {
        updateOntologyWith(provider, service, resource, type, value, timestamp, metadata, getOntModel());
    }

    public void updateOntologyWith(SNAResource snaResource) {
        final long timestamp = System.currentTimeMillis();
        updateOntologyWith(
                snaResource.getProvider(),
                snaResource.getService(),
                snaResource.getResource(),
                snaResource.getType(),
                snaResource.getValue(),
                String.valueOf(timestamp),
                snaResource.getMetadata()
        );
    }

    /**
     * Create individuals necessary to receive the information passed to this
     * method and link them in the ontology instance
     *
     * @param provider device name in sensinact
     * @param service group of particular information in sensinact
     * @param resource the leaf node that contains the metadata of the
     * information
     * @param value the current data stored by the 'resource'
     * @param model the ontology model
     */
    public void updateOntologyWith(String provider, String service, String resource, String type, String value, String timestamp, OntModel model) {
        updateOntologyWith(
                provider, service, resource, type, value, timestamp,
                Collections.EMPTY_MAP, model);
    }

    /**
     * Create individuals necessary to receive the information passed to this
     * method and link them in the ontology instance
     *
     * @param provider device name in sensinact
     * @param service group of particular information in sensinact
     * @param resource the leaf node that contains the metadata of the
     * information
     * @param value the current data stored by the 'resource'
     * @param model the ontology model
     */
    public void updateOntologyWith(String provider, String service, String resource, String type, String value, String timestamp, Map<String, String> metadata, OntModel model) {

        final SNAAHAOntologyType snaOntologyType
                = SNAAHAOntologyType.getSNAAHAOntologyType(type, service, resource);
        final String ontologyClassName = snaOntologyType.getOntologyClassName();
        OntClass ontologyClass = getOntologyClass(ontologyClassName);
        Individual individualResource = model.createIndividual(ontologyClass);

        Property providerDataProperty = getObjectProperty("provider");
        Property serviceDataProperty = getObjectProperty("service");
        Property nameDataProperty = getObjectProperty("name");
        Property typeDataProperty = getObjectProperty("type");
        Property timestampDataProperty = getObjectProperty("timestamp");
        Property dateTimestampDataProperty = getObjectProperty("dateTimestamp");
        if (value != null) {
            String correctedValue = snaOntologyType.computeValue(value);
            String correctedName = snaOntologyType.toAIOTESName(resource);
            Property valueProperty = getObjectProperty("value");
            individualResource.addLiteral(providerDataProperty, provider);
            individualResource.addLiteral(serviceDataProperty, service);
            individualResource.addLiteral(nameDataProperty, correctedName);
            individualResource.addLiteral(typeDataProperty, type);
            individualResource.addLiteral(valueProperty, correctedValue);
            individualResource.addLiteral(timestampDataProperty, timestamp);
            String dateTimestamp = toLocaDateTime(timestamp);
            individualResource.addLiteral(dateTimestampDataProperty, dateTimestamp);
            Property metadataProperty;
            for (Entry<String, String> entry : metadata.entrySet()) {
                metadataProperty = getObjectProperty(entry.getKey());
                individualResource.addLiteral(metadataProperty, entry.getValue());
            }
        }
    }

    private static String toLocaDateTime(String timestamp$) {
        String date = timestamp$;
        ZonedDateTime dateTime = null;
        try {
            final long timestamp = Long.parseLong(timestamp$);
            dateTime
                    = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            TimeZone.getDefault().toZoneId()
                    );
            date = dateTime.format(DATE_TIMESTAMP_FORMATTER);
        } catch (NumberFormatException e) {
            //nothing to do
        } catch (DateTimeException e) {
            date = e.getMessage();
//            if (localDateTime != null) {
//                date = localDateTime.toString();
//            }
        }
        return date;
    }

    private static long toTimeStamp(String dateTime$) {
        long timeStamp = System.currentTimeMillis();
        try {
         ZonedDateTime dateTime = ZonedDateTime.parse(dateTime$);
         timeStamp = dateTime.toEpochSecond();
        } catch (DateTimeParseException e) {
            
        } 
        return timeStamp;
    }

    private static interface ValueComputer {

        String computeValue(String value);
    }

    private static interface NameMapper {

        String toAIOTESName(String snaName);
        
        String toSNAName(String aiotesName);
    }

    private static enum SNAAHAOntologyType implements ValueComputer, NameMapper {
        DAY_LAYING("state", "BedOccupancyResource"),
        KPI("KPIResource") {
            @Override
            public String toAIOTESName(final String snaKpiName) {
                String aiotesKpiName = SNA2AIOTES_KPI.getProperty(snaKpiName, snaKpiName);
                if (aiotesKpiName == null) {
                    aiotesKpiName = snaKpiName;
                }
                return aiotesKpiName;
            }
            
            @Override
            public String toSNAName(final String aiotesKpiName) {
                String snaName = aiotesKpiName;
                String propertyValue;
                for (Entry entry : SNA2AIOTES_KPI.entrySet()) {
                    propertyValue = entry.getValue().toString();
                    if (propertyValue.equals(aiotesKpiName)) {
                        snaName = entry.getKey().toString();
                        break;
                    }
                };
                return snaName;
            }

        },
        NIGHT_RISING("state", "BedOccupancyResource") {
            @Override
            public String computeValue(final String value) {
                boolean isOutOfBed = Boolean.valueOf(value);
                boolean isInBed = !isOutOfBed;
                return String.valueOf(isInBed);
            }
        },
        PEDOMETER_MONITOR("last-day-step-counter", "StepNumberResource"),
        TEMPERATURE_ALERT("last-temperature", "TemperatureMonitorResource"),
        WEIGHT_MONITOR("last-weight", "WeightMonitorResource"),
        DEFAULT;

        private static final String DEFAULT_ONTOLOGY_CLASS_NAME = "Resource";
        private static final String DEFAULT_SERVICE = "monitor";
        private static final String ANY_RESOURCE = "*";
        private static final String DEFAULT_RESOURCE = ANY_RESOURCE;
        private final String ontologyClassName;
        private final String service;
        private final String resource;

        SNAAHAOntologyType() {
            this.ontologyClassName = DEFAULT_ONTOLOGY_CLASS_NAME;
            this.service = DEFAULT_SERVICE;
            this.resource = DEFAULT_RESOURCE;
        }

        SNAAHAOntologyType(final String ontologyClassName) {
            this.ontologyClassName = ontologyClassName;
            this.service = DEFAULT_SERVICE;
            this.resource = DEFAULT_RESOURCE;
        }

        SNAAHAOntologyType(final String resource, final String ontologyClassName) {
            this.ontologyClassName = ontologyClassName;
            this.service = DEFAULT_SERVICE;
            this.resource = resource;
        }

        private String getOntologyClassName() {
            return ontologyClassName;
        }

        private boolean isForService(final String service) {
            boolean isForService = this.service.equals(service);
            return isForService;
        }

        private boolean isForResource(final String resource) {
            boolean isForResource = this.resource.equals(ANY_RESOURCE) || this.resource.equals(resource);
            return isForResource;
        }

        @Override
        public String computeValue(String value) {
            return value;
        }

        @Override
        public String toAIOTESName(String snaName) {
            return snaName;
        }

        @Override
        public String toSNAName(String aiotesName) {
            return aiotesName;
        }

        private static SNAAHAOntologyType getSNAAHAOntologyType(final String ahaType, final String serviceId, final String resourceId) {
            SNAAHAOntologyType ontologyType;
            try {
                ontologyType = SNAAHAOntologyType.valueOf(ahaType);
                if (!ontologyType.isForService(serviceId) || !ontologyType.isForResource(resourceId)) {
                    ontologyType = DEFAULT;
                }
            } catch (Exception e) {
                ontologyType = DEFAULT;
            }
            return ontologyType;
        }
    }

    public OntModel getOntModel() {
        return this.model;
    }

    public void printOntology(JenaWriterType type) {
        getOntModel().write(System.out, type.name);
    }

    public String getStringOntology() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        getOntModel().write(bos, "TURTLE");
        try {
            return new String(bos.toByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("unsupported encoding {}", e.getMessage());
            LOG.debug("unsupported encoding", e);
            return null;
        }
    }

    public void saveOntology(String filepath, JenaWriterType writerType) throws IOException {
        new File(filepath).createNewFile();
        FileOutputStream fos = new FileOutputStream(filepath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        getOntModel().write(fos, writerType.name);
    }

    public List<SNAResource> getResourceList() {
        return getResourceList(model);
    }

    public List<SNAResource> getResourceList(final OntModel model) {

        final List<SNAResource> resources = new ArrayList<>();

        final Property providerDataProperty = getObjectProperty("provider");
        final Property serviceDataProperty = getObjectProperty("service");
        final Property nameDataProperty = getObjectProperty("name");
        final Property typeDataProperty = getObjectProperty("type");
        final Property timestampDataProperty = getObjectProperty("timestamp");
        final Property dateTimeDataProperty = getObjectProperty("dateTimestamp");
        final Property valueDataProperty = getObjectProperty("value");

        for (Iterator<Individual> it = model.listIndividuals(); it.hasNext();) {
            Individual resource = it.next();
            try {
                final String providerName = resource.getProperty(providerDataProperty).getString();
                final String serviceName = resource.getProperty(serviceDataProperty).getString();
                final String resourceName = resource.getProperty(nameDataProperty).getString();
                
                final String resourceType = resource.getProperty(typeDataProperty).getString();
                final String resouceValue = resource.getProperty(valueDataProperty).getString();
                final Statement timeStampStatement = resource.getProperty(timestampDataProperty);
                final SNAResource snaResource = new SNAResource(providerName, serviceName, resourceName, resourceType, resouceValue);
                if (timeStampStatement != null) {
                    final String timeStampValue = timeStampStatement.getString();
                    snaResource.putMetadata("timestamp", timeStampValue);
                } else {
                    final Statement dateTimeStatement = resource.getProperty(dateTimeDataProperty);
                    if (dateTimeStatement != null) {
                        final String dateTimeValue = dateTimeStatement.getString();
                        final String timeStampValue = String.valueOf(toTimeStamp(dateTimeValue));
                        snaResource.putMetadata("timestamp", timeStampValue);
                    }
                }
                if (resourceType.equals("KPI")) {
                    final String snaKpiName = SNAAHAOntologyType.KPI.toSNAName(resourceName);
                    snaResource.setResource(snaKpiName);
                    final Property targetDataProperty = getObjectProperty("target");
                    final String resourceTarget = resource.getProperty(targetDataProperty).getString();
                    snaResource.putMetadata("target", resourceTarget);
                }

                LOG.info("found update for resource {}", snaResource);
                resources.add(snaResource);
            } catch (Exception e) {
                LOG.error("not a complete resource description for individual {}: {}", resource, e.getMessage());
            }
        }
        return resources;
    }
}
