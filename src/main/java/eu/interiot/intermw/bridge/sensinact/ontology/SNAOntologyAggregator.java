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
import org.apache.jena.rdf.model.Literal;
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
    private final String filepath = "/ontology/SNAOntology.owl";

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
        InputStream is = this.getClass().getResourceAsStream(filepath);
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

    public OntModel transformOntology(String provider, String service, String resource, String value) {
        OntModel isolatedModel = ModelFactory.createOntologyModel();
        InputStream is = this.getClass().getResourceAsStream(filepath);
        RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
        updateOntologyWith(provider, service, resource, value, isolatedModel);

        return isolatedModel;
    }

    public Model createModel(String provider, String service, String resource, String value) {
        OntModel isolatedModel = ModelFactory.createOntologyModel();
        InputStream is = this.getClass().getResourceAsStream(filepath);
        RDFDataMgr.read(isolatedModel, is, Lang.RDFXML);
        updateOntologyWith(provider, service, resource, value, isolatedModel);
        return isolatedModel;
    }

    public void updateOntologyWith(String provider, String service, String resource, String value) {
        updateOntologyWith(provider, service, resource, value, getOntModel());
    }

    public void updateOntologyWith(SNAResource snaResource) {
        updateOntologyWith(snaResource.getProvider(), snaResource.getService(), snaResource.getResource(), snaResource.getValue(), getOntModel());
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
    public void updateOntologyWith(String provider, String service, String resource, String value, OntModel model) {

        String URLIndividualProvider = String.format("%s", provider);
        String URLIndividualService = String.format("%s/%s", provider, service);
        String URLIndividualResource = String.format("%s/%s/%s", provider, service, resource);

        Individual individualProvider = model.createIndividual(null, getOntologyClass("Provider"));
        Individual individualService = model.createIndividual(null, getOntologyClass("Service"));
        Individual individualResource = model.createIndividual(null, getOntologyClass("Resource"));

        /*
        Individual individualProvider=model.getIndividual(URLIndividualProvider)!=null?model.getIndividual(URLIndividualProvider):model.createIndividual(URLIndividualProvider,getOntologyClass("Provider"));
        Individual individualService=model.getIndividual(URLIndividualService)!=null?model.getIndividual(URLIndividualService):model.createIndividual(URLIndividualService,getOntologyClass("Service"));
        Individual individualResource=model.createIndividual(URLIndividualResource,getOntologyClass("Resource"));
         */
        DatatypeProperty nameDataProperty = getDataProperty("name");
        Property managesProperty = getObjectProperty("manages");
        individualProvider.addLiteral(nameDataProperty, provider);
        individualService.addLiteral(nameDataProperty, service);

        if (value != null) {
            DatatypeProperty valueProperty = getDataProperty("value");
            individualResource.addLiteral(nameDataProperty, resource);
            individualResource.addLiteral(valueProperty, value);
        }
        model.add(individualProvider, managesProperty, individualService);
        model.add(individualService, managesProperty, individualResource);

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
