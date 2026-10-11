#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
REPO='Masato-Nasu/HOLE'
URL="https://github.com/${REPO}.git"
command -v gh >/dev/null || { echo 'gh が必要です: sudo apt install gh'; exit 1; }
command -v git >/dev/null || { echo 'git が必要です: sudo apt install git'; exit 1; }
gh auth status >/dev/null || { echo 'GitHub 認証が必要です: gh auth login --web'; exit 1; }
if [ -d .git ] && [ "$(git rev-parse --show-toplevel)" != "$(pwd -P)" ]; then
  echo '別のリポジトリ内です。処理を中断します。' >&2; exit 1
fi
if [ ! -d .git ]; then git init -b main; fi
git branch -M main
git add -A
if ! git diff --cached --quiet; then git commit -m 'Publish HOLE Transparent Phone P0.1'; fi
if gh repo view "$REPO" >/dev/null 2>&1; then
  if git remote get-url origin >/dev/null 2>&1; then
    EXISTING="$(git remote get-url origin)"
    case "$EXISTING" in "$URL"|"git@github.com:${REPO}.git") ;; *) echo "別のoriginです: $EXISTING"; exit 1;; esac
  else
    git remote add origin "$URL"
  fi
  git push -u origin main
else
  gh repo create "$REPO" --public --description 'HOLE — Transparent Phone (Android optical overlay experiment)' --source . --remote origin --push
fi
if ! git rev-parse --verify -q refs/tags/v0.1.0 >/dev/null; then git tag v0.1.0; fi
git push origin v0.1.0
echo "GitHub: https://github.com/${REPO}"
echo "Actions: https://github.com/${REPO}/actions"
echo "Releases: https://github.com/${REPO}/releases"
