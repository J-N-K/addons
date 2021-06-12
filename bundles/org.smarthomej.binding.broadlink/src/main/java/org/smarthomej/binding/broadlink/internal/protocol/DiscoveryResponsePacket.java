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
package org.smarthomej.binding.broadlink.internal.protocol;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.broadlink.internal.util.Util;

/**
 * The {@link DiscoveryResponsePacket} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DiscoveryResponsePacket {
    private final Logger logger = LoggerFactory.getLogger(DiscoveryResponsePacket.class);
    private String mac;
    private int deviceType;
    private String deviceName;
    private String remoteAddress;

    public DiscoveryResponsePacket(DatagramPacket packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        deviceType = buffer.getShort(0x34);

        buffer.position(0x3a);
        byte[] macBuffer = new byte[6];
        buffer.get(macBuffer, 0, 6);
        mac = HexUtils.bytesToHex(Util.reverseByteArray(macBuffer), ":");

        Integer endPosition = IntStream.range(0x40, buffer.limit()).filter(i -> buffer.get(i) == 0).findFirst()
                .orElse(-1);

        if (endPosition == -1 || endPosition == 0x40) {
            deviceName = "";
        } else {
            int length = endPosition - 0x40;
            byte[] namebuffer = new byte[length];
            buffer.position(0x40);
            buffer.get(namebuffer, 0, length);
            deviceName = new String(namebuffer);
        }

        remoteAddress = packet.getAddress().getHostAddress();
    }

    public String getMac() {
        return mac;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String toString() {
        return "DiscoveryResponsePacket{" + "mac='" + mac + "'" + ", deviceType=" + deviceType + ", deviceName='"
                + deviceName + "'" + ", remoteAddress='" + remoteAddress + "'" + "}";
    }
}
