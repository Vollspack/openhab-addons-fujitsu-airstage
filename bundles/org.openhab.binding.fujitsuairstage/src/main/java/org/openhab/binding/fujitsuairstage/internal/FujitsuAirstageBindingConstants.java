/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.fujitsuairstage.internal;

import java.util.Set;

import org.openhab.core.thing.ThingTypeUID;

/**
 * Constants for the Fujitsu Airstage binding.
 *
 * @author Codex - Initial contribution
 */
public final class FujitsuAirstageBindingConstants {
    public static final String BINDING_ID = "fujitsuairstage";

    public static final ThingTypeUID THING_TYPE_AC = new ThingTypeUID(BINDING_ID, "ac");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_AC);

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_DEVICE_ID = "deviceId";

    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_MODE = "mode";
    public static final String CHANNEL_TARGET_TEMPERATURE = "target-temperature";
    public static final String CHANNEL_INDOOR_TEMPERATURE = "indoor-temperature";
    public static final String CHANNEL_OUTDOOR_TEMPERATURE = "outdoor-temperature";
    public static final String CHANNEL_FAN_SPEED = "fan-speed";
    public static final String CHANNEL_VERTICAL_DIRECTION = "vertical-direction";
    public static final String CHANNEL_VERTICAL_SWING = "vertical-swing";
    public static final String CHANNEL_ECONOMY = "economy";
    public static final String CHANNEL_POWERFUL = "powerful";
    public static final String CHANNEL_MINIMUM_HEAT = "minimum-heat";
    public static final String CHANNEL_LOW_NOISE = "low-noise";
    public static final String CHANNEL_FAN_CONTROL = "fan-control";
    public static final String CHANNEL_HUMAN_DETECT_AUTO_SAVE = "human-detect-auto-save";
    public static final String CHANNEL_ERROR_CODE = "error-code";
    public static final String CHANNEL_RAW_STATUS = "raw-status";

    private FujitsuAirstageBindingConstants() {
    }
}
