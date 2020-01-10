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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * This classes loads Sensinact Ontology and create individuals on that ontology
 * according to information sent to updateOntology
 */
public class SNAOntologyAggregator {

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
    private static final String SNA_ONTOLOGY_FILE_PATH = "/ontology/SNAOntology.owl";
    private static final String SNA_ONTOLOGY_FILE_PATH_PATTERN = "/ontology/%s/SNAOntology.owl";

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
        return model.getOntClass("http://sensinact.com#" + clazz);
    }

    public Property getObjectProperty(String property) {
        return model.getProperty("http://sensinact.com#" + property);
    }

    public DatatypeProperty getDataProperty(String property) {
        return model.getDatatypeProperty("http://sensinact.com#" + property);
    }

    public Model transformOntology(String provider, String service, String resource, String type, String value, String timestamp) {
        final OntModel emptyModel = ModelFactory.createOntologyModel();
        final OntModel isolatedModel = ModelFactory.createOntologyModel();
        String ontologyFilePath;
        InputStream is;
        ontologyFilePath = String.format(SNA_ONTOLOGY_FILE_PATH_PATTERN, type);
        is = this.getClass().getResourceAsStream(ontologyFilePath);
        if (is == null) {
            ontologyFilePath = SNA_ONTOLOGY_FILE_PATH;
            is = this.getClass().getResourceAsStream(ontologyFilePath);
        }
        RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
        updateOntologyWith(provider, service, resource, type, value, timestamp, isolatedModel);
        final Model minimalModel = isolatedModel.difference(emptyModel);
        return minimalModel;
    }
    
    public Model createModel(String provider, String service, String resource, String type, String value, String timestamp) {
        final Model isolatedModel = transformOntology(provider, service, resource, type, value, timestamp);
        return isolatedModel;
    }

    public void updateOntologyWith(String provider, String service, String resource, String type, String value, String timestamp) {
        updateOntologyWith(provider, service, resource, type, value, timestamp, getOntModel());
    }

    public void updateOntologyWith(SNAResource snaResource) {
        final long timestamp = System.currentTimeMillis();
        updateOntologyWith(
                snaResource.getProvider(), 
                snaResource.getService(), 
                snaResource.getResource(), 
                snaResource.getType(), 
                snaResource.getValue(),
                String.valueOf(timestamp)
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

        final SNAAHAOntologyType snaOntologyType = 
            SNAAHAOntologyType.getSNAAHAOntologyType(type, service, resource);
        final String ontologyClassName = snaOntologyType.getOntologyClassName();
        Individual individualResource = 
            model.createIndividual(getOntologyClass(ontologyClassName));

        Property providerDataProperty = getObjectProperty("provider");
        Property serviceDataProperty = getObjectProperty("service");
        Property nameDataProperty = getObjectProperty("name");
        Property typeDataProperty = getObjectProperty("type");
        Property timestampDataProperty = getObjectProperty("timestamp");

        if (value != null) {
            String correctedValue = snaOntologyType.computeValue(value);
            Property valueProperty = getObjectProperty("value");
            individualResource.addLiteral(providerDataProperty, provider);
            individualResource.addLiteral(serviceDataProperty, service);
            individualResource.addLiteral(nameDataProperty, resource);
            individualResource.addLiteral(typeDataProperty, type);
            individualResource.addLiteral(valueProperty, correctedValue);
            individualResource.addLiteral(timestampDataProperty, timestamp);
        }
    }

    private static interface ValueComputer {
        String computeValue(String value);
    }

    private static enum SNAAHAOntologyType implements ValueComputer {
        DAY_LAYING("state", "BedOccupancyResource"),
        KPI("KPIResource"),
        NIGHT_RISING("state", "BedOccupancyResource") {
            @Override
            public String computeValue(String value) {
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
            e.printStackTrace();
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

        final List<SNAResource> resources = new ArrayList<>();

        Property nameProperty = getObjectProperty("name");
        Property managesProperty = getObjectProperty("manages");
        DatatypeProperty valueDataProperty = getDataProperty("value");

        for (Iterator<Individual> it = getOntModel().listIndividuals(); it.hasNext();) {

            Individual indi = it.next();
            if (indi.getOntClass().equals(getOntologyClass("Provider"))) {

                final String providerName = indi.getProperty(nameProperty).getString();
                Statement service = indi.getProperty(managesProperty);
                final String serviceName = service.getProperty(nameProperty).getString();
                Statement resourceStatement = service.getProperty(managesProperty);
                final String resourceName = resourceStatement.getProperty(nameProperty).getString();
                final String resouceValue = resourceStatement.getProperty(valueDataProperty).getString();
                resources.add(new SNAResource(providerName, serviceName, resourceName, resouceValue));
            }
        }
        return resources;
    }
}