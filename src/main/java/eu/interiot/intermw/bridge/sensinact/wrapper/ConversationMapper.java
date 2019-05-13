package eu.interiot.intermw.bridge.sensinact.wrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This mapper keeps track on what device subscription is associated with what conversation
 */
public class ConversationMapper {

    private Map<String,Set<String>> map=new HashMap<>();

    public void subscriptionsPut(String deviceId,String conversationId){

        Set<String> conversations=map.get(deviceId);

        if(conversations==null){
            conversations=new HashSet<>();
            map.put(deviceId,conversations);
        }

        conversations.add(conversationId);

    }

    public void subscriptionsPut(List<String> deviceIds, String conversationId){

        for(String deviceId:deviceIds){
            subscriptionsPut(deviceId,conversationId);
        }

    }

    public Set<String> subscriptionsGet(String deviceId){
        Set<String> conversationId=map.get(deviceId);

        if(conversationId==null){
            return Collections.emptySet();
        }else {
            return conversationId;
        }

    }

    public Set<String> subscriptionGetList(Set<String> deviceIds){

        Set<String> conversationIds=new HashSet<String>();

        for(String deviceid:deviceIds){
            conversationIds.addAll(subscriptionsGet(deviceid));
        };

        return conversationIds;

    }

    public void removeConversation(String conversationId){
        for(Map.Entry<String,Set<String>> entries:map.entrySet()){

            Set<String> removeSet=new HashSet<>();

            for(String conversation:entries.getValue()){
                if(conversationId.equals(conversation))
                    removeSet.add(conversation);
            }
            map.get(entries.getKey()).removeAll(removeSet);
        }

    }

}
