# Fujitsu Airstage

Local openHAB 5.1.4 binding and API notes for a Fujitsu Airstage WLAN module.

## Device

- Air conditioner: Fujitsu `ASEH07KGTG`
- WLAN module host: `airstage.dhcp.internal.hennecke-net.org`
- IPv4 address: `192.0.2.10`
- MAC address: `aa:bb:cc:dd:ee:ff`
- Local API `device_id`: `AABBCCDDEEFF`

The local API does not require cloud credentials. The `device_id` is the MAC address without colons and must be uppercase.

## Contents

- [docs/api-notes.md](docs/api-notes.md): verified local REST API behavior.
- [docs/openhab-installation.md](docs/openhab-installation.md): build, installation, and configuration guide for openHAB 5.1.4.
- [docs/local-openhab-test-instance.md](docs/local-openhab-test-instance.md): local persisted openHAB 5.1.4 test runtime in this workspace.
- [bundles/org.openhab.binding.fujitsuairstage](bundles/org.openhab.binding.fujitsuairstage): openHAB binding source.

## Current Binding Scope

The first binding version targets local LAN control:

- Read power, mode, fan speed, setpoint, indoor temperature, vertical airflow, feature flags, and error code.
- Write power, mode, fan speed, setpoint, vertical airflow, economy, powerful, minimum heat, low noise, and fan control.
- Poll the device with one combined `GetParam` request.
- Treat empty or unsupported outdoor temperature as `UNDEF`.

Cloud login, account discovery, and cloud device management are intentionally out of scope.
