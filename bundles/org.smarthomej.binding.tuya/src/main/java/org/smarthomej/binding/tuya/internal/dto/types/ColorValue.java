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
package org.smarthomej.binding.tuya.internal.dto.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.HSBType;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link ColorValue} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ColorValue {
    @SerializedName("h")
    public int hue;
    @SerializedName("s")
    public int saturation;
    @SerializedName("v")
    public int brightness;

    public ColorValue(HSBType value) {
        hue = value.getHue().intValue();
        saturation = (int) (value.getSaturation().doubleValue() * 10.0);
        brightness = (int) (value.getBrightness().doubleValue() * 10.0);
    }
}
