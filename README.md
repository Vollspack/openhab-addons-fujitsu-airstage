Disclaimer: This project is independent and not affiliated with, authorized, or endorsed by Fujitsu. All product and company names are trademarks™ or registered® trademarks of their respective holders. Use of them does not imply any affiliation with or endorsement by them.

# Inoffical Fujitsu Airstage AddOn for OpenHAB

Local openHAB 5.1.4 binding and API notes for Fujitsu Airstage WLAN modules.

## Device Configuration

Configure each Thing with:

- WLAN module host: hostname or IPv4 address.
- Device ID: WLAN module MAC address without colons, uppercase, for example `AABBCCDDEEFF`.

The local API does not require cloud credentials. The `device_id` is the MAC address without colons and must be uppercase.

## Contents

- [docs/api-notes.md](docs/api-notes.md): verified local REST API behavior.
- [docs/openhab-installation.md](docs/openhab-installation.md): build, installation, and configuration guide for openHAB 5.1.4.
- [bundles/org.openhab.binding.fujitsuairstage](bundles/org.openhab.binding.fujitsuairstage): openHAB binding source.

## Current Binding Scope

The first binding version targets local LAN control:

- Read power, mode, fan speed, setpoint, indoor temperature, vertical airflow, feature flags, and error code.
- Write power, mode, fan speed, setpoint, vertical airflow, economy, powerful, minimum heat, low noise, and fan control.
- Poll the device with one combined `GetParam` request.
- Treat empty or unsupported outdoor temperature as `UNDEF`.
- Discover devices from Main UI by scanning local IPv4 networks and verifying candidates with the local API.

Cloud login, account discovery, and cloud device management are intentionally out of scope.
