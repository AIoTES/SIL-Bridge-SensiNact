package eu.interiot.translators.syntax.sensinact;

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.SensinactFactory;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.http.model.exception.InvalidConfigurationValueException;
import eu.interiot.intermw.bridge.sensinact.ontology.SNAOntologyAggregator;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class SensinactOntologyTest {

    SNAOntologyAggregator aggregator;

    @Before
    public void config() {
        aggregator=new SNAOntologyAggregator();
    }

    @Test
    @Ignore
    public void save() throws Exception {

        SensinactConfig sc=new SensinactConfig();
        sc.setVersion("v1");
        sc.setWebSocketPort("8092");
        sc.setProtocol("http");
        sc.setHost("sensinact.ddns.net");
        sc.setHttpPort("80");
        sc.setMaxDeviceNumber("100");


        SensinactAPI sensinact=SensinactFactory.createInstance(sc);

        sensinact.setListener((provider,service, resource, value)->{
            aggregator.updateOntologyWith(provider,service,resource,value);
        });

        sensinact.connect();

        Thread.sleep(60000);

        aggregator.saveOntology("/tmp/SNAOntologyWithData.owl", SNAOntologyAggregator.JenaWriterType.rdf);
    }

    @Test
    public void aggregatorResourceCountTest() throws IOException {

        aggregator.updateOntologyWith("p1","s1fromp1","r1froms1fromp1","value1");
        aggregator.updateOntologyWith("p1","s2fromp1","r1froms2fromp1","value2");
        aggregator.updateOntologyWith("p1","s2fromp1","r2froms2fromp1","value3");
        aggregator.updateOntologyWith("p2","s2","r2","value4");
        Assert.assertTrue(aggregator.getResourceList().size()==4);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource)->resource.getProvider().equals("p1")&&resource.getService().equals("s2fromp1")&&resource.getResource().contains("froms2fromp1")).toArray().length==2);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource)->resource.getProvider().equals("p1")).toArray().length==3);
        Assert.assertTrue(aggregator.getResourceList().stream().filter((resource)->resource.getProvider().equals("p2")).toArray().length==1);

    }

}
