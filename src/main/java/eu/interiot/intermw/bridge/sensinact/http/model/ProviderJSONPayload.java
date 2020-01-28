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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Map.Entry;

public class ProviderJSONPayload {

    public enum TYPE {
        Goodbye,
        Hello;
    }

    public static ProviderJSONPayloadBuilder builder() {
        return new ProviderJSONPayload.ProviderJSONPayloadBuilder();
    }

    public static class ProviderJSONPayloadBuilder {

        private JsonObject jsonObject = new JsonObject();

        private ProviderJSONPayloadBuilder() {

        }

        public ProviderJSONPayloadBuilder provider(String provider) {
            jsonObject.addProperty("provider", provider);
            return this;
        }

        public ProviderJSONPayloadBuilder service(String service) {
            jsonObject.addProperty("service", service);
            return this;
        }

        public ProviderJSONPayloadBuilder resource(String resource) {
            jsonObject.addProperty("resource", resource);
            return this;
        }

        public ProviderJSONPayloadBuilder type(String type) {
            jsonObject.addProperty("type", type);
            return this;
        }

        public ProviderJSONPayloadBuilder value(String value) {
            jsonObject.addProperty("value", value);
            return this;
        }

        public ProviderJSONPayloadBuilder timestamp(String timestamp) {
            jsonObject.addProperty("timestamp", timestamp);
            return this;
        }
        
        public ProviderJSONPayloadBuilder metadata(Map<String, String> metadata) {
            final JsonArray metadataArray = new JsonArray();
            JsonObject tempData;
            for (Entry<String, String> entry : metadata.entrySet()) {
                tempData = new JsonObject();
                tempData.addProperty("name", entry.getKey());
                tempData.addProperty("type", "string");
                tempData.addProperty("value", entry.getValue());
            }
            jsonObject.add("metadata", metadataArray);
            return this;
        }

        public ProviderJSONPayloadBuilder type(final TYPE value) {

            switch (value) {
                case Goodbye:
                    jsonObject.addProperty("type", "remove");
            }

            return this;
        }

        public JsonObject build() {
            return jsonObject;
        }

    }

}
