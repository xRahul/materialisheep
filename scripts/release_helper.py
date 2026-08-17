#!/usr/bin/env python3
"""
Release Helper Script for Materialisheep
Handles semantic version bumping, changelog generation, APK checksum calculation, and release packaging.
"""

import argparse
import hashlib
import os
import re
import subprocess
import sys
from typing import Dict, List, Optional, Tuple


def read_version_properties(filepath: str) -> Dict[str, str]:
    props = {}
    if not os.path.exists(filepath):
        return {
            "VERSION_MAJOR": "3",
            "VERSION_MINOR": "4",
            "VERSION_PATCH": "13",
            "VERSION_CODE": "93",
        }
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, val = line.split("=", 1)
                props[key.strip()] = val.strip()
    return props


def write_version_properties(filepath: str, props: Dict[str, str]) -> None:
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"VERSION_MAJOR={props.get('VERSION_MAJOR', '3')}\n")
        f.write(f"VERSION_MINOR={props.get('VERSION_MINOR', '4')}\n")
        f.write(f"VERSION_PATCH={props.get('VERSION_PATCH', '0')}\n")
        f.write(f"VERSION_CODE={props.get('VERSION_CODE', '1')}\n")


def calculate_bump(
    current_major: int,
    current_minor: int,
    current_patch: int,
    current_code: int,
    bump_type: str,
    custom_version: Optional[str] = None,
    custom_code: Optional[int] = None,
) -> Tuple[int, int, int, int]:
    if custom_version:
        # Strip optional leading 'v'
        clean_v = custom_version.lstrip("v").strip()
        parts = clean_v.split(".")
        if len(parts) >= 3:
            new_major = int(parts[0])
            new_minor = int(parts[1])
            # Handle potential suffix like 1-rc1 -> 1
            patch_match = re.match(r"^(\d+)", parts[2])
            new_patch = int(patch_match.group(1)) if patch_match else 0
        elif len(parts) == 2:
            new_major = int(parts[0])
            new_minor = int(parts[1])
            new_patch = 0
        else:
            raise ValueError(f"Invalid custom version format: {custom_version}")
    else:
        b_type = bump_type.lower()
        if b_type == "major":
            new_major = current_major + 1
            new_minor = 0
            new_patch = 0
        elif b_type == "minor":
            new_major = current_major
            new_minor = current_minor + 1
            new_patch = 0
        elif b_type == "patch":
            new_major = current_major
            new_minor = current_minor
            new_patch = current_patch + 1
        else:
            raise ValueError(f"Unknown bump type: {bump_type}. Choose 'patch', 'minor', or 'major'.")

    new_code = custom_code if custom_code is not None else current_code + 1
    return new_major, new_minor, new_patch, new_code


