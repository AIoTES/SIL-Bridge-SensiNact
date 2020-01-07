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

public class SubscriptionResponse {

    private final SubscriptionResponseSubscriptionId id;
    private final SNAResource resource;
    private final String type;
    private final String uri;
    private final Integer statusCode;

    public SubscriptionResponse(final String uri) {
        this.statusCode = 400;
        this.uri = uri;
        this.resource = null;
        this.id = new SubscriptionResponseSubscriptionId();
        this.type = null;
    }

    public SubscriptionResponse(final SNAResource resource) {
        this.statusCode = 200;
        this.uri = resource.toString();
        this.type = resource.getType();
        this.id = new SubscriptionResponseSubscriptionId();
        this.resource = resource;
    }

    public SubscriptionResponseSubscriptionId getResponse() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getUri() {
        return uri;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
    
    public SNAResource getResource() {
        return resource;
    }
}
