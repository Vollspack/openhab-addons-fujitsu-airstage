# Quick JAR Installation

This guide is for users who want to install the prebuilt JAR and add their Fujitsu Airstage air conditioner through openHAB Main UI autodiscovery.

## Requirements

- openHAB `5.1.4`.
- Java `21`, as required by openHAB 5.
- The Fujitsu Airstage WLAN module must be reachable from the openHAB host on the same local IPv4 network.
- Shell access to the openHAB host or another way to copy files into the openHAB `addons` directory.

## 1. Download the JAR

Download the release asset:

```text
org.openhab.binding.fujitsuairstage-5.1.4.jar
```

From the GitHub release page:

```text
https://github.com/Vollspack/openhab-addons-fujitsu-airstage/releases/tag/1.0.0
```

## 2. Copy the JAR into openHAB

On a package-based openHAB installation, copy the JAR to:

```text
/usr/share/openhab/addons/
```

Example:

```sh
sudo cp org.openhab.binding.fujitsuairstage-5.1.4.jar /usr/share/openhab/addons/
sudo chown openhab:openhab /usr/share/openhab/addons/org.openhab.binding.fujitsuairstage-5.1.4.jar
sudo chmod 0644 /usr/share/openhab/addons/org.openhab.binding.fujitsuairstage-5.1.4.jar
```

For Docker installations, copy or mount the JAR into the container's `/openhab/addons` directory.

## 3. Restart openHAB

Package-based installation:

```sh
sudo systemctl restart openhab
```

Docker installation:

```sh
docker restart <openhab-container-name>
```

Wait until openHAB Main UI is reachable again.

## 4. Start Autodiscovery

In openHAB Main UI:

1. Go to `Settings`.
2. Open `Things`.
3. Select `+` / `Add Thing`.
4. Choose `Fujitsu Airstage Binding`.
5. Press `Scan`.

The scan checks directly connected local IPv4 networks, derives candidate Device IDs from MAC addresses, and verifies candidates with the local Airstage API.

## 5. Add the Discovered Thing

After a successful scan, Main UI should show a discovered Fujitsu Airstage Thing.

1. Open the discovered Thing.
2. Verify that `Host` is filled with the module IP address.
3. Verify that `Device ID` is filled with the WLAN module MAC address without colons.
4. Add the Thing.

The Device ID format is uppercase MAC without separators, for example:

```text
AABBCCDDEEFF
```

## 6. Link Items

Open the newly created Thing and link the channels you want to use.

Typical channels:

- `power`
- `mode`
- `target-temperature`
- `indoor-temperature`
- `outdoor-temperature`
- `fan-speed`
- `vertical-direction`
- `vertical-swing`
- `economy`
- `powerful`
- `minimum-heat`
- `low-noise`
- `fan-control`
- `human-detect-auto-save`
- `error-code`
- `raw-status`

For temperature channels, openHAB should create `Number:Temperature` Items and display values with temperature units.

## Troubleshooting

- If the binding does not appear in `Choose Binding`, verify that the JAR is in the openHAB `addons` directory and restart openHAB.
- If discovery does not find the device, verify that the openHAB host and WLAN module are on the same local IPv4 network.
- If discovery still fails, create the Thing manually and enter the module IP address plus Device ID.
- If the Thing is offline, verify the Device ID casing. The local API expects uppercase MAC without colons.
- `outdoor-temperature` may remain `UNDEF` when the device does not report an outdoor temperature value.
