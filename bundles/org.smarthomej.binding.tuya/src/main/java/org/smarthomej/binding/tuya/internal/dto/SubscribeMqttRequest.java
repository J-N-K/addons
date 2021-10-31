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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link SubscribeMqttRequest} encapsulates the MQTT subscription request
 *
 * @author Jan N. Klug - Initial contribution
 */
public class SubscribeMqttRequest {
    public String uid;
    @SerializedName("link_id")
    public String linkId;
    @SerializedName("link_type")
    public String linkType = "mqtt";
    public String topics = "device";
    @SerializedName("msg_encrypted_version")
    public String version = "2.0";

    public SubscribeMqttRequest(String uid, String linkId) {
        this.uid = uid;
        this.linkId = linkId;
    }
}
