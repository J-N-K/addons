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

import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.SUPPORTED_THING_TYPE_UIDS;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.THING_TYPE_UID_RM;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link BroadlinkHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.broadlink", service = ThingHandlerFactory.class)
public class BroadlinkHandlerFactory extends BaseThingHandlerFactory {
    private final StorageService storageService;
    private final BroadlinkDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider;

    @Activate
    public BroadlinkHandlerFactory(@Reference StorageService storageService,
            @Reference BroadlinkDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        this.storageService = storageService;
        this.dynamicCommandDescriptionProvider = dynamicCommandDescriptionProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_UID_RM.equals(thingTypeUID)) {
            return new RMThingHandler(thing, storageService, dynamicCommandDescriptionProvider);
        }

        return null;
    }
}
