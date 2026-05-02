#!/usr/bin/env python3

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "iosApp" / "Configuration" / "Version.xcconfig"
RELEASE_OUTPUT_DIR = ROOT / "build" / "release"
APK_DIR = ROOT / "composeApp" / "build" / "outputs" / "apk" / "full" / "release"
GITHUB_OWNER = "TheMrClaus"
GITHUB_REPO = "OmnioMobile"
APP_DISPLAY_NAME = "OmnioMobile"
RELEASE_BRANCH = "main"
RELEASE_CHANNEL_RE = re.compile(
    rf"(^|[^A-Za-z0-9]){re.escape(RELEASE_BRANCH)}([^A-Za-z0-9]|$)",
    re.IGNORECASE,
)
DEFAULT_BETA_NOTICE = (
    "## This is a beta version intended for testing only. Expect breaking changes "
    "in updates. Normal users are advised to wait for the stable release."
)
VERSION_NAME_RE = re.compile(r"(?m)^(MARKETING_VERSION=)(.+)$")
VERSION_CODE_RE = re.compile(r"(?m)^(CURRENT_PROJECT_VERSION=)(\d+)$")
VERSION_PATTERN = re.compile(r"^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$")
PREFIX_RE = re.compile(
    r"^(feat|fix|ref|refactor|perf|ui|ux|build|style|docs|test|ci|chore)"
    r"(\([^)]+\))?:\s*",
    re.IGNORECASE,
)
LEADING_ISSUE_RE = re.compile(
    r"^(fixe?[sd]?|close[sd]?|resolve[sd]?|address(?:e[sd])?)\s*#\d+\s*,?\s*",
    re.IGNORECASE,
)
DROP_PATTERNS = (
    re.compile(r"^merge\b", re.IGNORECASE),
    re.compile(r"^revert\b", re.IGNORECASE),
    re.compile(r"^docs?[:\s]", re.IGNORECASE),
    re.compile(r"^ci[:\s]", re.IGNORECASE),
    re.compile(r"^test[:\s]", re.IGNORECASE),
    re.compile(r"^chore[:\s]", re.IGNORECASE),
    re.compile(r"^release[:\s]", re.IGNORECASE),
    re.compile(r"^bump\b", re.IGNORECASE),
)
TRIM_TOKENS = ("cw", "wip")
WORD_REPLACEMENTS = (
    (re.compile(r"\bui\b", re.IGNORECASE), "UI"),
    (re.compile(r"\btrakt\b", re.IGNORECASE), "Trakt"),
    (re.compile(r"\bomnio\b", re.IGNORECASE), "Omnio"),
)


def run(
    *args: str,
    check: bool = True,
    capture_output: bool = True,
    cwd: Path = ROOT,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        cwd=cwd,
        check=check,
        text=True,
        capture_output=capture_output,
        env=env,
    )


def git(*args: str, check: bool = True) -> str:
    return run("git", *args, check=check).stdout.strip()


def require_tool(name: str) -> None:
    if shutil.which(name) is None:
        raise SystemExit(f"Missing required tool: {name}")


def read_version_file() -> str:
    return VERSION_FILE.read_text(encoding="utf-8-sig")


def parse_versions(contents: str) -> tuple[str, int]:
    version_match = VERSION_NAME_RE.search(contents)
    code_match = VERSION_CODE_RE.search(contents)
    if version_match is None or code_match is None:
        raise SystemExit(f"Failed to find MARKETING_VERSION/CURRENT_PROJECT_VERSION in {VERSION_FILE}")
    return version_match.group(2), int(code_match.group(2))


def update_version_file(contents: str, version_name: str, version_code: int) -> str:
    contents, version_replacements = VERSION_NAME_RE.subn(
        rf"\g<1>{version_name}", contents, count=1
    )
    contents, code_replacements = VERSION_CODE_RE.subn(
        rf"\g<1>{version_code}", contents, count=1
    )
    if version_replacements != 1 or code_replacements != 1:
        raise SystemExit(f"Failed to update version values in {VERSION_FILE}")
    return contents


def write_version_file(contents: str) -> None:
    VERSION_FILE.write_text(contents, encoding="utf-8")


def default_release_tag(version_name: str) -> str:
    return f"v{version_name}-main"


def default_release_title(version_name: str) -> str:
    return f"{APP_DISPLAY_NAME} {version_name}"


def last_tag() -> str | None:
    result = run("git", "describe", "--tags", "--abbrev=0", check=False)
    tag = result.stdout.strip()
    return tag or None


