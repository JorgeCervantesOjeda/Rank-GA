#!/usr/bin/env python3
# C:/Users/usuario/ownCloud2/RankGA/scripts/render_racing_trajectory.py
# Build a standalone animated HTML visualizer from a logged ProblemRacing genome.

from __future__ import annotations

import argparse
import html
import json
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from racing_visualizer_template import css, javascript

GENES_PER_ANCHOR = 4
COUNT_OF_ANCHORS = 50
COUNT_OF_GENES = COUNT_OF_ANCHORS * GENES_PER_ANCHOR
GOAL_LAPS = 1.0
TIME_STEP_SECONDS = 0.2
STEERING_RATE_GAIN = 1.0
THROTTLE_ACCELERATION = 12.0
BRAKE_ACCELERATION = 18.0
DRAG_COEFFICIENT = 0.05
MAX_SPEED = 40.0
INVERSE_DISTANCE_POWER = 2.0
POLICY_EPSILON = 1.0e-9
@dataclass(frozen=True)
class Pose:
    x: float
    y: float
    heading: float
    normal_x: float
    normal_y: float
    progress: float

@dataclass(frozen=True)
class StartState:
    x: float
    y: float
    speed: float
    heading: float

@dataclass(frozen=True)
class CarState:
    x: float
    y: float
    speed: float
    heading: float
    time: float

class SimpleOvalTrack:
    def __init__(self) -> None:
        self.track_name = "simple_oval"
        self.center_x = 0.0
        self.center_y = 0.0
        self.radius = 25.0
        self.half_straight = 30.0
        self.half_width = 5.0
        self.straight_length = 2.0 * self.half_straight
        self.turn_length = math.pi * self.radius
        self.lap_length = 2.0 * self.straight_length + 2.0 * self.turn_length

    def is_inside_track(self, x: float, y: float) -> bool:
        return self._projection(x, y)[1] <= self.half_width

    def project_progress(self, x: float, y: float) -> float:
        return self._projection(x, y)[0]

    def centerline_pose(self, progress: float) -> Pose:
        p = progress % self.lap_length
        if p < self.straight_length:
            return self._pose(-self.half_straight + p, self.radius, 0.0, p)
        q = p - self.straight_length
        if q < self.turn_length:
            angle = math.pi / 2.0 - q / self.radius
            x = self.half_straight + self.radius * math.cos(angle)
            y = self.radius * math.sin(angle)
            heading = math.atan2(-math.cos(angle), math.sin(angle))
            return self._pose(x, y, heading, p)
        q -= self.turn_length
        if q < self.straight_length:
            return self._pose(self.half_straight - q, -self.radius, math.pi, p)
        q -= self.straight_length
        angle = -math.pi / 2.0 + q / self.radius
        x = -self.half_straight - self.radius * math.cos(angle)
        y = self.radius * math.sin(angle)
        heading = math.atan2(math.cos(angle), math.sin(angle))
        return self._pose(x, y, heading, p)

    def _pose(self, local_x: float, local_y: float, heading: float, progress: float) -> Pose:
        h = wrap_to_pi(heading)
        return Pose(
            self.center_x + local_x,
            self.center_y + local_y,
            h,
            -math.sin(h),
            math.cos(h),
            progress,
        )

    def _projection(self, x: float, y: float) -> tuple[float, float]:
        local_x = x - self.center_x
        local_y = y - self.center_y
        candidates = [
            self._project_top_straight(local_x, local_y),
            self._project_right_turn(local_x, local_y),
            self._project_bottom_straight(local_x, local_y),
            self._project_left_turn(local_x, local_y),
        ]
        return min(candidates, key=lambda item: item[1])

    def _project_top_straight(self, x: float, y: float) -> tuple[float, float]:
        px = clamp(x, -self.half_straight, self.half_straight)
        py = self.radius
        return px + self.half_straight, distance(x, y, px, py)

    def _project_right_turn(self, x: float, y: float) -> tuple[float, float]:
        angle = clamp(math.atan2(y, x - self.half_straight), -math.pi / 2.0, math.pi / 2.0)
        px = self.half_straight + self.radius * math.cos(angle)
        py = self.radius * math.sin(angle)
        progress = self.straight_length + (math.pi / 2.0 - angle) * self.radius
        return progress, distance(x, y, px, py)

    def _project_bottom_straight(self, x: float, y: float) -> tuple[float, float]:
        px = clamp(x, -self.half_straight, self.half_straight)
        py = -self.radius
        progress = self.straight_length + self.turn_length + (self.half_straight - px)
        return progress, distance(x, y, px, py)

    def _project_left_turn(self, x: float, y: float) -> tuple[float, float]:
        angle = clamp(math.atan2(y, -(x + self.half_straight)), -math.pi / 2.0, math.pi / 2.0)
        px = -self.half_straight - self.radius * math.cos(angle)
        py = self.radius * math.sin(angle)
        progress = self.straight_length * 2.0 + self.turn_length + (angle + math.pi / 2.0) * self.radius
        return progress, distance(x, y, px, py)

