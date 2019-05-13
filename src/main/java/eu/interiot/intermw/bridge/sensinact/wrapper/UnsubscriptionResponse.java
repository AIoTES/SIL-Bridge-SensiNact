package eu.interiot.intermw.bridge.sensinact.wrapper;

public class UnsubscriptionResponse {

    private UnsubscriptionResponseMessage response;
    private String type;
    private String uri;
    private Integer statusCode;

    public UnsubscriptionResponseMessage getResponse() {
        return response;
    }

    public void setResponse(UnsubscriptionResponseMessage response) {
        this.response = response;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
}
