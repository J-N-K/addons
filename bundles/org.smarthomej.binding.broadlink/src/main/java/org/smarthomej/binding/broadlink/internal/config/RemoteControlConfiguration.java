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
package org.smarthomej.binding.broadlink.internal.config;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkDeviceType;

/**
 * The {@link RemoteControlConfiguration} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RemoteControlConfiguration {
    private static final Pattern MAC_PATTERN = Pattern.compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");

    public String macAddress = "";
    public String ipAddress = "";
    public int deviceType = 0;

    public boolean isValid() {
        try {
            Inet4Address.getByName(ipAddress);
        } catch (UnknownHostException e) {
            return false;
        }
        if (!MAC_PATTERN.matcher(macAddress).matches()) {
            return false;
        }
        if (BroadlinkDeviceType.fromInt(deviceType) == BroadlinkDeviceType.UNKNOWN) {
            return false;
        }
        return true;
    }
}