class SimpleOvalBackend:
    def __init__(self, track: SimpleOvalTrack) -> None:
        self.track = track
        self.current = CarState(0.0, 0.0, 0.0, 0.0, 0.0)
        self.last_lap_progress = 0.0
        self.progress = 0.0

    def reset(self, start: StartState) -> CarState:
        if not self.track.is_inside_track(start.x, start.y):
            raise ValueError("start state must lie inside the track")
        self.current = CarState(start.x, start.y, start.speed, wrap_to_pi(start.heading), 0.0)
        self.last_lap_progress = self.track.project_progress(start.x, start.y)
        self.progress = 0.0
        return self.current

    def step(self, action: tuple[float, float, float]) -> tuple[CarState, float, bool]:
        steering, throttle, brake = action
        acceleration = (
            THROTTLE_ACCELERATION * throttle
            - BRAKE_ACCELERATION * brake
            - DRAG_COEFFICIENT * self.current.speed
        )
        next_speed = clamp(self.current.speed + acceleration * TIME_STEP_SECONDS, 0.0, MAX_SPEED)
        average_speed = 0.5 * (self.current.speed + next_speed)
        next_heading = wrap_to_pi(self.current.heading + STEERING_RATE_GAIN * steering * TIME_STEP_SECONDS)
        next_x = self.current.x + average_speed * math.cos(next_heading) * TIME_STEP_SECONDS
        next_y = self.current.y + average_speed * math.sin(next_heading) * TIME_STEP_SECONDS
        next_time = self.current.time + TIME_STEP_SECONDS
        self.current = CarState(next_x, next_y, next_speed, next_heading, next_time)
        next_lap_progress = self.track.project_progress(next_x, next_y)
        self.progress += wrap_progress_delta(next_lap_progress - self.last_lap_progress, self.track.lap_length)
        self.last_lap_progress = next_lap_progress
        off_track = not self.track.is_inside_track(next_x, next_y)
        return self.current, self.progress, off_track

def main() -> None:
    args = parse_args()
    population_path = args.population or find_latest_population_file()
    row = read_population_row(population_path, args.rank)
    genes, gene_format = parse_genes(row["genes"], COUNT_OF_GENES)
    track = SimpleOvalTrack()
    starts = build_fixed_start_states(track)
    goal_distance = GOAL_LAPS * track.lap_length
    time_limit = row["extra"].get("T", 25.628906)
    runs = [simulate_run(track, genes, start, time_limit, goal_distance, index) for index, start in enumerate(starts)]
    output_path = args.output or population_path.with_name(population_path.stem + "_trajectory.html")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        build_html(population_path, row, genes, gene_format, track, starts, runs, time_limit, goal_distance),
        encoding="utf-8",
    )
    print(f"visualizer={output_path}")
    print(f"source={population_path}")
    print(f"genes={len(genes)}")
    print(f"gene_format={gene_format}")
    print(f"runs={len(runs)}")

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--population", type=Path, default=None)
    parser.add_argument("--output", type=Path, default=None)
    parser.add_argument("--rank", type=int, default=None)
    return parser.parse_args()

