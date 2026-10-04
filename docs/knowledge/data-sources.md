# Data sources

All free, no API keys, personal/non-commercial use. Verify any new endpoint against live data
(curl) before writing code against it; the `f1-api-researcher` agent does this.

## Jolpica-F1 (Ergast-compatible): `https://api.jolpi.ca/ergast/f1/`

- **Rate limits:** 4 requests/s, 500/hour. Enforced client-side by `RequestSpacer` (250 ms).
- **User-Agent:** a custom one is required (set in `NetworkModule`).
- **Page size:** `limit` is capped at **100**. Page with `offset` until `MRData.total`. A page can end
  mid-race, so key rows by each race's own season and round.
- **Endpoints used:**
  - `{season}.json` (schedule)
  - `{season}/results.json` (paged, about 5 per season)
  - `{season}/qualifying.json` (paged)
  - `{season}/driverStandings.json`, `{season}/constructorStandings.json`
  - `{season}/drivers.json`, `{season}/constructors.json`
  - `circuits/{id}/seasons.json` (which years a circuit hosted)
  - `{season}/circuits/{id}/results.json`, `{season}/circuits/{id}/qualifying.json`
- **Quirks:**
  - `positionText`: `R` retired, `N` not classified, `W` withdrawn (did not start), `F` failed to
    qualify, `D` disqualified.
  - **Retired drivers still have a numeric `position`** (official classified order). Use it for
    head-to-head "who finished ahead"; use `positionText` to tell finished from retired.
  - `grid = 0` means a pit-lane start.
  - Every number is a string in JSON. Mappers use `toIntOrNull`/`toDoubleOrNull`; a bad value
    surfaces as an error through `syncCatching`, not a crash.
  - Qualifying rows only carry `Q2`/`Q3` for drivers who reached those segments.

## OpenF1: `https://api.openf1.org/v1/`

- **Coverage and access:** 2023 onwards. Historical data is free; live data during sessions is paid.
  **30 requests/minute** on the free tier, enforced by `RequestSpacer` (2 s).
- **Time filters:** `date>=X` / `date<=X` work URL-encoded, i.e. Retrofit `@Query("date>")` and
  `@Query("date<")`.
- **Circuit keys differ from Jolpica's:** `OpenF1Repository.circuitKey()` looks up a past Jolpica race
  date at the circuit via `sessions?date_start>=D&date_start<D+1`, then `sessions?circuit_key=K`
  lists every session there. That list **includes scheduled future sessions**; filter on
  `date_end < now` and `!is_cancelled`.
- **Matching drivers to Jolpica:** use the 3-letter `name_acronym` = Jolpica `code`, confirmed by
  surname with accents stripped, because codes repeat across eras.
- **Endpoints used:** `drivers` (team colours and headshots), `sessions`,
  `session_result?position=1` (pole), `laps`, `car_data` and `location` (one lap is about 330 samples
  each, ~4 Hz), `stints`, `race_control` (Safety Car messages; red flag = `flag == "RED"`),
  `overtakes`, `pit` (`lane_duration`).
- **Caching:** responses for finished sessions are cached permanently in Room (`api_cache`, JSON).
  The circuit session list is refreshed daily.

## Open-Meteo: `https://api.open-meteo.com/v1/forecast`

- **What we use:** the daily forecast (`precipitation_probability_max`, `temperature_2m_max`,
  `wind_speed_10m_max`) for the three race-weekend days, at the circuit's lat/long from Jolpica.
- **Range:** about 16 days ahead; beyond that the app shows when the forecast becomes available.
- **Credit:** attribution is required (shown on the Track tab).

## Looked at but not used

- **Kalshi / Polymarket prices:** public read APIs exist (Kalshi `trade-api/v2/markets`, prices in
  `*_dollars` fields). Market prices were shelved by the owner.
- **MultiViewer circuit API:** has pit-loss and corner data, but no documentation or terms, and
  returned stale data. Avoid.
- **Pirelli compound picks (C1–C5):** press releases only, no API. Not shown rather than guessed.
