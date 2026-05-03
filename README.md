# Local LLM Agent

An on-device AI coding assistant for Android. Runs 100% locally — no cloud, no internet required after model download.

**Stack:** Kotlin · Jetpack Compose · LiteRT-LM · Ktor local API · GitHub Actions CI/CD

---

## Always clone fresh with:
```bash
git clone https://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@github.com/laisa126/llm.git /tmp/repo
```

---

## App Architecture

```
LocalLLMApp (Application)
├── MainActivity
│   └── LocalLLMTheme(fontFamily, fontSize)  ← live from DataStore
│       ├── BootstrapScreen    — model extraction + loading
│       ├── OnboardingScreen   — first-run walkthrough
│       └── AppNavigation      — drawer + 7 screens
│           ├── ChatScreen     — chat + agent mode
│           ├── EditorScreen   — code editor + file tree
│           ├── TerminalScreen — shell execution
│           ├── PreviewScreen  — live HTML WebView
│           ├── ModelsScreen   — download/load 9 models
│           ├── DeveloperScreen— local OpenAI-compatible API
│           └── SettingsScreen — fonts, model params, API
│
├── MainViewModel
│   ├── LLMRepository       ← LiteRT-LM Engine/Conversation
│   ├── ToolEngine          ← 17 agent tools
│   ├── ChatHistoryRepository
│   ├── ProjectRepository
│   ├── ModelDownloadService (ForegroundService)
│   ├── LLMServerService    (ForegroundService, Ktor/Netty)
│   └── SettingsManager     (DataStore)
│
└── WebViewRegistry         ← screenshot tool bridge
```

---

## Model Catalog (9 models, all from litert-community)

| Model | Size | RAM | Format | Notes |
|-------|------|-----|--------|-------|
| Gemma 3 1B | 0.7GB | 3GB | .task | Bundled in APK |
| Gemma 3n E2B | 1.5GB | 4GB | .task | Vision |
| Gemma 3n E4B | 2.0GB | 6GB | .task | Vision |
| Gemma 4 E2B | 2.58GB | 4GB | .litertlm | 32K ctx |
| Gemma 4 E4B | 3.65GB | 6GB | .litertlm | 32K ctx |
| Phi-4 Mini | 2.3GB | 5GB | .task | Best reasoning |
| Llama 3.2 1B | 0.8GB | 3GB | .task | Fast |
| Llama 3.2 3B | 1.8GB | 5GB | .task | Balanced |
| Qwen 3.5 2B | 1.5GB | 4GB | .task | Multilingual |
| Qwen 3.5 4B | 2.5GB | 6GB | .task | Best multilingual |

---

## Agent Tools (17 total)

| # | Tool | Description |
|---|------|-------------|
| 1 | `read_file` | Read file contents |
| 2 | `write_file` | Write/overwrite a file |
| 3 | `create_file` | Create new file |
| 4 | `delete_file` | Delete a file |
| 5 | `list_files` | List directory contents |
| 6 | `make_dir` | Create directory |
| 7 | `apply_diff` | Apply unified diff patch |
| 8 | `grep_files` | Search text in files |
| 9 | `get_file_info` | File metadata |
| 10 | `run_command` | Execute shell command |
| 11 | `install_package` | npm/pip/apt install |
| 12 | `search_web` | DuckDuckGo instant answers |
| 13 | `http_get` | Fetch URL content |
| 14 | `list_processes` | List running processes |
| 15 | `kill_process` | Kill a process by PID |
| 16 | `download_zip` | Download + extract ZIP from URL |
| 17 | `screenshot` | Capture Preview tab WebView |

---

## Current State (per session)

### Completed
- [x] Crash fixes (theme parent, FileProvider, emit() in channelFlow)
- [x] LiteRT-LM engine migration (from MediaPipe)
- [x] 9-model catalog with auto HF token (no user input)
- [x] Agent tools 1–17 all wired
- [x] download_zip + screenshot tools
- [x] Full UI redesign (cyan/purple theme, glass cards)
- [x] Customizable fonts (sans/serif/mono + size picker)
- [x] Claude-style agent steps (3-dot thinking, collapsible I/O)
- [x] Emoji-free UI — all Material icons
- [x] Splash screen with app icon
- [x] NetworkOnMainThreadException fix in agent tools

### Remaining (5 sessions)

---

## SESSION PROMPTS
> Paste the entire block below when starting a new chat

---

