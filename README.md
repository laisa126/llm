# 🤖 LocalLLM

> **A local Lovable/Bolt alternative for Android.** Full AI coding assistant that runs entirely on-device using Gemma 4 via MediaPipe — no internet, no cloud, no API costs.

[![Build APK](https://github.com/LaiserDev/LocalLLM/actions/workflows/build.yml/badge.svg)](https://github.com/LaiserDev/LocalLLM/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## ✨ Features

### 🧠 AI Engine
- **Gemma 4 4B** — Google's latest on-device model (vision + code)
- **Gemma 3 4B** — Vision + code, 6GB+ RAM
- **Gemma 3 1B** — Ultra-fast, runs on any Android
- Streaming token output with tokens/sec display
- **Agent mode** — AI calls tools autonomously (read/write files, run commands, install packages)

### 💻 Code Editor
- Full file tree explorer with expand/collapse
- Multi-tab code editor with syntax highlighting
- Create, rename, delete files and folders
- Save with unsaved-change indicator
- AI assistant panel per file (explain, refactor, review)

### 🔧 Tools & Skills
- **15 built-in tools** the AI can call:
  - `read_file`, `write_file`, `create_file`, `delete_file`
  - `list_files`, `make_dir`, `apply_diff`, `grep_files`
  - `run_command`, `install_package` (npm/pip/apt/yarn)
  - `search_web`, `http_get`
  - `list_processes`, `kill_process`, `get_file_info`
- **12 built-in skills** (templates):
  - Generate Project, Fix Bug, Explain Code, Refactor
  - Write Tests, Write Docs, Shell Script, Code Review
  - Optimize Performance, Convert Language, API Integration, Regex Builder
- Custom skills — create your own with `{{VARIABLE}}` placeholders

### 📁 Projects
- Create projects from scratch
- **Generate full project from a prompt** — AI writes all files as JSON, app creates them
- ZIP export with timestamp
- ZIP import (share projects, import from other tools)
- Auto-detect run command (npm start, python manage.py, cargo run, etc.)

### 🖥️ Terminal
- Real shell via `ProcessBuilder` (`/system/bin/sh`)
- Built-in commands: `cd`, `ls`, `pwd`, `clear`
- Command history
- **Package installer UI** — npm, pip, apt, yarn
- One-tap: Run Project, Kill Process, Clear

### 🌐 Live Preview
- WebView-based HTML/CSS/JS live preview
- Loads relative assets from project folder
- Auto-refresh when file saved

### 🔌 Developer API
- **Ktor HTTP server on port 8080** runs as foreground service
- Endpoints:
  - `GET /health` — status check (public)
  - `POST /v1/chat` — LLM completion (auth required)
  - `POST /v1/code` — code generation (auth required)
  - `GET /v1/models` — loaded model info (auth required)
- API key auth via `X-API-Key` header
- Keys validated against Supabase in real-time
- Usage counter incremented per request
- Network IP shown in app for LAN access

---

## 🚀 Quick Start

### Prerequisites
- Android device with Android 10+ (API 29+)
- 6GB+ RAM recommended
- 3–8GB free storage (for models)

### Install
1. Download the latest APK from [Releases](https://github.com/LaiserDev/LocalLLM/releases)
2. On device: **Settings → Security → Install Unknown Apps → Enable**
3. Open APK → Install
4. Open app → go to **Models** tab → Download a model
5. Tap **Load Model** once downloaded
6. Go to **Chat** and start coding!

---

## 🏗️ Architecture

```
LocalLLM/
├── .github/
│   └── workflows/
│       ├── build.yml          ← APK build + GitHub Release CI
│       └── dashboard.yml      ← Vercel deploy CI
│
├── app/src/main/java/com/laiserdev/localllm/
│   ├── LocalLLMApp.kt         ← Application class, DI
│   ├── MainActivity.kt        ← Entry point
│   │
│   ├── data/
│   │   ├── model/Models.kt    ← All domain models (ChatMessage, Project, Skill, ApiKey...)
│   │   └── repository/
│   │       ├── LLMRepository.kt         ← MediaPipe inference wrapper
│   │       ├── ProjectRepository.kt     ← File system, ZIP import/export, AI project gen
│   │       ├── ModelDownloadService.kt  ← Foreground service, downloads .task files
│   │       └── ApiKeyRepository.kt      ← Supabase API key validation
│   │
│   ├── server/
│   │   └── LLMServerService.kt  ← Ktor HTTP server, auth middleware, /v1/* routes
│   │
│   ├── util/
│   │   ├── ToolEngine.kt        ← Agentic loop, 15 tools, <tool_call> parser
│   │   ├── SkillsManager.kt     ← 12 built-in skills + custom skill CRUD
│   │   ├── PackageManager.kt    ← npm/pip/apt/yarn installer, runtime detection
│   │   ├── TerminalExecutor.kt  ← ProcessBuilder shell, builtin commands
│   │   └── SettingsManager.kt   ← DataStore preferences
│   │
│   └── ui/
│       ├── MainViewModel.kt     ← Central state (chat, projects, models, terminal)
│       ├── AppNavigation.kt     ← Bottom nav, NavHost
│       ├── theme/Theme.kt       ← VSCode dark color palette
│       └── screens/
│           ├── chat/            ← Chat UI, agent mode, skill picker, image input
│           ├── editor/          ← File tree, code editor, AI panel, tabs
│           ├── terminal/        ← Interactive shell, package installer
│           ├── preview/         ← WebView HTML live preview
│           ├── models/          ← Download/load models, hardware info
│           ├── developer/       ← API docs, server toggle, endpoint display
│           └── settings/        ← System prompt, temperature, tokens, font size
│
├── supabase/
│   └── schema.sql             ← Run in Supabase SQL editor to create api_keys table
│
└── localllm-dashboard/        ← React + Vite web app (deploy to Vercel)
    ├── src/App.jsx             ← API key signup page
    └── vercel.json
```

---

## 🔧 Setup & Configuration

### 1. Supabase Setup

1. Create a project at [supabase.com](https://supabase.com)
2. Go to **SQL Editor** → paste contents of `supabase/schema.sql` → Run
3. Copy your **Project URL** and **anon public key** from Settings → API

### 2. GitHub Secrets

Go to your repo → **Settings → Secrets → Actions** → add:

| Secret | Value |
|--------|-------|
| `SUPABASE_URL` | `https://xxx.supabase.co` |
| `SUPABASE_ANON_KEY` | `eyJ...` |
| `KEYSTORE_BASE64` | Base64-encoded `.jks` file (for release signing) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
| `VERCEL_TOKEN` | From vercel.com → Settings → Tokens |
| `VERCEL_ORG_ID` | From `.vercel/project.json` |
| `VERCEL_PROJECT_ID` | From `.vercel/project.json` |

### 3. Generate a Keystore (one time)

```bash
keytool -genkey -v \
  -keystore localllm-release.jks \
  -alias localllm \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Base64 encode for GitHub secret
base64 -i localllm-release.jks | pbcopy   # macOS
base64 localllm-release.jks               # Linux
```

### 4. Release an APK

```bash
# Creates a GitHub Release with signed APK attached
git tag v1.0.0
git push origin v1.0.0
```

### 5. Deploy Dashboard

```bash
cd localllm-dashboard
cp .env.example .env
# Fill in your Supabase values
npm install
npm run dev        # local dev
npm run build      # production build

# Or push to main — GitHub Actions auto-deploys to Vercel
```

---

## 🔌 API Usage (for developers)

```bash
# 1. Start the server in the app (Developer tab → toggle ON)

# 2. Health check
curl http://DEVICE_IP:8080/health

# 3. Chat completion
curl -X POST http://DEVICE_IP:8080/v1/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: llm_yourkey" \
  -d '{"prompt": "Write a Python function to reverse a string"}'

# 4. Code generation
curl -X POST http://DEVICE_IP:8080/v1/code \
  -H "Content-Type: application/json" \
  -H "X-API-Key: llm_yourkey" \
  -d '{"prompt": "Build an Express.js REST API with CRUD for users"}'
```

### Response format
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "def reverse_string(s): return s[::-1]",
  "model": "gemma3-4b",
  "tokensUsed": 0,
  "latencyMs": 1420
}
```

---

## 🛠️ Local Development

```bash
# Clone
git clone https://github.com/LaiserDev/LocalLLM.git
cd LocalLLM

# Open in Android Studio (Hedgehog or newer)
# File → Open → select LocalLLM/

# Create local.properties (not committed)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| MediaPipe Tasks GenAI | On-device LLM inference (Gemma) |
| Ktor Server (Netty) | Local HTTP API server |
| Supabase Kotlin | API key validation |
| Sora Editor | Code editor with syntax highlighting |
| Zip4j | ZIP import/export |
| Coil | Image loading |
| RichText | Markdown rendering for AI output |
| Jetpack Compose + Material3 | UI framework |
| DataStore | Settings persistence |

---

## 🗺️ Roadmap

- [ ] Git integration (clone, commit, push via JGit)
- [ ] Multi-turn agent memory across sessions
- [ ] Image generation (with compatible model)
- [ ] Voice input → code
- [ ] Plugin system for custom tools
- [ ] ngrok tunnel for remote API access
- [ ] Split-screen: Editor + Preview side by side
- [ ] Diff viewer for AI code changes

---

## 📄 License

MIT — free for personal and commercial use.

---

Built by [LaiserDev](https://github.com/LaiserDev) 🚀
