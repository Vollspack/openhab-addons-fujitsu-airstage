/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fujitsuairstage.internal;

/**
 * Thing configuration for a local Fujitsu Airstage WLAN module.
 *
 * @author Codex - Initial contribution
 */
public class FujitsuAirstageConfiguration {
    public String host = "";
    public String deviceId = "";
    public int refreshInterval = 30;
    public int timeout = 3000;
}
