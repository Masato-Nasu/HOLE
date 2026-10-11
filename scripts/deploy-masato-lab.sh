#!/usr/bin/env bash
set -euo pipefail
# Deploy exactly one new static page from this publication bundle into a known, current Pages worktree.
cd "$(dirname "$0")/.."
if [[ $# -ne 1 ]]; then
  echo '使い方: bash scripts/deploy-masato-lab.sh /path/to/CURRENT-masato-lab-pages-folder' >&2
  exit 1
fi
SITE="$(realpath "$1")"
[[ -d "$SITE" && -f "$SITE/tools.html" && -f "$SITE/editor.html" ]] || { echo 'MASATO LAB の最新サイトフォルダが確認できません。変更しません。' >&2; exit 1; }
[[ -f "$SITE/functions/api/data.js" || -d "$SITE/functions" ]] || { echo 'Workers Functions がありません。別のフォルダの可能性があります。変更しません。' >&2; exit 1; }
[[ ! -e "$SITE/tools/hole/index.html" ]] || { echo 'HOLEページがすでに存在します。上書きせず中断します。'; exit 1; }
mkdir -p "$SITE/tools/hole"
# Publish exactly the page already committed to the connected MASATO-LAB GitHub repository.
command -v curl >/dev/null || { echo 'curl が必要です'; exit 1; }
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
curl --fail --location --silent --show-error 'https://raw.githubusercontent.com/Masato-Nasu/MASATO-LAB/main/tools/hole/index.html' -o "$TMP"
grep -q 'HOLE — Transparent Phone' "$TMP" || { echo 'GitHub のHOLEページを検証できません'; exit 1; }
cp "$TMP" "$SITE/tools/hole/index.html"
echo "新しいページ1ファイルのみ追加: $SITE/tools/hole/index.html"
echo '既存のページやCloudflare KVには触れていません。'
echo 'これから既存サイトフォルダをPagesにデプロイします。'
npx wrangler pages deploy "$SITE" --project-name masato-lab
