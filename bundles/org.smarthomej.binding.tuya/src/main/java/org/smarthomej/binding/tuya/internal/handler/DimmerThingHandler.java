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
package org.smarthomej.binding.tuya.internal.handler;

import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_DIMMER_DIMMER_1;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_DIMMER_DIMMER_2;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_DIMMER_TYPE_1;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_DIMMER_TYPE_2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.api.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.dto.CommandRequest;
import org.smarthomej.binding.tuya.internal.dto.DeviceSchema;
import org.smarthomej.binding.tuya.internal.dto.StatusInfo;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

import com.google.gson.Gson;

/**
 * The {@link DimmerThingHandler} handles commands and state updates for dimmers
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DimmerThingHandler extends AbstractTuyaThingHandler {
    private final Logger logger = LoggerFactory.getLogger(DimmerThingHandler.class);
    private final SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider;

    public DimmerThingHandler(Thing thing, Gson gson, StorageService storageService,
            SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        super(thing, gson, storageService);
        this.dynamicCommandDescriptionProvider = dynamicCommandDescriptionProvider;
    }

    @Override
    public void processStatusMessage(StatusInfo status) {
        logger.trace("'{}' received status message '{}'", thing.getUID(), status);
        if ("switch_led_1".equals(status.code)) {
            updateState(CHANNEL_DIMMER_DIMMER_1, OnOffType.from("true".equals(status.value)));
        } else if ("bright_value_1".equals(status.code)) {
            PercentType newState = new PercentType(
                    new BigDecimal(status.value).divide(new BigDecimal(10), RoundingMode.HALF_UP));
            updateState(CHANNEL_DIMMER_DIMMER_1, newState);
        } else if ("switch_led_2".equals(status.code)) {
            updateState(CHANNEL_DIMMER_DIMMER_2, OnOffType.from("true".equals(status.value)));
        } else if ("bright_value_2".equals(status.code)) {
            PercentType newState = new PercentType(
                    new BigDecimal(status.value).divide(new BigDecimal(10), RoundingMode.HALF_UP));
            updateState(CHANNEL_DIMMER_DIMMER_2, newState);
        }
    }

    @Override
    protected void processSchema(DeviceSchema schema) {
        schema.functions.stream().filter(fcn -> "led_type_1".equals(fcn.code)).findAny().ifPresent(workModes -> {
            DeviceSchema.Range range = Objects
                    .requireNonNull(gson.fromJson(workModes.values, DeviceSchema.Range.class));
            ChannelUID channelUID = new ChannelUID(thing.getUID(), CHANNEL_DIMMER_TYPE_1);
            dynamicCommandDescriptionProvider.setCommandOptions(channelUID, toCommandOptionList(range.range));
        });
        schema.functions.stream().filter(fcn -> "led_type_2".equals(fcn.code)).findAny().ifPresent(workModes -> {
            DeviceSchema.Range range = Objects
                    .requireNonNull(gson.fromJson(workModes.values, DeviceSchema.Range.class));
            ChannelUID channelUID = new ChannelUID(thing.getUID(), CHANNEL_DIMMER_TYPE_2);
            dynamicCommandDescriptionProvider.setCommandOptions(channelUID, toCommandOptionList(range.range));
        });
    }

    @Override
    protected void checkThing(DeviceSchema schema, ThingBuilder thingBuilder, ThingHandlerCallback callback) {
        ThingUID thingUID = thing.getUID();
        boolean changed = false;

        if (schema.hasFunction("switch_led_1") || schema.hasFunction("bright_value_1")) {
            if (thing.getChannel(CHANNEL_DIMMER_DIMMER_1) == null) {
                ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_DIMMER_DIMMER_1);
                Channel channel = callback.createChannelBuilder(channelUID, SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS).build();
                thingBuilder.withChannel(channel);
                changed = true;
            }
        }

        if (schema.hasFunction("switch_led_2") || schema.hasFunction("bright_value_2")) {
            if (thing.getChannel(CHANNEL_DIMMER_DIMMER_2) == null) {
                ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_DIMMER_DIMMER_2);
                Channel channel = callback.createChannelBuilder(channelUID, SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS).build();
                thingBuilder.withChannel(channel);
                changed = true;
            }
        }

        if (changed) {
            updateThing(thingBuilder.build());
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        TuyaOpenAPI api = this.api;
        if (api == null) {
            logger.warn("Command '{}' to channel '{}' ignored: API not available", command, channelUID);
            return;
        }

        CommandRequest commandRequest = null;
        String channelId = channelUID.getId();
        if (CHANNEL_DIMMER_DIMMER_1.equals(channelId)) {
            if (command instanceof OnOffType) {
                commandRequest = new CommandRequest(List.of( //
                        new CommandRequest.Command<>("switch_led_1", OnOffType.ON.equals(command))));
            } else if (command instanceof PercentType) {
                double value = ((PercentType) command).doubleValue();
                commandRequest = new CommandRequest(List.of( //
                        new CommandRequest.Command<>("bright_value_1", (int) Math.max(value * 10.0, 10.0)), //
                        new CommandRequest.Command<>("switch_led_1", value > 0.0)));
            }
        } else if (CHANNEL_DIMMER_DIMMER_2.equals(channelId)) {
            if (command instanceof OnOffType) {
                commandRequest = new CommandRequest(List.of( //
                        new CommandRequest.Command<>("switch_led_2", OnOffType.ON.equals(command))));
            } else if (command instanceof PercentType) {
                double value = ((PercentType) command).doubleValue();
                commandRequest = new CommandRequest(List.of( //
                        new CommandRequest.Command<>("bright_value_2", (int) Math.max(value * 10.0, 10.0)), //
                        new CommandRequest.Command<>("switch_led_2", value > 0.0)));
            }
        }

        if (commandRequest != null) {
            api.sendCommand(configuration.deviceId, commandRequest).handle((result, t) -> {
                if (t != null) {
                    logger.warn("Command '{}' to channel '{}' failed: {}", command, channelUID, t.getMessage());
                }
                return null;
            });
        }
    }
}
