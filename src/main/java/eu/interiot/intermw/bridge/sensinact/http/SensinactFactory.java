package eu.interiot.intermw.bridge.sensinact.http;

import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.v1.SensinactCommunicationBridgeV1;
import eu.interiot.intermw.bridge.sensinact.v2.SensinactCommunicationBridgeV2;
import eu.interiot.intermw.bridge.sensinact.wrapper.SensinactAPI;

public class SensinactFactory {

    private SensinactFactory(){};

    public static SensinactAPI createInstance(SensinactConfig config) throws Exception{

        SensinactAPI sensinact;

        if(config.getVersion().equals("v1")){
            sensinact = new SensinactCommunicationBridgeV1();
        }else if(config.getVersion().equals("v2")){
            sensinact = new SensinactCommunicationBridgeV2();
        }else {
            throw new Exception(String.format("Sensinact API version %s is invalid",config.getVersion()));
        }

        sensinact.setConfig(config);

        return sensinact;

    }

}
