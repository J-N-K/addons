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
package org.smarthomej.binding.broadlink.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkDeviceType;

/**
 * The {@link DiscoveryTest} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DiscoveryTest {

    @Test
    public void deviceTypes() {
        assertEquals(BroadlinkDeviceType.RM3Q, BroadlinkDeviceType.fromInt(0x5f36));
        assertEquals(BroadlinkDeviceType.RM4, BroadlinkDeviceType.fromInt(0x6027));
        assertEquals(BroadlinkDeviceType.UNKNOWN, BroadlinkDeviceType.fromInt(0x1000));
    }
}
