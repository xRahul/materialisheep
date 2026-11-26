#!/usr/bin/env python3
"""Validate store metadata before an F-Droid or IzzyOnDroid submission.

Checks the fastlane tree, image sizes, the F-Droid recipe and the release tag.
Exits non-zero with one line per problem.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

import yaml
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
APP_ID = "in.rahulja.materialhn"
META = ROOT / "fastlane" / "metadata" / "android" / "en-US"
RECIPE = ROOT / "metadata" / f"{APP_ID}.yml"
VERSION_PROPS = ROOT / "version.properties"

errors: list[str] = []
notes: list[str] = []


def check(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def read_version() -> tuple[str, int]:
    props = {}
    for line in VERSION_PROPS.read_text().splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    version = f"{props['VERSION_MAJOR']}.{props['VERSION_MINOR']}.{props['VERSION_PATCH']}"
    return version, int(props["VERSION_CODE"])


def main() -> int:
    version, code = read_version()

    check((META / "title.txt").is_file(), "missing fastlane title.txt")
    check((META / "short_description.txt").is_file(), "missing fastlane short_description.txt")
    check((META / "full_description.txt").is_file(), "missing fastlane full_description.txt")

    short = (META / "short_description.txt").read_text().strip()
    check(len(short) <= 80, f"short_description.txt is {len(short)} chars, limit is 80")

    icon = META / "images" / "icon.png"
    feature = META / "images" / "featureGraphic.png"
    check(icon.is_file(), "missing images/icon.png")
    check(feature.is_file(), "missing images/featureGraphic.png")
    if icon.is_file():
        check(Image.open(icon).size == (512, 512), "icon.png must be 512x512")
    if feature.is_file():
        check(Image.open(feature).size == (1024, 500), "featureGraphic.png must be 1024x500")

    screenshots = sorted((META / "images" / "phoneScreenshots").glob("*.png"))
    check(len(screenshots) >= 2, "need at least two phone screenshots")

    check(RECIPE.is_file(), f"missing {RECIPE.relative_to(ROOT)}")
    if RECIPE.is_file():
        recipe = yaml.safe_load(RECIPE.read_text())
        check(recipe.get("License") == "GPL-3.0-or-later", "recipe License must be GPL-3.0-or-later")
        check(recipe.get("CurrentVersion") == version,
              f"recipe CurrentVersion {recipe.get('CurrentVersion')} != version.properties {version}")
        check(recipe.get("CurrentVersionCode") == code,
              f"recipe CurrentVersionCode {recipe.get('CurrentVersionCode')} != version.properties {code}")
        builds = recipe.get("Builds") or []
        check(bool(builds), "recipe has no Builds entry")
        if builds:
            check(builds[-1].get("versionCode") == code, "last Builds entry does not match version.properties")
            check(builds[-1].get("commit", "").startswith("v"), "last Builds commit must be a release tag")
        check(recipe.get("UpdateCheckMode") == "Tags", "recipe UpdateCheckMode must be Tags")

    app_name = ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"
    text = app_name.read_text()
    match = re.search(r'<string name="app_name">([^<]+)</string>', text)
    check(match is not None and match.group(1) == "Material HN", "app_name must be Material HN")

    gradle = (ROOT / "app" / "build.gradle").read_text()
    check(f'applicationId "{APP_ID}"' in gradle, f"applicationId must be {APP_ID}")

    for problem in errors:
        print(f"error: {problem}")
    for note in notes:
        print(f"note: {note}")
    if errors:
        return 1
    print(f"store metadata ok: {APP_ID} {version} ({code})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
