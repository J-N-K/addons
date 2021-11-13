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
package org.smarthomej.binding.tuya.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link TuyaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaBindingConstants {
    private static final String BINDING_ID = "tuya";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_PROJECT = new ThingTypeUID(BINDING_ID, "project");
    public static final ThingTypeUID THING_TYPE_LIGHT = new ThingTypeUID(BINDING_ID, "light");

    public static final String PROPERTY_UUID = "uuid";
    public static final String PROPERTY_CATEGORY = "category";

    public static final String CONFIG_LOCAL_KEY = "localKey";
    public static final String CONFIG_DEVICE_ID = "deviceId";

    public static final String STORAGE_SCHEMA = "schema";

    public static final String CHANNEL_COLOR = "color";
    public static final String CHANNEL_WHITE_BRIGHTNESS = "dimmer";
    public static final String CHANNEL_WHITE_TEMPERATURE = "colortemp";
    public static final String CHANNEL_WORKMODE = "workMode";
    public static final String CHANNEL_SCENEDATA = "sceneData";

    public static final ChannelTypeUID CHANNEL_TYPE_UID_WORKMODE = new ChannelTypeUID(BINDING_ID, CHANNEL_WORKMODE);
    public static final ChannelTypeUID CHANNEL_TYPE_UID_SCENEDATA = new ChannelTypeUID(BINDING_ID, CHANNEL_SCENEDATA);
}
