#!/usr/bin/env python3
"""按 Conventional Commits 汇总上个 tag 至当前 tag 的变更，生成 Release Body。

用法: gen_release_notes.py --tag v0.5.0 --repo owner/name > release-notes.md
需要完整 tag 历史（actions/checkout 需 fetch-depth: 0）。
"""
import argparse
import re
import subprocess
import sys

SECTIONS = [
    ("feat", "✨ 新功能"),
    ("fix", "🐛 问题修复"),
    ("perf", "⚡ 性能优化"),
    ("refactor", "♻️ 代码重构"),
    ("docs", "📚 文档"),
    ("test", "✅ 测试"),
    ("build", "🔧 其他改进"),
    ("ci", "🔧 其他改进"),
    ("chore", "🔧 其他改进"),
    ("style", "🔧 其他改进"),
    ("revert", "🔧 其他改进"),
]
MISC_TITLE = "🔧 其他改进"
BREAKING_TITLE = "⚠️ 破坏性变更"
SUBJECT_RE = re.compile(r"^(\w+)(\(.*\))?!?:\s*(.+)$")


def run_git(*args: str) -> str:
    return subprocess.check_output(
        ["git", *args], text=True, encoding="utf-8", errors="replace"
    ).strip()


def previous_tag(tag: str) -> str:
    try:
        return run_git("describe", "--tags", "--abbrev=0", f"{tag}^")
    except subprocess.CalledProcessError:
        return ""


def categorize(subjects: list) -> tuple:
    titles = []
    for _, title in SECTIONS:
        if title not in titles:
            titles.append(title)
    titles.append(BREAKING_TITLE)
    titles.append(MISC_TITLE)
    buckets = {title: [] for title in titles}
    type_to_title = dict(SECTIONS)
    for s in subjects:
        m = SUBJECT_RE.match(s)
        if m is None:
            buckets[MISC_TITLE].append(s)
            continue
        type_, desc = m.group(1), m.group(3)
        breaking = s.rstrip().endswith("!")
        if breaking:
            buckets[BREAKING_TITLE].append(desc)
            continue
        buckets[type_to_title.get(type_, MISC_TITLE)].append(desc)
    return buckets


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Generate categorized release notes")
    parser.add_argument("--tag", required=True, help="current tag, e.g. v0.5.0")
    parser.add_argument("--repo", required=True, help="owner/name of the repository")
    args = parser.parse_args()

    prev = previous_tag(args.tag)
    rng = f"{prev}..{args.tag}" if prev else args.tag
    try:
        subjects = [l for l in run_git("log", rng, "--pretty=%s").splitlines() if l.strip()]
    except subprocess.CalledProcessError as e:
        print(f"git log failed: {e}", file=sys.stderr)
        return 1

    buckets = categorize(subjects)
    order = [BREAKING_TITLE]
    for _, title in SECTIONS:
        if title not in order:
            order.append(title)
    if MISC_TITLE not in order:
        order.append(MISC_TITLE)

    lines = []
    for title in order:
        items = buckets.get(title, [])
        if not items:
            continue
        lines.append(f"## {title}")
        lines.extend(f"- {item}" for item in items)
        lines.append("")

    if not lines:
        lines = ["此版本无显著变更。", ""]

    if prev:
        lines.append(
            f"**完整变更日志**: https://github.com/{args.repo}/compare/{prev}...{args.tag}"
        )
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
