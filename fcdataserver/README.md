# FC26 Career Archive (backend)

Spring Boot + SQLite API for the manager-mode player stats UI, and the Windows installer build.

The UI lives in the sibling `fcplayerdata` folder. **End users should download the installer**, not compile this project.

## Download the installer

Do not commit the `.exe` to Git. Publish it on GitHub Releases:

**[Download the latest installer](https://github.com/<your-account>/<repo>/releases/latest)**

Local build output: `dist/FC26Career-Setup.exe`. How to install and use the app is documented in the frontend README.

## Run in development

1. JDK **17** and Maven (or the included `mvnw`).
2. From `fcplayerdata`, run `npm run build` so the UI is copied into `src/main/resources/static/` (that folder is gitignored).
3. Start this project and open `http://127.0.0.1:11899`.

For UI hot reload, skip the frontend build: `npm run dev` on port 5173 plus this server on 11899.

SQLite is stored at `data/fcdata.db` under the working directory (gitignored).

## Build the installer

Install Node.js, JDK 17+, Maven, and Inno Setup 6. Then from the **frontend** folder:

```bash
npm run package:win
```

Output: `dist/FC26Career-Setup.exe`. Upload it to a Release. Do not commit `dist/`.
