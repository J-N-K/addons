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
package org.smarthomej.binding.deconz.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.smarthomej.binding.deconz.internal.types.ChannelInfo;

/**
 * The {@link ChannelUpdater} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ChannelUpdater {

    private ChannelUpdater() {
        // prevent initialization
    }

    private static final Map<String, StateChannelUpdater> STATE_CHANNEL_CONVERTERS = Map.ofEntries(
            Map.entry("DateTimeType", ChannelUpdater::defaultDateTimeTypeConverter),
            Map.entry("DecimalType", ChannelUpdater::defaultDecimalTypeConverter),
            Map.entry("OnOffType", ChannelUpdater::defaultOnOffTypeConverter),
            Map.entry("QuantityType", ChannelUpdater::defaultQuantityTypeConverter),
            Map.entry("StringType", ChannelUpdater::defaultStringTypeConverter));

    @FunctionalInterface
    public interface StateChannelUpdater {
        void update(ChannelInfo channelInfo, Object value, BiConsumer<String, State> update);
    }

    public static Optional<StateChannelUpdater> get(String converterId) {
        return Optional.ofNullable(STATE_CHANNEL_CONVERTERS.get(converterId));
    }

    private static void defaultDateTimeTypeConverter(ChannelInfo channelInfo, Object value,
            BiConsumer<String, State> update) {
        if (value instanceof String) {
            update.accept(channelInfo.channelId, Util.convertTimestampToDateTime((String) value));
        }
    }

    private static void defaultDecimalTypeConverter(ChannelInfo channelInfo, Object value,
            BiConsumer<String, State> update) {
        if ((value instanceof Integer)) {
            update.accept(channelInfo.channelId, new DecimalType(((Integer) value).longValue()));
        }
    }

    private static void defaultOnOffTypeConverter(ChannelInfo channelInfo, Object value,
            BiConsumer<String, State> update) {
        if (value instanceof Boolean) {
            update.accept(channelInfo.channelId, OnOffType.from((Boolean) value));
        }
    }

    private static void defaultQuantityTypeConverter(ChannelInfo channelInfo, Object value,
            BiConsumer<String, State> update) {
        Double scaling = Objects.requireNonNullElse((Double) channelInfo.converterParams.get("scaling"), 1.0);
        String unit = (String) channelInfo.converterParams.get("unit");
        if ((value instanceof Integer) && unit != null) {
            update.accept(channelInfo.channelId,
                    new QuantityType<>(((Number) value).doubleValue() * scaling + " " + unit));
        }
    }

    private static void defaultStringTypeConverter(ChannelInfo channelInfo, Object value,
            BiConsumer<String, State> update) {
        if (value instanceof String) {
            update.accept(channelInfo.channelId, new StringType((String) value));
        }
    }
}