### Session S1 — Agent Loop Hardening
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Always clone fresh with:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current state: App builds and runs. LiteRT-LM engine, 9 models, 17 agent tools all working.

SESSION GOAL — Fix the agent loop in ToolEngine.kt:
1. Robust tool call parser — add regex fallback if JSON arg parsing fails, so malformed model output doesn't silently crash the step
2. Dynamic context window — read the loaded model ID from LLMRepository and set maxContextChars to 28000 for Gemma 4 models (.litertlm), 3000 for others
3. Retry on tool error — if a tool returns isError=true, inject the error back into the prompt and let the agent retry (max 2 retries per tool)
4. FinalAnswer fallback — if the loop hits max iterations without a FinalAnswer tag, emit the last AI text response as the final answer instead of nothing

Do all four. Push when done.
```

---

### Session S2 — Vision / Image Input
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Always clone fresh with:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current state: App builds. Agent loop hardened. 9 models (Gemma 3n E2B/E4B and Gemma 4 E2B/E4B support vision).

SESSION GOAL — Add image input to ChatScreen:
1. Show image attach button (+) in input bar only when a vision-capable model is loaded (supportsVision = true in LLMModel)
2. On tap, open Android image picker (READ_MEDIA_IMAGES permission already granted)
3. Show selected image thumbnail above the input bar before sending
4. Pass image as base64 to LLMRepository.generateStream() — LiteRT-LM Conversation API supports image input via Message with image content parts
5. Display image thumbnail inline in the chat message bubble (user side)
6. Clear image after sending

Do all steps. Push when done.
```

---

### Session S3 — UI Polish (implement HTML preview design)
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Always clone fresh with:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current state: App builds. Vision support added.

SESSION GOAL — Implement the modern UI design into actual Kotlin Compose screens:
Reference design is at: github.com/laisa126/llm (check localllm_v3.html in repo root if present)
Design tokens already in Theme.kt: BgDeep, BgSurface, BgElevated, AccentCyan, AccentPurple, AccentBlue, AccentGreen

Key changes needed:
1. ModelsScreen — glass cards with glow border on loaded model, RAM chip badges, format badge (TASK/LITERTLM)
2. ChatScreen empty state — centered orb avatar with radial glow, suggestion chips in 2x2 grid
3. Input bar — rounded pill shape with gradient send button when text present
4. EditorScreen — tighter file tree with colored extension dots, tab strip with active underline gradient
5. TerminalScreen — window chrome dots (red/yellow/green), monospace body
6. All top bars — consistent height, model pill centered, icon buttons

Do all screens. Push when done.
```

---

### Session S4 — CI/CD + Build Verification
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Always clone fresh with:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current state: App code complete. UI polished.

SESSION GOAL — Fix CI/CD and verify the build:
1. Check .github/workflows/build.yml — verify LiteRT-LM gradle dep resolves (com.google.ai.edge.litertlm:litertlm-android:latest.release needs maven repo declared)
2. Add maven { url = "https://maven.google.com" } to repositories if missing
3. Verify bundled model download step uses correct URL and filename (Gemma3-1B-IT_multi-prefill-seq_q8_ekv1280.task → gemma3-1b-q8.task)
4. Check APK signing config — ensure keystore secrets are wired correctly
5. Check release creation step — APK should be uploadable as a release asset
6. Fix any build errors from the log if CI has failed

Check the latest CI run first. Push fixes when done.
```

---

### Session S5 — Final QA + Play Store Prep
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Always clone fresh with:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current state: App fully built, CI passing, APK releasing.

SESSION GOAL — Final QA and Play Store prep:
1. Static crash audit — scan all Kotlin files for unguarded !! operators, missing null checks, potential ANR (network/IO on main thread)
2. Check AndroidManifest — verify all permissions are declared, no missing service declarations
3. ProGuard — verify all LiteRT-LM classes are kept, no runtime ClassNotFoundExceptions
4. Generate Play Store assets list (what screenshots/descriptions are needed)
5. Update README with final architecture, feature list, and build instructions
6. Tag a v1.0.0 release on GitHub

Do all steps. Push when done.
```

---

## Build Instructions

```bash
# Requirements: Android Studio, JDK 17, Android SDK 35

git clone https://ghp_TOKEN@github.com/laisa126/llm.git
cd llm
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

---

## HF Token
Baked into ModelDownloadService. Token has read-only scope.
All litert-community model licenses must be accepted at huggingface.co.

---

## License
MIT
