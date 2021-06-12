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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.broadlink.internal.types.CommandCode;
import org.smarthomej.binding.broadlink.internal.util.Util;

/**
 * The {@link PacketHeader} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PacketHeader {
    public static final int HEADER_LENGTH = 56;

    // header positions
    private static final int POSITION_PACKET_CHECKSUM = 0x20;
    private static final int POSITION_ERROR_CODE = 0x22;
    private static final int POSITION_DEVICE_TYPE = 0x24;
    private static final int POSITION_COMMAND_CODE = 0x26;
    private static final int POSITION_PACKET_COUNTER = 0x28;
    private static final int POSITION_MAC_ADDRESS = 0x2a;
    private static final int POSITION_DEVICE_ID = 0x30;
    private static final int POSITION_PAYLOAD_CHECKSUM = 0x34;

    private final Logger logger = LoggerFactory.getLogger(PacketHeader.class);

    private ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH);

    public PacketHeader() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(HexUtils.hexToBytes("5aa5aa555aa5aa55"));
    }

    public PacketHeader(byte[] bytes) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(bytes, 0, HEADER_LENGTH);

        if (bytes.length > HEADER_LENGTH) {
            logger.trace("Skipped payload with length {}", bytes.length - HEADER_LENGTH);
        } else if (bytes.length < HEADER_LENGTH) {
            logger.trace("Header may be incomplete, only {} bytes set", bytes.length);
        }
    }

    public void setMacAddress(String macAddress) {
        buffer.position(POSITION_MAC_ADDRESS);
        buffer.put(Util.reverseByteArray(HexUtils.hexToBytes(macAddress, ":")));
    }

    public String getMacAddress() {
        byte[] mac = new byte[6];
        buffer.position(POSITION_MAC_ADDRESS);
        buffer.get(mac, 0, 6);
        return HexUtils.bytesToHex(Util.reverseByteArray(mac), ":");
    }

    public void setDeviceType(int deviceType) {
        buffer.putShort(POSITION_DEVICE_TYPE, (short) deviceType);
    }

    public int getDeviceType() {
        return buffer.getShort(POSITION_DEVICE_TYPE);
    }

    public void setPayloadChecksum(short payloadChecksum) {
        buffer.putShort(POSITION_PAYLOAD_CHECKSUM, payloadChecksum);
    }

    public short getPayloadChecksum() {
        return buffer.getShort(POSITION_PAYLOAD_CHECKSUM);
    }

    public void setPacketChecksum(short packetChecksum) {
        buffer.putShort(POSITION_PACKET_CHECKSUM, packetChecksum);
    }

    public short getPacketChecksum() {
        return buffer.getShort(POSITION_PACKET_CHECKSUM);
    }

    public void setErrorCode(short errorCode) {
        buffer.putShort(POSITION_ERROR_CODE, errorCode);
    }

    public short getErrorCode() {
        return buffer.getShort(POSITION_ERROR_CODE);
    }

    public void setCommandCode(CommandCode commandCode) {
        buffer.putShort(POSITION_COMMAND_CODE, commandCode.getValue());
    }

    public CommandCode getCommandCode() {
        return CommandCode.fromValue(buffer.getShort(POSITION_COMMAND_CODE));
    }

    public void setPacketCounter(short packetCounter) {
        buffer.putShort(POSITION_PACKET_COUNTER, packetCounter);
    }

    public short getPacketCounter() {
        return buffer.getShort(POSITION_PACKET_COUNTER);
    }

    public void setDeviceId(byte[] deviceId) {
        buffer.position(POSITION_DEVICE_ID);
        buffer.put(deviceId);
    }

    public byte[] getDeviceId() {
        byte[] deviceId = new byte[4];
        buffer.position(POSITION_DEVICE_ID);
        buffer.get(deviceId, 0, 4);
        return deviceId;
    }

    public PacketHeader clone() {
        return new PacketHeader(getBytes());
    }

    /**
     * get this header as byte array
     *
     * @return
     */
    public byte[] getBytes() {
        byte[] copy = new byte[HEADER_LENGTH];
        buffer.position(0);
        buffer.get(copy, 0, HEADER_LENGTH);
        return copy;
    }
}
