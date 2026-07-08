#!/usr/bin/env python3
# C:/Users/usuario/ownCloud2/RankGA/scripts/build_racing_generic_visualizer.py
# Build a generic localhost racing visualizer that loads population files on demand.

from __future__ import annotations

import html
import json
from pathlib import Path

import render_racing_trajectory as renderer
from racing_visualizer_template import css, javascript


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    output = root / "figures" / "racing-generic-visualizer.html"
    output.write_text(build_html(), encoding="utf-8")
    print(f"visualizer={output}")


def build_html() -> str:
    track = renderer.SimpleOvalTrack()
    starts = renderer.build_fixed_start_states(track)
    data = {
        "source": "",
        "geneFormat": "",
        "logged": None,
        "timeLimit": 0.0,
        "goalDistance": track.lap_length * renderer.GOAL_LAPS,
        "lapLength": track.lap_length,
        "maxSpeed": renderer.MAX_SPEED,
        "simulation": {
            "countOfAnchors": renderer.COUNT_OF_ANCHORS,
            "backend": "kinematic",
            "genesPerAnchor": renderer.GENES_PER_ANCHOR,
            "goalLaps": renderer.GOAL_LAPS,
            "timeStepSeconds": renderer.TIME_STEP_SECONDS,
            "steeringRateGain": renderer.STEERING_RATE_GAIN,
            "throttleAcceleration": renderer.THROTTLE_ACCELERATION,
            "brakeAcceleration": renderer.BRAKE_ACCELERATION,
            "dragCoefficient": renderer.DRAG_COEFFICIENT,
            "inverseDistancePower": renderer.INVERSE_DISTANCE_POWER,
            "policyEpsilon": renderer.POLICY_EPSILON,
            "trackRadius": track.radius,
            "trackHalfStraight": track.half_straight,
            "trackHalfWidth": track.half_width,
            "policyCenterX": track.center_x,
            "policyCenterY": track.center_y,
            "policyHalfRangeX": track.policy_half_range_x(),
            "policyHalfRangeY": track.policy_half_range_y(),
            "policySpeedScale": renderer.MAX_SPEED,
            "rarsMass": renderer.RARS_MASS,
            "rarsMaxPower": renderer.RARS_MAX_POWER,
            "rarsFrictionCoefficient": renderer.RARS_FRICTION_COEFFICIENT,
            "rarsSlipSpeedScale": renderer.RARS_SLIP_SPEED_SCALE,
            "rarsDragCoefficient": renderer.RARS_DRAG_COEFFICIENT,
            "rarsGravity": renderer.RARS_GRAVITY,
        },
        "track": renderer.build_track_points(track),
        "anchors": [],
        "starts": [start.__dict__ for start in starts],
        "runs": [],
    }
    encoded = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Visualizador genérico Racing RankGA</title>
<style>{css()}</style>
</head>
<body>
<main>
  <section class="panel">
    <h1>Visualizador genérico Racing RankGA</h1>
    <p>Carga cualquier archivo <code>*_0.csv</code> vigente desde <code>runs/problem-racing</code>.</p>
    <p class="warning" id="geneWarning"></p>
    <div class="stats" id="stats"></div>
    <p class="warning" id="viewportWarning"></p>
    <div class="controls">
      <button id="play">Reproducir</button>
      <button id="reset">Reiniciar</button>
      <button id="selectPopulation" type="button">Seleccionar archivo</button>
      <select id="serverPopulationSelect" class="fileSelect" style="display:none"></select>
      <input id="populationFile" type="file" accept=".csv,text/csv">
      <button id="reloadPopulation" type="button">Recargar archivo</button>
      <p class="hint" id="fileStatus">Selecciona un archivo de población o abre esta página con <code>?file=runs/problem-racing/archivo_0.csv</code>.</p>
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
<script>const DATA={html.escape(encoded, quote=False)};{javascript()}</script>
</body>
</html>
"""


if __name__ == "__main__":
    main()
