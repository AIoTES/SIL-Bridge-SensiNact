package eu.interiot.intermw.bridge.sensinact.http.model;

import com.google.gson.JsonObject;

public class ProviderJSONPayload {

    public enum TYPE {
        Goodbye,
        Hello;
    }

    public static ProviderJSONPayloadBuilder builder(){
        return new ProviderJSONPayload.ProviderJSONPayloadBuilder();
    }

    public static class ProviderJSONPayloadBuilder {

        private JsonObject jsonObject=new JsonObject();

        private ProviderJSONPayloadBuilder(){

        }

        public ProviderJSONPayloadBuilder provider(String provider){
            jsonObject.addProperty("provider",provider);
            return this;
        }

        public ProviderJSONPayloadBuilder service(String service){
            jsonObject.addProperty("service",service);
            return this;
        }

        public ProviderJSONPayloadBuilder resource(String resource){
            jsonObject.addProperty("resource",resource);
            return this;
        }

        public ProviderJSONPayloadBuilder value(String value){
            jsonObject.addProperty("value",value);
            return this;
        }

        public ProviderJSONPayloadBuilder type(final TYPE value){

            switch(value){
                case Goodbye:
                    jsonObject.addProperty("type","remove");
            }

            return this;
        }

        public JsonObject build(){
            return jsonObject;
        }

    }

}
