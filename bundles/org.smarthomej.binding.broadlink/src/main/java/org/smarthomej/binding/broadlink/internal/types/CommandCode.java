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
package org.smarthomej.binding.broadlink.internal.types;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link CommandCode} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public enum CommandCode {
    AUTH(0x65),
    COMMAND(0x6a),
    UNKNOWN(0xff);

    short commandCode;

    CommandCode(int commandCode) {
        this.commandCode = (short) commandCode;
    }

    public short getValue() {
        return this.commandCode;
    }

    public static CommandCode fromValue(short value) {
        return Objects.requireNonNull(
                Arrays.stream(CommandCode.values()).filter(v -> v.commandCode == value).findFirst().orElse(UNKNOWN));
    }
}
