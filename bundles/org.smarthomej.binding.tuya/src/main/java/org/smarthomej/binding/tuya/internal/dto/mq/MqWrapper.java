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
package org.smarthomej.binding.tuya.internal.dto.mq;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MqWrapper} encapsulates the encrypted MQTT message
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class MqWrapper {
    public String data = "";
    public String protocol = "";
    public String pv = "";
    public String sign = "";
    public long t;
}
