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
package org.smarthomej.binding.tuya.internal.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CryptoUtil} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CryptoUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtil.class);

    private CryptoUtil() {
        // prevent instantiation
    }

    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.getBytes(StandardCharsets.UTF_8));
            return HexUtils.bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Algorithm SHA-256 not found. This should never happen. Check your Java setup.");
        }
        return "";
    }

    public static String md5(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data.getBytes(StandardCharsets.UTF_8));
            return HexUtils.bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Algorithm MD5 not found. This should never happen. Check your Java setup.");
        }
        return "";
    }

    public static String hmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            return HexUtils.bytesToHex(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            //
        }
        return "";
    }

    public static final int GCM_TAG_LENGTH = 16;

    public static Optional<String> decrypt(String msg, String password, long t) {
        try {
            byte[] rawBuffer = Base64.getDecoder().decode(msg);
            // first four bytes are IV length
            int ivLength = rawBuffer[0] << 24 | (rawBuffer[1] & 0xFF) << 16 | (rawBuffer[2] & 0xFF) << 8
                    | (rawBuffer[3] & 0xFF);
            // data length is full length without IV length and IV
            int dataLength = rawBuffer.length - 4 - ivLength;
            SecretKey secretKey = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), 8, 16, "AES");
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, rawBuffer, 4, ivLength);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] aad = new byte[] { 0x00, 0x00, (byte) ((t >> 24) & 0xff), (byte) ((t >> 16) & 0xff),
                    (byte) ((t >> 8) & 0xff), (byte) (t & 0xff) };
            cipher.updateAAD(aad);
            byte[] decoded = cipher.doFinal(rawBuffer, 4 + ivLength, dataLength);
            return Optional.of(new String(Arrays.copyOfRange(decoded, 0, dataLength - GCM_TAG_LENGTH)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.warn("Decryption of MQ failed: {}", e.getMessage());
        }

        return Optional.empty();
    }
}
