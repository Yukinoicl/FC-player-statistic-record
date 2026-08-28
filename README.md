# FC26 Career Archive

A local app for logging EA Sports FC 26 manager-mode player stats: appearances, goals, assists, titles, and ratings by season. Data stays on your PC. No account required.

```
fcdata/
  fcplayerdata/    Vue UI
  fcdataserver/    Spring Boot + SQLite + Windows installer
```

End users can install the Windows package. You do not need Java, Node, or Git.

## Download the installer (recommended)

Do **not** commit the `.exe` to Git. Upload `FC26Career-Setup.exe` as a [GitHub Release](https://github.com/Yukinoicl/FC-player-statistic-record/releases/latest) asset instead.

**[Download the latest installer](https://github.com/Yukinoicl/FC-player-statistic-record/releases/latest)**

- File: `FC26Career-Setup.exe`
- OS: Windows 10 / 11 64-bit

When you build locally, the installer is at `fcdataserver/dist/FC26Career-Setup.exe`. On GitHub: **Releases → Create a new release → attach that exe**.

---

## For players: using the app

### Install

1. Download `FC26Career-Setup.exe` and run it.
2. Click through the wizard (a desktop shortcut **FC26经理模式档案** is created by default).
3. Administrator rights are not required.

### Open

1. Use the desktop shortcut.
2. After a few seconds the browser opens the page.
3. A football icon appears in the system tray. The app keeps running in the background; **closing the browser does not quit it**.

If you cannot see the tray icon, open the **^** overflow area on the taskbar.

The installed site is usually `http://fc-data-record.localhost` (a port is added if local port 80 is already in use).

### Record stats

1. Click **+ Player**, enter a name, confirm.
2. Click **Add season** and set the player's age for that season.
3. Each season has three rows: **UCL**, **League**, and **Total** (totals are calculated).
4. Fill in appearances, goals, assists, league titles, continental titles, and rating.
5. Click **Save**. Use **Edit** to change a season later.
6. Career totals are shown at the top (UCL / league / all, plus appearance-weighted rating).
7. Drag player tabs to reorder them. Use the top-right switch for Chinese / English.

### Quit and uninstall

- **Quit:** right-click the tray icon → **Exit**. Closing the page alone leaves the backend running.
- **Uninstall:** Start menu item **Uninstall FC26经理模式档案**, or Windows **Settings → Apps**.
- Player data stays under `%LOCALAPPDATA%\FC26Career\data` by default and is not removed with the program.

---

## For developers: running from source

### Daily development

1. Start `fcdataserver` from IDEA, or with Maven. Port **11899**.
2. In `fcplayerdata`:

```bash
npm install
npm run dev
```

Open the URL Vite prints (default `http://localhost:5173`). `/api` is proxied to the backend.

### Serve the production UI from Spring only

From `fcplayerdata`:

```bash
npm run build
```

Static files go to `fcdataserver/src/main/resources/static/` (gitignored). Start the backend and open `http://127.0.0.1:11899`.

SQLite is stored at `fcdataserver/data/fcdata.db` (gitignored).

### Build the Windows installer

You need Node.js, JDK 17+ (`jpackage`), Maven, and [Inno Setup 6](https://jrsoftware.org/isinfo.php).

From `fcplayerdata`:

```bash
npm run package:win
```

Output: `fcdataserver/dist/FC26Career-Setup.exe`. Attach it to a GitHub Release. Do not `git add` the exe.
