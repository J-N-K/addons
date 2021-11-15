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

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.tuya.internal.handler.DimmerThingHandler;
import org.smarthomej.binding.tuya.internal.handler.LightThingHandler;
import org.smarthomej.binding.tuya.internal.handler.ProjectHandler;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

import com.google.gson.Gson;

/**
 * The {@link TuyaHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.tuya", service = ThingHandlerFactory.class)
@SuppressWarnings("unused")
public class TuyaHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_PROJECT, THING_TYPE_LIGHT,
            THING_TYPE_DIMMER);

    private final SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider;
    private final HttpClient httpClient;
    private final StorageService storageService;
    private final Gson gson = new Gson();

    @Activate
    public TuyaHandlerFactory(@Reference HttpClientFactory httpClientFactory, @Reference StorageService storageService,
            @Reference SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.storageService = storageService;
        this.dynamicCommandDescriptionProvider = dynamicCommandDescriptionProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_PROJECT.equals(thingTypeUID)) {
            return new ProjectHandler((Bridge) thing, httpClient);
        } else if (THING_TYPE_LIGHT.equals(thingTypeUID)) {
            return new LightThingHandler(thing, gson, storageService, dynamicCommandDescriptionProvider);
        } else if (THING_TYPE_DIMMER.equals(thingTypeUID)) {
            return new DimmerThingHandler(thing, gson, storageService, dynamicCommandDescriptionProvider);
        }

        return null;
    }
}
