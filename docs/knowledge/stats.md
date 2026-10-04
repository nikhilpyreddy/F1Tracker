# Derived stats: definitions

Everything is a pure function in `domain/stats`. Every rate is a `Rate(hits, total)` so the UI can
always show the sample size.

## Circuit base rates (`CircuitStats`, last `CIRCUIT_HISTORY_SEASONS` = 10 seasons at the circuit)

| Stat | Definition | Market it informs |
|---|---|---|
| Won from pole / front row / top 3 | Races whose winner started P1 / P1–2 / P1–3 | Winner |
| Winner's average grid | Mean starting slot of winners (pit-lane starts excluded) | Winner |
| Podium starts | Podium finishers who started P1–3, over all podium finishers | Podium |
| Top-10 from outside top-10 grid | Count per race; a pit-lane start counts as outside | Top 10 |
| Retirements | `R`/`N` among starters (`W`/`F` didn't start) | Finish / DNF |
| Fastest lap by winner / podium | Over races that have fastest-lap data (`fastestLapRank == 1`) | Fastest lap |

Tapping a stat opens `circuitStatDetails`, which lists the races behind it (newest first) with a
"Counts / Doesn't count" mark.

## Form (`DriverForm`)

- **Driver form:** the last 5 races newest first (finish and grid), season points, and retirements
  over starts.
- **Teammate head-to-head:** compares only races where a team ran exactly two cars. The grid side uses
  the effective grid (pit lane = last). The finish side uses the **official classified order**, so a
  double retirement goes to whoever lasted longer.

## Qualifying (`QualifyingStats`)

- **Gap to pole:** measured **within the segment the driver reached**. A Q2 exit is compared with the
  pole-sitter's Q2 time, not their Q3 lap.
- **Form:** average position, poles, front rows, Q3 appearances (top 10), and the last 5 sessions.
- **Teammate head-to-head:** on actual qualifying position, not grid (the grid includes penalties).
- **Pole history:** the pole-sitter each year, joined to their race finish. Pole-to-win counts only
  years where the race result is known.

## Track profile (`TrackProfile`, from the latest pole lap's OpenF1 telemetry)

- **Lap length:** sum of speed × Δt. Bahrain came out at 5,339 m against the official 5,412 m, about
  1% low at ~4 Hz.
- **Full throttle:** throttle ≥ 98, time-weighted. The longest flat-out run is the longest continuous
  such stretch.
- **Braking corners:** speed local minima that come after a drop of at least 25 km/h from the
  previous peak. Classified by minimum speed: slow < 130 ≤ medium < 210 ≤ fast. Flat-out kinks are
  deliberately not counted.
- **Braking zones:** transitions from brake 0 to brake > 0.
- **Map:** location samples matched to the nearest-in-time telemetry sample's speed.

## Strategy (`StrategySummary`, OpenF1 races since 2023)

- **Stint laps:** `lap_end − lap_start + 1`. Stops = stints − 1. The sequence is compound initials,
  e.g. `M–H`.
- **Common strategies and stop shares:** counted over classified finishers only.
- **Safety Car / VSC:** counted from messages starting `SAFETY CAR DEPLOYED` / `VIRTUAL SAFETY CAR
  DEPLOYED`. A red flag is a `Flag` whose `flag == "RED"`.
- **Pit lane time:** median `lane_duration` per race.
- **Finishing order:** from Jolpica, joined on driver code for each year.
