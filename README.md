# Mess Menu

Offline Android app that shows today's mess menu, imported from a `.xlsx` file on your phone.

- Import `.xlsx` files via the system file picker
- Persists the last imported file (survives app restarts)
- Automatically shows the current day's menu, and re-reads it when the date changes
- Separate Breakfast / Lunch / Snacks / Dinner boxes
- Handles ordinal dates such as 17th / 31st
- Keeps bracketed menu text as-is
- Today's date is shown as `dd-MONTH-yyyy (day)`

## What changed from the previous reconstruction, and why

The parsing rules, UI, and flow are all the same as what was locked before. One real change:
**Apache POI has been removed** and replaced with a small built-in `.xlsx` reader
(`XlsxReader` in `ExcelParser.kt`).

Reason: `org.apache.poi:poi-ooxml` depends on `javax.xml.stream` (StAX), which Android does not
ship. It compiles without any error, but on a real device it throws
`NoClassDefFoundError` the moment you try to open a file — a long-documented issue
(see `github.com/centic9/poi-on-android`). Since `.xlsx` is just a zip of small XML files, the
replacement reader uses only `java.util.zip` and Android's built-in XML pull parser
(`android.util.Xml`), both of which ship with every Android version. Same output shape
(rows of cell text), same column mapping (B/C/D/E → Breakfast/Lunch/Snacks/Dinner), same
today-detection rules — just a reliable way of getting the text out of the file.

If your real spreadsheet's layout turns out to differ from what's assumed here (which
columns hold which meal, how the day header is written), send me the actual `.xlsx` and
I'll match the parser to it exactly rather than guessing.

## Building the APK

You have three options — pick whichever is easiest for you.

### Option A — Android Studio (recommended, easiest)
1. Open this folder in Android Studio.
2. Let it sync (first sync downloads dependencies — needs internet).
3. Run ▶ on a device/emulator, or **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
4. The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Option B — Command line
Requires a JDK (17+) on your machine; the Gradle wrapper included here (`gradlew`) downloads
the correct Gradle version itself on first run.

```
./gradlew assembleDebug
```
(Windows: `gradlew.bat assembleDebug`)

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Option C — GitHub Actions (no local setup at all)
This repo includes `.github/workflows/build-apk.yml`, which builds a debug APK automatically
on every push.
1. Push this project to a GitHub repo (this folder = repo root).
2. Open the **Actions** tab → the latest run → **Artifacts** → download `MessMenu-debug-apk`.
3. Unzip it — that's your installable `app-debug.apk`.

## Installing on your phone
The debug APK is unsigned for release but installable as-is. Transfer it to your phone and
open it (you'll need to allow "install unknown apps" for whichever app you used to transfer
it — Files, WhatsApp, Drive, etc.).

## Project layout
```
MessMenu/
├── app/src/main/java/com/example/messmenuapp/
│   ├── MainActivity.kt
│   └── ui/
│       ├── DashboardScreen.kt
│       ├── DashboardViewModel.kt
│       └── ExcelParser.kt
├── app/src/main/AndroidManifest.xml
├── app/src/main/res/
└── .github/workflows/build-apk.yml
```
