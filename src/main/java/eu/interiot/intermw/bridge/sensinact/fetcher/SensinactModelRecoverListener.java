package eu.interiot.intermw.bridge.sensinact.fetcher;

public interface SensinactModelRecoverListener {

    void notify(String provider,String service,String resource,String value);

}
