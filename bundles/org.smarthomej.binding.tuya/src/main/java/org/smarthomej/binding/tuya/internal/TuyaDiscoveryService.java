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

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_DEVICE_ID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_LOCAL_KEY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_CATEGORY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_UUID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.THING_TYPE_LIGHT;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.handler.ProjectHandler;

/**
 * The {@link TuyaDiscoveryService} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_LIGHT);

    private static final Map<ThingTypeUID, List<String>> THING_TYPE_CATEGORY = Map.of( //
            THING_TYPE_LIGHT, List.of("dj", "xdd", "fwd", "dc", "dd", "gyd", "fsd"));
    private static final int SEARCH_TIME = 5;

    private final Logger logger = LoggerFactory.getLogger(TuyaDiscoveryService.class);
    private @Nullable ProjectHandler bridgeHandler;

    public TuyaDiscoveryService() {
        super(SUPPORTED_THING_TYPES, SEARCH_TIME, false);
    }

    @Override
    protected void startScan() {
        ProjectHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            logger.warn("Could not start discovery, bridge handler not set");
            return;
        }

        if (!ThingStatus.ONLINE.equals(bridgeHandler.getThing().getStatus())) {
            logger.warn("Tried to start scan but bridge '{}' is OFFLINE.", bridgeHandler.getThing().getUID());
        }

        ThingUID bridgeUid = bridgeHandler.getThing().getUID();

        bridgeHandler.getAllDevices().thenAccept(deviceList -> deviceList.forEach(device -> {
            ThingTypeUID thingTypeUID = THING_TYPE_CATEGORY.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(device.category)).findAny().map(Map.Entry::getKey)
                    .orElse(null);
            if (thingTypeUID == null) {
                logger.warn("Could not determine correct thing type for category '{}', device '{}'", device.category,
                        device.name);
                return;
            }
            ThingUID thingUid = new ThingUID(thingTypeUID, bridgeUid, UIDUtils.encode(device.uuid));
            Map<String, Object> properties = new HashMap<>();
            properties.put(PROPERTY_UUID, device.uuid);
            properties.put(PROPERTY_CATEGORY, device.category);
            properties.put(CONFIG_LOCAL_KEY, device.localKey);
            properties.put(CONFIG_DEVICE_ID, device.id);
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUid).withBridge(bridgeUid)
                    .withLabel(device.name).withRepresentationProperty(PROPERTY_UUID).withProperties(properties)
                    .build();
            thingDiscovered(discoveryResult);
        }));
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ProjectHandler) {
            this.bridgeHandler = (ProjectHandler) thingHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void deactivate() {
        BridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            logger.warn("Bridgehandler not found, could not cleanup discovery results.");
            return;
        }
        removeOlderResults(new Date().getTime(), bridgeHandler.getThing().getUID());
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }
}
