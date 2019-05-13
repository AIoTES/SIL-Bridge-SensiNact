package eu.interiot.intermw.bridge.sensinact.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"sensinatResourceURL"})
public class UnsubscriptionRequest {

    private String usid;
    private String sensinatResourceURL;

    public String getUsid() {
        return usid;
    }

    public void setUsid(String usid) {
        this.usid = usid;
    }

    public String getSensinatResourceURL() {
        return sensinatResourceURL;
    }

    public void setSensinatResourceURL(String sensinatResourceURL) {
        this.sensinatResourceURL = sensinatResourceURL;
    }
}
