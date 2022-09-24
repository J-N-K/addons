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
package org.smarthomej.binding.deconz.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.deconz.internal.handler.DeconzBridgeHandler;
import org.smarthomej.binding.deconz.internal.handler.GenericSensorThingHandler;
import org.smarthomej.binding.deconz.internal.handler.GroupThingHandler;
import org.smarthomej.binding.deconz.internal.handler.LightThingHandler;
import org.smarthomej.binding.deconz.internal.handler.SensorThermostatThingHandler;
import org.smarthomej.binding.deconz.internal.handler.SensorThingHandler;
import org.smarthomej.binding.deconz.internal.netutils.AsyncHttpClient;

import com.google.gson.Gson;

/**
 * The {@link DeconzHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author David Graeff - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.deconz")
@NonNullByDefault
public class DeconzHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(DeconzBridgeHandler.SUPPORTED_THING_TYPES, LightThingHandler.SUPPORTED_THING_TYPE_UIDS,
                    SensorThingHandler.SUPPORTED_THING_TYPES, SensorThingHandler.DEPRECATED_THING_TYPES,
                    SensorThermostatThingHandler.SUPPORTED_THING_TYPES, GenericSensorThingHandler.SUPPORTED_THING_TYPES,
                    GroupThingHandler.SUPPORTED_THING_TYPE_UIDS)
            .flatMap(Set::stream).collect(Collectors.toSet());

    private final Gson gson;
    private final WebSocketFactory webSocketFactory;
    private final HttpClientFactory httpClientFactory;
    private final DeconzDynamicStateDescriptionProvider stateDescriptionProvider;
    private final DeconzDynamicCommandDescriptionProvider commandDescriptionProvider;

    @Activate
    public DeconzHandlerFactory(final @Reference WebSocketFactory webSocketFactory,
            final @Reference HttpClientFactory httpClientFactory,
            final @Reference DeconzDynamicStateDescriptionProvider stateDescriptionProvider,
            final @Reference DeconzDynamicCommandDescriptionProvider commandDescriptionProvider) {
        this.webSocketFactory = webSocketFactory;
        this.httpClientFactory = httpClientFactory;
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.commandDescriptionProvider = commandDescriptionProvider;

        gson = Util.createCustomizedGson();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (DeconzBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new DeconzBridgeHandler((Bridge) thing, webSocketFactory,
                    new AsyncHttpClient(httpClientFactory.getCommonHttpClient()), gson);
        } else if (LightThingHandler.SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            return new LightThingHandler(thing, gson, stateDescriptionProvider, commandDescriptionProvider);
        } else if (SensorThingHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)
                || SensorThingHandler.DEPRECATED_THING_TYPES.contains(thingTypeUID)) {
            return new SensorThingHandler(thing, gson);
        } else if (SensorThermostatThingHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new SensorThermostatThingHandler(thing, gson);
        } else if (GroupThingHandler.SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            return new GroupThingHandler(thing, gson, commandDescriptionProvider);
        } else if (GenericSensorThingHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new GenericSensorThingHandler(thing, gson);
        }

        return null;
    }
}
