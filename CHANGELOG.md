# Changelog

## 1.0.1 - 2026-06-19

### Fixed

- Fixed indoor temperature decoding for values returned by `iu_indoor_tmp`.
  The local API reports this value as a Celsius temperature offset by `5000`,
  so the binding now decodes it as `(raw - 5000) / 100`.
- Fixed outdoor temperature handling when the local API exposes both
  `ou_outdoor_tmp` and `iu_outdoor_tmp`.
  The binding now prefers `ou_outdoor_tmp` when it is populated, because this
  parameter reports centi-degrees Celsius and matches the Airstage app's outdoor
  temperature display more closely. If `ou_outdoor_tmp` is empty or unsupported,
  the binding falls back to `iu_outdoor_tmp`, decoded as `(raw - 5000) / 100`.
- Fixed polling failures caused by oversized combined `GetParam` requests.
  Some Airstage WLAN modules reject requests with more than 20 parameters and
  return `{"result":"NG","error":"0002"}`. This error can look like a device ID
  problem, but in this case it is caused by the parameter count. The binding now
  keeps the regular status polling request within that limit.
- Fixed stale openHAB states after failed polling. When the oversized request was
  rejected, the Thing went offline and linked Items could continue to show old
  persisted values. Keeping the polling request within the device limit restores
  normal status updates.

### Changed

- Added `iu_outdoor_tmp` to the status data considered by the binding while
  keeping the total polling list at 20 parameters.
- Removed the unused `iu_af_inc_vrt` parameter from the regular polling list to
  stay within the module's combined-request limit.
- Updated API notes and installation documentation to describe the temperature
  encodings and the 20-parameter `GetParam` limit.

### Notes

- Existing Thing and Item links do not need to be changed. Channel IDs are
  unchanged.
- `raw-status` now includes `iu_outdoor_tmp` when the device reports it.

## 1.0.0 - 2026-06-15

- Initial public release.
