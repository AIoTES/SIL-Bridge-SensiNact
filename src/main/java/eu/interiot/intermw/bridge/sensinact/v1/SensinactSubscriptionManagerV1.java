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
package eu.interiot.intermw.bridge.sensinact.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;
import eu.interiot.intermw.bridge.sensinact.wrapper.HTTP;
import eu.interiot.intermw.bridge.sensinact.wrapper.SubscriptionRequest;
import eu.interiot.intermw.bridge.sensinact.wrapper.SubscriptionResponse;
import eu.interiot.intermw.bridge.sensinact.wrapper.UnsubscriptionRequest;
import eu.interiot.intermw.bridge.sensinact.wrapper.UnsubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This singleton keeps an association between the Driver (userid,testbedid,resourceid,etc..) and the CallbackId (in Sensinact contect), this is necessary since the CallbackId is mandatory to execute a unsubscription in Sensinact
 */
public class SensinactSubscriptionManagerV1 {

    private static final Logger LOG = LoggerFactory.getLogger(SensinactSubscriptionManagerV1.class);
    //private static SensinactSubscriptionManagerV1 instance;
    private Map<String,String> callbackIdsPool=new HashMap<String,String>();

    //format:http://sensinact.ddns.net:8080/
    private String sensinactURL;

    public SensinactSubscriptionManagerV1(SensinactConfig config){
        sensinactURL=String.format("%s://%s:%s/",config.getProtocol(),config.getHost(),config.getHttpPort());
    }

    public SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception {

        String deviceName=provider;
        String serviceName=service;
        String resourceName=resource;

        SubscriptionRequest sr=new SubscriptionRequest();
        sr.setSensinatResourceURL(String.format("%s%s", sensinactURL, String.format("sensinact/providers/%s/services/%s/resources/%s/SUBSCRIBE",deviceName,serviceName,resourceName)));
        sr.setCallback(callback);

        LOG.info("Sending subscription request to {} with body {}",sr.getSensinatResourceURL(),new ObjectMapper().writeValueAsString(sr));

        HTTP http=new HTTP();
        http.setMethod("POST");
        http.setUrl(sr.getSensinatResourceURL());
        http.setBodyRequest(new ObjectMapper().writeValueAsString(sr));
        http.submit();
        LOG.info("Received response from {} with body {}",sr.getSensinatResourceURL(),http.getBodyResponse());

        SubscriptionResponse subscriptionResponse=new ObjectMapper().readValue(http.getBodyResponse(),SubscriptionResponse.class);

        if(!(subscriptionResponse.getStatusCode()>=200 && subscriptionResponse.getStatusCode()<300)){
            throw new Exception("IoT gateway responded with failure code for subscription");
        }


        String requestId=userId;//generateRequestId(userId, testbedID, resourceID, experimentId);

        LOG.info("Associating Request ID {} with Sensinact Callback ID {}",requestId,subscriptionResponse.getResponse().getSubscriptionId());
        callbackIdsPool.put(requestId,subscriptionResponse.getResponse().getSubscriptionId());

        return subscriptionResponse;
    }

    public UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception {

        LOG.info("Looking for Request ID {} in the internal table",userId);

        String callbackId=callbackIdsPool.remove(userId);
        if(callbackId==null){
            throw new Exception("Callback Id not internally registered");
        }

        String deviceName=provider;
        String serviceName=service;
        String resourceName=resource;
        UnsubscriptionRequest unsubscriptionRequest=new UnsubscriptionRequest();
        unsubscriptionRequest.setSensinatResourceURL(String.format("%s%s", sensinactURL, String.format("sensinact/providers/%s/services/%s/resources/%s/UNSUBSCRIBE", deviceName, serviceName, resourceName)));

        unsubscriptionRequest.setUsid(callbackId);

        HTTP http=new HTTP();
        http.setMethod("POST");
        http.setUrl(unsubscriptionRequest.getSensinatResourceURL());
        http.setBodyRequest(new ObjectMapper().writeValueAsString(unsubscriptionRequest));

        UnsubscriptionResponse unsubscriptionResponse=new ObjectMapper().readValue(http.submit(), UnsubscriptionResponse.class);

        if(!(unsubscriptionResponse.getStatusCode()>=200 && unsubscriptionResponse.getStatusCode()<300)){
            throw new Exception("IoT gateway responded with failure code for unsubscription");
        }

        return unsubscriptionResponse;

    }

}
