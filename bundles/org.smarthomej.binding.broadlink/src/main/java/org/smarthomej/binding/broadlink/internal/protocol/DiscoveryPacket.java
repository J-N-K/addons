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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.broadlink.internal.util.Util;

/**
 * The {@link DiscoveryPacket} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DiscoveryPacket {
    private ByteBuffer packet = ByteBuffer.allocate(48);

    public DiscoveryPacket(byte[] address, int port) {
        packet.order(ByteOrder.LITTLE_ENDIAN);

        // date and time
        packet.position(0x08);
        ZonedDateTime dateTime = ZonedDateTime.now();
        packet.putInt(dateTime.getOffset().getTotalSeconds());
        packet.putShort((short) dateTime.getYear());
        packet.put((byte) dateTime.getSecond());
        packet.put((byte) dateTime.getMinute());
        packet.put((byte) dateTime.getHour());
        packet.put((byte) dateTime.getDayOfWeek().getValue());
        packet.put((byte) dateTime.getDayOfMonth());
        packet.put((byte) dateTime.getMonth().getValue());

        // IP and port
        packet.position(0x18);
        packet.put(address);
        packet.putInt(port);

        packet.put(0x26, (byte) 0x06);

        packet.position(0x20);
        packet.putShort(Util.checkSum(0, packet.array()));
    }

    public DatagramPacket getDatagramPacket() throws UnknownHostException {
        byte[] bytes = new byte[48];
        packet.position(0);
        packet.get(bytes);

        return new DatagramPacket(bytes, 48, InetAddress.getByName("255.255.255.255"), 80);
    }
}
