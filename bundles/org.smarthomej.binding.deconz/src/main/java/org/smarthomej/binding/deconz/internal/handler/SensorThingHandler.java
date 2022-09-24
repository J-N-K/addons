/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.deconz.internal.handler;

import static org.openhab.core.library.unit.MetricPrefix.*;
import static org.openhab.core.library.unit.SIUnits.*;
import static org.openhab.core.library.unit.Units.*;
import static org.smarthomej.binding.deconz.internal.BindingConstants.*;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.smarthomej.binding.deconz.internal.dto.SensorConfig;
import org.smarthomej.binding.deconz.internal.dto.SensorState;
import org.smarthomej.binding.deconz.internal.dto.SensorUpdateConfig;

import com.google.gson.Gson;

/**
 * This sensor Thing doesn't establish any connections, that is done by the bridge Thing.
 *
 * It waits for the bridge to come online, grab the websocket connection and bridge configuration
 * and registers to the websocket connection as a listener.
 *
 * A REST API call is made to get the initial sensor state.
 *
 * Every sensor and switch is supported by this Thing, because a unified state is kept
 * in {@link #sensorState}. Every field that got received by the REST API for this specific
 * sensor is published to the framework.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class SensorThingHandler extends SensorBaseThingHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_PRESENCE_SENSOR,
            THING_TYPE_DAYLIGHT_SENSOR, THING_TYPE_POWER_SENSOR, THING_TYPE_CONSUMPTION_SENSOR, THING_TYPE_LIGHT_SENSOR,
            THING_TYPE_PRESSURE_SENSOR, THING_TYPE_SWITCH, THING_TYPE_OPENCLOSE_SENSOR, THING_TYPE_WATERLEAKAGE_SENSOR,
            THING_TYPE_ALARM_SENSOR, THING_TYPE_BATTERY_SENSOR, THING_TYPE_CARBONMONOXIDE_SENSOR,
            THING_TYPE_AIRQUALITY_SENSOR, THING_TYPE_COLOR_CONTROL, THING_TYPE_MOISTURE_SENSOR);

    public static final Set<ThingTypeUID> DEPRECATED_THING_TYPES = Set.of(THING_TYPE_FIRE_SENSOR,
            THING_TYPE_HUMIDITY_SENSOR, THING_TYPE_TEMPERATURE_SENSOR, THING_TYPE_LIGHT_SENSOR,
            THING_TYPE_VIBRATION_SENSOR);
    private static final List<String> CONFIG_CHANNELS = List.of(CHANNEL_BATTERY_LEVEL, CHANNEL_BATTERY_LOW,
            CHANNEL_ENABLED, CHANNEL_TEMPERATURE);

    public SensorThingHandler(Thing thing, Gson gson) {
        super(thing, gson);
    }

    @Override
    public void initialize() {
        if (DEPRECATED_THING_TYPES.contains(thing.getThingTypeUID())) {
            changeThingType(THING_TYPE_GENERIC_SENSOR, getConfig());
        } else {
            super.initialize();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            sensorState.remove("buttonevent");
            valueUpdated(channelUID, sensorState, false);
            return;
        }
        switch (channelUID.getId()) {
            case CHANNEL_ENABLED:
                if (command instanceof OnOffType) {
                    SensorUpdateConfig newConfig = new SensorUpdateConfig();
                    newConfig.on = OnOffType.ON.equals(command);
                    sendCommand(newConfig, command, channelUID, null);
                }
                break;
        }
    }

    @Override
    protected void valueUpdated(ChannelUID channelUID, SensorConfig newConfig) {
        super.valueUpdated(channelUID, newConfig);
        switch (channelUID.getId()) {
            case CHANNEL_ENABLED:
                updateState(channelUID, OnOffType.from(newConfig.on));
                break;
            case CHANNEL_TEMPERATURE:
                Float temperature = newConfig.temperature;
                if (temperature != null) {
                    updateState(channelUID, new QuantityType<>(temperature / 100, CELSIUS));
                }
                break;
        }
    }

    @Override
    protected void valueUpdated(ChannelUID channelUID, SensorState newState, boolean initializing) {
        super.valueUpdated(channelUID, newState, initializing);
        switch (channelUID.getId()) {
            case CHANNEL_BATTERY_LEVEL:
                updateDecimalTypeChannel(channelUID, newState.get("battery"));
                break;
            case CHANNEL_LIGHT:
                Boolean dark = (Boolean) newState.get("dark");
                if (dark != null) {
                    Boolean daylight = (Boolean) newState.get("daylight");
                    if (dark) { // if it's dark, it's dark ;)
                        updateState(channelUID, new StringType("Dark"));
                    } else if (daylight != null) { // if its not dark, it might be between darkness and daylight
                        if (daylight) {
                            updateState(channelUID, new StringType("Daylight"));
                        } else {
                            updateState(channelUID, new StringType("Sunset"));
                        }
                    } else { // if no daylight value is known, we assume !dark means daylight
                        updateState(channelUID, new StringType("Daylight"));
                    }
                }
                break;
            case CHANNEL_POWER:
                updateQuantityTypeChannel(channelUID, newState.get("power"), WATT);
                break;
            case CHANNEL_CONSUMPTION:
                updateQuantityTypeChannel(channelUID, newState.get("consumption"), WATT_HOUR);
                break;
            case CHANNEL_VOLTAGE:
                updateQuantityTypeChannel(channelUID, newState.get("voltage"), VOLT);
                break;
            case CHANNEL_CURRENT:
                updateQuantityTypeChannel(channelUID, newState.get("current"), MILLI(AMPERE));
                break;
            case CHANNEL_LIGHT_LUX:
                updateQuantityTypeChannel(channelUID, newState.get("lux"), LUX);
                break;
            case CHANNEL_COLOR:
                final int @Nullable [] xy = (int[]) newState.get("xy");
                if (xy != null && xy.length == 2) {
                    updateState(channelUID, HSBType.fromXY((float) xy[0], (float) xy[1]));
                }
                break;
            case CHANNEL_LIGHT_LEVEL:
                updateDecimalTypeChannel(channelUID, newState.get("lightlevel"));
                break;
            case CHANNEL_DARK:
                updateSwitchChannel(channelUID, newState.get("dark"));
                break;
            case CHANNEL_DAYLIGHT:
                updateSwitchChannel(channelUID, newState.get("daylight"));
                break;
            case CHANNEL_TEMPERATURE:
                updateQuantityTypeChannel(channelUID, newState.get("temperature"), CELSIUS, 1.0 / 100);
                break;
            case CHANNEL_HUMIDITY:
                updateQuantityTypeChannel(channelUID, newState.get("humidity"), PERCENT, 1.0 / 100);
                break;
            case CHANNEL_PRESSURE:
                updateQuantityTypeChannel(channelUID, newState.get("pressure"), HECTO(PASCAL));
                break;
            case CHANNEL_PRESENCE:
                updateSwitchChannel(channelUID, newState.get("presence"));
                break;
            case CHANNEL_VALUE:
                updateDecimalTypeChannel(channelUID, newState.get("status"));
                break;
            case CHANNEL_OPENCLOSE:
                Boolean open = (Boolean) newState.get("open");
                if (open != null) {
                    updateState(channelUID, open ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
                }
                break;
            case CHANNEL_WATERLEAKAGE:
                updateSwitchChannel(channelUID, newState.get("water"));
                break;
            case CHANNEL_FIRE:
                updateSwitchChannel(channelUID, newState.get("fire"));
                break;
            case CHANNEL_ALARM:
                updateSwitchChannel(channelUID, newState.get("alarm"));
                break;
            case CHANNEL_TAMPERED:
                updateSwitchChannel(channelUID, newState.get("tampered"));
                break;
            case CHANNEL_VIBRATION:
                updateSwitchChannel(channelUID, newState.get("vibration"));
                break;
            case CHANNEL_CARBONMONOXIDE:
                updateSwitchChannel(channelUID, newState.get("carbonmonoxide"));
                break;
            case CHANNEL_AIRQUALITY:
                String airquality = (String) newState.get("airquality");
                if (airquality != null) {
                    updateState(channelUID, new StringType(airquality));
                }
                break;
            case CHANNEL_AIRQUALITYPPB:
                updateQuantityTypeChannel(channelUID, newState.get("airqualityppb"), PARTS_PER_BILLION);
                break;
            case CHANNEL_MOISTURE:
                updateQuantityTypeChannel(channelUID, newState.get("moisture"), PERCENT);
                break;
            case CHANNEL_BUTTON:
                updateDecimalTypeChannel(channelUID, newState.get("buttonevent"));
                break;
            case CHANNEL_BUTTONEVENT:
                Integer buttonevent = (Integer) newState.get("buttonevent");
                if (buttonevent != null && !initializing) {
                    triggerChannel(channelUID, String.valueOf(buttonevent));
                }
                break;
            case CHANNEL_GESTURE:
                updateDecimalTypeChannel(channelUID, newState.get("gesture"));
                break;
            case CHANNEL_GESTUREEVENT:
                Integer gesture = (Integer) newState.get("gesture");
                if (gesture != null && !initializing) {
                    triggerChannel(channelUID, String.valueOf(gesture));
                }
                break;
        }
    }

    @Override
    protected boolean createTypeSpecificChannels(ThingBuilder thingBuilder, SensorConfig sensorConfig,
            SensorState sensorState) {
        boolean thingEdited = false;

        // some Xiaomi sensors
        if (sensorConfig.temperature != null && createChannel(thingBuilder, CHANNEL_TEMPERATURE, ChannelKind.STATE)) {
            thingEdited = true;
        }

        // ZHAPresence - e.g. IKEA TRÅDFRI motion sensor
        if (sensorState.containsKey("dark") && createChannel(thingBuilder, CHANNEL_DARK, ChannelKind.STATE)) {
            thingEdited = true;
        }

        // ZHAConsumption - e.g Bitron 902010/25 or Heiman SmartPlug
        if (sensorState.containsKey("power") && createChannel(thingBuilder, CHANNEL_POWER, ChannelKind.STATE)) {
            thingEdited = true;
        }
        // ZHAConsumption - e.g. Linky devices second channel
        if (sensorState.containsKey("consumption2")
                && createChannel(thingBuilder, CHANNEL_CONSUMPTION_2, ChannelKind.STATE)) {
            thingEdited = true;
        }

        // ZHAPower - e.g. Heiman SmartPlug
        if (sensorState.containsKey("voltage") && createChannel(thingBuilder, CHANNEL_VOLTAGE, ChannelKind.STATE)) {
            thingEdited = true;
        }
        if (sensorState.containsKey("current") && createChannel(thingBuilder, CHANNEL_CURRENT, ChannelKind.STATE)) {
            thingEdited = true;
        }

        // IAS Zone sensor - e.g. Heiman HS1MS motion sensor
        if (sensorState.containsKey("tampered") && createChannel(thingBuilder, CHANNEL_TAMPERED, ChannelKind.STATE)) {
            thingEdited = true;
        }

        // e.g. Aqara Cube
        if (sensorState.containsKey("gesture") && (createChannel(thingBuilder, CHANNEL_GESTURE, ChannelKind.STATE)
                || createChannel(thingBuilder, CHANNEL_GESTUREEVENT, ChannelKind.TRIGGER))) {
            thingEdited = true;
        }

        return thingEdited;
    }

    @Override
    protected List<String> getConfigChannels() {
        return CONFIG_CHANNELS;
    }
}
