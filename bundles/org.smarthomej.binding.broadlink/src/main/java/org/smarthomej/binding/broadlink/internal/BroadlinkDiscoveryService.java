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
package org.smarthomej.binding.broadlink.internal;

import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.CONFIGURATION_DEVICE_TYPE;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.CONFIGURATION_IP_ADDRESS;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.CONFIGURATION_MAC_ADDRESS;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.SUPPORTED_THING_TYPE_UIDS;
import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.THING_TYPE_UID_RM;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.util.UIDUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.broadlink.internal.protocol.DiscoveryPacket;
import org.smarthomej.binding.broadlink.internal.protocol.DiscoveryResponsePacket;
import org.smarthomej.binding.broadlink.internal.types.BroadlinkDeviceType;

/**
 * The {@link BroadlinkDiscoveryService} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { BroadlinkDiscoveryService.class, DiscoveryService.class })
@NonNullByDefault
public class BroadlinkDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(BroadlinkDiscoveryService.class);

    private final NetworkAddressService networkAddressService;

    private boolean discoveryRunning = false;

    @Activate
    public BroadlinkDiscoveryService(@Reference NetworkAddressService networkAddressService)
            throws IllegalArgumentException {
        super(SUPPORTED_THING_TYPE_UIDS, 30);

        this.networkAddressService = networkAddressService;
    }

    @Override
    protected void startScan() {
        if (!discoveryRunning) {
            discoveryRunning = true;
            scheduler.execute(this::discover);
        }
    }

    @Override
    protected void stopScan() {
        discoveryRunning = false;
    }

    private void discover() {
        logger.debug("Starting discovery cycle");
        try {
            String ipAddress = networkAddressService.getPrimaryIpv4HostAddress();
            if (ipAddress == null) {
                logger.warn("Failed initialize discovery: could not determine primary network address");
                return;
            }
            SocketAddress address = new InetSocketAddress(InetAddress.getByName(ipAddress), 0);
            try (DatagramSocket socket = new DatagramSocket(address)) {
                socket.setReuseAddress(true);
                socket.setSoTimeout(1000);

                // send discovery packet
                DiscoveryPacket discoveryPacket = new DiscoveryPacket(socket.getLocalAddress().getAddress(),
                        socket.getLocalPort());
                socket.send(discoveryPacket.getDatagramPacket());

                while (discoveryRunning) {
                    byte[] buf = new byte[512];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                        DiscoveryResponsePacket discoveryResponsePacket = new DiscoveryResponsePacket(packet);
                        logger.debug("Received discovery response packet: {}", discoveryResponsePacket);

                        int deviceType = discoveryResponsePacket.getDeviceType();
                        String macAddress = discoveryResponsePacket.getMac();
                        String remoteAddress = discoveryResponsePacket.getRemoteAddress();
                        String deviceName = discoveryResponsePacket.getDeviceName();

                        BroadlinkDeviceType broadlinkDeviceType = BroadlinkDeviceType
                                .fromInt(discoveryResponsePacket.getDeviceType());

                        ThingTypeUID thingTypeUID = null;
                        switch (broadlinkDeviceType) {
                            case RM:
                            case RM2:
                            case RM3:
                            case RM3Q:
                            case RM4:
                                thingTypeUID = THING_TYPE_UID_RM;
                                if (deviceName.isBlank()) {
                                    deviceName = "Broadlink Remote Control";
                                }
                                break;
                            default:
                        }

                        if (thingTypeUID != null) {
                            ThingUID thingUID = new ThingUID(thingTypeUID, UIDUtils.encode(macAddress));
                            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                                    .withThingType(thingTypeUID).withLabel(deviceName)
                                    .withRepresentationProperty(CONFIGURATION_MAC_ADDRESS)
                                    .withProperty(CONFIGURATION_MAC_ADDRESS, macAddress)
                                    .withProperty(CONFIGURATION_IP_ADDRESS, remoteAddress)
                                    .withProperty(CONFIGURATION_DEVICE_TYPE, deviceType).build();
                            thingDiscovered(discoveryResult);
                        }
                    } catch (SocketTimeoutException e) {
                        // do nothing
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to initialize discovery: {}", e.getMessage());
        }

        removeOlderResults(getTimestampOfLastScan());
    }
}
