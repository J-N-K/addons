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
package org.smarthomej.binding.broadlink.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Util} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Util {

    private Util() {
        // prevent instantiation
    }

    public static byte[] reverseByteArray(byte[] bytes) {
        int len = bytes.length;
        byte[] reverseBytes = new byte[bytes.length];
        for (int i = 0; i < len; i++) {
            reverseBytes[i] = bytes[len - i - 1];
        }
        return reverseBytes;
    }

    /**
     * calculate checksum for one or more byte arrays
     *
     * @param bytes one or more byte arrays
     * @param
     * @return the checksum
     */
    public static short checkSum(int subtrahend, byte @Nullable []... bytes) {
        int checksum = 0xbeaf - subtrahend;

        if (bytes != null) {
            for (byte[] arr : bytes) {
                for (int i = 0; i < arr.length; i++) {
                    checksum += arr[i] & 0xff;
                }
            }
        }

        return (short) (checksum & 0xffff);
    }
}
