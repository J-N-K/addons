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
package org.smarthomej.automation.jrule.internal;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.jrule.internal.events.JRuleEventSubscriber;
import org.smarthomej.automation.jrule.internal.handler.JRuleHandler;

/**
 * The {@link JRuleFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Joseph (Seaside) Hagberg - Initial contribution
 */
@Component(configurationPid = "automation.jrule")
@NonNullByDefault
public class JRuleFactory {

    private static final long INIT_DELAY_DEFAULT = 5;
    private final ItemRegistry itemRegistry;
    private final JRuleEventSubscriber eventSubscriber;
    private final EventPublisher eventPublisher;
    private final VoiceManager voiceManager;
    private final JRuleHandler jRuleHandler;
    @Nullable
    private static CompletableFuture<Void> initFuture = null;

    private static final Logger logger = LoggerFactory.getLogger(JRuleFactory.class);
    private static final Object INIT_DELAY_PROPERTY = "init.delay";
    private static final int DEFAULT_INIT_DELAY = 5;

    @Activate
    public JRuleFactory(Map<String, Object> properties, final @Reference JRuleEventSubscriber eventSubscriber,
            final @Reference ItemRegistry itemRegistry, final @Reference EventPublisher eventPublisher,
            final @Reference VoiceManager voiceManager) {
        this.itemRegistry = itemRegistry;
        this.eventSubscriber = eventSubscriber;
        this.eventPublisher = eventPublisher;
        this.voiceManager = voiceManager;
        jRuleHandler = new JRuleHandler(properties, itemRegistry, eventPublisher, eventSubscriber, voiceManager);
        createDelayedInitialization(getInitDelaySeconds(properties));
    }

    private int getInitDelaySeconds(Map<String, Object> properties) {
        Object initDelay = properties.get(INIT_DELAY_PROPERTY);
        int delay = DEFAULT_INIT_DELAY;
        if (initDelay != null) {
            try {
                delay = Integer.parseInt((String) initDelay);
            } catch (Exception x) {
                // Best effort
            }
        }
        return delay;
    }

    private synchronized CompletableFuture<Void> createDelayedInitialization(int delayInSeconds) {
        initFuture = JRuleUtil.delayedExecution(delayInSeconds, TimeUnit.SECONDS);
        return initFuture.thenAccept(s -> {
            logger.info("Initializing Java Rules Engine");
            jRuleHandler.initialize();
            initFuture = null;
        });
    }

    @Deactivate
    public synchronized void dispose() {
        if (initFuture != null) {
            initFuture.cancel(true);
            initFuture = null;
        }
        jRuleHandler.dispose();
    }
}
