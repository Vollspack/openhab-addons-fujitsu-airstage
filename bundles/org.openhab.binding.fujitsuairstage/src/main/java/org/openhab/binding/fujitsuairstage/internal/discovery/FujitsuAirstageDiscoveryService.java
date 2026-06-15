/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fujitsuairstage.internal.discovery;

import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CONFIG_DEVICE_ID;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CONFIG_HOST;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.THING_TYPE_AC;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageApiClient;
import org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageConfiguration;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manual discovery for Fujitsu Airstage WLAN modules on directly connected IPv4 networks.
 *
 * @author Codex - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.fujitsuairstage")
public class FujitsuAirstageDiscoveryService extends AbstractDiscoveryService {
    private static final int DISCOVERY_TIMEOUT_SECONDS = 30;
    private static final int API_TIMEOUT_MILLIS = 1500;
    private static final int SOCKET_PROBE_TIMEOUT_MILLIS = 200;
    private static final int MAX_ADDRESSES_PER_INTERFACE = 254;
    private static final Pattern MAC_PATTERN = Pattern.compile("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$");

    private final Logger logger = LoggerFactory.getLogger(FujitsuAirstageDiscoveryService.class);
    private final HttpClientFactory httpClientFactory;

    @Activate
    public FujitsuAirstageDiscoveryService(@Reference HttpClientFactory httpClientFactory) {
        super(Set.of(THING_TYPE_AC), DISCOVERY_TIMEOUT_SECONDS, false);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Fujitsu Airstage discovery scan");
        scheduler.execute(this::scan);
    }

    private void scan() {
        Set<String> addresses = discoverLocalSubnetAddresses();
        if (addresses.isEmpty()) {
            logger.debug("No local IPv4 subnet addresses found for Fujitsu Airstage discovery");
            return;
        }

        probeHttpPort(addresses);

        Map<String, String> arpEntries = readArpEntries();
        arpEntries.forEach((host, macAddress) -> {
            if (!addresses.contains(host)) {
                return;
            }

            String deviceId = toDeviceId(macAddress);
            if (verifyAirstageDevice(host, deviceId)) {
                ThingUID thingUID = new ThingUID(THING_TYPE_AC, deviceId.toLowerCase(Locale.ROOT));
                DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withProperty(CONFIG_HOST, host)
                        .withProperty(CONFIG_DEVICE_ID, deviceId).withRepresentationProperty(CONFIG_DEVICE_ID)
                        .withLabel("Fujitsu Airstage " + macAddress.toUpperCase(Locale.ROOT)).build();
                thingDiscovered(result);
            }
        });
    }

    private boolean verifyAirstageDevice(String host, String deviceId) {
        FujitsuAirstageConfiguration configuration = new FujitsuAirstageConfiguration();
        configuration.host = host;
        configuration.deviceId = deviceId;
        configuration.timeout = API_TIMEOUT_MILLIS;

        try {
            new FujitsuAirstageApiClient(httpClientFactory, configuration).getStatus();
            logger.debug("Discovered Fujitsu Airstage device at {}", host);
            return true;
        } catch (Exception e) {
            logger.trace("Host {} is not a Fujitsu Airstage device using discovered MAC {}", host, deviceId, e);
            return false;
        }
    }

    private Set<String> discoverLocalSubnetAddresses() {
        Set<String> addresses = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address instanceof Inet4Address inet4Address) {
                        addresses
                                .addAll(addressesForInterface(inet4Address, interfaceAddress.getNetworkPrefixLength()));
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Could not enumerate network interfaces for Fujitsu Airstage discovery", e);
        }
        return addresses;
    }

    private Set<String> addressesForInterface(Inet4Address interfaceAddress, short prefixLength) {
        int scanPrefix = prefixLength < 24 ? 24 : prefixLength;
        long hostCount = 1L << (32 - scanPrefix);
        if (hostCount > MAX_ADDRESSES_PER_INTERFACE + 2L || hostCount < 4) {
            return Set.of();
        }

        long ownAddress = ipv4ToLong(interfaceAddress);
        long mask = 0xffffffffL << (32 - scanPrefix) & 0xffffffffL;
        long network = ownAddress & mask;
        long broadcast = network | (~mask & 0xffffffffL);

        Set<String> addresses = new HashSet<>();
        for (long current = network + 1; current < broadcast; current++) {
            if (current != ownAddress) {
                addresses.add(longToIpv4(current));
            }
        }
        return addresses;
    }

    private void probeHttpPort(Set<String> addresses) {
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        try {
            for (String address : addresses) {
                executorService.submit(() -> probeHttpPort(address));
            }
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
    }

    private void probeHttpPort(String address) {
        SocketAddress socketAddress = new InetSocketAddress(address, 80);
        try (Socket socket = new Socket()) {
            socket.connect(socketAddress, SOCKET_PROBE_TIMEOUT_MILLIS);
        } catch (IOException e) {
            logger.trace("No HTTP port reached at {}", address, e);
        }
    }

    private Map<String, String> readArpEntries() {
        Path arpTable = Path.of("/proc/net/arp");
        if (!Files.isReadable(arpTable)) {
            logger.debug("ARP table {} is not readable; Fujitsu Airstage discovery cannot derive device IDs", arpTable);
            return Map.of();
        }

        try (Stream<String> lines = Files.lines(arpTable)) {
            return lines.skip(1).map(line -> line.trim().split("\\s+")).filter(parts -> parts.length >= 4)
                    .filter(parts -> MAC_PATTERN.matcher(parts[3]).matches()).filter(parts -> !isZeroMac(parts[3]))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[3], (first, second) -> first));
        } catch (IOException e) {
            logger.debug("Could not read ARP table for Fujitsu Airstage discovery", e);
            return Map.of();
        }
    }

    private static boolean isZeroMac(String macAddress) {
        return "00:00:00:00:00:00".equals(macAddress);
    }

    private static String toDeviceId(String macAddress) {
        return macAddress.replace(":", "").toUpperCase(Locale.ROOT);
    }

    private static long ipv4ToLong(Inet4Address address) {
        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xffL) << 24 | (bytes[1] & 0xffL) << 16 | (bytes[2] & 0xffL) << 8 | bytes[3] & 0xffL;
    }

    private static String longToIpv4(long address) {
        return String.format(Locale.ROOT, "%d.%d.%d.%d", address >> 24 & 0xff, address >> 16 & 0xff,
                address >> 8 & 0xff, address & 0xff);
    }
}
