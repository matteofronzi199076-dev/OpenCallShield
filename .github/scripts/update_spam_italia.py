#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Create an attributable Italian + original OpenCallShield spam feed.

Only the generated data in data/italia is CC BY-SA 4.0; this script is MIT.
No Android code or original feed is modified. Use --offline-* and immutable
source --*-ref values for reproducible local validation without network access.
"""

from __future__ import annotations

import argparse
import copy
import csv
import hashlib
import io
import json
import os
from pathlib import Path
import re
import sys
import tempfile
from datetime import date
from typing import Any
from urllib.parse import urlencode
from urllib.request import Request, urlopen


UPSTREAM_REPOSITORY = "jhonsu01/OpenCallShield"
UPSTREAM_PATH = "spam_numbers.json"
ITALIAN_REPOSITORY = "thesqual87/blocklist-telefonica-italia"
ITALIAN_PATH = "data/blocklist.csv"
DEFAULT_OUTPUT_DIR = Path(__file__).resolve().parents[2] / "data" / "italia"
CSV_HEADER = (
    "numero", "categoria", "prima_segnalazione", "ultima_attivita",
    "segnalazioni", "note",
)
CATEGORIES = {
    "truffa", "finanza", "energia", "telefonia", "sondaggi", "pubblicita",
    "ping", "altro",
}
CATEGORY_TAGS = {
    "truffa": "scam",
    "finanza": "telemarketing",
    "energia": "telemarketing",
    "telefonia": "telemarketing",
    "pubblicita": "telemarketing",
    "sondaggi": "spam",
    "ping": "spam",
    "altro": "spam",
}
ITALIAN_NUMBER_RE = re.compile(r"\+39[03][0-9]{5,10}\Z")
GIT_SHA_RE = re.compile(r"[0-9a-fA-F]{40}\Z")
ISO_DATE_RE = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}\Z")
MAX_REPORTS = 2_147_483_647  # Android's reports field is a Kotlin Int.
MAX_DOWNLOAD_BYTES = 20 * 1024 * 1024
CC_BY_SA = "https://creativecommons.org/licenses/by-sa/4.0/"


class FeedError(ValueError):
    """Invalid or unavailable source; output must not be published."""


def parse_date(value: Any, field: str) -> date:
    if not isinstance(value, str) or not ISO_DATE_RE.fullmatch(value):
        raise FeedError(f"{field}: expected an ISO date YYYY-MM-DD")
    try:
        return date.fromisoformat(value)
    except ValueError as exc:
        raise FeedError(f"{field}: invalid calendar date {value!r}") from exc


def parse_upstream(text: str) -> dict[str, Any]:
    """Validate the original format, preserving every original record/field."""
    try:
        feed = json.loads(text)
    except (json.JSONDecodeError, ValueError) as exc:
        raise FeedError("Original feed is not valid JSON") from exc
    if not isinstance(feed, dict):
        raise FeedError("Original feed must be a JSON object")
    if not isinstance(feed.get("version"), str) or not feed["version"]:
        raise FeedError("Original feed is missing its version")
    parse_date(feed.get("updated_at"), "original.updated_at")
    numbers = feed.get("numbers")
    if not isinstance(numbers, list) or not numbers:
        raise FeedError("Original feed must contain a nonempty numbers array")
    seen: set[str] = set()
    for index, record in enumerate(numbers, 1):
        if not isinstance(record, dict):
            raise FeedError(f"Original record {index} must be an object")
        number = record.get("number")
        if not isinstance(number, str) or not number.strip():
            raise FeedError(f"Original record {index} has no number")
        if number in seen:
            raise FeedError(f"Original feed contains duplicate number {number!r}")
        seen.add(number)
        reports = record.get("reports")
        if type(reports) is not int or not 1 <= reports <= MAX_REPORTS:
            raise FeedError(f"Original record {index} has invalid reports")
        if not isinstance(record.get("tag"), str) or not record["tag"]:
            raise FeedError(f"Original record {index} has no tag")
    return feed


def parse_italian_csv(text: str) -> list[dict[str, Any]]:
    """Validate complete Italian numbers and collapse exact duplicate rows.

    Conflicting duplicate entries fail: report counts must remain attributable
    to the source, rather than being summed or estimated by this generator.
    """
    try:
        rows = list(csv.reader(io.StringIO(text.lstrip("\ufeff")), strict=True))
    except csv.Error as exc:
        raise FeedError(f"Italian source is not valid CSV: {exc}") from exc
    if not rows or tuple(rows[0]) != CSV_HEADER:
        raise FeedError(f"Italian CSV header must be {','.join(CSV_HEADER)}")
    entries: dict[str, dict[str, Any]] = {}
    for line, row in enumerate(rows[1:], 2):
        if len(row) != len(CSV_HEADER):
            raise FeedError(f"Italian CSV line {line}: expected six columns")
        number, category, first, last, reports, note = row
        if not ITALIAN_NUMBER_RE.fullmatch(number):
            raise FeedError(
                f"Italian CSV line {line}: expected an exact +39 number "
                "with 6-11 national digits, starting with 0 or 3"
            )
        if category not in CATEGORIES:
            raise FeedError(f"Italian CSV line {line}: unknown category {category!r}")
        first_date = parse_date(first, f"Italian CSV line {line} prima_segnalazione")
        last_date = parse_date(last, f"Italian CSV line {line} ultima_attivita")
        if first_date > last_date:
            raise FeedError(f"Italian CSV line {line}: first report is after last activity")
        if not re.fullmatch(r"[0-9]+", reports) or not 1 <= int(reports) <= MAX_REPORTS:
            raise FeedError(f"Italian CSV line {line}: invalid report count")
        entry = {
            "number": number,
            "category": category,
            "first_report": first,
            "last_activity": last,
            "reports": int(reports),
            "note": note,
        }
        if number in entries and entries[number] != entry:
            raise FeedError(f"Italian CSV line {line}: conflicting duplicate {number}")
        entries[number] = entry
    if not entries:
        raise FeedError("Italian CSV contains no numbers; refusing an empty update")
    return [entries[number] for number in sorted(entries)]


def italian_aliases(number: str) -> tuple[str, str, str]:
    """Match the current app's exact-string lookup in three complete forms."""
    if not ITALIAN_NUMBER_RE.fullmatch(number):
        raise FeedError(f"Cannot create aliases for invalid Italian number {number!r}")
    return number, number[3:], "0039" + number[3:]


