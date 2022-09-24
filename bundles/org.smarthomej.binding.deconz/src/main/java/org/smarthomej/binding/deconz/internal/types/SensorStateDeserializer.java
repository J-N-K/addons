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
package org.smarthomej.binding.deconz.internal.types;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.deconz.internal.dto.SensorState;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Custom deserializer for {@link ResourceType}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class SensorStateDeserializer implements JsonDeserializer<SensorState> {

    @Override
    public SensorState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Map<String, @Nullable Object> data = context.deserialize(json, Map.class);

        SensorState state = new SensorState();
        data.forEach((k, v) -> {
            if (v instanceof Number) {
                state.put(k, ((Number) v).intValue());
            } else if ("orientation".equals(k) && v instanceof List) {
                // vibration sensors report an array of integers
                int[] v2 = ((List<Number>) v).stream().mapToInt(Number::intValue).toArray();
                state.put(k, v2);
            } else {
                state.put(k, v);
            }
        });
        return state;
    }
}
