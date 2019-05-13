package eu.interiot.intermw.bridge.sensinact.wrapper;

public class SNAResource {

    private String provider;
    private String service;
    private String resource;
    private String value;

    public SNAResource(String provider, String service, String resource) {
        this.provider = provider;
        this.service = service;
        this.resource = resource;
    }

    public SNAResource(String provider, String service, String resource, String value) {
        this.provider = provider;
        this.service = service;
        this.resource = resource;
        this.value = value;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SNAResource{" + "provider='" + provider + '\'' + ", service='" + service + '\'' + ", resource='" + resource + '\'' + ", value='" + value + '\'' + '}';
    }
}
