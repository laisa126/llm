# 🚀 LocalLLM — Setup Guide (GitHub Actions Only)

> You do **NOT** need Android Studio, Gradle, or Java installed locally.
> Everything builds in the cloud. Just push code → get APK.

---

## Step 1 — Fork / Create the Repo

```bash
# Option A: Fork this repo on GitHub (click Fork button)

# Option B: Push this ZIP as a new repo
git init
git add .
git commit -m "feat: initial LocalLLM project"
git remote add origin https://github.com/YOUR_USERNAME/LocalLLM.git
git push -u origin main
```

---

## Step 2 — Set Up Supabase (5 minutes)

1. Go to [supabase.com](https://supabase.com) → New project
2. **SQL Editor** → paste `supabase/schema.sql` → **Run**
3. Go to **Settings → API** → copy:
   - `Project URL` → e.g. `https://abcxyz.supabase.co`
   - `anon public` key → starts with `eyJ...`

---

## Step 3 — Add GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**

### Required secrets:

| Secret Name | Where to get it | Required for |
|-------------|-----------------|--------------|
| `SUPABASE_URL` | Supabase → Settings → API | Debug + Release |
| `SUPABASE_ANON_KEY` | Supabase → Settings → API | Debug + Release |
| `KEYSTORE_BASE64` | See below | Release APK only |
| `KEYSTORE_PASSWORD` | You choose | Release APK only |
| `KEY_ALIAS` | You choose | Release APK only |
| `KEY_PASSWORD` | You choose | Release APK only |

> **Note:** For debug builds (PRs, pushes to main), the CI auto-generates
> a temporary keystore. You only need the keystore secrets for proper
> signed release APKs (tagged releases).

### Generate a keystore for release signing:

You need Java installed just for this one-time step, OR use GitHub Codespaces:

```bash
keytool -genkey -v \
  -keystore localllm-release.jks \
  -alias localllm \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=LocalLLM, OU=App, O=LaiserDev, C=KE" \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD

# Base64 encode it for the GitHub secret
base64 -i localllm-release.jks   # Linux / macOS
```

Copy the entire base64 output as `KEYSTORE_BASE64`.

---

## Step 4 — Deploy the Dashboard (API Key Signup)

### Option A: Vercel (recommended, free)

1. Go to [vercel.com](https://vercel.com) → New Project
2. Import your GitHub repo
3. Set **Root Directory** to `localllm-dashboard`
4. Add environment variables:
   - `VITE_SUPABASE_URL` = your Supabase URL
   - `VITE_SUPABASE_ANON_KEY` = your anon key
5. Deploy → you get a URL like `localllm-dashboard.vercel.app`

For CI auto-deploy on push, add to GitHub Secrets:
| `VERCEL_TOKEN` | vercel.com → Settings → Tokens |
| `VERCEL_ORG_ID` | from `.vercel/project.json` after first deploy |
| `VERCEL_PROJECT_ID` | from `.vercel/project.json` |

### Option B: Netlify / GitHub Pages
Works too — just point build to `localllm-dashboard/` with `npm run build`.

---

## Step 5 — Build Your First APK

### Automatic: every push to `main`
```bash
git add .
git commit -m "feat: my changes"
git push origin main
```
→ GitHub Actions runs automatically → APK appears in **Actions → your workflow run → Artifacts**

### Manual: trigger from GitHub UI
1. Go to **Actions** tab
2. Click **Build & Release APK**
3. Click **Run workflow** → choose `debug` or `release`
4. Wait ~5 minutes → download APK from Artifacts

### Release: tag a version
```bash
git tag v1.0.0
git push origin v1.0.0
```
→ CI builds release APK → creates GitHub Release with APK attached → users can download directly

---

## Step 6 — Install on Your Phone

1. Download the APK from **Actions → Artifacts** or **Releases**
2. On your Android phone:
   - **Settings → Security** (or **Privacy**)
   - Enable **Install Unknown Apps** (for your browser or Files app)
3. Open the APK file → **Install**
4. Open **LocalLLM** → go to **Models** tab
5. Download **Gemma 3 1B** (fastest, ~700MB) to start
6. Tap **Load** → go to **Chat** → start coding!

---

## Workflow Overview

```
You push code
     ↓
GitHub Actions triggers
     ↓
┌─────────────────────────────────────────────────────┐
│  Job 1: Build                                       │
│  1. Checkout code                                   │
│  2. Setup JDK 17 + Android SDK 35                   │
│  3. Setup Gradle 8.7 (no local install needed)      │
│  4. Create local.properties from env                │
│  5. Inject SUPABASE_URL/KEY as build config         │
│  6. Auto-generate debug keystore OR decode release  │
│  7. ./gradlew assembleDebug / assembleRelease       │
│  8. Rename APK, upload as artifact                  │
└─────────────────────────────────────────────────────┘
     ↓ (only on git tag v*)
┌─────────────────────────────────────────────────────┐
│  Job 2: Release                                     │
│  1. Download artifact                               │
│  2. Generate changelog from git log                 │
│  3. Create GitHub Release with APK attached         │
└─────────────────────────────────────────────────────┘
     ↓ (always)
┌─────────────────────────────────────────────────────┐
│  Job 3: Summary                                     │
│  Writes build result to Actions step summary UI     │
└─────────────────────────────────────────────────────┘
```

---

## Troubleshooting CI

| Problem | Fix |
|---------|-----|
| `SDK location not found` | CI creates `local.properties` automatically — should not happen |
| `Keystore not found` | Check `KEYSTORE_BASE64` secret is set and valid base64 |
| `Duplicate class` | Run `./gradlew dependencies` locally or check for version conflicts |
| `Resource linking failed` | Check `res/values/` XML files for syntax errors |
| `Out of memory` | Add `-Xmx4g` to `gradle.properties` `org.gradle.jvmargs` |
| Build takes >10 min | Enable Gradle caching (already configured via `gradle/actions/setup-gradle`) |

---

## Quick Reference

```bash
# Trigger debug build
git push origin main

# Trigger release
git tag v1.2.3 && git push origin v1.2.3

# Manual trigger (any branch)
# → GitHub → Actions → Build & Release APK → Run workflow
```
