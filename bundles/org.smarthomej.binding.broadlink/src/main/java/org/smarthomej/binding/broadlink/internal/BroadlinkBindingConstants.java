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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link BroadlinkBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BroadlinkBindingConstants {
    public static final String BINDING_ID = "broadlink";

    // List of all Thing Type UIDs

    public static final ThingTypeUID THING_TYPE_UID_RM = new ThingTypeUID(BINDING_ID, "rm");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Set.of(THING_TYPE_UID_RM);

    // configuration parameters
    public static final String CONFIGURATION_MAC_ADDRESS = "macAddress";
    public static final String CONFIGURATION_IP_ADDRESS = "ipAddress";
    public static final String CONFIGURATION_DEVICE_TYPE = "deviceType";

    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_DEVICE_KEY = "deviceKey";

    public static final ChannelTypeUID TEMPERATURE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "temperature");
    public static final ChannelTypeUID HUMIDITY_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "humidity");

    public static final String CHANNEL_COMMAND = "command";
    public static final String TEMPERATURE_CHANNEL = "temperature";
    public static final String HUMIDITY_CHANNEL = "humidity";
}
