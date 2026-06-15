/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fujitsuairstage.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.io.net.http.HttpClientFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Small client for the undocumented local Fujitsu Airstage REST API.
 *
 * @author Codex - Initial contribution
 */
@NonNullByDefault
public class FujitsuAirstageApiClient {
    private static final String[] STATUS_PARAMETERS = { "iu_onoff", "iu_op_mode", "iu_fan_spd", "iu_set_tmp",
            "iu_af_inc_vrt", "iu_af_dir_vrt", "iu_af_swg_vrt", "iu_af_swg_hrz", "iu_af_dir_hrz", "ou_low_noise",
            "iu_fan_ctrl", "iu_hmn_det_auto_save", "iu_min_heat", "iu_powerful", "iu_economy", "iu_err_code",
            "iu_demand", "iu_fltr_sign_reset", "iu_indoor_tmp", "ou_outdoor_tmp" };

    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String deviceId;
    private final int timeout;

    public FujitsuAirstageApiClient(HttpClientFactory httpClientFactory, FujitsuAirstageConfiguration configuration) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.baseUrl = normalizeBaseUrl(configuration.host);
        this.deviceId = normalizeDeviceId(configuration.deviceId);
        this.timeout = configuration.timeout;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public AirstageStatus getStatus()
            throws InterruptedException, TimeoutException, ExecutionException, AirstageApiException {
        JsonObject request = baseRequest("03");
        request.add("list", gson.toJsonTree(STATUS_PARAMETERS));
        JsonObject response = post("GetParam", request);
        if (!isOk(response)) {
            throw new AirstageApiException("GetParam failed with error " + getString(response, "error"));
        }
        return new AirstageStatus(response);
    }

    public AirstageStatus setValue(String parameter, String value)
            throws InterruptedException, TimeoutException, ExecutionException, AirstageApiException {
        JsonObject values = new JsonObject();
        values.add(parameter, new JsonPrimitive(value));
        JsonObject request = baseRequest("02");
        request.add("value", values);
        JsonObject response = post("SetParam", request);
        if (!isOk(response)) {
            throw new AirstageApiException("SetParam failed with error " + getString(response, "error"));
        }
        return new AirstageStatus(response);
    }

    private JsonObject post(String path, JsonObject request)
            throws InterruptedException, TimeoutException, ExecutionException, AirstageApiException {
        ContentResponse response = httpClient.newRequest(baseUrl + path).method(HttpMethod.POST)
                .header(HttpHeader.CONTENT_TYPE, "text/plain").content(new StringContentProvider(gson.toJson(request)),
                        "text/plain")
                .timeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS).send();
        if (response.getStatus() != HttpStatus.OK_200) {
            throw new AirstageApiException("HTTP status " + response.getStatus());
        }
        return gson.fromJson(response.getContentAsString(), JsonObject.class);
    }

    private JsonObject baseRequest(String setLevel) {
        JsonObject request = new JsonObject();
        request.add("device_id", new JsonPrimitive(deviceId));
        request.add("device_sub_id", new JsonPrimitive(0));
        request.add("req_id", new JsonPrimitive(""));
        request.add("modified_by", new JsonPrimitive(""));
        request.add("set_level", new JsonPrimitive(setLevel));
        return request;
    }

    private static boolean isOk(JsonObject response) {
        return "OK".equals(getString(response, "result"));
    }

    private static String getString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return "";
        }
        return object.get(name).getAsString();
    }

    private static String normalizeBaseUrl(String host) {
        String trimmed = Objects.requireNonNullElse(host, "").trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.endsWith("/") ? trimmed : trimmed + "/";
        }
        return "http://" + trimmed + "/";
    }

    private static String normalizeDeviceId(String deviceId) {
        return Objects.requireNonNullElse(deviceId, "").replace(":", "").trim().toUpperCase();
    }

    public static final class AirstageStatus {
        private final JsonObject root;
        private final JsonObject value;

        AirstageStatus(JsonObject root) {
            this.root = root;
            this.value = root.has("value") && root.get("value").isJsonObject() ? root.getAsJsonObject("value")
                    : new JsonObject();
        }

        public String rawJson() {
            return root.toString();
        }

        public String get(String parameter) {
            if (!value.has(parameter) || value.get(parameter).isJsonNull()) {
                return "";
            }
            return value.get(parameter).getAsString();
        }

        public Map<String, String> asMap() {
            return value.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                    entry -> entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString()));
        }
    }

    public static class AirstageApiException extends Exception {
        private static final long serialVersionUID = 1L;

        public AirstageApiException(String message) {
            super(message);
        }
    }
}
