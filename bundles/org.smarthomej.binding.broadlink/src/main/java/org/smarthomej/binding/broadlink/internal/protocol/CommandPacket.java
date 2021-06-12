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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkPacketException;
import org.smarthomej.binding.broadlink.internal.types.CommandCode;
import org.smarthomej.binding.broadlink.internal.util.Encryption;
import org.smarthomej.binding.broadlink.internal.util.Util;

/**
 * The {@link CommandPacket} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CommandPacket {
    private final Encryption encryption;
    private PacketHeader packetHeaderTemplate;

    public CommandPacket(short deviceType, String macAddress, Encryption encryption) {
        this.encryption = encryption;

        packetHeaderTemplate = new PacketHeader();
        packetHeaderTemplate.setDeviceType(deviceType);
        packetHeaderTemplate.setMacAddress(macAddress);
    }

    public void setDeviceId(byte[] deviceId) {
        packetHeaderTemplate.setDeviceId(deviceId);
    }

    public byte[] create(CommandCode commandCode, byte[] payload) throws BroadlinkPacketException {
        byte[] encryptedPayload = encryption.encrypt(payload)
                .orElseThrow(() -> new BroadlinkPacketException("Encryption error"));

        PacketHeader packetHeader = packetHeaderTemplate.clone();
        packetHeader.setCommandCode(commandCode);
        packetHeader.setPayloadChecksum(Util.checkSum(0, payload));
        packetHeader.setPacketChecksum(Util.checkSum(0, packetHeader.getBytes(), payload));

        byte[] packet = new byte[PacketHeader.HEADER_LENGTH + encryptedPayload.length];
        System.arraycopy(packetHeader.getBytes(), 0, packet, 0, PacketHeader.HEADER_LENGTH);
        System.arraycopy(encryptedPayload, 0, packet, PacketHeader.HEADER_LENGTH, encryptedPayload.length);

        return packet;
    }
}
