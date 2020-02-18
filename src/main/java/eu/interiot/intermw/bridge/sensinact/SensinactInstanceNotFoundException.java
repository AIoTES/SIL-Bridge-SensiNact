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
package eu.interiot.intermw.bridge.sensinact;


/**
 * Exception when sensiNact instance is not found with given id.
 * @author sb252289
 */
public class SensinactInstanceNotFoundException extends Exception {

    private final String name;
    private final String baseEndpoint;
    
    public SensinactInstanceNotFoundException(final String name) {
        this(name, "undefined base endpoint");
    }
    
    public SensinactInstanceNotFoundException(final String name, final String baseEndpoint) {
        super(String.format("sensiNact instance not found %s - %s", name, baseEndpoint));
        this.name = name;
        this.baseEndpoint = baseEndpoint;
    }
    
    protected String getSensinactInstanceName() {
        return this.name;
    }
    
    protected String getBaseEndpoint() {
        return this.baseEndpoint;
    }
}