def run_git(cmd: List[str]) -> str:
    result = subprocess.run(["git"] + cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
    return result.stdout.strip()


def get_previous_tag(current_tag: Optional[str] = None) -> Optional[str]:
    try:
        tags_output = run_git(["tag", "--sort=-v:refname"])
        tags = [t.strip() for t in tags_output.splitlines() if t.strip()]
        if not tags:
            return None
        if current_tag and current_tag in tags:
            idx = tags.index(current_tag)
            if idx + 1 < len(tags):
                return tags[idx + 1]
            return None
        return tags[0]
    except Exception:
        return None


def categorize_commit(subject: str) -> str:
    sub = subject.strip().lower()
    if any(sub.startswith(p) for p in ["feat:", "feat(", "feature:", "feature("]) or "✨" in sub:
        return "features"
    if any(sub.startswith(p) for p in ["fix:", "fix(", "bug:", "bugfix:"]) or "🐛" in sub or "hotfix" in sub:
        return "fixes"
    if any(sub.startswith(p) for p in ["perf:", "perf(", "optimize:", "⚡"]):
        return "performance"
    if any(sub.startswith(p) for p in ["security:", "sec:", "refactor:", "refactor(", "🧹", "🛡️"]):
        return "quality"
    if any(sub.startswith(p) for p in ["deps:", "dependencies:", "build:", "build(", "ci:", "ci(", "chore:", "chore("]):
        return "maintenance"
    if any(sub.startswith(p) for p in ["test:", "test(", "docs:", "doc:"]) or "🧪" in sub:
        return "tests_docs"
    return "other"


def format_commit_line(commit_hash: str, subject: str, author: str, repo: Optional[str] = None) -> str:
    # Clean up subject
    clean_sub = subject.strip()
    
    # Format PR links: (#123) -> ([#123](https://github.com/repo/pull/123))
    if repo:
        clean_sub = re.sub(
            r"#(\d+)",
            rf"[#\1](https://github.com/{repo}/pull/\1)",
            clean_sub,
        )
        short_hash = commit_hash[:8]
        hash_link = f"[`{short_hash}`](https://github.com/{repo}/commit/{commit_hash})"
    else:
        short_hash = commit_hash[:8]
        hash_link = f"`{short_hash}`"

    return f"- {clean_sub} ({hash_link} by @{author})"


def generate_changelog(
    prev_tag: Optional[str],
    current_tag: str,
    repo: Optional[str] = "xRahul/materialisheep",
    apk_dir: Optional[str] = None,
) -> str:
    range_spec = f"{prev_tag}..HEAD" if prev_tag else "HEAD"
    try:
        log_raw = run_git(["log", range_spec, '--pretty=format:%H%x09%s%x09%an'])
    except Exception:
        log_raw = ""

    categories = {
        "features": ("🚀 Features & Enhancements", []),
        "fixes": ("🐛 Bug Fixes", []),
        "performance": ("⚡ Performance Improvements", []),
        "quality": ("🛡️ Security & Code Quality", []),
        "maintenance": ("📦 Dependencies & Maintenance", []),
        "tests_docs": ("🧪 Tests & Documentation", []),
        "other": ("📝 Other Changes", []),
    }

    if log_raw:
        for line in log_raw.splitlines():
            parts = line.split("\t")
            if len(parts) >= 3:
                c_hash, c_subject, c_author = parts[0], parts[1], parts[2]
                # Filter out release commits
                if re.match(r"^chore\(release\):", c_subject, re.IGNORECASE):
                    continue
                cat_key = categorize_commit(c_subject)
                formatted_line = format_commit_line(c_hash, c_subject, c_author, repo)
                categories[cat_key][1].append(formatted_line)

    lines = []
    lines.append(f"## Release {current_tag}")
    lines.append("")
    if prev_tag and repo:
        lines.append(f"**Full Changelog**: [https://github.com/{repo}/compare/{prev_tag}...{current_tag}](https://github.com/{repo}/compare/{prev_tag}...{current_tag})")
        lines.append("")

    has_content = False
    for _, (title, item_list) in categories.items():
        if item_list:
            has_content = True
            lines.append(f"### {title}")
            for item in item_list:
                lines.append(item)
            lines.append("")

    if not has_content:
        lines.append("Maintenance release and quality improvements.")
        lines.append("")

    # Add APK Details & Checksums if APK directory exists
    if apk_dir and os.path.isdir(apk_dir):
        apk_files = [f for f in os.listdir(apk_dir) if f.endswith(".apk")]
        if apk_files:
            lines.append("### 📦 Assets & Verification")
            lines.append("| File | Size | SHA-256 Checksum |")
            lines.append("|---|---|---|")
            for apk_name in sorted(apk_files):
                apk_path = os.path.join(apk_dir, apk_name)
                size_mb = os.path.getsize(apk_path) / (1024 * 1024)
                with open(apk_path, "rb") as f:
                    sha256 = hashlib.sha256(f.read()).hexdigest()
                lines.append(f"| `{apk_name}` | {size_mb:.2f} MB | `{sha256}` |")
            lines.append("")

    lines.append("---")
    lines.append("*Compatible with Android 12+ (API 31–36)*")
    return "\n".join(lines)


def run_tests() -> None:
    print("Running release_helper tests...")
    # Test 1: Patch bump
    assert calculate_bump(3, 4, 13, 93, "patch") == (3, 4, 14, 94)
    # Test 2: Minor bump
    assert calculate_bump(3, 4, 13, 93, "minor") == (3, 5, 0, 94)
    # Test 3: Major bump
    assert calculate_bump(3, 4, 13, 93, "major") == (4, 0, 0, 94)
    # Test 4: Custom version
    assert calculate_bump(3, 4, 13, 93, "patch", custom_version="4.1.2") == (4, 1, 2, 94)
    # Test 5: Categorize commit
    assert categorize_commit("feat(ui): add dark mode switch") == "features"
    assert categorize_commit("fix: resolve crash on startup") == "fixes"
    assert categorize_commit("perf: pre-compile regex") == "performance"
    assert categorize_commit("deps: bump dagger") == "maintenance"
    print("All release_helper tests passed successfully!")


def main():
    parser = argparse.ArgumentParser(description="Materialisheep Release Helper")
    subparsers = parser.add_subparsers(dest="command")

    # Bump command
    bump_parser = subparsers.add_parser("bump", help="Bump version in version.properties")
    bump_parser.add_argument("--properties-file", default="version.properties", help="Path to version.properties")
    bump_parser.add_argument("--type", choices=["patch", "minor", "major"], default="patch", help="Bump type")
    bump_parser.add_argument("--custom-version", default=None, help="Explicit x.y.z version")
    bump_parser.add_argument("--custom-code", type=int, default=None, help="Explicit versionCode")
    bump_parser.add_argument("--dry-run", action="store_true", help="Print changes without writing file")

    # Changelog command
    ch_parser = subparsers.add_parser("changelog", help="Generate release changelog")
    ch_parser.add_argument("--prev-tag", default=None, help="Previous release tag (default: latest tag)")
    ch_parser.add_argument("--curr-tag", required=True, help="Current release tag (e.g. v3.4.14)")
    ch_parser.add_argument("--repo", default="xRahul/materialisheep", help="GitHub repo for PR and commit links")
    ch_parser.add_argument("--apk-dir", default=None, help="Path to release APK directory for checksums")
    ch_parser.add_argument("--output", default=None, help="Output markdown file path")

    # Test argument
    parser.add_argument("--test", action="store_true", help="Run self-tests")

    args = parser.parse_args()

    if args.test:
        run_tests()
        sys.exit(0)

    if args.command == "bump":
        props = read_version_properties(args.properties_file)
        cur_maj = int(props.get("VERSION_MAJOR", "3"))
        cur_min = int(props.get("VERSION_MINOR", "4"))
        cur_patch = int(props.get("VERSION_PATCH", "13"))
        cur_code = int(props.get("VERSION_CODE", "93"))

        new_maj, new_min, new_patch, new_code = calculate_bump(
            cur_maj,
            cur_min,
            cur_patch,
            cur_code,
            args.type,
            custom_version=args.custom_version if args.custom_version else None,
            custom_code=args.custom_code,
        )

        new_version_str = f"{new_maj}.{new_min}.{new_patch}"
        new_tag_str = f"v{new_version_str}"

        updated_props = {
            "VERSION_MAJOR": str(new_maj),
            "VERSION_MINOR": str(new_min),
            "VERSION_PATCH": str(new_patch),
            "VERSION_CODE": str(new_code),
        }

        if not args.dry_run:
            write_version_properties(args.properties_file, updated_props)

        print(f"BUMPED_VERSION={new_version_str}")
        print(f"BUMPED_TAG={new_tag_str}")
        print(f"BUMPED_CODE={new_code}")

        # Set GitHub Actions output if in CI environment
        github_output = os.getenv("GITHUB_OUTPUT")
        if github_output and os.path.exists(github_output):
            with open(github_output, "a", encoding="utf-8") as gh_out:
                gh_out.write(f"version={new_version_str}\n")
                gh_out.write(f"tag={new_tag_str}\n")
                gh_out.write(f"version_code={new_code}\n")

    elif args.command == "changelog":
        prev_tag = args.prev_tag or get_previous_tag(args.curr_tag)
        changelog_md = generate_changelog(
            prev_tag=prev_tag,
            current_tag=args.curr_tag,
            repo=args.repo,
            apk_dir=args.apk_dir,
        )

        if args.output:
            with open(args.output, "w", encoding="utf-8") as f:
                f.write(changelog_md)
            print(f"Changelog written to {args.output}")
        else:
            print(changelog_md)

    else:
        parser.print_help()


if __name__ == "__main__":
    main()
