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
