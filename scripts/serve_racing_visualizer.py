#!/usr/bin/env python3
# C:/Users/usuario/ownCloud2/RankGA/scripts/serve_racing_visualizer.py
# Serve the RankGA racing visualizer over localhost so browser file permissions work.

from __future__ import annotations

import argparse
import functools
import http.server
import json
import socketserver
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse


DEFAULT_PORT = 8765


class VisualizerRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args: object, root: Path, **kwargs: object) -> None:
        self.root = root
        super().__init__(*args, directory=str(root), **kwargs)

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/api/populations":
            self.write_populations()
            return
        if parsed.path == "/api/population":
            self.write_population(parsed.query)
            return
        super().do_GET()

    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def write_populations(self) -> None:
        runs_dir = self.root / "runs" / "problem-racing"
        rows = []
        for path in sorted(runs_dir.glob("Racing_simple_oval_50_seed*_0.csv"), key=lambda item: item.stat().st_mtime, reverse=True):
            stat = path.stat()
            rows.append({
                "path": path.relative_to(self.root).as_posix(),
                "name": path.name,
                "size": stat.st_size,
                "mtime": stat.st_mtime,
            })
        self.write_json({"populations": rows})

    def write_population(self, query: str) -> None:
        values = parse_qs(query)
        requested = values.get("file", [""])[0]
        if not requested:
            self.send_error(400, "missing file")
            return
        try:
            path = self.resolve_population_path(requested)
        except ValueError as error:
            self.send_error(400, str(error))
            return
        if not path.exists():
            self.send_error(404, "population file not found")
            return
        try:
            text = path.read_text(encoding="utf-8")
        except OSError as error:
            self.send_error(409, f"population file is temporarily unavailable: {error}")
            return
        payload = text.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/csv; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def resolve_population_path(self, requested: str) -> Path:
        relative = Path(unquote(requested))
        path = (self.root / relative).resolve()
        allowed = (self.root / "runs" / "problem-racing").resolve()
        if allowed not in path.parents:
            raise ValueError("population file must be inside runs/problem-racing")
        if not path.name.endswith("_0.csv"):
            raise ValueError("population file must be a *_0.csv file")
        return path

    def write_json(self, data: dict[str, object]) -> None:
        payload = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


class ReusableTcpServer(socketserver.TCPServer):
    allow_reuse_address = True


def main() -> None:
    args = parse_args()
    root = Path(args.root).resolve()
    if not root.exists():
        raise FileNotFoundError(f"visualizer root does not exist: {root}")
    handler = functools.partial(VisualizerRequestHandler, root=root)
    with ReusableTcpServer((args.host, args.port), handler) as server:
        url = f"http://{args.host}:{args.port}/figures/racing-generic-visualizer.html"
        print(f"serving={root}")
        print(f"url={url}")
        print("stop=Ctrl+C")
        server.serve_forever()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    return parser.parse_args()


if __name__ == "__main__":
    main()
