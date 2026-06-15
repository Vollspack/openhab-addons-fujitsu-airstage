/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fujitsuairstage.internal;

import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_ECONOMY;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_ERROR_CODE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_FAN_CONTROL;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_FAN_SPEED;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_HUMAN_DETECT_AUTO_SAVE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_INDOOR_TEMPERATURE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_LOW_NOISE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_MINIMUM_HEAT;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_MODE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_OUTDOOR_TEMPERATURE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_POWER;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_POWERFUL;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_RAW_STATUS;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_TARGET_TEMPERATURE;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_VERTICAL_DIRECTION;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.CHANNEL_VERTICAL_SWING;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageApiClient.AirstageApiException;
import org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageApiClient.AirstageStatus;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thing handler for a local Fujitsu Airstage air conditioner.
 *
 * @author Codex - Initial contribution
 */
@NonNullByDefault
public class FujitsuAirstageHandler extends BaseThingHandler {
    private static final Map<String, String> BOOLEAN_CHANNELS = Map.of(CHANNEL_POWER, "iu_onoff",
            CHANNEL_VERTICAL_SWING, "iu_af_swg_vrt", CHANNEL_ECONOMY, "iu_economy", CHANNEL_POWERFUL, "iu_powerful",
            CHANNEL_MINIMUM_HEAT, "iu_min_heat", CHANNEL_LOW_NOISE, "ou_low_noise", CHANNEL_FAN_CONTROL, "iu_fan_ctrl",
            CHANNEL_HUMAN_DETECT_AUTO_SAVE, "iu_hmn_det_auto_save");

    private static final Map<String, String> NUMBER_CHANNELS = Map.of(CHANNEL_MODE, "iu_op_mode", CHANNEL_FAN_SPEED,
            "iu_fan_spd", CHANNEL_VERTICAL_DIRECTION, "iu_af_dir_vrt");

    private final Logger logger = LoggerFactory.getLogger(FujitsuAirstageHandler.class);
    private final HttpClientFactory httpClientFactory;

    private @Nullable FujitsuAirstageApiClient client;
    private @Nullable ScheduledFuture<?> pollingJob;

