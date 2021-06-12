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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AuthResponsePayload} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AuthResponsePayload {
    public byte[] deviceKey = new byte[16];
    public byte[] deviceId = new byte[4];

    public AuthResponsePayload(byte[] bytes) {
        System.arraycopy(bytes, 0, deviceId, 0, 4);
        System.arraycopy(bytes, 0x04, deviceKey, 0, 16);
    }
}