def last_main_tag() -> str | None:
    result = run(
        "git",
        "tag",
        "--list",
        "*-main",
        "--sort=-creatordate",
        check=False,
    )
    tag = next((line.strip() for line in result.stdout.splitlines() if line.strip()), None)
    return tag or last_tag()


def release_range(previous_tag: str | None) -> str | None:
    if previous_tag:
        return f"{previous_tag}..HEAD"
    return None


def commit_subjects(previous_tag: str | None) -> list[str]:
    args = ["log", "--reverse", "--pretty=format:%s"]
    revision_range = release_range(previous_tag)
    if revision_range:
        args.append(revision_range)
    output = git(*args)
    return [line.strip() for line in output.splitlines() if line.strip()]


def normalize_subject(subject: str) -> str | None:
    if not subject:
        return None

    candidate = subject.strip()
    for pattern in DROP_PATTERNS:
        if pattern.search(candidate):
            return None

    candidate = PREFIX_RE.sub("", candidate)
    while True:
        updated = LEADING_ISSUE_RE.sub("", candidate)
        if updated == candidate:
            break
        candidate = updated

    candidate = candidate.strip(" .")
    if not candidate:
        return None

    if candidate.lower().startswith(("with ", "one more ", "trying ")):
        return None

    words = candidate.split()
    while words and words[-1].lower() in TRIM_TOKENS:
        words.pop()
    candidate = " ".join(words).strip()
    if not candidate:
        return None

    for pattern, replacement in WORD_REPLACEMENTS:
        candidate = pattern.sub(replacement, candidate)
    candidate = re.sub(r"\s{2,}", " ", candidate).strip()
    if not candidate:
        return None

    if candidate.islower():
        candidate = candidate[:1].upper() + candidate[1:]

    return candidate


def dedupe_keep_latest(items: list[str], limit: int) -> list[str]:
    deduped: list[str] = []
    seen: set[str] = set()
    for item in items:
        key = item.casefold()
        if key in seen:
            continue
        seen.add(key)
        deduped.append(item)
    if limit > 0 and len(deduped) > limit:
        deduped = deduped[-limit:]
    return deduped


def parse_extra_notes(notes_text: str | None) -> list[str]:
    parsed: list[str] = []
    if not notes_text:
        return parsed

    for raw_line in notes_text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("- "):
            line = line[2:].strip()
        parsed.append(line)
    return parsed


def build_release_notes(
    previous_tag: str | None,
    custom_notes: str | None,
    extra_notes: str | None,
) -> str:
    extra_items = parse_extra_notes(extra_notes)

    if custom_notes and custom_notes.strip():
        lines = [custom_notes.strip()]
        if extra_items:
            lines.extend(["", "### Additional Notes"])
            lines.extend(f"- {item}" for item in extra_items)
        return "\n".join(lines).strip() + "\n"

    normalized = [
        note
        for note in (normalize_subject(subject) for subject in commit_subjects(previous_tag))
        if note
    ]
    bullet_items = dedupe_keep_latest(normalized, 10)
    bullet_items.extend(extra_items)

    if not bullet_items:
        bullet_items = ["Beta maintenance update"]

    lines = [DEFAULT_BETA_NOTICE, "", "### Improvements & Fixes"]
    lines.extend(f"- {item}" for item in bullet_items)
    return "\n".join(lines).strip() + "\n"


def release_notes_path(note_key: str) -> Path:
    RELEASE_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    safe_name = note_key.replace("/", "-")
    return RELEASE_OUTPUT_DIR / f"release-notes-{safe_name}.md"


def write_release_notes(note_key: str, notes: str) -> Path:
    path = release_notes_path(note_key)
    path.write_text(notes, encoding="utf-8")
    return path


def current_branch() -> str:
    branch = git("rev-parse", "--abbrev-ref", "HEAD")
    if branch == "HEAD":
        branch = os.environ.get("GITHUB_REF_NAME", "").strip()
    if not branch:
        raise SystemExit("Unable to determine the current branch for publishing")
    return branch


def ensure_release_branch(branch_name: str) -> None:
    if branch_name != RELEASE_BRANCH:
        raise SystemExit(
            f"Draft and publish releases must run from the {RELEASE_BRANCH} branch. "
            f"Current branch: {branch_name}"
        )


def ensure_release_tag_channel(release_tag: str) -> None:
    if not RELEASE_CHANNEL_RE.search(release_tag):
        raise SystemExit(
            f"Release tag must include the {RELEASE_BRANCH} channel marker so the updater "
            f"can discover it. Current tag: {release_tag}"
        )


def ensure_clean_worktree() -> None:
    status = git("status", "--short")
    if status:
        raise SystemExit(
            "Refusing to publish from a dirty worktree. Commit or stash changes first."
        )


