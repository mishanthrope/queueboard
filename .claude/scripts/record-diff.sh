#!/usr/bin/env bash

set -euo pipefail

root="$(git rev-parse --show-toplevel)"
cd "$root"
git_dir="$(git rev-parse --git-dir)"
baseline_file="$git_dir/claude-baseline-tree"
tmp_index="$git_dir/claude-snap-index"

snapshot() {
    rm -f "$tmp_index"
    GIT_INDEX_FILE="$tmp_index" git add -A -- . ':!diff.txt' 2>/dev/null
    GIT_INDEX_FILE="$tmp_index" git write-tree
    rm -f "$tmp_index"
}

current="$(snapshot)"

if [[ -f "$baseline_file" ]]; then
    baseline="$(cat "$baseline_file")"
    {
        echo "=================================================================="
        echo "Prompt: ${1:-<no description>}"
        echo "Date:   $(date '+%Y-%m-%d %H:%M:%S')"
        echo "=================================================================="
        git diff --stat "$baseline" "$current"
        echo
        git diff "$baseline" "$current"
        echo
    } >> diff.txt
fi

echo "$current" > "$baseline_file"
