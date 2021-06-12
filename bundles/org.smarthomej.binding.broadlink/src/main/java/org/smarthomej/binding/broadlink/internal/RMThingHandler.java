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

import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.CHANNEL_COMMAND;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.HUMIDITY_CHANNEL;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.HUMIDITY_CHANNEL_TYPE_UID;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.TEMPERATURE_CHANNEL;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.TEMPERATURE_CHANNEL_TYPE_UID;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandOption;
import org.openhab.core.util.HexUtils;
import org.openhab.core.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.broadlink.internal.config.RemoteControlConfiguration;
import org.smarthomej.binding.broadlink.internal.protocol.CommandPacket;
import org.smarthomej.binding.broadlink.internal.protocol.CommandResponsePacket;
import org.smarthomej.binding.broadlink.internal.protocol.PacketHeader;
import org.smarthomej.binding.broadlink.internal.protocol.payload.AuthPayload;
import org.smarthomej.binding.broadlink.internal.protocol.payload.AuthResponsePayload;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkCommunicationException;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkDeviceType;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkPacketException;
import org.smarthomej.binding.broadlink.internal.types.CommandCode;
import org.smarthomej.binding.broadlink.internal.util.Encryption;

/**
 * The {@link RMThingHandler} is the thing handler for remote control
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RMThingHandler extends BaseThingHandler {
    private static final String ENTER_LEARNING = "03";
    private static final String READ_CODE = "04";
    private static final String SEND_CODE = "02000000";

    private final Logger logger = LoggerFactory.getLogger(RMThingHandler.class);
    private final StorageService storageService;
    private final BroadlinkDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider;

    private final Encryption encryption = new Encryption();
    private BroadlinkDeviceType deviceType = BroadlinkDeviceType.UNKNOWN;

    private RemoteControlConfiguration config = new RemoteControlConfiguration();
    private @NonNullByDefault({}) CommandPacket commandPacket;
    private Map<String, String> commands = Map.of();

    private boolean authenticated = false;

    public RMThingHandler(Thing thing, StorageService storageService,
            BroadlinkDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        super(thing);

        this.storageService = storageService;
        this.dynamicCommandDescriptionProvider = dynamicCommandDescriptionProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_COMMAND.equals(channelUID.getId()) && command instanceof StringType) {
            String commandSequence = commands.get(command.toString());
            if (commandSequence == null) {
                logger.warn("Command '{}' is unknown for thing '{}'", command, thing.getUID());
                return;
            }
            String payload = deviceType.getSendPrefix() + SEND_CODE + commandSequence;
            try {
                CommandResponsePacket commandResponsePacket = authenticatedSendCommand(CommandCode.COMMAND, payload);
                short errorCode = commandResponsePacket.getPacketHeader().getErrorCode();
                handleError(errorCode);
            } catch (BroadlinkCommunicationException e) {
                logger.warn("'{}' failed sending command '{}': {}", thing.getUID(), command, e.getMessage());
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(RemoteControlConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "configuration invalid");
            return;
        }

        deviceType = BroadlinkDeviceType.fromInt(config.deviceType);

        if (deviceType == BroadlinkDeviceType.RM2 || deviceType == BroadlinkDeviceType.RM4) {
            // RM2 and RM4 have additional channels
            checkChannels();
        }

        commandPacket = new CommandPacket((short) config.deviceType, config.macAddress, encryption);
        setCommandOptions();

        updateStatus(ThingStatus.UNKNOWN);

        readStatus();
    }

    private void checkChannels() {
        ThingBuilder thingBuilder = editThing();
        boolean changed = false;

        Channel temperatureChannel = thing.getChannel(TEMPERATURE_CHANNEL);
        if (temperatureChannel == null) {
            thingBuilder.withChannels(ChannelBuilder.create(new ChannelUID(thing.getUID(), TEMPERATURE_CHANNEL))
                    .withType(TEMPERATURE_CHANNEL_TYPE_UID).build());
            changed = true;
        }

        Channel humidityChannel = thing.getChannel(HUMIDITY_CHANNEL);
        if (humidityChannel == null && deviceType == BroadlinkDeviceType.RM4) {
            // only present on RM4
            thingBuilder.withChannels(ChannelBuilder.create(new ChannelUID(thing.getUID(), HUMIDITY_CHANNEL))
                    .withType(HUMIDITY_CHANNEL_TYPE_UID).build());
            changed = true;
        }
        if (changed) {
            updateThing(thingBuilder.build());
        }
    }

    private void readStatus() {
        try {
            String commandPayload = deviceType == BroadlinkDeviceType.RM4 ? "24" : "01";

            CommandResponsePacket commandResponsePacket = authenticatedSendCommand(CommandCode.COMMAND,
                    deviceType.getRequestPrefix() + commandPayload);
            PacketHeader packetHeader = commandResponsePacket.getPacketHeader();
            short errorCode = packetHeader.getErrorCode();
            handleError(errorCode);
            byte[] payload = commandResponsePacket.getPayload();

            if (deviceType == BroadlinkDeviceType.RM2) {
                double temperature = (int) payload[0x04] + ((int) payload[0x05]) / 10.0;
                updateState(TEMPERATURE_CHANNEL, new QuantityType<>(temperature, SIUnits.CELSIUS));
            }
            if (deviceType == BroadlinkDeviceType.RM4) {
                double temperature = (int) payload[0x06] * 10 + ((int) payload[0x07]) / 100.0;
                updateState(TEMPERATURE_CHANNEL, new QuantityType<>(temperature, SIUnits.CELSIUS));

                double humidity = (int) payload[0x08] + ((int) payload[0x09]) / 100.0;
                updateState(TEMPERATURE_CHANNEL, new QuantityType<>(humidity, Units.PERCENT));
            }
        } catch (BroadlinkCommunicationException e) {
            logger.warn("Failed to refresh status for thing '{}': {}", thing.getUID(), e.getMessage());
        }
    }

    public void learnIr(String commandName) {
        doLearning().handle((code, t) -> {
            if (t != null) {
                logger.warn("Thing '{}' failed to retrieve code for '{}': {}", thing.getUID(), commandName,
                        t.getMessage());
            } else {
                logger.debug("Thing '{}' now has '{}' associated with '{}'", thing.getUID(), commandName, code);
                Storage<String> storage = storageService
                        .getStorage(UIDUtils.encode(thing.getUID().getAsString()) + ".commands");
                storage.put(commandName, code);
                setCommandOptions();
            }
            return null;
        });
    }

    private void setCommandOptions() {
        Storage<String> storage = storageService
                .getStorage(UIDUtils.encode(thing.getUID().getAsString()) + ".commands");

        commands = storage.stream().filter(c -> c.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, c -> Objects.requireNonNull(c.getValue())));
        dynamicCommandDescriptionProvider.setCommandOptions(new ChannelUID(thing.getUID(), CHANNEL_COMMAND),
                commands.keySet().stream().map(s -> new CommandOption(s, s)).collect(Collectors.toList()));
    }

    private CompletableFuture<String> doLearning() {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            CommandResponsePacket commandResponsePacket = authenticatedSendCommand(CommandCode.COMMAND,
                    deviceType.getRequestPrefix() + ENTER_LEARNING);
            PacketHeader packetHeader = commandResponsePacket.getPacketHeader();
            short errorCode = packetHeader.getErrorCode();
            handleError(errorCode);
            scheduler.execute(() -> getLearningCode(future, 0));

        } catch (BroadlinkCommunicationException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void handleError(short errorCode) throws BroadlinkCommunicationException {
        if (errorCode == 0) {
            updateStatus(ThingStatus.ONLINE);
            return;
        }

        if (errorCode == -7) {
            authenticated = false;
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Code: " + errorCode);
        throw new BroadlinkCommunicationException("Broadlink-Error: " + errorCode);
    }

    private void getLearningCode(CompletableFuture<String> future, int count) {
        try {
            CommandResponsePacket commandResponsePacket = authenticatedSendCommand(CommandCode.COMMAND,
                    deviceType.getRequestPrefix() + READ_CODE);
            PacketHeader packetHeader = commandResponsePacket.getPacketHeader();

            short errorCode = packetHeader.getErrorCode();
            if (errorCode == -5 || errorCode == -10) {
                logger.trace("Ignoring error '{}' while checking status", packetHeader.getErrorCode());
                if (count < 30) {
                    scheduler.schedule(() -> getLearningCode(future, count + 1), 1000, TimeUnit.MILLISECONDS);
                } else {
                    future.completeExceptionally(new BroadlinkCommunicationException("Did not receive code"));
                }
            }
            handleError(errorCode);
            // the code starts at byte 0x04 + commandPrefix length
            String payload = HexUtils.bytesToHex(commandResponsePacket.getPayload());
            future.complete(payload.substring(8 + deviceType.getRequestPrefix().length()));
        } catch (BroadlinkCommunicationException e) {
            future.completeExceptionally(e);
        }
    }

    private boolean authenticate() {
        if (authenticated) {
            return true;

        }
        try {
            AuthPayload authPayload = new AuthPayload();
            CommandResponsePacket commandResponsePacket = sendCommand(CommandCode.AUTH, authPayload.getBytes());
            PacketHeader responsePacketHeader = commandResponsePacket.getPacketHeader();
            byte[] payload = commandResponsePacket.getPayload();

            if (responsePacketHeader.getErrorCode() == 0 && payload.length != 0) {
                AuthResponsePayload authResponsePayload = new AuthResponsePayload(payload);
                encryption.setDeviceKey(authResponsePayload.deviceKey);
                commandPacket.setDeviceId(authResponsePayload.deviceId);

                authenticated = true;
                return true;
            }
            throw new BroadlinkCommunicationException(responsePacketHeader.getErrorCode());
        } catch (BroadlinkCommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

        return false;
    }

    private CommandResponsePacket authenticatedSendCommand(CommandCode commandCode, String payload)
            throws BroadlinkCommunicationException {
        if (!authenticate()) {
            throw new BroadlinkCommunicationException("Not authenticated");
        }
        return sendCommand(commandCode, payload);
    }

    private CommandResponsePacket sendCommand(CommandCode commandCode, String payload)
            throws BroadlinkCommunicationException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setReuseAddress(true);
            socket.setSoTimeout(1000);

            byte[] buf = new byte[512];

            byte[] commandPacket = this.commandPacket.create(commandCode, HexUtils.hexToBytes(payload));
            logger.trace("Thing '{}' sends '{}'", thing.getUID(), HexUtils.bytesToHex(commandPacket));

            DatagramPacket datagramPacket = new DatagramPacket(commandPacket, commandPacket.length,
                    InetAddress.getByName(config.ipAddress), 80);
            socket.send(datagramPacket);

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            byte[] responseBuffer = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), responseBuffer, 0, packet.getLength());
            CommandResponsePacket commandResponsePacket = new CommandResponsePacket(responseBuffer, encryption);
            logger.trace("Thing '{}' received {}", thing.getUID(), commandResponsePacket);

            return commandResponsePacket;
        } catch (IOException | BroadlinkPacketException e) {
            throw new BroadlinkCommunicationException(e.getMessage());
        }
    }

    public void dispose() {
        super.dispose();
    }
}