def ensure_tag_available(release_tag: str) -> None:
    tag_check = run(
        "git", "rev-parse", "-q", "--verify", f"refs/tags/{release_tag}", check=False
    )
    if tag_check.returncode == 0:
        raise SystemExit(f"Tag already exists: {release_tag}")


def build_release() -> list[Path]:
    subprocess.run(
        ["./gradlew", ":composeApp:assembleFullRelease"],
        cwd=ROOT,
        check=True,
        text=True,
    )
    assets = sorted(APK_DIR.glob("*.apk"))
    if not assets:
        raise SystemExit(f"No APK assets found in {APK_DIR}")
    return assets


def commit_version_bump(commit_message: str) -> None:
    subprocess.run(["git", "add", str(VERSION_FILE.relative_to(ROOT))], cwd=ROOT, check=True)
    subprocess.run(["git", "commit", "-m", commit_message], cwd=ROOT, check=True, text=True)


def pull_branch_rebase(branch_name: str) -> bool:
    previous_head = git("rev-parse", "HEAD")
    subprocess.run(["git", "pull", "--rebase", "origin", branch_name], cwd=ROOT, check=True)
    return git("rev-parse", "HEAD") != previous_head


def create_release_tag(release_tag: str, release_title: str) -> None:
    subprocess.run(
        ["git", "tag", "-a", release_tag, "-m", f"Release {release_title}"],
        cwd=ROOT,
        check=True,
        text=True,
    )


def push_branch_and_tag(branch_name: str, release_tag: str) -> None:
    subprocess.run(["git", "push", "origin", f"HEAD:{branch_name}"], cwd=ROOT, check=True)
    subprocess.run(["git", "push", "origin", release_tag], cwd=ROOT, check=True)


def create_github_release(
    release_tag: str,
    release_title: str,
    notes_path: Path,
    assets: list[Path],
    *,
    draft: bool,
) -> None:
    require_tool("gh")
    command = [
        "gh",
        "release",
        "create",
        release_tag,
        *[str(asset) for asset in assets],
        "--title",
        release_title,
        "--notes-file",
        str(notes_path),
    ]
    if draft:
        command.append("--draft")
    subprocess.run(command, cwd=ROOT, check=True, text=True)


