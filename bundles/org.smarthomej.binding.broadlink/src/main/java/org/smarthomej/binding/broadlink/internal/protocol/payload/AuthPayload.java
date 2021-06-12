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
package org.smarthomej.binding.broadlink.internal.protocol.payload;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AuthPayload} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AuthPayload {
    private final Logger logger = LoggerFactory.getLogger(AuthPayload.class);
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^\\d{15}$");
    private ByteBuffer packet = ByteBuffer.allocate(80);

    public AuthPayload() {
        this("111111111111111");
    }

    public AuthPayload(String deviceId) {
        packet.position(0x04);

        Matcher m = DEVICE_ID_PATTERN.matcher(deviceId);
        if (!m.matches()) {
            throw new IllegalArgumentException("deviceId must contain exactly 15 numeric digits");
        }
        for (byte b : deviceId.getBytes(StandardCharsets.UTF_8)) {
            packet.put(b);
        }
        packet.put((byte) 0x31);

        packet.put(0x1e, (byte) 0x01);
        packet.put(0x2d, (byte) 0x01);

        packet.position(0x30);
        for (byte b : "SmartHome/J".getBytes(StandardCharsets.UTF_8)) {
            packet.put(b);
        }
    }

    public String getBytes() {
        return HexUtils.bytesToHex(packet.array());
    }
}
