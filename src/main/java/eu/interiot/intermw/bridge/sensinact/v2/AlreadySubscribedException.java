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
package eu.interiot.intermw.bridge.sensinact.v2;

import eu.interiot.intermw.bridge.sensinact.wrapper.SNAResource;

/**
 * Exception when resource is not found with given id for given provider and service.
 * @author sb252289
 */
public class AlreadySubscribedException extends Exception {

    public AlreadySubscribedException(SNAResource resource) {
        super(String.format("already subscribed to resource %s/%s/%s", resource.getProvider(), resource.getService(), resource.getResource()));
    }
}
