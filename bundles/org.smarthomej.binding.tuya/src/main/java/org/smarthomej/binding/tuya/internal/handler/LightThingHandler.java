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
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_COLOR;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_COLOR;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_TYPE_UID_WORKMODE;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_WHITE_BRIGHTNESS;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_WHITE_TEMPERATURE;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CHANNEL_WORKMODE;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.STORAGE_SCHEMA;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandOption;
import org.openhab.core.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.api.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.dto.CommandRequest;
import org.smarthomej.binding.tuya.internal.dto.DeviceSchema;
import org.smarthomej.binding.tuya.internal.dto.mq.MqMessage;
import org.smarthomej.binding.tuya.internal.dto.types.ColorValue;
import org.smarthomej.binding.tuya.internal.util.DeviceConfiguration;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

import com.google.gson.Gson;

/**
 * The {@link LightThingHandler} handles commands for lights
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class LightThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(LightThingHandler.class);

    private final Storage<String> storage;
    private final Gson gson;
    private final SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider;
    private @Nullable TuyaOpenAPI api;

    private DeviceConfiguration configuration = new DeviceConfiguration();
    private @Nullable DeviceSchema schema;

    private String colorCommand = "colour_data";
    private String onOffCommand = "switch_led";
    private String brightnessCommand = "bright_value";
    private String temperatureCommand = "temp_value";

    public LightThingHandler(Thing thing, Gson gson, StorageService storageService,
            SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        super(thing);
        this.gson = gson;
        this.storage = storageService.getStorage(UIDUtils.encode(thing.getUID().toString()));
        this.dynamicCommandDescriptionProvider = dynamicCommandDescriptionProvider;
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(DeviceConfiguration.class);

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge not found");
            return;
        }

        schema = gson.fromJson(storage.get(STORAGE_SCHEMA), DeviceSchema.class);

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        bridgeStatusChanged(bridge.getStatusInfo());
    }

    public String getDeviceId() {
        return configuration.deviceId;
    }

    public void processStatusMessage(MqMessage.Status status) {
        logger.trace("'{}' received status message '{}'", thing.getUID(), status);
        if ("colour_data".equals(status.code)) {
            ColorValue colorValue = Objects.requireNonNull(gson.fromJson(status.value, ColorValue.class));
            DecimalType hue = new DecimalType(colorValue.hue);
            PercentType sat = new PercentType(new BigDecimal(colorValue.saturation / 10.0));
            PercentType brightness = new PercentType(new BigDecimal(colorValue.brightness / 10.0));
            updateState(CHANNEL_COLOR, new HSBType(hue, sat, brightness));
        } else if ("switch_led".equals(status.code)) {
            updateState(CHANNEL_COLOR, OnOffType.from("true".equals(status.value)));
        } else if ("bright_value".equals(status.code)) {
            PercentType newState = new PercentType(
                    new BigDecimal(status.value).divide(new BigDecimal(10), RoundingMode.HALF_UP));
            updateState(CHANNEL_WHITE_BRIGHTNESS, newState);
        } else if ("temp_value".equals(status.code)) {
            PercentType newState = new PercentType(new BigDecimal(100)
                    .subtract(new BigDecimal(status.value).divide(new BigDecimal(10), RoundingMode.HALF_UP)));
            updateState(CHANNEL_WHITE_TEMPERATURE, newState);
        } else if ("work_mode".equals(status.code)) {
            updateState(CHANNEL_WORKMODE, new StringType(status.value));
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge is missing");
                return;
            }
            ProjectHandler bridgeHandler = (ProjectHandler) bridge.getHandler();
            if (bridgeHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge has no handler");
                return;
            }
            api = bridgeHandler.getApi();
            if (api == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Api missing");
                return;
            }
            DeviceSchema schema = this.schema;
            if (schema == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device schema missing");
                scheduler.execute(this::updateSchema);
                return;
            }
            processSchema(schema);
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            api = null;
        }
    }

    private void processSchema(DeviceSchema schema) {
        if (schema.hasFunction("colour_data_v2")) {
            colorCommand = "colour_data_v2";
        }
        if (schema.hasFunction("bright_value_v2")) {
            brightnessCommand = "bright_value_v2";
        }
        schema.functions.stream().filter(fcn -> "work_mode".equals(fcn.code)).findAny().ifPresent(workModes -> {
            DeviceSchema.Range range = Objects
                    .requireNonNull(gson.fromJson(workModes.values, DeviceSchema.Range.class));
            ChannelUID channelUID = new ChannelUID(thing.getUID(), CHANNEL_WORKMODE);
            dynamicCommandDescriptionProvider.setCommandOptions(channelUID, toCommandOptionList(range.range));
        });
    }

    protected List<CommandOption> toCommandOptionList(List<String> options) {
        return options.stream().map(c -> new CommandOption(c, c)).collect(Collectors.toList());
    }

    private void checkThing() {
        DeviceSchema schema = this.schema;
        if (schema == null) {
            return;
        }

        ThingBuilder thingBuilder = editThing();
        ThingHandlerCallback callback = getCallback();
        ThingUID thingUID = thing.getUID();
        boolean changed = false;

        if (callback == null) {
            return;
        }

        if (schema.hasFunction("colour_data")) {
            if (thing.getChannel(CHANNEL_COLOR) == null) {
                ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_COLOR);
                Channel channel = callback.createChannelBuilder(channelUID, SYSTEM_CHANNEL_TYPE_UID_COLOR).build();
                thingBuilder.withChannel(channel);
                changed = true;
            }
        }

        if (schema.hasFunction("bright_value")) {
            if (thing.getChannel(CHANNEL_WHITE_BRIGHTNESS) == null) {
                ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_WHITE_BRIGHTNESS);
                Channel channel = callback.createChannelBuilder(channelUID, SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS).build();
                thingBuilder.withChannel(channel);
                changed = true;
            }
        }

        if (schema.hasFunction("temp_value")) {
            ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_WHITE_TEMPERATURE);
            Channel channel = callback.createChannelBuilder(channelUID, SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE)
                    .build();
            thingBuilder.withChannel(channel);
            changed = true;
        }

        if (schema.hasFunction("work_mode")) {
            if (thing.getChannel(CHANNEL_WORKMODE) == null) {
                ChannelUID channelUID = new ChannelUID(thingUID, CHANNEL_WORKMODE);
                Channel channel = callback.createChannelBuilder(channelUID, CHANNEL_TYPE_UID_WORKMODE).build();
                thingBuilder.withChannel(channel);
                changed = true;
            }
        }

        if (changed) {
            updateThing(thingBuilder.build());
        }
        updateStatus(ThingStatus.ONLINE);
    }

    private void updateSchema() {
        TuyaOpenAPI api = this.api;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API not found");
            scheduler.schedule(this::updateSchema, 60, TimeUnit.SECONDS);
            return;
        }

        if (!api.isConnected()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API not connected");
            scheduler.schedule(this::updateSchema, 60, TimeUnit.SECONDS);
            return;
        }

        api.getDeviceSchema(configuration.deviceId).handle((deviceSchema, t) -> {
            if (t != null) {
                logger.warn("Failed to retrieve device schema, retrying");
                scheduler.schedule(this::updateSchema, 60, TimeUnit.SECONDS);
                return null;
            }
            storage.put(STORAGE_SCHEMA, gson.toJson(deviceSchema));
            this.schema = deviceSchema;
            checkThing();
            return null;
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            TuyaOpenAPI api = this.api;
            if (api == null) {
                logger.warn("Command '{}' to channel '{}' ignored: API not available", command, channelUID);
                return;
            }

            CommandRequest commandRequest = null;
            String channelId = channelUID.getId();
            if (CHANNEL_COLOR.equals(channelId)) {
                if (command instanceof HSBType) {
                    boolean switchLed = ((HSBType) command).getBrightness().doubleValue() > 0.0;
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>(colorCommand, new ColorValue((HSBType) command)), //
                            new CommandRequest.Command<>(onOffCommand, switchLed)));
                } else if (command instanceof OnOffType) {
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>(onOffCommand, OnOffType.ON.equals(command))));
                }
            } else if (CHANNEL_WHITE_BRIGHTNESS.equals(channelId)) {
                if (command instanceof PercentType) {
                    double value = ((PercentType) command).doubleValue();
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>(brightnessCommand, (int) Math.max(value * 10.0, 10.0)), //
                            new CommandRequest.Command<>(onOffCommand, value > 0.0)));
                } else if (command instanceof OnOffType) {
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>(onOffCommand, OnOffType.ON.equals(command))));
                }
            } else if (CHANNEL_WHITE_TEMPERATURE.equals(channelId)) {
                if (command instanceof PercentType) {
                    double value = ((PercentType) command).doubleValue();
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>(temperatureCommand, (int) (1000 - value * 10.0))));
                }
            } else if (CHANNEL_WORKMODE.equals(channelId)) {
                if (command instanceof StringType) {
                    commandRequest = new CommandRequest(List.of( //
                            new CommandRequest.Command<>("work_mode", command.toString())));
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
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    @Override
    public void handleRemoval() {
        // remove device schema from database
        storage.put(STORAGE_SCHEMA, null);
        super.handleRemoval();
    }
}
