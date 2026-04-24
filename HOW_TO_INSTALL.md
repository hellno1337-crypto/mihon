# How to get the APK on your phone — no Android Studio needed

## Step 1 — Push this folder to GitHub

1. Go to https://github.com/new and create a **private** repo (e.g. `nhentai-mihon-ext`)
2. Open a terminal in this folder and run:
   ```bash
   git init
   git add .
   git commit -m "initial"
   git remote add origin https://github.com/YOUR_USERNAME/nhentai-mihon-ext.git
   git push -u origin main
   ```

## Step 2 — GitHub Actions builds the APK for you

- As soon as you push, GitHub Actions starts automatically.
- Go to your repo → **Actions** tab → watch the build run (~3-5 minutes).
- When it finishes, a **Release** is created at:
  `https://github.com/YOUR_USERNAME/nhentai-mihon-ext/releases/latest`

## Step 3 — Download & install on your phone

1. Open that Releases page **on your phone's browser**.
2. Tap the `.apk` file to download it.
3. Open the APK from your Downloads — Android will ask to "Install unknown app".
   - If blocked: Settings → Apps → your browser → Install unknown apps → Allow.
4. Tap **Install**.

## Step 4 — Enable in Mihon

1. Open Mihon → **Browse** (bottom nav).
2. Tap the **Extensions** tab.
3. You'll see **NHentai** listed — tap **Trust** then the extension loads.
4. ⚠️ If it doesn't appear: Mihon → Settings → Browse → turn on **Show NSFW sources**.

## Updating the extension

After any code change, just `git push` again. GitHub Actions rebuilds and creates a new release automatically. Mihon will show an update badge on the extension.

---

## What the extension supports

| Feature | Details |
|---|---|
| Browse Popular | Sorted by all-time popular |
| Browse Latest | Newest uploads |
| Search | Text + tag/artist/character/parody/group filters |
| Language filter | All / English / Japanese / Chinese |
| Sort filter | Recent, Popular Today, Popular Week, All Time |
| Manga detail | Title, artist, tags, characters, parody, page count |
| Chapter list | Always one chapter per gallery (it's a doujinshi) |
| Pages | Full image list from nhentai CDN |
| Cloudflare | Handled automatically by Mihon's built-in WebView bypass |
