/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.smarthomej.automation.jrule.items;

import java.util.Set;

import org.smarthomej.automation.jrule.internal.handler.JRuleEventHandler;
import org.smarthomej.automation.jrule.rules.JRuleOnOffValue;
import org.smarthomej.automation.jrule.rules.JRulePlayPauseValue;

/**
 * The {@link JRuleGroupItem} Items
 *
 * @author Joseph (Seaside) Hagberg - Initial contribution
 */
public class JRuleGroupItem extends JRuleItem {

    public static Set<String> members(String groupName) {
        return JRuleEventHandler.get().getGroupMemberNames(groupName);
    }

    public static void sendCommandToAll(String groupName, String value) {
        final Set<String> groupMemberNames = JRuleEventHandler.get().getGroupMemberNames(groupName);
        groupMemberNames.forEach(m -> sendCommand(m, value));
    }

    public static String getState(String itemName) {
        return JRuleEventHandler.get().getStringValue(itemName);
    }

    public static JRuleOnOffValue getStateAsOnOffValue(String itemName) {
        return JRuleEventHandler.get().getOnOffValue(itemName);
    }

    public static JRulePlayPauseValue getStateAsPlayPauseValue(String itemName) {
        return JRuleEventHandler.get().getPauseValue(itemName);
    }

    public static void sendCommand(String itemName, JRulePlayPauseValue value) {
        JRuleEventHandler.get().sendCommand(itemName, value);
    }

    public static void sendCommand(String itemName, JRuleOnOffValue value) {
        JRuleEventHandler.get().sendCommand(itemName, value);
    }

    public static void sendCommand(String itemName, String value) {
        JRuleEventHandler.get().sendCommand(itemName, value);
    }

    public static void postUpdate(String itemName, String value) {
        JRuleEventHandler.get().postUpdate(itemName, value);
    }

    public static void postUpdate(String itemName, JRulePlayPauseValue value) {
        JRuleEventHandler.get().postUpdate(itemName, value);
    }

    public static void postUpdate(String itemName, JRuleOnOffValue value) {
        JRuleEventHandler.get().postUpdate(itemName, value);
    }
}
