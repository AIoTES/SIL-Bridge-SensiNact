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

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import org.junit.Ignore;

public class GOIoTPTest {

    @Test
    public void test01() {

        /*
        qsd OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec null);

         */
        try
        {
            final String ontoFile="/home/nj246216/projects/interiot/ontology/GOIoTP.owl";
            OntModel model= ModelFactory.createOntologyModel();
            model.read(new FileInputStream(ontoFile),null,"TTL");
            OntClass platform=model.getOntClass("http://inter-iot.eu/GOIoTP#IoTDevice");
            Individual ind=model.createIndividual("sensinact",platform);

            DatatypeProperty dtp=model.getDatatypeProperty("http://inter-iot.eu/GOIoTP#hasName");

            System.out.println("Data type ---->"+dtp);
            System.out.println("Plateform ---->"+platform);

            ind.addLiteral(dtp,"This is sensinact platform");

            System.out.println("---- Assertions in the data ----");
            for (Iterator<Resource> i = ind.listRDFTypes(false); i.hasNext(); ) {
                System.out.println( ind.getURI() + " is a " + i.next() );
            }

            System.out.println("\n---- Inferred assertions ----");
            Individual sensind = model.getIndividual( "sensinact" );
            for (Iterator<Resource> i = sensind.listRDFTypes(false); i.hasNext(); ) {
                System.out.println( sensind.getURI() + " is a " + i.next() );
            }
            System.out.println("Ontology " + ontoFile + " loaded.");
            System.out.println("********************");
            sensind.getModel().write(System.out, "TURTLE");
            System.out.println("/********************");

        }
        catch (JenaException je)
        {
            System.err.println("ERROR" + je.getMessage());
            je.printStackTrace();
            System.exit(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
