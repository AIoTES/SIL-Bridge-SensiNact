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
package eu.interiot.intermw.bridge.sensinact.wrapper;

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import java.text.MessageFormat;

import java.util.List;
import java.util.Map;

/**
 * This is the interface that represents the boundaries between InterIot and
 * Sensinact.
 */
public interface SensinactAPI {

    static final String RESOURCE_URI_PATTERN = "{0}/{1}/{2}";
    static final MessageFormat RESOURCE_URI_FORMAT = new MessageFormat(RESOURCE_URI_PATTERN);
    static final String UNKNOWN_USER = "unknown user";
    static final String NO_CALLBACK = "";
    
    /**
     * getter for the name of this sensiNact platform instance.
     * @return the name of this sensiNact platform instance.
     */
    String getName();
    
    /**
     * getter for the base endpoint of this sensiNact platform instance.
     * @return the base endpoint of this sensiNact platform instance.
     */
    String getBaseEndpoint();
    
    SubscriptionResponse subscribe(String resourceURI) throws Exception;
    
    SubscriptionResponse subscribe(String provider, String service, String resource) throws Exception;
    
    SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception;

    UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception;

    void updateResource(String provider, String service, String resource, String type, String value, Map<String, String> metadata) throws Exception;

    void removeResource(String provider, String service, String resource) throws Exception;

    List<SNAResource> listResources();

    void act(String provider, String service, String resource) throws Exception;

    void setListener(SensinactModelRecoverListener listener);

    void connect();

    void disconnect();
}
