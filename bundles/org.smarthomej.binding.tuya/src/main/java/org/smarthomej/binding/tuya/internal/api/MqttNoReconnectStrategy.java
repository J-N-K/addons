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
package org.smarthomej.binding.tuya.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.mqtt.reconnect.AbstractReconnectStrategy;

/**
 * The {@link MqttNoReconnectStrategy} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class MqttNoReconnectStrategy extends AbstractReconnectStrategy {
    @Override
    public boolean isReconnecting() {
        return false;
    }

    @Override
    public void lostConnection() {
    }

    @Override
    public void connectionEstablished() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
