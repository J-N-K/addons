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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Encryption} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Encryption {
    private final Logger logger = LoggerFactory.getLogger(Encryption.class);
    private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(
            HexUtils.hexToBytes("562e17996d093d28ddb3ba695a2e6f58"));

    public static final byte[] INITIAL_KEY = HexUtils.hexToBytes("097628343fe99e23765c1513accf8b02");

    private SecretKeySpec deviceKey;

    public Encryption() {
        this.deviceKey = new SecretKeySpec(INITIAL_KEY, "AES");
    }

    public void setDeviceKey(byte[] deviceKey) {
        this.deviceKey = new SecretKeySpec(deviceKey, "AES");
    }

    public Optional<byte[]> encrypt(byte[] payload) {
        int neededPadding = 16 - payload.length % 16;
        if (neededPadding == 0) {
            return Optional.ofNullable(doEncryption(1, payload));
        }
        byte[] paddedPayload = new byte[payload.length + neededPadding];
        System.arraycopy(payload, 0, paddedPayload, 0, payload.length);
        return Optional.ofNullable(doEncryption(1, paddedPayload));
    }

    public Optional<byte[]> decrypt(byte[] payload) {
        return Optional.ofNullable(doEncryption(2, payload));
    }

    private byte @Nullable [] doEncryption(int mode, byte[] payload) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(mode, deviceKey, IV_PARAMETER_SPEC);
            return cipher.doFinal(payload);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            logger.warn("Crypt error:", e);
            return null;
        }
    }
}
