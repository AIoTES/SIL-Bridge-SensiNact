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

public class SNAResource {

    public static final String DEFAULT_VALUE = null;
    public static final String NOT_A_FUNCTION = "not-a-function";
    public static final String DEFAULT_TYPE = NOT_A_FUNCTION;
    private String provider;
    private String service;
    private String resource;
    private String value;
    private String type;

    public SNAResource(String uri, String value) {
        String[] resourcePath = uri.split("/");
        this.provider = resourcePath[1];
        this.service = resourcePath[2];
        this.resource = resourcePath[3];
        this.type = DEFAULT_TYPE;
        this.value = value;
    }

    public SNAResource(String provider, String service, String resource, String type) {
        this.provider = provider;
        this.service = service;
        this.resource = resource;
        this.type = type;
        this.value = DEFAULT_VALUE;
    }

    public SNAResource(String provider, String service, String resource, String type, String value) {
        this(provider, service, resource, type);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SNAResource{" + "provider='" + provider + '\'' + ", service='" + service + '\'' + ", resource='" + resource + '\'' + ", type='" + type + '\'' + ", value='" + value + '\'' + '}';
    }
}
