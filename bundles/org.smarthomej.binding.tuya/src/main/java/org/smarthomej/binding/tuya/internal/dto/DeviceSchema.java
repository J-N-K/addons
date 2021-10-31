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
package org.smarthomej.binding.tuya.internal.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link DeviceSchema} encapsulates the command and status specification of a device
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class DeviceSchema {
    public String category = "";
    public List<Description> functions = List.of();
    public List<Description> status = List.of();

    @Override
    public String toString() {
        return "DeviceSpecification{category='" + category + "', functions=" + functions + ", status=" + status + "}";
    }

    public static class Description {
        public String code = "";
        public String desc = "";
        public String name = "";
        public String type = "";
        public String values = "";

        @Override
        public String toString() {
            return "Description{code='" + code + "', desc='" + desc + "', name='" + name + "', type='" + type
                    + "', values='" + values + "'}";
        }
    }

    public static class Range {
        public List<String> range = List.of();

        @Override
        public String toString() {
            return "Range{range=" + range + "}";
        }
    }

    public boolean hasFunction(String fcn) {
        return functions.stream().anyMatch(f -> fcn.equals(f.code));
    }
}
