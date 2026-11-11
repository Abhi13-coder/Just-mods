<p align="center">
  <img src="logo.svg" alt="AI Edge Gallery ARM32 logo" width="280">
</p>

<h1 align="center">AI Edge Gallery · ARM32 Edition</h1>

<p align="center">
  Google's AI Edge Gallery rebuilt for <b>32-bit Android 10</b> phones,<br>
  with a Linux terminal, tools for the model and a native mod loader.<br>
  Built and maintained by <b>Abhi</b>, entirely from a phone.
</p>

<p align="center">
  <a href="https://github.com/Abhi13-coder">GitHub</a> ·
  <a href="https://buymeacoffee.com/abhi13.coder">Buy me a coffee</a>
</p>

---

## What is this?

The official AI Edge Gallery app does not run on 32-bit (armeabi-v7a) phones with Android 10. This project builds it for them with a GitHub Actions workflow (`Gallery Android10 ARM32`), so no PC is needed.

What the workflow does:

- Patches Gallery to `minSdk 29` / `targetSdk 29` and `armeabi-v7a` only
- Compiles the LiteRT-LM native library (`liblitertlm_jni.so`) for ARM32 and saves it in this repo, so later builds skip the long compile
- Repairs the library so it does not need Android 11+ libc symbols, and adds a small unwinder shim
- Patches Android 12+ API calls that would crash on Android 10
- Checks the finished APK before uploading it (libraries, dependencies and symbols)

## What is added on top of Gallery

**Terminal**
- Built-in Linux terminal ("Gallery Terminal") running Alpine Linux (armv7) through proot
- Sandbox is capped at 1 GB; model downloads and imports are not affected

**Native mod loader**
- Loads 32-bit ARM `.so` files from the app's `mods` folder at app start
- Skips files that are not 32-bit ARM, and writes `mod_loader.log` in the same folder

**Tools for the model** (all off by default, switched in the chat bar)
- Web and Tools buttons next to Skills and MCP, in AI Chat and Agent Skills
- Web search (DuckDuckGo, SearXNG, Marginalia, YaCy, Wikipedia, Bing as last resort) and page reading
- Built-in Agent Browser: open, read, list elements, click, type, scroll, run JavaScript
- Terminal commands and workspace files: list, read, write, copy
- HTML preview window
- Background jobs (schedule, list, cancel) and notifications
- Memory read/write tools
- Specialist models through "Orchestra" bundles (vision, speech to text, text to speech, classify, detect, segment, embed)

**Agent Center and safety**
- Activity log of everything the AI did, with copy, share, save and clear
- STOP button: stops the agent service, disables all jobs, kills sandbox processes and locks every tool
- Optional 24/7 agent service for background jobs

**Other changes**
- Models without constrained decoding (for example LFM2) no longer fail to start; the app retries without it
- Max tokens limit raised from 4096 to 32768, and imported models get an editable slider
- Crash-log recorder: after a crash, the log is saved and copied to the clipboard on next launch
- About / Support screen with links to my GitHub and Buy Me a Coffee

> Small models work best with few or no tools turned on.
## GET THE APP

## Releases

**Download the latest APK:** [Latest ARM32 Release](https://github.com/Abhi13-coder/Just-mods/releases/latest)

## Build it yourself

1. Fork the repo, open **Actions**, choose **Gallery Android10 ARM32** and press **Run workflow**.
2. Optional inputs:
   - `rebuild_so`: force a fresh LiteRT-LM compile even if a saved `.so` exists
   - `litert_lm_ref`: LiteRT-LM branch, tag or commit for the JNI library and Kotlin API
3. The first compile is slow. After that the saved `.so` is reused. The APK and build logs are uploaded as artifacts.

## Support the project

If this app is useful to you:

<p align="center">
  <a href="https://buymeacoffee.com/abhi13.coder"><b>☕ Buy me a coffee</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Abhi13-coder"><b>⭐ Star &amp; follow on GitHub</b></a>
</p>

Bug reports and ideas are welcome in **Issues**.

## Credits and license

- Based on Google's open-source [AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0) and [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM).
- ARM32 build, patches, tools and branding by **Abhi**.
- Unofficial project, not affiliated with or endorsed by Google.
