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
package org.smarthomej.automation.jrule.internal.watch;

import static java.nio.file.StandardWatchEventKinds.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.jrule.internal.JRuleConstants;

/**
 * The {@link JRuleRulesWatcher}
 *
 * @author Joseph (Seaside) Hagberg - Initial contribution
 */
public class JRuleRulesWatcher implements Runnable {

    private static final String BASIC_IS_DIRECTORY = "basic:isDirectory";

    private final Path watchFolder;

    private final Logger logger = LoggerFactory.getLogger(JRuleRulesWatcher.class);

    public static final String PROPERTY_ENTRY_CREATE = "ENTRY_CREATE";
    public static final String PROPERTY_ENTRY_MODIFY = "ENTRY_MODIFY";
    public static final String PROPERTY_ENTRY_DELETE = "ENTRY_DELETE";

    private final PropertyChangeSupport propertyChangeSupport;

    public JRuleRulesWatcher(Path watchFolder) {
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.watchFolder = watchFolder;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        logger.debug("Adding listener for watcher");
        propertyChangeSupport.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        logger.debug("Adding listener for Watcher");
        propertyChangeSupport.removePropertyChangeListener(pcl);
    }

    @Override
    public void run() {
        try {
            Boolean isFolder = (Boolean) Files.getAttribute(watchFolder, BASIC_IS_DIRECTORY);
            if (!isFolder) {
                logger.error("Failed to watch folder since it is not a directory: {}",
                        watchFolder.toFile().getAbsolutePath());
                return;
            }
        } catch (IOException ioe) {
            logger.error("Failed to start watching folder: {}", watchFolder.toFile().getAbsolutePath(), ioe);
            return;
        }
        logger.debug("Watching for rule changes: {}", watchFolder);
        FileSystem fs = watchFolder.getFileSystem();
        try {
            WatchService service = fs.newWatchService();
            watchFolder.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key = null;
            while (true) {
                key = service.take();
                Kind<?> kind = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    kind = watchEvent.kind();
                    if (OVERFLOW == kind) {
                        logger.debug("overflow");
                        continue;
                    }
                    Path newPath = ((WatchEvent<Path>) watchEvent).context();
                    if (!newPath.getFileName().toString().endsWith(JRuleConstants.JAVA_FILE_TYPE)) {
                        continue;
                    }
                    if (ENTRY_CREATE == kind) {
                        logger.debug("New Path created in watchFolder");
                        propertyChangeSupport.firePropertyChange(PROPERTY_ENTRY_CREATE, null, newPath);
                    } else if (ENTRY_MODIFY == kind) {
                        logger.debug("New path modified: {} fn: {}", newPath, newPath.getFileName());
                        propertyChangeSupport.firePropertyChange(PROPERTY_ENTRY_MODIFY, null, newPath);
                    } else if (ENTRY_DELETE == kind) {
                        logger.debug("New path deleted: {}", newPath);
                        propertyChangeSupport.firePropertyChange(PROPERTY_ENTRY_DELETE, null, newPath);
                    } else {
                        logger.debug("Unhandled case: {}", kind.name());
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Watcher Thread interrupted, closing down");
            return;
        } catch (Exception e) {
            logger.error("Folder watcher terminated due to exception", e);
            return;
        }
    }
}
