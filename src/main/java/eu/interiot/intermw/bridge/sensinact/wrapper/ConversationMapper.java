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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This mapper keeps track on what device subscription is associated with what
 * conversation
 */
public class ConversationMapper {

    private Map<String, Set<String>> map = new HashMap<>();

    public void subscriptionsPut(String deviceId, String conversationId) {

        Set<String> conversations = map.get(deviceId);

        if (conversations == null) {
            conversations = new HashSet<>();
            map.put(deviceId, conversations);
        }

        conversations.add(conversationId);

    }

    public void subscriptionsPut(List<String> deviceIds, String conversationId) {

        for (String deviceId : deviceIds) {
            subscriptionsPut(deviceId, conversationId);
        }

    }

    public Set<String> subscriptionsGet(String deviceId) {
        Set<String> conversationId = map.get(deviceId);

        if (conversationId == null) {
            return Collections.emptySet();
        } else {
            return conversationId;
        }

    }

    public Set<String> subscriptionGetList(Set<String> deviceIds) {

        Set<String> conversationIds = new HashSet<String>();

        for (String deviceid : deviceIds) {
            conversationIds.addAll(subscriptionsGet(deviceid));
        };

        return conversationIds;

    }

    public void removeConversation(String conversationId) {
        for (Map.Entry<String, Set<String>> entries : map.entrySet()) {

            Set<String> removeSet = new HashSet<>();

            for (String conversation : entries.getValue()) {
                if (conversationId.equals(conversation)) {
                    removeSet.add(conversation);
                }
            }
            map.get(entries.getKey()).removeAll(removeSet);
        }

    }

}