def load_custom_notes(args: argparse.Namespace) -> str | None:
    if args.custom_notes:
        return args.custom_notes
    if args.custom_notes_file:
        return Path(args.custom_notes_file).read_text(encoding="utf-8")
    return None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Preview, draft, or publish an OmnioMobile beta GitHub release using either "
            "auto mode (version bump from Version.xcconfig) or manual mode "
            "(explicit release tag/title without editing Version.xcconfig)."
        )
    )
    parser.add_argument(
        "version",
        nargs="?",
        help=(
            "Target MARKETING_VERSION for auto mode, for example 0.1.1-beta.20260502-1. "
            "Required unless --manual-release is set."
        ),
    )
    parser.add_argument(
        "--manual-release",
        action="store_true",
        help="Skip changing Version.xcconfig and require --release-tag plus --release-title.",
    )
    parser.add_argument(
        "--version-code",
        type=int,
        help="Explicit CURRENT_PROJECT_VERSION to write. Defaults to current versionCode + 1.",
    )
    parser.add_argument(
        "--release-tag",
        help="Release tag override. Defaults to v<version>-main in auto mode.",
    )
    parser.add_argument(
        "--release-title",
        help="Release title override. Defaults to 'OmnioMobile <version>' in auto mode.",
    )
    parser.add_argument(
        "--custom-notes",
        help="Full markdown release notes that replace generated notes.",
    )
    parser.add_argument(
        "--custom-notes-file",
        help="Path to a markdown file whose contents replace generated notes.",
    )
    parser.add_argument(
        "--extra-notes",
        help="Optional multi-line text. Each non-empty line is appended as a bullet.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Generate preview notes only. No version edits, builds, tags, or releases.",
    )
    parser.add_argument(
        "--draft",
        action="store_true",
        help="Build, tag, push, and create the GitHub release as a draft.",
    )
    parser.add_argument(
        "--publish",
        action="store_true",
        help="Build, tag, push, and create a normal GitHub release.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    selected_modes = [args.dry_run, args.draft, args.publish]
    if sum(1 for enabled in selected_modes if enabled) != 1:
        raise SystemExit("Use exactly one of --dry-run, --draft, or --publish.")
    if args.custom_notes and args.custom_notes_file:
        raise SystemExit("Use either --custom-notes or --custom-notes-file, not both.")

    original_contents = read_version_file()
    current_version_name, current_version_code = parse_versions(original_contents)
    previous_tag = last_main_tag()

    if args.manual_release:
        if args.version:
            raise SystemExit("Do not provide version when --manual-release is set.")
        if args.version_code is not None:
            raise SystemExit("--version-code is not supported when --manual-release is set.")
        if not args.release_tag:
            raise SystemExit("--release-tag is required when --manual-release is set.")
        if not args.release_title:
            raise SystemExit("--release-title is required when --manual-release is set.")
        target_version_name = current_version_name
        next_version_code = current_version_code
        release_tag = args.release_tag
        release_title = args.release_title
        commit_message = "manual release: no version bump"
        notes_key = release_tag
    else:
        if not args.version:
            raise SystemExit("version is required unless --manual-release is set.")
        if not VERSION_PATTERN.match(args.version):
            raise SystemExit(f"Invalid version format: {args.version}")
        target_version_name = args.version
        next_version_code = (
            args.version_code if args.version_code is not None else current_version_code + 1
        )
        if next_version_code < 1:
            raise SystemExit("versionCode must be a positive integer.")
        release_tag = args.release_tag or default_release_tag(target_version_name)
        release_title = args.release_title or default_release_title(target_version_name)
        commit_message = f"release: {release_tag}"
        notes_key = target_version_name

    ensure_release_tag_channel(release_tag)

    custom_notes = load_custom_notes(args)
    notes = build_release_notes(
        previous_tag=previous_tag,
        custom_notes=custom_notes,
        extra_notes=args.extra_notes,
    )
    notes_path = write_release_notes(notes_key, notes)

    print(f"Current versionName: {current_version_name}")
    print(f"Current versionCode: {current_version_code}")
    print(f"Target versionName: {target_version_name}")
    print(f"Target versionCode: {next_version_code}")
    print(f"Release tag: {release_tag}")
    print(f"Release title: {release_title}")
    print(f"Commit message: {commit_message}")
    print(f"Previous tag: {previous_tag or 'none'}")
    print(f"Release notes: {notes_path.relative_to(ROOT)}")
    print()
    print(notes.strip())
    print()

    if args.dry_run:
        print("Dry run only; no version file edits, builds, tags, or releases.")
        return 0

    ensure_clean_worktree()
    ensure_tag_available(release_tag)

    version_file_written = False
    committed_or_tagged = False

    try:
        branch_name = current_branch()
        ensure_release_branch(branch_name)

        if not args.manual_release:
            updated_contents = update_version_file(
                original_contents,
                version_name=target_version_name,
                version_code=next_version_code,
            )
            write_version_file(updated_contents)
            version_file_written = True
            print(f"Updated {VERSION_FILE.relative_to(ROOT)}")

        if args.manual_release:
            if pull_branch_rebase(branch_name):
                previous_tag = last_main_tag()
                notes = build_release_notes(
                    previous_tag=previous_tag,
                    custom_notes=custom_notes,
                    extra_notes=args.extra_notes,
                )
                notes_path = write_release_notes(notes_key, notes)
                print(f"Regenerated release notes after rebasing onto {branch_name}.")
            assets = build_release()
        else:
            assets = build_release()
            commit_version_bump(commit_message)
            if pull_branch_rebase(branch_name):
                previous_tag = last_main_tag()
                notes = build_release_notes(
                    previous_tag=previous_tag,
                    custom_notes=custom_notes,
                    extra_notes=args.extra_notes,
                )
                notes_path = write_release_notes(notes_key, notes)
                print(f"Regenerated release notes after rebasing onto {branch_name}.")
                assets = build_release()
                print(f"Rebuilt release assets after rebasing onto {branch_name}.")

        print("Built release assets:")
        for asset in assets:
            print(f"- {asset.relative_to(ROOT)}")

        create_release_tag(release_tag, release_title)
        push_branch_and_tag(branch_name, release_tag)
        committed_or_tagged = True

        create_github_release(
            release_tag,
            release_title,
            notes_path,
            assets,
            draft=args.draft,
        )

        if args.draft:
            print(
                f"Created draft GitHub release {release_tag} "
                f"({release_title}) from branch {branch_name}"
            )
        else:
            print(
                f"Published GitHub release {release_tag} "
                f"({release_title}) from branch {branch_name}"
            )
    except Exception:
        if version_file_written and not committed_or_tagged:
            write_version_file(original_contents)
        raise

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        if exc.stdout:
            sys.stdout.write(exc.stdout)
        if exc.stderr:
            sys.stderr.write(exc.stderr)
        raise SystemExit(exc.returncode) from exc
