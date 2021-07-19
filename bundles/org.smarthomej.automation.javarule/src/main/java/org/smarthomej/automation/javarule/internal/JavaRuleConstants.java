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
package org.smarthomej.automation.javarule.internal;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.smarthomej.automation.javarule.rules.annotation.ChannelEventTrigger;
import org.smarthomej.automation.javarule.rules.annotation.DayOfWeekCondition;
import org.smarthomej.automation.javarule.rules.annotation.EphemerisDaysetCondition;
import org.smarthomej.automation.javarule.rules.annotation.EphemerisHolidayCondition;
import org.smarthomej.automation.javarule.rules.annotation.EphemerisNotHolidayCondition;
import org.smarthomej.automation.javarule.rules.annotation.EphemerisWeekdayCondition;
import org.smarthomej.automation.javarule.rules.annotation.EphemerisWeekendCondition;
import org.smarthomej.automation.javarule.rules.annotation.GenericCompareCondition;
import org.smarthomej.automation.javarule.rules.annotation.GenericCronTrigger;
import org.smarthomej.automation.javarule.rules.annotation.GenericEventCondition;
import org.smarthomej.automation.javarule.rules.annotation.GenericEventTrigger;
import org.smarthomej.automation.javarule.rules.annotation.GroupStateChangeTrigger;
import org.smarthomej.automation.javarule.rules.annotation.GroupStateUpdateTrigger;
import org.smarthomej.automation.javarule.rules.annotation.ItemCommandTrigger;
import org.smarthomej.automation.javarule.rules.annotation.ItemStateChangeTrigger;
import org.smarthomej.automation.javarule.rules.annotation.ItemStateCondition;
import org.smarthomej.automation.javarule.rules.annotation.ItemStateUpdateTrigger;
import org.smarthomej.automation.javarule.rules.annotation.SystemStartlevelTrigger;
import org.smarthomej.automation.javarule.rules.annotation.ThingStatusChangeTrigger;
import org.smarthomej.automation.javarule.rules.annotation.ThingStatusUpdateTrigger;
import org.smarthomej.automation.javarule.rules.annotation.TimeOfDayCondition;
import org.smarthomej.automation.javarule.rules.annotation.TimeOfDayTrigger;

/**
 * The {@link JavaRuleConstants} class defines common constants, which are
 * used across the Java Rule automation.
 *
 * @author Jan N. Klug - Initial contribution
 *
 */
@NonNullByDefault
public class JavaRuleConstants {
    public static final String ANNOTATION_DEFAULT = "\u0002\u0003";

    public static final String HELPER_JAR = "helper.jar";
    public static final String DEPENDENCY_JAR = "dependency.jar";

    public static final String RULES_PACKAGE = "org.smarthomej.automation.javarule.rules.user.";
    public static final String HELPER_PACKAGE = "org.smarthomej.automation.javarule.helper";

    public static final String RULES_DIR_START = "rules";

    public static final Path WORKING_DIR = Path.of(OpenHAB.getConfigFolder(), "automation", "javarule");
    public static final Path LIB_DIR = WORKING_DIR.resolve("deplib");
    public static final Path EXT_LIB_DIR = WORKING_DIR.resolve("lib");
    public static final Path RULES_DIR = WORKING_DIR
            .resolve(Path.of(RULES_DIR_START, "org", "smarthomej", "automation", "javarule", "rules", "user"));

    public static final String JAVA_FILE_TYPE = ".java";
    public static final Predicate<Path> JAVA_FILE_FILTER = p -> p.toString().endsWith(JAVA_FILE_TYPE);

    public static final String CLASS_FILE_TYPE = ".class";
    public static final Predicate<Path> CLASS_FILE_Filter = p -> p.toString().endsWith(CLASS_FILE_TYPE);

    public static final String JAVA_CLASS_PATH_PROPERTY = "java.class.path";

    public final static Map<Class<? extends Annotation>, String> TRIGGER_FROM_ANNOTATION = Map.ofEntries(
            Map.entry(ItemCommandTrigger.class, "core.ItemCommandTrigger"),
            Map.entry(ItemStateChangeTrigger.class, "core.ItemStateChangeTrigger"),
            Map.entry(ItemStateUpdateTrigger.class, "core.ItemStateUpdateTrigger"),
            Map.entry(GroupStateChangeTrigger.class, "core.GroupStateChangeTrigger"),
            Map.entry(GroupStateUpdateTrigger.class, "core.GroupStateUpdateTrigger"),
            Map.entry(ChannelEventTrigger.class, "core.ChannelEventTrigger"),
            Map.entry(GenericCronTrigger.class, "timer.GenericCronTrigger"),
            Map.entry(TimeOfDayTrigger.class, "timer.TimeOfDayTrigger"),
            Map.entry(GenericEventTrigger.class, "core.GenericEventTrigger"),
            Map.entry(ThingStatusUpdateTrigger.class, "core.ThingStatusUpdateTrigger"),
            Map.entry(ThingStatusChangeTrigger.class, "core.ThingStatusChangeTrigger"),
            Map.entry(SystemStartlevelTrigger.class, "core.SystemStartlevelTrigger"));

    public final static Map<Class<? extends Annotation>, String> CONDITION_FROM_ANNOTATION = Map.ofEntries(
            Map.entry(ItemStateCondition.class, "core.ItemStateCondition"),
            Map.entry(GenericCompareCondition.class, "core.GenericCompareCondition"),
            Map.entry(DayOfWeekCondition.class, "timer.DayOfWeekCondition"),
            Map.entry(EphemerisWeekdayCondition.class, "ephemeris.WeekdayCondition"),
            Map.entry(EphemerisWeekendCondition.class, "ephemeris.WeekendCondition"),
            Map.entry(EphemerisHolidayCondition.class, "ephemeris.HolidayCondition"),
            Map.entry(EphemerisNotHolidayCondition.class, "ephemeris.NotHolidayCondition"),
            Map.entry(EphemerisDaysetCondition.class, "ephemeris.DaysetCondition"),
            Map.entry(GenericEventCondition.class, "core.GenericEventCondition"),
            Map.entry(TimeOfDayCondition.class, "core.TimeOfDayCondition"));
}
