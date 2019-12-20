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

    private SubscriptionResponseSubscriptionId response;
    private String type;
    private String uri;
    private Integer statusCode;

    public SubscriptionResponse(final String uri) {
        this.statusCode = 400;
        this.uri = uri;
        this.response = new SubscriptionResponseSubscriptionId();
    }

    public SubscriptionResponse(final String uri, final String type) {
        this.statusCode = 200;
        this.uri = uri;
        this.type = type;
        this.response = new SubscriptionResponseSubscriptionId();
    }

    public SubscriptionResponseSubscriptionId getResponse() {
        return response;
    }

    public void setResponse(SubscriptionResponseSubscriptionId response) {
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
