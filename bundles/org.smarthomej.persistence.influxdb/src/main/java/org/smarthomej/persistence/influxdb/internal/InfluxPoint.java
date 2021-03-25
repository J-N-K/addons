/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.persistence.influxdb.internal;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Point data to be stored in InfluxDB
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault({ DefaultLocation.PARAMETER })
public class InfluxPoint {
    private String measurementName;
    private Instant time;
    private Object value;
    private Map<String, String> tags;

    private InfluxPoint(Builder builder) {
        measurementName = builder.measurementName;
        time = builder.time;
        value = builder.value;
        tags = builder.tags;
    }

    public static Builder newBuilder(String measurementName) {
        return new Builder(measurementName);
    }

    public String getMeasurementName() {
        return measurementName;
    }

    public Instant getTime() {
        return time;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    public static final class Builder {
        private String measurementName;
        private Instant time;
        private Object value;
        private Map<String, String> tags = new HashMap<>();

        private Builder(String measurementName) {
            this.measurementName = measurementName;
        }

        public Builder withTime(Instant val) {
            time = val;
            return this;
        }

        public Builder withValue(Object val) {
            value = val;
            return this;
        }

        public Builder withTag(String name, String value) {
            tags.put(name, value);
            return this;
        }

        public InfluxPoint build() {
            return new InfluxPoint(this);
        }
    }

    @Override
    public @NonNull String toString() {
        return "InfluxPoint{" + "measurementName='" + measurementName + '\'' + ", time=" + time + ", value=" + value
                + ", tags=" + tags + '}';
    }
}
