# openHAB 5.1.4 Installation and Configuration

This guide installs the local Fujitsu Airstage binding for openHAB 5.1.4.

## Build Prerequisites

Build on a machine with:

- Java Development Kit suitable for openHAB 5.1.4 add-on builds.
- Maven 3.9 or newer.
- Network access to Maven repositories.

This workspace currently has neither Java nor Maven installed, so the bundle was not built locally here.

## Build

From the repository root:

```sh
mvn clean package
```

The expected output is a Karaf/openHAB bundle JAR under:

```text
bundles/org.openhab.binding.fujitsuairstage/target/org.openhab.binding.fujitsuairstage-5.1.4.jar
```

If Maven cannot resolve the openHAB add-on parent POM in a standalone checkout, build inside a matching `openhab-addons` checkout:

```sh
git clone --branch 5.1.4 https://github.com/openhab/openhab-addons.git
cp -a bundles/org.openhab.binding.fujitsuairstage openhab-addons/bundles/
cd openhab-addons
mvn -pl :org.openhab.binding.fujitsuairstage -am clean package
```

## Install Into openHAB

Copy the built JAR into the openHAB add-ons directory:

```sh
sudo cp bundles/org.openhab.binding.fujitsuairstage/target/org.openhab.binding.fujitsuairstage-5.1.4.jar /usr/share/openhab/addons/
sudo systemctl restart openhab
```

On Docker installations, mount or copy the JAR into the container's `/openhab/addons` directory and restart the container.

## Thing Configuration

Create a Thing in Main UI or textual configuration.

Example `.things` file:

```text
Thing fujitsuairstage:ac:livingroom "Fujitsu Airstage" [
  host="192.0.2.10",
  deviceId="AABBCCDDEEFF",
  refreshInterval=30,
  timeout=3000
]
```

## Example Items

```text
Switch Fujitsu_Airstage_Power "Power" { channel="fujitsuairstage:ac:livingroom:power" }
Number Fujitsu_Airstage_Mode "Mode [%d]" { channel="fujitsuairstage:ac:livingroom:mode" }
Number:Temperature Fujitsu_Airstage_Target_Temperature "Target [%.1f %unit%]" { channel="fujitsuairstage:ac:livingroom:target-temperature" }
Number:Temperature Fujitsu_Airstage_Indoor_Temperature "Indoor [%.1f %unit%]" { channel="fujitsuairstage:ac:livingroom:indoor-temperature" }
Number:Temperature Fujitsu_Airstage_Outdoor_Temperature "Outdoor [%.1f %unit%]" { channel="fujitsuairstage:ac:livingroom:outdoor-temperature" }
Number Fujitsu_Airstage_Fan_Speed "Fan Speed [%d]" { channel="fujitsuairstage:ac:livingroom:fan-speed" }
Number Fujitsu_Airstage_Vertical_Direction "Vertical Direction [%d]" { channel="fujitsuairstage:ac:livingroom:vertical-direction" }
Switch Fujitsu_Airstage_Vertical_Swing "Vertical Swing" { channel="fujitsuairstage:ac:livingroom:vertical-swing" }
Switch Fujitsu_Airstage_Economy "Economy" { channel="fujitsuairstage:ac:livingroom:economy" }
Switch Fujitsu_Airstage_Powerful "Powerful" { channel="fujitsuairstage:ac:livingroom:powerful" }
Switch Fujitsu_Airstage_Minimum_Heat "Minimum Heat" { channel="fujitsuairstage:ac:livingroom:minimum-heat" }
Switch Fujitsu_Airstage_Low_Noise "Low Noise" { channel="fujitsuairstage:ac:livingroom:low-noise" }
String Fujitsu_Airstage_Error_Code "Error Code [%s]" { channel="fujitsuairstage:ac:livingroom:error-code" }
String Fujitsu_Airstage_Raw_Status "Raw Status [%s]" { channel="fujitsuairstage:ac:livingroom:raw-status" }
```

Mode values:

```text
0 = AUTO
1 = COOL
2 = DRY
3 = FAN
4 = HEAT
```

Fan speed values from community testing:

```text
0 = AUTO
2 = LOW
5 = NORMAL
8 = MEDIUM
11 = HIGH
```

## Notes

- `outdoor-temperature` is exposed, but the tested device returned an empty value. The binding reports this as `UNDEF`.
- `indoor-temperature` is converted from `iu_indoor_tmp` using centi-degrees Fahrenheit to Celsius.
- The binding uses one combined status request per poll interval.
- The local API is undocumented and reverse engineered, so firmware updates may change behavior.
