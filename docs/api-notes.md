# Fujitsu Airstage Local API Notes

These notes document the local API behavior observed against the Fujitsu Airstage WLAN module at `192.0.2.10`.

## Base Information

- Hostname: `airstage.dhcp.internal.hennecke-net.org`
- IP address: `192.0.2.10`
- MAC address: `aa:bb:cc:dd:ee:ff`
- Device ID: `AABBCCDDEEFF`
- Status endpoint: `POST http://192.0.2.10/GetParam`
- Command endpoint: `POST http://192.0.2.10/SetParam`
- Tested content types: `text/plain` and `application/json`

The API accepts JSON request bodies. The community examples use `Content-Type: text/plain`; direct testing showed `application/json` also works.

## Device ID

The `device_id` is case-sensitive.

- `AABBCCDDEEFF`: works.
- `aabbccddeeff`: returns `{"result":"NG","error":"0002"}`.

## Read Request

```sh
curl -sS --max-time 10 \
  -H 'Content-Type: text/plain' \
  --data '{"device_id":"AABBCCDDEEFF","device_sub_id":0,"req_id":"","modified_by":"","set_level":"03","list":["iu_onoff","iu_op_mode","iu_set_tmp"]}' \
  http://192.0.2.10/GetParam
```

Example response:

```json
{
  "value": {
    "iu_onoff": "0",
    "iu_set_tmp": "245",
    "iu_op_mode": "1"
  },
  "read_res": "ack",
  "device_id": "AABBCCDDEEFF",
  "device_sub_id": 0,
  "req_id": "",
  "modified_by": "",
  "set_level": "03",
  "cause": "",
  "result": "OK",
  "error": ""
}
```

## Write Request

```sh
curl -sS --max-time 10 \
  -H 'Content-Type: text/plain' \
  --data '{"device_id":"AABBCCDDEEFF","device_sub_id":0,"req_id":"","modified_by":"","set_level":"02","value":{"iu_onoff":"1"}}' \
  http://192.0.2.10/SetParam
```

The write endpoint returned `write_res=ack` and `result=OK` during testing.

## Confirmed Parameters

| Parameter | Direction | Meaning | Notes |
| --- | --- | --- | --- |
| `iu_onoff` | read/write | Power | `0` off, `1` on |
| `iu_op_mode` | read/write | Operation mode | `0` auto, `1` cool, `2` dry, `3` fan, `4` heat |
| `iu_fan_spd` | read/write | Fan speed | Seen value `2`; community mapping: `0` auto, `2` low, `5` normal, `8` medium, `11` high |
| `iu_set_tmp` | read/write | Target temperature | Tenths of a degree Celsius, e.g. `245` = 24.5 C |
| `iu_af_inc_vrt` | read | Vertical airflow capability/position count | Seen value `4` |
| `iu_af_dir_vrt` | read/write | Vertical airflow direction | Community mapping: `1` 0 deg, `2` 30 deg, `3` 60 deg, `4` 90 deg |
| `iu_af_swg_vrt` | read/write | Vertical swing | `0` off, `1` on |
| `iu_af_swg_hrz` | read/write | Horizontal swing | `65535` means unavailable on this device |
| `iu_af_dir_hrz` | read/write | Horizontal direction | `65535` means unavailable on this device |
| `ou_low_noise` | read/write | Outdoor low-noise mode | `0` off, `1` on |
| `iu_fan_ctrl` | read/write | Fan control / fan energy save | `0` off, `1` on |
| `iu_hmn_det_auto_save` | read/write | Human-detection auto-save | `0` off, `1` on |
| `iu_min_heat` | read/write | Minimum heat | `0` off, `1` on |
| `iu_powerful` | read/write | Powerful mode | `0` off, `1` on |
| `iu_economy` | read/write | Economy mode | `0` off, `1` on |
| `iu_err_code` | read | Error code | Seen value `0` |
| `iu_demand` | read | Demand control | Seen value `0` |
| `iu_fltr_sign_reset` | read/write | Filter sign reset | Seen value `65535`, likely unavailable |
| `iu_indoor_tmp` | read | Indoor temperature | Appeared as centi-degrees Fahrenheit |
| `ou_outdoor_tmp` | read | Outdoor temperature | Valid parameter, but returned empty |

## Temperature Conversion

`iu_set_tmp` uses tenths of a degree Celsius:

```text
celsius = raw / 10
raw = celsius * 10
```

`iu_indoor_tmp` returned `7325`. This is plausible as centi-degrees Fahrenheit:

```text
fahrenheit = raw / 100
celsius = (fahrenheit - 32) * 5 / 9
```

For `7325`, this gives approximately `22.9 C`.

`ou_outdoor_tmp` exists but returned an empty string, including after the unit was switched on for about 20 seconds. The binding should publish `UNDEF` for empty outdoor temperature.

## Error Codes Observed

| Error | Meaning inferred from tests |
| --- | --- |
| `0002` | Invalid device ID or wrong case |
| `0013` | Unknown or unsupported parameter |

## Useful Combined Status Request

The following combined request worked and should be used by the binding for polling:

```json
{
  "device_id": "AABBCCDDEEFF",
  "device_sub_id": 0,
  "req_id": "",
  "modified_by": "",
  "set_level": "03",
  "list": [
    "iu_onoff",
    "iu_op_mode",
    "iu_fan_spd",
    "iu_set_tmp",
    "iu_af_inc_vrt",
    "iu_af_dir_vrt",
    "iu_af_swg_vrt",
    "iu_af_swg_hrz",
    "iu_af_dir_hrz",
    "ou_low_noise",
    "iu_fan_ctrl",
    "iu_hmn_det_auto_save",
    "iu_min_heat",
    "iu_powerful",
    "iu_economy",
    "iu_err_code",
    "iu_demand",
    "iu_fltr_sign_reset",
    "iu_indoor_tmp",
    "ou_outdoor_tmp"
  ]
}
```

## Sources

- Local testing against `192.0.2.10`.
- openHAB community thread: <https://community.openhab.org/t/fujitsu-airstage-control/168250>
- Home Assistant integration reference: <https://github.com/danielkaldheim/ha_airstage>
