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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;
import org.openhab.core.io.transport.mqtt.MqttConnectionState;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.ProjectConfiguration;
import org.smarthomej.binding.tuya.internal.TuyaDiscoveryService;
import org.smarthomej.binding.tuya.internal.api.ApiStatusCallback;
import org.smarthomej.binding.tuya.internal.api.MqttNoReconnectStrategy;
import org.smarthomej.binding.tuya.internal.api.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.dto.DeviceListInfo;
import org.smarthomej.binding.tuya.internal.dto.SubscribeMqttResponse;
import org.smarthomej.binding.tuya.internal.dto.mq.MqMessage;
import org.smarthomej.binding.tuya.internal.dto.mq.MqWrapper;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

import com.google.gson.Gson;

/**
 * The {@link ProjectHandler} is responsible for handling communication
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ProjectHandler extends BaseBridgeHandler
        implements ApiStatusCallback, MqttConnectionObserver, MqttMessageSubscriber {
    private final Logger logger = LoggerFactory.getLogger(ProjectHandler.class);

    private final Gson gson = new Gson();
    private final Map<String, AbstractTuyaThingHandler> deviceIdToThingHandler = new HashMap<>();

    // outgoing: Tuya API, incoming: MQTT
    private final TuyaOpenAPI api;
    private @Nullable MqttBrokerConnection mqttConnection;
    private SubscribeMqttResponse mqttData = new SubscribeMqttResponse();

    private @Nullable ScheduledFuture<?> apiConnectFuture;
    private @Nullable ScheduledFuture<?> renewMqttConnectionFuture;

    private boolean disposing = false;

    public ProjectHandler(Bridge thing, HttpClient httpClient) {
        super(thing);

        api = new TuyaOpenAPI(this, scheduler, gson, httpClient);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        ProjectConfiguration config = getConfigAs(ProjectConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }

        api.setConfiguration(config);
        updateStatus(ThingStatus.UNKNOWN);

        stopApiConnectFuture();
        apiConnectFuture = scheduler.schedule(api::login, 0, TimeUnit.SECONDS);
    }

    @Override
    public void tuyaOpenApiStatus(boolean status) {
        if (!status) {
            stopApiConnectFuture();
            apiConnectFuture = scheduler.schedule(api::login, 60, TimeUnit.SECONDS);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } else {
            stopApiConnectFuture();
            updateStatus(ThingStatus.ONLINE);
            disposing = false;
            startMqttConnection();
        }
    }

    public @Nullable TuyaOpenAPI getApi() {
        return api;
    }

    public CompletableFuture<List<DeviceListInfo>> getAllDevices() {
        if (api.isConnected()) {
            return api.getDeviceList();
        }
        return CompletableFuture.failedFuture(new IllegalStateException("not connected"));
    }

    private void startMqttConnection() {
        if (!api.isConnected()) {
            logger.warn("Cannot establish MQTT connection without being logged in. Retrying after successful login.");
            return;
        }
        stopMqttConnection().thenCompose(r -> api.getMqttSubscription()).thenAccept(mqttData -> {
            this.mqttData = mqttData;
            String address = mqttData.url;
            if (address.indexOf("/") > 0) {
                address = address.substring(address.lastIndexOf("/") + 1);
            }
            logger.debug("Using MQTT data: {}", mqttData);
            String[] addressParts = address.split(":");
            MqttBrokerConnection conn = new MqttBrokerConnection(MqttBrokerConnection.Protocol.TCP,
                    MqttBrokerConnection.MqttVersion.V3, addressParts[0], Integer.parseInt(addressParts[1]), true,
                    mqttData.clientId);
            conn.setCredentials(mqttData.username, mqttData.password);
            conn.addConnectionObserver(this);
            conn.setReconnectStrategy(new MqttNoReconnectStrategy());
            this.mqttConnection = conn;
            conn.start();

            // renew MQTT subscription 60 seconds before expiry
            renewMqttConnectionFuture = scheduler.schedule(this::startMqttConnection, mqttData.expireTime - 60,
                    TimeUnit.SECONDS);
        });
    }

    private CompletableFuture<Boolean> stopMqttConnection() {
        ScheduledFuture<?> renewMqttConnection = this.renewMqttConnectionFuture;
        if (renewMqttConnection != null) {
            renewMqttConnection.cancel(true);
            this.renewMqttConnectionFuture = null;
        }
        MqttBrokerConnection mqttConnection = this.mqttConnection;
        if (mqttConnection != null) {
            this.mqttConnection = null;
            return mqttConnection.stop();
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void connectionStateChanged(MqttConnectionState mqttConnectionState, @Nullable Throwable throwable) {
        if (MqttConnectionState.CONNECTED.equals(mqttConnectionState)) {
            logger.debug("Established MQTT connection, subscribing to '{}'", mqttData.sourceTopic.device);
            MqttBrokerConnection connection = this.mqttConnection;
            if (connection != null) {
                connection.subscribe(mqttData.sourceTopic.device, this).handle((success, t) -> {
                    if (t != null) {
                        logger.warn("Subscribing failed, retrying.", t);
                        scheduler.execute(this::startMqttConnection);
                    } else if (success) {
                        logger.trace("Successfully subscribed to '{}'", mqttData.sourceTopic.device);
                    } else {
                        logger.warn("Subscription failed, retrying.");
                        scheduler.execute(this::startMqttConnection);
                    }
                    return null;
                });
            }
        } else if (MqttConnectionState.DISCONNECTED.equals(mqttConnectionState)) {
            logger.debug("MQTT connection disconnected.");
            if (!disposing) {
                startMqttConnection();
            }
        } else {
            logger.trace("MQTT connection changed state to {}", mqttConnectionState);
        }
    }

    @Override
    public void processMessage(String s, byte[] bytes) {
        String msg = new String(bytes);
        logger.trace("Received: {}", msg);
        MqWrapper mqWrapper = Objects.requireNonNull(gson.fromJson(msg, MqWrapper.class));

        CryptoUtil.decrypt(mqWrapper.data, mqttData.password, mqWrapper.t).ifPresent(decoded -> {
            logger.trace("Received decoded message {}", decoded);
            MqMessage message = Objects.requireNonNull(gson.fromJson(decoded, MqMessage.class));

            AbstractTuyaThingHandler handler = deviceIdToThingHandler.get(message.devId);
            if (handler == null) {
                logger.debug("Ignoring message {}, no handler in Map.", message);
                return;
            }
            message.status.forEach(handler::processStatusMessage);
        });
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);

        if (childHandler instanceof AbstractTuyaThingHandler) {
            String deviceId = ((AbstractTuyaThingHandler) childHandler).getDeviceId();
            deviceIdToThingHandler.put(deviceId, (AbstractTuyaThingHandler) childHandler);
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof AbstractTuyaThingHandler) {
            String deviceId = ((AbstractTuyaThingHandler) childHandler).getDeviceId();
            deviceIdToThingHandler.remove(deviceId);
        }

        super.childHandlerDisposed(childHandler, childThing);
    }

    private void stopApiConnectFuture() {
        ScheduledFuture<?> apiConnectFuture = this.apiConnectFuture;
        if (apiConnectFuture != null) {
            apiConnectFuture.cancel(true);
            this.apiConnectFuture = null;
        }
    }

    @Override
    public void dispose() {
        stopApiConnectFuture();
        disposing = true;
        stopMqttConnection();
        api.disconnect();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(TuyaDiscoveryService.class);
    }
}
