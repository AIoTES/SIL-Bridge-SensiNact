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

import java.util.regex.Pattern;

public class SensinactConfig {

    private static final String hostMask=".*";
    private static final String protocolMask="(http|https)";
    private static final String versionMask="(v1|v2)";
    private static final String httpPortMask="[0-9]+";
    private static final String webSocketPortMask="[0-9]+";
    private static final String maxDeviceNumberMask="(-)*[0-9]+";

    private String protocol; //value example http,https
    private String version; //value example v1,v2
    private String host; //value example 127.0.0.1, localhost, sensinact.ddns.net
    private Integer httpPort;//value example 8080 8090
    private Integer websocketPort;//value example 8092
    private Integer maxDeviceNumber;//value example 100

    public SensinactConfig() {

    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) throws InvalidConfigurationValueException{
        validate(protocol,protocolMask);
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) throws InvalidConfigurationValueException{
        validate(version,versionMask);
        this.version = version;
    }

    private void validate(String value, String mask) throws InvalidConfigurationValueException{
        Pattern p=Pattern.compile(mask);
        if(value==null||!p.matcher(value).matches()){
            throw new InvalidConfigurationValueException(String.format("Invalid configuration value %s for mask %s",value,mask));
        }
    }

    public void setHost(String host) throws InvalidConfigurationValueException{
        validate(host,hostMask);
        this.host=host;
    }

    public void setHttpPort(String port) throws InvalidConfigurationValueException{
        validate(port,httpPortMask);
        try {
            this.httpPort=Integer.parseInt(port);
        }catch(Exception e){
            throw new InvalidConfigurationValueException("Invalid HTTP port configuration");
        }

    }

    public void setWebSocketPort(String port) throws InvalidConfigurationValueException{
        validate(port,webSocketPortMask);
        try {
            this.websocketPort=Integer.parseInt(port);
        }catch(Exception e){
            throw new InvalidConfigurationValueException("Invalid WebSocket port configuration");
        }

    }

    public void setMaxDeviceNumber(String maxNumber) throws InvalidConfigurationValueException{
        validate(maxNumber,maxDeviceNumberMask);
        try {
            this.maxDeviceNumber=Integer.parseInt(maxNumber);
        }catch(Exception e){
            throw new InvalidConfigurationValueException("Invalid Maximum number of devices");
        }

    }

    public String getHost() {
        return host;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public Integer getWebsocketPort() {
        return websocketPort;
    }

    public Integer getMaxDeviceNumber() {
        return maxDeviceNumber;
    }
}
