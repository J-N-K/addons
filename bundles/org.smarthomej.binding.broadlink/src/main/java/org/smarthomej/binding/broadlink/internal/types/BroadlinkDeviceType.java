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
package org.smarthomej.binding.broadlink.internal.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link BroadlinkDeviceType} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public enum BroadlinkDeviceType {
    RM("", "", "273d", "278f"),
    RM2("", "", "2712", "272a", "277c", "2783", "2787", "278b", "2797", "279d", "27a1", "27a6", "27a9"),
    RM3("", "", "2737", "27c2"),
    RM3Q("0400", "0d00", "5f36"),
    RM4("0400", "0d00", "51da", "6020-602f", "610e-62bf", "649b", "6539"),
    UNKNOWN("", "");

    private List<Integer> codes = new ArrayList<>();
    private String requestPrefix;
    private String sendPrefix;

    BroadlinkDeviceType(String requestPrefix, String sendPrefix, String... codes) {
        this.requestPrefix = requestPrefix;
        this.sendPrefix = sendPrefix;
        for (String code : codes) {
            if (code.contains("-")) {
                String[] c = code.split("-");
                this.codes.addAll(IntStream.range(Integer.parseInt(c[0], 16), Integer.parseInt(c[1], 16)).boxed()
                        .collect(Collectors.toList()));
            } else {
                this.codes.add(Integer.parseInt(code, 16));
            }
        }
    }

    public static BroadlinkDeviceType fromInt(Integer i) {
        return Objects.requireNonNull(Arrays.stream(BroadlinkDeviceType.values()).filter(v -> v.codes.contains(i))
                .findFirst().orElse(UNKNOWN));
    }

    public String getRequestPrefix() {
        return requestPrefix;
    }

    public String getSendPrefix() {
        return sendPrefix;
    }
}