def merge_feeds(
    upstream: dict[str, Any], italian: list[dict[str, Any]],
) -> tuple[dict[str, Any], int]:
    """Preserve original entries; append Italian exact aliases without sums."""
    if not italian:
        raise FeedError("Italian source contains no entries")
    merged = copy.deepcopy(upstream)
    known = {record["number"] for record in merged["numbers"]}
    added = 0
    for entry in sorted(italian, key=lambda record: record["number"]):
        for alias in italian_aliases(entry["number"]):
            if alias in known:
                continue
            merged["numbers"].append({
                "number": alias,
                "reports": entry["reports"],
                "tag": CATEGORY_TAGS[entry["category"]],
            })
            known.add(alias)
            added += 1
    merged["updated_at"] = max(
        parse_date(upstream["updated_at"], "original.updated_at"),
        *(parse_date(entry["last_activity"], "italian.last_activity") for entry in italian),
    ).isoformat()
    return merged, added


def validate_ref(ref: str | None, label: str) -> str:
    if not isinstance(ref, str) or not GIT_SHA_RE.fullmatch(ref):
        raise FeedError(f"{label} must be a full 40-character Git commit SHA")
    return ref.lower()


def source_metadata(
    upstream_text: str, italian_text: str,
    upstream: dict[str, Any], italian: list[dict[str, Any]],
    upstream_ref: str, italian_ref: str,
    merged: dict[str, Any], added: int,
) -> dict[str, Any]:
    """Attribution and source revisions, with no changing generation clock."""
    upstream_ref = validate_ref(upstream_ref, "upstream_ref")
    italian_ref = validate_ref(italian_ref, "italian_ref")

    def source(repository: str, path: str, ref: str, text: str) -> dict[str, Any]:
        return {
            "repository": f"https://github.com/{repository}",
            "path": path,
            "git_ref": ref,
            "url": f"https://github.com/{repository}/blob/{ref}/{path}",
            "raw_url": f"https://raw.githubusercontent.com/{repository}/{ref}/{path}",
            "sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
        }

    original_source = source(UPSTREAM_REPOSITORY, UPSTREAM_PATH, upstream_ref, upstream_text)
    original_source.update({
        "name": "OpenCallShield — database originale",
        "license": "MIT",
        "license_url": f"https://github.com/{UPSTREAM_REPOSITORY}/blob/{upstream_ref}/LICENSE",
        "updated_at": upstream["updated_at"],
        "unique_numbers": len({record["number"] for record in upstream["numbers"]}),
    })
    italian_source = source(ITALIAN_REPOSITORY, ITALIAN_PATH, italian_ref, italian_text)
    italian_source.update({
        "name": "Blocklist telefonica Italia",
        "attribution": "Blocklist telefonica Italia, community Kallm / thesqual87",
        "license": "CC-BY-SA-4.0",
        "license_url": CC_BY_SA,
        "source_license_url": f"https://github.com/{ITALIAN_REPOSITORY}/blob/{italian_ref}/LICENSE",
        "updated_at": max(record["last_activity"] for record in italian),
        "unique_numbers": len(italian),
        "alias_records": 3 * len(italian),
        "added_records": added,
    })
    return {
        "schema_version": 1,
        "updated_at": merged["updated_at"],
        "license": "CC-BY-SA-4.0",
        "license_url": CC_BY_SA,
        "modifications": (
            "CSV italiano convertito nel formato OpenCallShield e unito al database "
            "originale; aggiunte le forme esatte +39, nazionale e 0039. "
            "Nessuna regola di prefisso; nessun incremento delle segnalazioni."
        ),
        "matching": "exact_numbers_only",
        "number_of_records": len(merged["numbers"]),
        "sources": [original_source, italian_source],
    }


