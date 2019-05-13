package eu.interiot.intermw.bridge.sensinact.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"sensinatResourceURL"})
public class SubscriptionRequest {

    private String sensinatResourceURL;
    private String callback;

    public String getSensinatResourceURL() {
        return sensinatResourceURL;
    }

    public void setSensinatResourceURL(String sensinatResourceURL) {
        this.sensinatResourceURL = sensinatResourceURL;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }
}
