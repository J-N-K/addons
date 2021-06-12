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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link BroadlinkPacketException} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BroadlinkPacketException extends Exception {
    public static final long serialVersionUID = 1L;

    public BroadlinkPacketException(Throwable cause) {
        super(cause);
    }

    public BroadlinkPacketException(String message) {
        super(message);
    }
}
