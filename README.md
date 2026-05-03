# Local LLM Agent

On-device AI coding assistant for Android. No cloud, no internet after model download.

**Stack:** Kotlin · Jetpack Compose · LiteRT-LM · Ktor · GitHub Actions CI/CD

---

## Clone fresh:
```bash
git clone https://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@github.com/laisa126/llm.git /tmp/repo
```

---

## Completed Sessions

| Session | Commit | What was done |
|---------|--------|---------------|
| Crash fixes | 25ab03f | Theme parent, FileProvider, emit() in channelFlow |
| Engine swap | b620ff0 | MediaPipe → LiteRT-LM, 9-model catalog, HF token baked in |
| UI redesign | a763084 | Cyan/purple theme, glass cards, app icon integrated |
| Agent tools | dc8b385 | download_zip + screenshot tools added |
| Font system | 636ce5e | Sans/serif/mono picker, Claude-style agent steps, no emojis |
| S1 Agent hardening | 217a459 | Robust parser, dynamic 28K ctx, retry, FinalAnswer fallback |
| S2 Vision (partial) | 6f82adb | LLMRepository vision via LiteRT-LM Content.ImageBytes API |

**Current HEAD:** `6f82adb`

---

## Remaining Sessions

### S2 CONTINUE — Wire Vision into UI (START HERE)
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Stack: Kotlin, Jetpack Compose, LiteRT-LM, Ktor, GitHub Actions CI/CD

Clone fresh:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

Current HEAD: 6f82adb

What's already done:
- LLMRepository.generateStreamWithImage(prompt, systemPrompt, imageUri) is implemented
- LLMRepository.supportsVision() returns true/false based on loaded model
- LLMRepository.loadModel() now accepts supportsVision param and sets visionBackend

What still needs doing (this session):
1. MainViewModel.loadModel() — pass supportsVision from the LLMModel object when calling llmRepository.loadModel()
2. MainViewModel.sendMessage(content, imageUri) — call generateStreamWithImage() instead of generateStream() when imageUri is non-null
3. ChatScreen — show the image attach button ONLY when llmRepo.supportsVision() is true (gate it on a supportsVision StateFlow from ViewModel)
4. ChatScreen — show selected image thumbnail above input bar before sending (already has selectedImageUri state, just needs the thumbnail UI)
5. ChatScreen — display image thumbnail inline in user message bubble when msg.imageUri != null (already has the field, verify Coil AsyncImage is wired)
6. After sending, clear selectedImageUri (already done in ViewModel, verify)

Check all 6 points, fix what's missing, push. Do not rebuild what already works.
```

---

### S3 — UI Polish
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Clone fresh:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

SESSION GOAL — Implement modern UI design into Compose screens.
Design tokens in Theme.kt: BgDeep, BgSurface, BgElevated, AccentCyan, AccentPurple, AccentBlue, AccentGreen.

Changes needed:
1. ModelsScreen — glass cards, glow border on loaded model, RAM badges, TASK/LITERTLM format badge
2. ChatScreen — gradient send button when text present, rounded pill input bar
3. EditorScreen — colored extension dots in file tree, tab strip with gradient active underline
4. TerminalScreen — macOS window chrome dots (red/yellow/green) in header
5. All top bars — consistent height 56dp, model pill centered, uniform icon buttons

Push when done.
```

---

### S4 — CI/CD Fix
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Clone fresh:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

SESSION GOAL — Fix CI/CD:
1. Check latest GitHub Actions run — fetch logs from github.com/laisa126/llm/actions
2. LiteRT-LM needs maven { url = "https://maven.google.com" } — verify it's in settings.gradle.kts
3. Verify bundled model download URL and filename in build.yml (gemma3-1b-q8.task)
4. Fix any build errors in the log
5. Verify APK signing and release asset upload steps

Push fixes when done.
```

---

### S5 — Final QA + v1.0.0
```
I'm building LocalLLM — an Android AI coding assistant app.
GitHub repo: github.com/laisa126/llm
GitHub token: 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

Clone fresh:
git clone https://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@github.com/laisa126/llm.git /tmp/repo

SESSION GOAL — Final QA + release:
1. Crash audit — scan all Kotlin for unguarded !! operators and IO on main thread
2. Verify AndroidManifest — all permissions, service declarations present
3. ProGuard — verify LiteRT-LM classes kept, add missing rules if needed
4. Tag v1.0.0 release on GitHub
5. Update README with final build instructions
```

---

## Architecture

```
MainActivity → LocalLLMTheme(fontFamily, fontSize)
├── BootstrapScreen
├── OnboardingScreen
└── AppNavigation (drawer + 7 screens)
    ├── ChatScreen       ← agent mode + vision
    ├── EditorScreen
    ├── TerminalScreen
    ├── PreviewScreen    ← WebViewRegistry for screenshot tool
    ├── ModelsScreen     ← 9 models, litert-community HF
    ├── DeveloperScreen
    └── SettingsScreen   ← font picker, temp, tokens

MainViewModel
├── LLMRepository        ← LiteRT-LM Engine/Conversation + vision
├── ToolEngine           ← 17 tools, robust parser, dynamic ctx
├── ChatHistoryRepository
├── ProjectRepository
├── ModelDownloadService ← ForegroundService, HF token baked in
├── LLMServerService     ← Ktor/Netty OpenAI-compatible API
└── SettingsManager      ← DataStore
```

## Agent Tools (17)
read_file, write_file, create_file, delete_file, list_files, make_dir,
apply_diff, grep_files, get_file_info, run_command, install_package,
search_web, http_get, list_processes, kill_process, download_zip, screenshot

## Models (9, litert-community)
Gemma 3 1B (bundled), Gemma 3n E2B, Gemma 3n E4B,
Gemma 4 E2B, Gemma 4 E4B, Phi-4 Mini,
Llama 3.2 1B, Llama 3.2 3B, Qwen 3.5 2B, Qwen 3.5 4B

## HF Token
Baked into ModelDownloadService. Read-only scope. Accept all model licenses at huggingface.co/litert-community.
