# Local openHAB Test Instance

This workspace has a local openHAB 5.1.4 ZIP runtime for binding tests.

## Paths

- Runtime: `/home/user/codex/.tools/openhab-runtime-5.1.4`
- Helper scripts: `/home/user/codex/.tools/openhab-test-5.1.4`
- JDK: `/home/user/codex/.tools/jdk-21`
- Binding JAR source: `/home/user/codex/.tools/openhab-addons-5.1.4/bundles/org.openhab.binding.fujitsuairstage/target/org.openhab.binding.fujitsuairstage-5.1.4.jar`
- Installed binding JAR: `/home/user/codex/.tools/openhab-runtime-5.1.4/addons/org.openhab.binding.fujitsuairstage-5.1.4.jar`

## Ports

- HTTP: `http://127.0.0.1:18080`
- HTTPS: `https://127.0.0.1:18443`
- Karaf SSH: `127.0.0.1:18101`
- Java debug: `15005`

The runtime is bound to localhost only.

## Commands

```sh
/home/user/codex/.tools/openhab-test-5.1.4/install-fujitsu-addon
/home/user/codex/.tools/openhab-test-5.1.4/start
/home/user/codex/.tools/openhab-test-5.1.4/status
/home/user/codex/.tools/openhab-test-5.1.4/stop
```

## Test Configuration

The runtime includes textual configuration for the known Fujitsu Airstage module:

- Thing file: `/home/user/codex/.tools/openhab-runtime-5.1.4/conf/things/fujitsu-airstage.things`
- Items file: `/home/user/codex/.tools/openhab-runtime-5.1.4/conf/items/fujitsu-airstage.items`
- Thing UID: `fujitsuairstage:ac:aseh07kgtg`
- Host: `192.0.2.10`
- Device ID: `AABBCCDDEEFF`

The persisted Thing only polls status. It does not issue power/mode/setpoint commands unless a linked Item receives a command.

## Verification

Verified on 2026-06-15:

```text
GET http://127.0.0.1:18080/rest/
runtimeInfo.version = 5.1.4
```

The Fujitsu thing type was available through:

```text
GET http://127.0.0.1:18080/rest/thing-types/fujitsuairstage:ac
```

The runtime log loaded:

```text
fujitsu-airstage.items
fujitsu-airstage.things
```

Some REST endpoints for concrete Things/Items require authentication until the local UI/user setup is completed.
