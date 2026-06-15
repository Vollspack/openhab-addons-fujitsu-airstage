# openHAB 5.1.4 Installation and Configuration

This guide installs the local Fujitsu Airstage binding for openHAB 5.1.4.

## Build Prerequisites

Build on a machine with:

- Java Development Kit 21.
- Maven 3.9 or newer.
- Network access to Maven repositories.

This workspace was verified with:

- Eclipse Temurin JDK `21.0.11`, installed locally under `/home/user/codex/.tools/jdk-21`.
- Apache Maven `3.9.9`, installed locally under `/home/user/codex/.tools/maven`.

## Build

Standalone Maven builds can fail because the openHAB add-on parent POM is not published like a normal library dependency. The verified build path is to build the binding inside a matching `openhab-addons` 5.1.4 checkout.

From the repository root:

```sh
git clone --depth 1 --branch 5.1.4 https://github.com/openhab/openhab-addons.git /tmp/openhab-addons-5.1.4
cp -a bundles/org.openhab.binding.fujitsuairstage /tmp/openhab-addons-5.1.4/bundles/
```

Add this module to `/tmp/openhab-addons-5.1.4/bundles/pom.xml`:

```xml
<module>org.openhab.binding.fujitsuairstage</module>
```

Then build:

```sh
cd /tmp/openhab-addons-5.1.4
JAVA_HOME=/path/to/jdk-21 PATH=/path/to/maven/bin:/path/to/jdk-21/bin:$PATH \
  mvn -pl :org.openhab.binding.fujitsuairstage -am package -Dspotless.check.skip=true -DskipChecks
```

The local verified output was:

```text
/home/user/codex/.tools/openhab-addons-5.1.4/bundles/org.openhab.binding.fujitsuairstage/target/org.openhab.binding.fujitsuairstage-5.1.4.jar
```

The generated JAR size was `20827` bytes.

## Install Into openHAB

Copy the built JAR into the openHAB add-ons directory:

```sh
sudo cp /path/to/org.openhab.binding.fujitsuairstage-5.1.4.jar /usr/share/openhab/addons/
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
