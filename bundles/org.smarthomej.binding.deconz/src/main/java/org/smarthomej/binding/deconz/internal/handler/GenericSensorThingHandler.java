/**
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

import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.smarthomej.binding.deconz.internal.BindingConstants.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.deconz.internal.ChannelUpdater;
import org.smarthomej.binding.deconz.internal.dto.DeconzBaseMessage;
import org.smarthomej.binding.deconz.internal.dto.SensorConfig;
import org.smarthomej.binding.deconz.internal.dto.SensorMessage;
import org.smarthomej.binding.deconz.internal.dto.SensorState;
import org.smarthomej.binding.deconz.internal.dto.SensorUpdateConfig;
import org.smarthomej.binding.deconz.internal.types.ChannelInfo;
import org.smarthomej.binding.deconz.internal.types.ResourceType;

import com.google.gson.Gson;

/**
 * The sensor {@link GenericSensorThingHandler} automatically detects available channels based on the information
 * received on initialization and updates the thing accordingly. After initialization, it forwards commands to the
 * bridge or states to the framework.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GenericSensorThingHandler extends DeconzBaseThingHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_GENERIC_SENSOR);

    private static final List<String> CONFIG_CHANNELS = List.of(CHANNEL_BATTERY_LEVEL, CHANNEL_BATTERY_LOW,
            CHANNEL_ENABLED, CHANNEL_TEMPERATURE);

    private final Logger logger = LoggerFactory.getLogger(GenericSensorThingHandler.class);

    private SensorState sensorState = new SensorState();
    private SensorConfig sensorConfig = new SensorConfig();

    private boolean ignoreConfigurationUpdate = false;

    public GenericSensorThingHandler(Thing thing, Gson gson) {
        super(thing, gson, ResourceType.SENSORS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            sensorState.remove("buttonevent");
            // TODO: handle refresh
            //valueUpdated(channelUID, sensorState, false);
            return;
        }
        if (CHANNEL_ENABLED.equals(channelUID.getId())) {
            if (command instanceof OnOffType) {
                SensorUpdateConfig newConfig = new SensorUpdateConfig();
                newConfig.on = OnOffType.ON.equals(command);
                sendCommand(newConfig, command, channelUID, null);
            }
        }
    }

    protected void processStateResponse(DeconzBaseMessage stateResponse) {
        if (!(stateResponse instanceof SensorMessage)) {
            return;
        }

        SensorMessage sensorMessage = (SensorMessage) stateResponse;
        sensorConfig = Objects.requireNonNullElse(sensorMessage.config, new SensorConfig());
        sensorState = Objects.requireNonNullElse(sensorMessage.state, new SensorState());

        // Add some information about the sensor
        if (!sensorConfig.reachable) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Not reachable");
            return;
        }

        Map<String, String> editProperties = editProperties();
        editProperties.put(UNIQUE_ID, sensorMessage.uniqueid);
        editProperties.put(Thing.PROPERTY_FIRMWARE_VERSION, sensorMessage.swversion);
        editProperties.put(Thing.PROPERTY_VENDOR, sensorMessage.manufacturername);
        editProperties.put(Thing.PROPERTY_MODEL_ID, sensorMessage.modelid);

        ignoreConfigurationUpdate = true;

        updateProperties(editProperties);

        // Some sensors support optional channels
        // (see https://github.com/dresden-elektronik/deconz-rest-plugin/wiki/Supported-Devices#sensors)
        // any battery-powered sensor
        ThingBuilder thingBuilder = editThing();
        boolean thingEdited = false;

        if (sensorConfig.battery != null) {
            if (createChannel(thingBuilder, CHANNEL_BATTERY_LEVEL, ChannelKind.STATE)) {
                thingEdited = true;
            }
            if (createChannel(thingBuilder, CHANNEL_BATTERY_LOW, ChannelKind.STATE)) {
                thingEdited = true;
            }
        }

        if (sensorState.containsKey("lowbattery") && sensorConfig.battery == null) {
            // if sensorConfig.battery != null the channel is already added
            if (createChannel(thingBuilder, CHANNEL_BATTERY_LOW, ChannelKind.STATE)) {
                thingEdited = true;
            }
        }

        if (checkAndCreateChannels(thingBuilder, sensorConfig, sensorState)) {
            thingEdited = true;
        }

        if (checkLastSeen(thingBuilder, sensorMessage.lastseen)) {
            thingEdited = true;
        }

        // if the thing was edited, we update it now
        if (thingEdited) {
            logger.debug("Thing configuration changed, updating thing.");
            updateThing(thingBuilder.build());
        }
        ignoreConfigurationUpdate = false;

        // Initial data
        updateChannels(sensorConfig);
        updateChannels(sensorState, true);

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void messageReceived(DeconzBaseMessage message) {
        logger.error("{} received {}", thing.getUID(), message);
        if (message instanceof SensorMessage) {
            SensorMessage sensorMessage = (SensorMessage) message;
            SensorConfig sensorConfig = sensorMessage.config;
            if (sensorConfig != null) {
                if (sensorConfig.reachable) {
                    updateStatus(ThingStatus.ONLINE);
                    updateChannels(sensorConfig);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Not reachable");
                }
            }
            SensorState sensorState = sensorMessage.state;
            if (sensorState != null) {
                updateChannels(sensorState, false);
            }

        }
    }

    private void updateChannels(SensorState newState, boolean initializing) {
        sensorState = newState;
        newState.forEach((k, v) -> valueUpdated(k, v, initializing));
    }

    private void updateChannels(SensorConfig newConfig) {
        this.sensorConfig = newConfig;
        thing.getChannels().stream().map(Channel::getUID)
                .filter(channelUID -> CONFIG_CHANNELS.contains(channelUID.getId()))
                .forEach((channelUID) -> valueUpdated(channelUID, newConfig));
    }

    protected void valueUpdated(ChannelUID channelUID, SensorConfig newConfig) {
        Integer batteryLevel = newConfig.battery;

        switch (channelUID.getId()) {
            case CHANNEL_BATTERY_LEVEL:
                if (batteryLevel != null) {
                    updateState(channelUID, new DecimalType(batteryLevel.longValue()));
                }
                break;
            case CHANNEL_BATTERY_LOW:
                if (batteryLevel != null) {
                    updateState(channelUID, OnOffType.from(batteryLevel <= 10));
                }
                break;
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

    private void valueUpdated(String key, @Nullable Object value, boolean initializing) {
        ChannelInfo channelInfo = SENSOR_CHANNEL_MAP.get(key);
        if (value != null && channelInfo != null) {
            if ("TriggerChannelConverter".equals(channelInfo.converter)) {
                if (!initializing) {
                    // trigger only if we are not initializing the thing
                    triggerChannel(channelInfo.channelId, value.toString());
                }
            } else {
                ChannelUpdater.get(channelInfo.converter).ifPresent(c -> c.update(channelInfo, value, this::updateState));
            }
        }
    }

    private boolean checkAndCreateChannels(ThingBuilder thingBuilder, SensorConfig sensorConfig,
            SensorState sensorState) {
        boolean thingEdited = false;

        // some Xiaomi sensors
        if (sensorConfig.temperature != null && createChannel(thingBuilder, CHANNEL_TEMPERATURE, ChannelKind.STATE)) {
            thingEdited = true;
        }

        for (String stateKey : sensorState.keySet()) {
            ChannelInfo channelInfo = SENSOR_CHANNEL_MAP.get(stateKey);
            if (channelInfo != null) {
                if (createChannel(thingBuilder, channelInfo.channelId, new ChannelTypeUID(channelInfo.channelTypeUID),
                        ChannelKind.STATE)) {
                    thingEdited = true;
                }
            }
        }

        if (sensorState.containsKey("gesture") && (createChannel(thingBuilder, CHANNEL_GESTURE, ChannelKind.STATE)
                || createChannel(thingBuilder, CHANNEL_GESTUREEVENT, ChannelKind.TRIGGER))) {
            thingEdited = true;
        }

        if (sensorState.containsKey("button") && (createChannel(thingBuilder, CHANNEL_BUTTONEVENT, ChannelKind.TRIGGER))) {
            thingEdited = true;
        }

        return thingEdited;
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        if (!ignoreConfigurationUpdate) {
            super.handleConfigurationUpdate(configurationParameters);
        }
    }
}