def find_latest_population_file() -> Path:
    candidates = sorted(
        Path("runs/problem-racing").glob("Racing_simple_oval_50_seed1234_*_0.csv"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )
    if not candidates:
        raise FileNotFoundError("No racing population file was found in runs/problem-racing")
    return candidates[0]

def read_population_row(path: Path, rank: int | None) -> dict[str, object]:
    best_row = None
    for line in path.read_text(encoding="utf-8").splitlines()[1:]:
        parts = line.split(",", 4)
        if len(parts) != 5:
            continue
        current_rank = int(parts[0].strip())
        if current_rank == rank:
            extra_text = parts[3].strip()
            return {
                "rank": current_rank,
                "mutationIntensity": float(parts[1].strip()),
                "fitness": float(parts[2].strip()),
                "extraText": extra_text,
                "extra": parse_extra(extra_text),
                "genes": parts[4].strip(),
            }
        row = {
            "rank": current_rank,
            "mutationIntensity": float(parts[1].strip()),
            "fitness": float(parts[2].strip()),
            "extraText": parts[3].strip(),
            "extra": parse_extra(parts[3].strip()),
            "genes": parts[4].strip(),
        }
        if best_row is None or row["fitness"] > best_row["fitness"]:
            best_row = row
    if rank is None and best_row is not None:
        return best_row
    raise ValueError(f"rank {rank} was not found in {path}")

def parse_extra(extra_text: str) -> dict[str, object]:
    match = re.search(
        r"distance=(?P<distance>[0-9.Ee+-]+)\s+offTrack=(?P<offTrack>true|false)\s+"
        r"goalReached=(?P<goalReached>true|false)\s+avgSpeed=(?P<avgSpeed>[0-9.Ee+-]+)\s+"
        r"T=(?P<T>[0-9.Ee+-]+)\s+M=(?P<M>\d+)\s+"
        r"fitness=(?P<fitness>[0-9.Ee+-]+)\s+"
        r"safeRuns=(?P<safeRuns>\d+)\s+"
        r"avgDistance=(?P<avgDistance>[0-9.Ee+-]+)\s+"
        r"goalRuns=(?P<goalRuns>\d+)\s+"
        r"avgGoalTime=(?P<avgGoalTime>[0-9.Ee+-]+)",
        extra_text,
    )
    if not match:
        raise ValueError(f"Could not parse current racing extra string: {extra_text}")
    return {
        "distance": float(match.group("distance")),
        "offTrack": match.group("offTrack") == "true",
        "goalReached": match.group("goalReached") == "true",
        "avgSpeed": float(match.group("avgSpeed")),
        "T": float(match.group("T")),
        "M": int(match.group("M")),
        "fitness": float(match.group("fitness")),
        "safeRuns": int(match.group("safeRuns")),
        "avgDistance": float(match.group("avgDistance")),
        "goalRuns": int(match.group("goalRuns")),
        "avgGoalTime": float(match.group("avgGoalTime")),
    }

def parse_genes(text: str, expected_count: int) -> tuple[list[float], str]:
    whitespace_tokens = text.split()
    if len(whitespace_tokens) == expected_count:
        return parse_gene_tokens(whitespace_tokens), "full-precision whitespace"

    raise ValueError(
        f"Expected {expected_count} whitespace-separated genes in the current full-population format, "
        f"but parsed {len(whitespace_tokens)} tokens"
    )

def parse_gene_tokens(tokens: list[str]) -> list[float]:
    return [float(token) for token in tokens]

def build_fixed_start_states(track: SimpleOvalTrack) -> list[StartState]:
    specs = [(10.0, 0.0, 0.0), (50.0, 1.0, 0.12), (80.0, -0.8, -0.18), (140.0, 0.7, 0.10),
             (190.0, -0.6, -0.10), (225.0, 0.5, 0.16), (260.0, -0.4, -0.08)]
    states = []
    for progress, offset, heading_offset in specs:
        pose = track.centerline_pose(progress)
        states.append(StartState(
            pose.x + offset * pose.normal_x,
            pose.y + offset * pose.normal_y,
            0.0,
            pose.heading + heading_offset,
        ))
    return states

def simulate_run(
    track: SimpleOvalTrack,
    genes: list[float],
    start: StartState,
    time_limit: float,
    goal_distance: float,
    run_index: int,
) -> dict[str, object]:
    backend = SimpleOvalBackend(track)
    state = backend.reset(start)
    points = [point_dict(state, 0.0, True)]
    off_track = False
    while state.time < time_limit:
        policy = interpolate_policy(genes, state)
        action = adapt_action(policy, state)
        state, progress, off_track = backend.step(action)
        points.append(point_dict(state, progress, not off_track))
        if off_track or progress >= goal_distance:
            break
    reached_goal = backend.progress >= goal_distance and not off_track
    average_speed = (goal_distance if reached_goal else backend.progress) / state.time if state.time > 0.0 else 0.0
    return {
        "index": run_index,
        "points": points,
        "distance": backend.progress,
        "offTrack": off_track,
        "goalReached": reached_goal,
        "avgSpeed": average_speed,
        "duration": state.time,
    }

def interpolate_policy(genes: list[float], state: CarState) -> tuple[float, float]:
    weighted_speed = 0.0
    weighted_direction_x = 0.0
    weighted_direction_y = 0.0
    total_weight = 0.0
    for anchor_index in range(COUNT_OF_ANCHORS):
        offset = anchor_index * GENES_PER_ANCHOR
        anchor_x, anchor_y, speed_target, direction_target = genes[offset:offset + GENES_PER_ANCHOR]
        d = math.hypot(state.x - anchor_x, state.y - anchor_y)
        weight = 1.0 / ((d + POLICY_EPSILON) ** INVERSE_DISTANCE_POWER)
        weighted_speed += weight * speed_target
        weighted_direction_x += weight * math.cos(direction_target)
        weighted_direction_y += weight * math.sin(direction_target)
        total_weight += weight
    if total_weight <= 0.0:
        raise ValueError("policy interpolation produced non-positive total weight")
    return weighted_speed / total_weight, math.atan2(weighted_direction_y, weighted_direction_x)

def adapt_action(policy: tuple[float, float], state: CarState) -> tuple[float, float, float]:
    speed_target, direction_target = policy
    steering = clamp(wrap_to_pi(direction_target - state.heading), -1.0, 1.0)
    speed_error = speed_target - state.speed
    throttle = clamp(0.2 * speed_error, 0.0, 1.0) if speed_error > 0.0 else 0.0
    brake = clamp(-0.2 * speed_error, 0.0, 1.0) if speed_error < 0.0 else 0.0
    return steering, throttle, brake

def build_html(
    source: Path,
    row: dict[str, object],
    genes: list[float],
    gene_format: str,
    track: SimpleOvalTrack,
    starts: list[StartState],
    runs: list[dict[str, object]],
    time_limit: float,
    goal_distance: float,
) -> str:
    data = {
        "source": str(source),
        "geneFormat": gene_format,
        "logged": row["extra"],
        "timeLimit": time_limit,
        "goalDistance": goal_distance,
        "lapLength": track.lap_length,
        "maxSpeed": MAX_SPEED,
        "simulation": {
            "countOfAnchors": COUNT_OF_ANCHORS,
            "genesPerAnchor": GENES_PER_ANCHOR,
            "goalLaps": GOAL_LAPS,
            "timeStepSeconds": TIME_STEP_SECONDS,
            "steeringRateGain": STEERING_RATE_GAIN,
            "throttleAcceleration": THROTTLE_ACCELERATION,
            "brakeAcceleration": BRAKE_ACCELERATION,
            "dragCoefficient": DRAG_COEFFICIENT,
            "inverseDistancePower": INVERSE_DISTANCE_POWER,
            "policyEpsilon": POLICY_EPSILON,
            "trackRadius": track.radius,
            "trackHalfStraight": track.half_straight,
            "trackHalfWidth": track.half_width,
        },
        "track": build_track_points(track),
        "anchors": build_anchors(genes),
        "starts": [start.__dict__ for start in starts],
        "runs": runs,
    }
    encoded = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Visualizador Racing RankGA</title>
<style>{css()}</style>
</head>
<body>
<main>
  <section class="panel">
    <h1>Visualizador de trayectoria Racing RankGA</h1>
    <p>Reconstrucción desde <code>{html.escape(str(source))}</code>.</p>
    <p class="warning" id="geneWarning"></p>
    <div class="stats" id="stats"></div>
    <p class="warning" id="viewportWarning"></p>
    <div class="controls">
      <button id="play">Reproducir</button>
      <button id="reset">Reiniciar</button>
      <label>Archivo de población <input id="populationFile" type="file" accept=".csv,text/csv"></label>
      <p class="hint" id="fileStatus">Puedes cargar un archivo vigente <code>*_0.csv</code>; se usará la fila con mayor fitness.</p>
      <label>Corrida <select id="runSelect"></select></label>
      <label>Velocidad <input id="speed" type="range" min="0.2" max="8" step="0.2" value="1.5"></label>
      <label><input id="showAnchors" type="checkbox" checked> Anclas y targets</label>
      <label><input id="showAnchorBox" type="checkbox" checked> Caja de anclas</label>
      <label><input id="showTrails" type="checkbox" checked> Trazas</label>
      <button id="fitTrack" type="button">Vista pista</button>
      <button id="fitAnchors" type="button">Vista anclas</button>
      <p class="hint">Zoom: rueda del mouse. Pan: arrastrar el canvas.</p>
    </div>
  </section>
  <section class="canvasWrap">
    <canvas id="canvas" width="1100" height="720"></canvas>
  </section>
</main>
<script>const DATA={encoded};{javascript()}</script>
</body>
</html>
"""

def build_track_points(track: SimpleOvalTrack) -> dict[str, list[dict[str, float]]]:
    center = []
    left = []
    right = []
    for index in range(360):
        pose = track.centerline_pose(track.lap_length * index / 360.0)
        center.append({"x": pose.x, "y": pose.y})
        left.append({"x": pose.x + track.half_width * pose.normal_x, "y": pose.y + track.half_width * pose.normal_y})
        right.append({"x": pose.x - track.half_width * pose.normal_x, "y": pose.y - track.half_width * pose.normal_y})
    return {"center": center, "left": left, "right": right}

def build_anchors(genes: list[float]) -> list[dict[str, float]]:
    anchors = []
    for index in range(COUNT_OF_ANCHORS):
        offset = index * GENES_PER_ANCHOR
        anchors.append({
            "x": genes[offset],
            "y": genes[offset + 1],
            "speed": genes[offset + 2],
            "direction": genes[offset + 3],
        })
    return anchors

def point_dict(state: CarState, progress: float, inside: bool) -> dict[str, float | bool]:
    return {
        "x": state.x,
        "y": state.y,
        "speed": state.speed,
        "heading": state.heading,
        "time": state.time,
        "progress": progress,
        "inside": inside,
    }

def clamp(value: float, min_value: float, max_value: float) -> float:
    return max(min_value, min(max_value, value))

def distance(x: float, y: float, projected_x: float, projected_y: float) -> float:
    return math.hypot(x - projected_x, y - projected_y)

def wrap_to_pi(angle: float) -> float:
    wrapped = angle
    while wrapped <= -math.pi:
        wrapped += 2.0 * math.pi
    while wrapped > math.pi:
        wrapped -= 2.0 * math.pi
    return wrapped

def wrap_progress_delta(delta: float, lap_length: float) -> float:
    if delta > lap_length / 2.0:
        return delta - lap_length
    if delta < -lap_length / 2.0:
        return delta + lap_length
    return delta

if __name__ == "__main__":
    main()
