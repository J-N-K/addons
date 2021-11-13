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

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.STORAGE_SCHEMA;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.CommandOption;
import org.openhab.core.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.api.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.dto.DeviceSchema;
import org.smarthomej.binding.tuya.internal.dto.StatusInfo;
import org.smarthomej.binding.tuya.internal.util.DeviceConfiguration;

import com.google.gson.Gson;

/**
 * The {@link AbstractTuyaThingHandler} is the base class for Tuya Device thing handlers
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractTuyaThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(LightThingHandler.class);
    private final Storage<String> storage;
    protected final Gson gson;
    protected @Nullable TuyaOpenAPI api;
    protected DeviceConfiguration configuration = new DeviceConfiguration();
    protected @Nullable DeviceSchema schema;
    private @Nullable ScheduledFuture<?> requestJob;

    public AbstractTuyaThingHandler(Thing thing, Gson gson, StorageService storageService) {
        super(thing);
        this.storage = storageService.getStorage(UIDUtils.encode(thing.getUID().toString()));
        this.gson = gson;
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

    /**
     * process a status message
     *
     * @param status a single status method from MQTT connection or state request
     */
    public abstract void processStatusMessage(StatusInfo status);

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
            scheduler.execute(this::getInitialState);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            api = null;
        }
    }

    protected abstract void processSchema(DeviceSchema schema);

    protected List<CommandOption> toCommandOptionList(List<String> options) {
        return options.stream().map(c -> new CommandOption(c, c)).collect(Collectors.toList());
    }

    private Optional<TuyaOpenAPI> getApiOrReschedule(Runnable method) {
        TuyaOpenAPI api = this.api;
        if (api == null || !api.isConnected()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API not found or not connected");
            stopRequestJob();
            requestJob = scheduler.schedule(method, 60, TimeUnit.SECONDS);
            return Optional.empty();
        }

        return Optional.of(api);
    }

    private void getInitialState() {
        getApiOrReschedule(this::getInitialState).ifPresent(api -> api.getDeviceStatus(configuration.deviceId)
                .thenAccept(statusList -> statusList.forEach(this::processStatusMessage)));
    }

    private void updateSchema() {
        getApiOrReschedule(this::updateSchema)
                .ifPresent(api -> api.getDeviceSchema(configuration.deviceId).handle((deviceSchema, t) -> {
                    if (t != null) {
                        logger.warn("Failed to retrieve device schema, retrying");
                        stopRequestJob();
                        requestJob = scheduler.schedule(this::updateSchema, 60, TimeUnit.SECONDS);
                        return null;
                    }
                    storage.put(STORAGE_SCHEMA, gson.toJson(deviceSchema));
                    this.schema = deviceSchema;
                    checkThing();
                    return null;
                }));
    }

    protected abstract void checkThing();

    private void stopRequestJob() {
        ScheduledFuture<?> future = requestJob;
        if (future != null) {
            future.cancel(true);
            requestJob = null;
        }
    }

    @Override
    public void handleRemoval() {
        // remove device schema from database
        storage.put(STORAGE_SCHEMA, null);
        super.handleRemoval();
    }

    @Override
    public void dispose() {
        stopRequestJob();
    }
}
