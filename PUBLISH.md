# HOLE — publication steps (Linux Mint)

## 1. GitHub repository, build, release

Unzip `HOLE-PUBLISH-v0.1.0.zip` into Downloads. Open the terminal and run:

```bash
cd "$(xdg-user-dir DOWNLOAD)/HOLE-PUBLISH-v0.1.0"
sudo apt update && sudo apt install -y gh git
gh auth login --web
bash scripts/publish-github.sh
```

The shell script will create **only** `Masato-Nasu/HOLE`, commit this archive, push `main`, tag `v0.1.0` and push the tag. **If that repository already exists**, it checks its origin and pushes only to that repository. It will not change TANGO-CHO or any other project.

GitHub Actions automatically builds the test APK and publishes a GitHub Release from the `v0.1.0` tag. Verify a green check on the repository's **Actions** tab and that the APK actually appears in **Releases**. If the workflow fails, there is no downloadable release APK yet.

The build requires network access to fetch Gradle and Android dependencies. You can also build locally with Android Studio (JDK 17, SDK 35).

## 2. Authentic screenshot / short video

Install the debug APK and launch HOLE on the phone. With USB debugging enabled and the phone connected:

```bash
cd "$(xdg-user-dir DOWNLOAD)/HOLE-PUBLISH-v0.1.0"
bash scripts/capture-screen.sh docs/screenshots/HOLE-home.png
# Change to 50% overlay with another app visible, then:
bash scripts/capture-screen.sh docs/screenshots/HOLE-active-50.png
```

These photos are *not* present in the archive; the script captures real device pixels and requires `adb` (`sudo apt install adb` on most Mint systems). For the see-through illusion itself, it is better to **film the phone with a second camera** so the outside scene and physical frame are visible. The readme can be updated after real footage is captured.

## 3. MASATO LAB Tools (safe, no overwrite)

The live site's tools are stored in Cloudflare KV, **not** the old repository fallback `data.json`. Don't upload a stale JSON over the live site.

Open `https://masato-lab.pages.dev/editor/` and choose Tools → add item:

- TITLE: `HOLE — Transparent Phone`
- DESCRIPTION: `スマートフォンの画面を、向こう側が見える「穴」に見せるAndroidアプリ。背面カメラを半透明で重ね、50%ではほかのアプリも操作できます。`
- CATEGORY: `PHOTO・CAMERA`
- PLATFORM: `ANDROID`
- APP URL: `https://github.com/Masato-Nasu/HOLE/releases/latest`
- REPO URL: `https://github.com/Masato-Nasu/HOLE`
- TAGS: `Android, Camera, Overlay, Experimental, Optical Illusion`
- IMAGE: use the **real screenshot** after capturing it, rather than an artificial screen reproduction.

Click **サイトに保存**. The app URL must not be published until the release APK appears.

A standalone landing page is staged in the [MASATO-LAB GitHub repository](https://github.com/Masato-Nasu/MASATO-LAB/blob/main/tools/hole/index.html). To publish that single page from your **current** Cloudflare Pages local folder (never from an outdated clone), run:

`bash scripts/deploy-masato-lab.sh /path/to/CURRENT-masato-lab-pages-folder`

The script copies exactly one new `tools/hole/index.html` and then deploys. It refuses to overwrite an existing page. Do not use an outdated site folder: deploying it could roll back unrelated static changes.

Alternative for data.json export users: download the current editor's JSON to Downloads, run
`python3 scripts/add-to-masato-lab.py "$(xdg-user-dir DOWNLOAD)/data.json"`, then import the resulting file into the editor and **サイトに保存**. This script preserves all other arrays and keys.

## 4. Public copy

- `docs/BLOG-JA.md` blog article
- `docs/X-POSTS.md` Japanese/English tweets
- `docs/MASATO-LAB-ENTRY.json` one tool entry data, provided separately

## Status note

We can't confirm a build APK or live site's publication before Github Actions and a Cloudflare editor save are actually performed. Please verify the links after those two steps.
