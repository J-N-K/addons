/**
 * Copyright (c) 2021 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.binding.tuya.internal.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link SubscribeMqttResponse} encapsulates the MQTT Subscription response
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class SubscribeMqttResponse {
    @SerializedName("client_id")
    public String clientId = "";
    @SerializedName("expire_time")
    public int expireTime = 0;
    public String password = "";
    @SerializedName("sink_topic")
    public Topic sinkTopic = new Topic();
    @SerializedName("source_topic")
    public Topic sourceTopic = new Topic();
    public String url = "";
    public String username = "";

    @Override
    public String toString() {
        return "SubscribeMqttResponse{clientId='" + clientId + "', expireTime=" + expireTime + ", password='" + password
                + "', sinkTopic=" + sinkTopic + ", sourceTopic=" + sourceTopic + ", url='" + url + "', username='"
                + username + "'}";
    }

    public static class Topic {
        public String device = "";

        @Override
        public String toString() {
            return "Topic{device='" + device + "'}";
        }
    }
}
