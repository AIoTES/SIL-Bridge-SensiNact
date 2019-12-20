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
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;

import java.util.List;

/**
 * This is the interface that represents the boundaries between InterIot and
 * Sensinact.
 */
public interface SensinactAPI {

    SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception;

    UnsubscriptionResponse unsubscribe(String userId, String provider, String service, String resource, String callback) throws Exception;

    void createDevice(String provider, String service, String resource, String type, String value) throws Exception;

    void removeDevice(String provider, String service, String resource) throws Exception;

    List<SNAResource> listDevices();

    void act(String provider, String service, String resource) throws Exception;

    void setConfig(SensinactConfig config);

    void setListener(SensinactModelRecoverListener listener);

    void connect();

    void disconnect();
}
