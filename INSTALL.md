# Installing LocalLLM

LocalLLM is distributed as a direct APK — no Play Store needed.

## How to install

1. On your Android device, go to **Settings → Apps → Special app access → Install unknown apps**
2. Enable "Allow from this source" for your browser or file manager
3. Download the latest APK from the [Releases](../../releases/latest) page
4. Open the downloaded APK and tap **Install**

## First launch

A one-time setup screen (~30 seconds) extracts the bundled Gemma 3 1B model. After that, the app is fully offline.

## Minimum requirements

| Requirement | Value |
|---|---|
| Android | 10+ (API 29) |
| RAM | 3GB free (Gemma 3 1B) |
| Storage | ~1.2GB after extraction |

## Building from source

```bash
git clone https://github.com/laisa126/llm.git
cd llm
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/*.apk
```

## CI / automated builds

Push a tag starting with `v` to trigger a GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow will build, validate, and publish the APK automatically.
To include the bundled Gemma 3 1B model in the APK, run the **Upload Bundled Model** workflow once first (Actions tab → Upload Bundled Model → Run workflow).
