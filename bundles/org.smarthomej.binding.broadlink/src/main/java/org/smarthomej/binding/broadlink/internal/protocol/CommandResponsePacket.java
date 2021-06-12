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

import static org.smarthomej.binding.broadlink.internal.protocol.PacketHeader.HEADER_LENGTH;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkPacketException;
import org.smarthomej.binding.broadlink.internal.util.Encryption;
import org.smarthomej.binding.broadlink.internal.util.Util;

/**
 * The {@link CommandResponsePacket} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CommandResponsePacket {
    private final Logger logger = LoggerFactory.getLogger(CommandResponsePacket.class);

    private final PacketHeader packetHeader;
    private final byte @Nullable [] payload;

    public CommandResponsePacket(byte[] bytes, Encryption encryption) throws BroadlinkPacketException {
        packetHeader = new PacketHeader(bytes);

        short packetChecksum = packetHeader.getPacketChecksum();
        short calculatedPacketChecksum = Util.checkSum((int) (bytes[0x20] & 0xff) + (int) (bytes[0x21] & 0xff), bytes);

        if (packetChecksum != calculatedPacketChecksum) {
            logger.trace("Packet checksum = '{}', calculated = '{}'", packetChecksum, calculatedPacketChecksum);
            throw new BroadlinkPacketException("Packet checksum error");
        }

        int payloadLength = bytes.length - HEADER_LENGTH;
        if (payloadLength > 0) {
            byte[] rawPayload = new byte[bytes.length - HEADER_LENGTH];
            System.arraycopy(bytes, HEADER_LENGTH, rawPayload, 0, payloadLength);

            payload = encryption.decrypt(rawPayload)
                    .orElseThrow(() -> new BroadlinkPacketException("Decryption error"));

            short payloadChecksum = packetHeader.getPayloadChecksum();
            short calculatedPayloadChecksum = Util.checkSum(0, payload);
            if (payloadChecksum != calculatedPayloadChecksum) {
                logger.trace("Payload checksum = '{}', calculated = '{}'", payloadChecksum, calculatedPayloadChecksum);
                throw new BroadlinkPacketException("Payload checksum error");
            }
        } else {
            payload = null;
            logger.debug("No Payload found, calculated length is '{}'", payloadLength);
        }
    }

    public PacketHeader getPacketHeader() {
        return packetHeader;
    }

    public byte[] getPayload() {
        return Objects.requireNonNullElse(payload, new byte[0]);
    }

    @Override
    public String toString() {
        return "CommandResponsePacket[packetHeader='" + HexUtils.bytesToHex(packetHeader.getBytes())
                + "', decrypted payload='" + HexUtils.bytesToHex(getPayload()) + "']";
    }
}
