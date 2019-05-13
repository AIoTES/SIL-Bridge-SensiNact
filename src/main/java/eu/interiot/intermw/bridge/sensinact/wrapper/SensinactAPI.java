package eu.interiot.intermw.bridge.sensinact.wrapper;

import eu.interiot.intermw.bridge.sensinact.fetcher.SensinactModelRecoverListener;
import eu.interiot.intermw.bridge.sensinact.http.model.SensinactConfig;

import java.util.List;

/**
 * This is the interface that represents the boundaries between InterIot and Sensinact.
 */
public interface SensinactAPI {
    SubscriptionResponse subscribe(String userId, String provider, String service, String resource, String callback) throws Exception;
    UnsubscriptionResponse unsubscribe(String userId, String provider,String service, String resource, String callback) throws Exception;
    void createDevice(String provider,String service,String resource,String value) throws Exception;
    void removeDevice(String provider,String service,String resource) throws Exception;
    List<SNAResource> listDevices();
    void act(String provider,String service,String resource) throws Exception;
    void setConfig(SensinactConfig config);
    void setListener(SensinactModelRecoverListener listener);
    void connect();
    void disconnect();
}