def download_text(url: str, *, api: bool = False) -> str:
    headers = {"User-Agent": "OpenCallShield-Italian-Feed", "Accept": "application/vnd.github+json" if api else "text/plain"}
    token = os.environ.get("GITHUB_TOKEN")
    if api and token:
        headers["Authorization"] = f"Bearer {token}"
    try:
        with urlopen(Request(url, headers=headers), timeout=60) as response:
            payload = response.read(MAX_DOWNLOAD_BYTES + 1)
        if len(payload) > MAX_DOWNLOAD_BYTES:
            raise FeedError(f"Source exceeds {MAX_DOWNLOAD_BYTES} bytes: {url}")
        return payload.decode("utf-8")
    except (OSError, UnicodeError) as exc:
        raise FeedError(f"Cannot read source {url}: {exc}") from exc


def resolve_file_ref(repository: str, path: str) -> str:
    """Use the latest file-changing commit, avoiding unrelated daily changes."""
    query = urlencode({"path": path, "sha": "main", "per_page": 1})
    text = download_text(f"https://api.github.com/repos/{repository}/commits?{query}", api=True)
    try:
        commits = json.loads(text)
    except json.JSONDecodeError as exc:
        raise FeedError(f"Cannot resolve Git commit for {repository}/{path}") from exc
    if not isinstance(commits, list) or not commits or not isinstance(commits[0], dict):
        raise FeedError(f"No source commit found for {repository}/{path}")
    return validate_ref(commits[0].get("sha"), f"{repository}/{path} ref")


def load_source(
    repository: str, path: str, offline_file: Path | None,
    explicit_ref: str | None, label: str,
) -> tuple[str, str]:
    if offline_file is not None:
        ref = validate_ref(explicit_ref, f"--{label}-ref (required for offline input)")
        try:
            return offline_file.read_text(encoding="utf-8"), ref
        except (OSError, UnicodeError) as exc:
            raise FeedError(f"Cannot read offline source {offline_file}: {exc}") from exc
    ref = validate_ref(explicit_ref, f"--{label}-ref") if explicit_ref else resolve_file_ref(repository, path)
    return download_text(f"https://raw.githubusercontent.com/{repository}/{ref}/{path}"), ref


def json_text(document: dict[str, Any]) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2, allow_nan=False) + "\n"


def write_if_changed(path: Path, contents: str) -> bool:
    encoded = contents.encode("utf-8")
    if path.is_file() and path.read_bytes() == encoded:
        return False
    # Called only after all source validation and both serializations succeed.
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary: str | None = None
    try:
        with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as output:
            temporary = output.name
            output.write(encoded)
        Path(temporary).replace(path)
    finally:
        if temporary is not None and Path(temporary).exists():
            Path(temporary).unlink()
    return True


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline-upstream", type=Path, help="Read the original JSON locally")
    parser.add_argument("--offline-italian", type=Path, help="Read the Italian CSV locally")
    parser.add_argument("--upstream-ref", default=os.environ.get("UPSTREAM_REF"), help="Immutable original Git commit SHA")
    parser.add_argument("--italian-ref", default=os.environ.get("ITALIAN_REF"), help="Immutable Italian Git commit SHA")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    args = parser.parse_args(argv)
    try:
        upstream_text, upstream_ref = load_source(
            UPSTREAM_REPOSITORY, UPSTREAM_PATH, args.offline_upstream, args.upstream_ref, "upstream",
        )
        italian_text, italian_ref = load_source(
            ITALIAN_REPOSITORY, ITALIAN_PATH, args.offline_italian, args.italian_ref, "italian",
        )
        upstream = parse_upstream(upstream_text)
        italian = parse_italian_csv(italian_text)
        merged, added = merge_feeds(upstream, italian)
        metadata = source_metadata(
            upstream_text, italian_text, upstream, italian,
            upstream_ref, italian_ref, merged, added,
        )
        # Serialize everything before the first write, too.
        documents = {
            "spam_numbers.json": json_text(merged),
            "sources.json": json_text(metadata),
        }
        changed = []
        for filename, contents in documents.items():
            if write_if_changed(args.output_dir / filename, contents):
                changed.append(filename)
        print(
            f"Originali: {len(upstream['numbers'])}; italiani unici: {len(italian)}; "
            f"nuove forme esatte: {added}; totale: {len(merged['numbers'])}."
        )
        print("Aggiornati: " + ", ".join(changed) if changed else "Fonti identiche: nessun file modificato.")
        return 0
    except (FeedError, OSError, ValueError) as exc:
        print(f"ERRORE: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
