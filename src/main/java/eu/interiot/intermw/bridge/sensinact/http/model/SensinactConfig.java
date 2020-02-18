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
package eu.interiot.intermw.bridge.sensinact.http.model;

import eu.interiot.intermw.bridge.sensinact.http.model.exception.InvalidConfigurationValueException;
import java.text.MessageFormat;
import java.text.ParseException;

import java.util.regex.Pattern;

public class SensinactConfig {

    public static final String DEFAULT_VERSION = "v2";
    public static final int DEFAULT_DEVICE_NUMBER = 100;
    private static final String HOST_MASK = ".*";
    private static final String PROTOCOL_MASK = "(http|https)";
    private static final String VERSION_MASK = "(v1|v2)";
    private static final String HTTP_PORT_MASK = "[0-9]+";
    private static final String MAX_DEVICE_NUMBER_MASK = "(-)*[0-9]+";
    private static final String BASE_ENDPOINT_PATTERN = "{0}://{1}:{2}";
    private static final MessageFormat BASE_ENDPOINT_FORMAT = new MessageFormat(BASE_ENDPOINT_PATTERN);

    private String protocol; //value example http,https
    private String version; //value example v1,v2
    private String host; //value example 127.0.0.1, localhost, sensinact.ddns.net
    private String httpPort;//value example 8080 8090
    private int websocketPort;//value example 8092
    private int maxDeviceNumber;//value example 100
    private String name;

    public SensinactConfig() {
        this.version = DEFAULT_VERSION;
        this.maxDeviceNumber = DEFAULT_DEVICE_NUMBER;
    }
    
    public SensinactConfig(final String name, final String baseEndpoint) throws InvalidConfigurationValueException {
        this();
        this.name = name;
        this.setBaseEndpoint(baseEndpoint);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }

    public String getBaseEndpoint() {
        return BASE_ENDPOINT_FORMAT.format(new Object[] {protocol, host, httpPort});
    }

    public final void setBaseEndpoint(String baseEndpoint) throws InvalidConfigurationValueException {
        try {
            Object[] parsed = BASE_ENDPOINT_FORMAT.parse(baseEndpoint);
            setProtocol(parsed[0].toString());
            setHost(parsed[1].toString());
            setHttpPort(parsed[2].toString());
        } catch (Exception ex) {
            throw new InvalidConfigurationValueException(ex);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) throws InvalidConfigurationValueException {
        validate(protocol, PROTOCOL_MASK);
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) throws InvalidConfigurationValueException {
        validate(version, VERSION_MASK);
        this.version = version;
    }

    private void validate(String value, String mask) throws InvalidConfigurationValueException {
        Pattern p = Pattern.compile(mask);
        if (value == null || !p.matcher(value).matches()) {
            throw new InvalidConfigurationValueException(String.format("Invalid configuration value %s for mask %s", value, mask));
        }
    }

    public void setHost(String host) throws InvalidConfigurationValueException {
        validate(host, HOST_MASK);
        this.host = host;
    }

    public void setHttpPort(String port) throws InvalidConfigurationValueException {
        validate(port, HTTP_PORT_MASK);
        this.httpPort = port;
    }

    public void setMaxDeviceNumber(String maxNumber) throws InvalidConfigurationValueException {
        validate(maxNumber, MAX_DEVICE_NUMBER_MASK);
        try {
            this.maxDeviceNumber = Integer.parseInt(maxNumber);
        } catch (Exception e) {
            throw new InvalidConfigurationValueException("Invalid Maximum number of devices");
        }
    }

    public String getHost() {
        return host;
    }

    public String getHttpPort() {
        return httpPort;
    }

    /**
     * 
     * @return 
     */
    @Deprecated
    public Integer getWebsocketPort() {
        return websocketPort;
    }

    public Integer getMaxDeviceNumber() {
        return maxDeviceNumber;
    }
}
