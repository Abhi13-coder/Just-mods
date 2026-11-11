<p align="center">
  <img src="logo.svg" alt="AI Edge Gallery ARM32 logo" width="280">
</p>

<h1 align="center">AI Edge Gallery · ARM32 Edition</h1>

<p align="center">
  Run on-device AI models on old 32-bit Android phones.<br>
  Built and maintained by <b>Abhi</b>, entirely from a phone.
</p>

<p align="center">
  <a href="https://github.com/Abhi13-coder">GitHub</a> ·
  <a href="https://buymeacoffee.com/YOUR_NAME">Buy me a coffee</a>
</p>

---

## What is this?

Google's **AI Edge Gallery** app does not ship for 32-bit (ARM32) phones running Android 10. This project is an unofficial community build that makes it work on them. It is compiled automatically by a GitHub Actions workflow, so nobody needs a PC to build or update it.

I made and tested it on a Redmi 9A (2 GB RAM, 32-bit Android 10).

## Features

- Runs on ARM32 / Android 10 devices
- Fully offline chat with small on-device models (MediaPipe `.task` files)
- Extra tools panel inside chat
- Built-in mod loader support
- About / Support screen with links to my GitHub and Buy Me a Coffee
- Free and open, no ads, no tracking added by this build

> Edit this list to match exactly what your build includes.

## Install

1. Open the **Releases** page of this repo.
2. Download the latest `.apk`.
3. Allow "Install unknown apps" for your browser or file manager when Android asks.
4. Open the app and load a model.

## Model tips for low-RAM phones

- Use small models (around 0.5B parameters, for example Qwen2.5-0.5B).
- Keep the context length modest (about 1280 works well on 2 GB RAM).
- Close other apps first. The first few messages are slower, then it speeds up.

## Build it yourself

1. Fork this repository.
2. Go to **Actions**, pick the ARM32 workflow, and press **Run workflow**.
3. When it finishes, download the APK from the run's **Artifacts**.

No laptop needed. Everything runs on GitHub's servers.

## Support the project

If this app is useful to you, you can support me here:

<p align="center">
  <a href="https://buymeacoffee.com/YOUR_NAME"><b>☕ Buy me a coffee</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Abhi13-coder"><b>⭐ Star &amp; follow on GitHub</b></a>
</p>

Bug reports and ideas are welcome in **Issues**.

## Credits and license

- Based on Google's open-source [AI Edge Gallery](https://github.com/google-ai-edge/gallery), licensed under Apache 2.0.
- ARM32 build, patches, tools and branding by **Abhi**.
- This is an unofficial project and is not affiliated with or endorsed by Google.

<p align="center"><sub>Made with a phone, a pencil-sketch logo and a lot of patience.</sub></p>