    public FujitsuAirstageHandler(Thing thing, HttpClientFactory httpClientFactory) {
        super(thing);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void initialize() {
        FujitsuAirstageConfiguration configuration = getConfigAs(FujitsuAirstageConfiguration.class);
        if (configuration.host == null || configuration.host.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Host must be configured");
            return;
        }
        if (configuration.deviceId == null || configuration.deviceId.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID must be configured");
            return;
        }

        client = new FujitsuAirstageApiClient(httpClientFactory, configuration);
        int refreshInterval = Math.max(5, configuration.refreshInterval);
        updateStatus(ThingStatus.UNKNOWN);
        pollingJob = scheduler.scheduleWithFixedDelay(this::poll, 0, refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
        client = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::poll);
            return;
        }

        FujitsuAirstageApiClient localClient = client;
        if (localClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Client not initialized");
            return;
        }

        scheduler.execute(() -> {
            try {
                String channelId = channelUID.getIdWithoutGroup();
                String booleanParameter = BOOLEAN_CHANNELS.get(channelId);
                String numberParameter = NUMBER_CHANNELS.get(channelId);

                if (CHANNEL_TARGET_TEMPERATURE.equals(channelId)) {
                    localClient.setValue("iu_set_tmp", commandToTargetTemperature(command));
                } else if (booleanParameter != null) {
                    localClient.setValue(booleanParameter, commandToBooleanValue(command));
                } else if (numberParameter != null) {
                    localClient.setValue(numberParameter, commandToIntegerValue(command));
                } else {
                    logger.debug("Ignoring command for unsupported channel {}", channelId);
                    return;
                }
                poll();
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid command {} for channel {}", command, channelUID, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (ExecutionException | TimeoutException | AirstageApiException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }

    private void poll() {
        FujitsuAirstageApiClient localClient = client;
        if (localClient == null) {
            return;
        }
        try {
            AirstageStatus status = localClient.getStatus();
            updateFromStatus(status);
            updateStatus(ThingStatus.ONLINE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (ExecutionException | TimeoutException | AirstageApiException | RuntimeException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateFromStatus(AirstageStatus status) {
        updateState(CHANNEL_POWER, toOnOff(status.get("iu_onoff")));
        updateState(CHANNEL_MODE, toDecimal(status.get("iu_op_mode")));
        updateState(CHANNEL_FAN_SPEED, toDecimal(status.get("iu_fan_spd")));
        updateState(CHANNEL_TARGET_TEMPERATURE, targetTemperature(status.get("iu_set_tmp")));
        updateState(CHANNEL_INDOOR_TEMPERATURE, indoorTemperature(status.get("iu_indoor_tmp")));
        updateState(CHANNEL_OUTDOOR_TEMPERATURE, outdoorTemperature(status.get("ou_outdoor_tmp")));
        updateState(CHANNEL_VERTICAL_DIRECTION, toDecimal(status.get("iu_af_dir_vrt")));
        updateState(CHANNEL_VERTICAL_SWING, toOnOff(status.get("iu_af_swg_vrt")));
        updateState(CHANNEL_ECONOMY, toOnOff(status.get("iu_economy")));
        updateState(CHANNEL_POWERFUL, toOnOff(status.get("iu_powerful")));
        updateState(CHANNEL_MINIMUM_HEAT, toOnOff(status.get("iu_min_heat")));
        updateState(CHANNEL_LOW_NOISE, toOnOff(status.get("ou_low_noise")));
        updateState(CHANNEL_FAN_CONTROL, toOnOff(status.get("iu_fan_ctrl")));
        updateState(CHANNEL_HUMAN_DETECT_AUTO_SAVE, toOnOff(status.get("iu_hmn_det_auto_save")));
        updateState(CHANNEL_ERROR_CODE, new org.openhab.core.library.types.StringType(status.get("iu_err_code")));
        updateState(CHANNEL_RAW_STATUS, new org.openhab.core.library.types.StringType(status.rawJson()));
    }

    private static State toOnOff(String rawValue) {
        if (rawValue.isBlank() || "65535".equals(rawValue)) {
            return UnDefType.UNDEF;
        }
        return "1".equals(rawValue) ? OnOffType.ON : OnOffType.OFF;
    }

    private static State toDecimal(String rawValue) {
        if (rawValue.isBlank() || "65535".equals(rawValue)) {
            return UnDefType.UNDEF;
        }
        return new DecimalType(rawValue);
    }

    private static State targetTemperature(String rawValue) {
        BigDecimal raw = parseNullableDecimal(rawValue);
        if (raw == null) {
            return UnDefType.UNDEF;
        }
        return celsius(raw.divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP));
    }

    private static State indoorTemperature(String rawValue) {
        BigDecimal raw = parseNullableDecimal(rawValue);
        if (raw == null) {
            return UnDefType.UNDEF;
        }
        BigDecimal fahrenheit = raw.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal celsius = fahrenheit.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(9), 1, RoundingMode.HALF_UP);
        return celsius(celsius);
    }

    private static State outdoorTemperature(String rawValue) {
        BigDecimal raw = parseNullableDecimal(rawValue);
        if (raw == null) {
            return UnDefType.UNDEF;
        }
        BigDecimal fahrenheit = raw.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal celsius = fahrenheit.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(9), 1, RoundingMode.HALF_UP);
        return celsius(celsius);
    }

    private static QuantityType<Temperature> celsius(BigDecimal value) {
        return new QuantityType<>(value, SIUnits.CELSIUS);
    }

    private static @Nullable BigDecimal parseNullableDecimal(String rawValue) {
        if (rawValue.isBlank() || "65535".equals(rawValue)) {
            return null;
        }
        return new BigDecimal(rawValue);
    }

    private static String commandToBooleanValue(Command command) {
        if (command instanceof OnOffType) {
            return OnOffType.ON.equals(command) ? "1" : "0";
        }
        String value = command.toString();
        if ("1".equals(value) || "ON".equalsIgnoreCase(value)) {
            return "1";
        }
        if ("0".equals(value) || "OFF".equalsIgnoreCase(value)) {
            return "0";
        }
        throw new IllegalArgumentException("Expected ON/OFF command");
    }

    private static String commandToIntegerValue(Command command) {
        if (command instanceof DecimalType decimal) {
            return Integer.toString(decimal.intValue());
        }
        return Integer.toString(new BigDecimal(command.toString()).intValue());
    }

    private static String commandToTargetTemperature(Command command) {
        BigDecimal celsius;
        if (command instanceof QuantityType<?> quantityCommand) {
            QuantityType<?> converted = quantityCommand.toUnit(SIUnits.CELSIUS);
            if (converted == null) {
                throw new IllegalArgumentException("Temperature command cannot be converted to Celsius");
            }
            celsius = converted.toBigDecimal();
        } else if (command instanceof DecimalType decimalCommand) {
            celsius = decimalCommand.toBigDecimal();
        } else {
            celsius = new BigDecimal(command.toString());
        }
        return celsius.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
