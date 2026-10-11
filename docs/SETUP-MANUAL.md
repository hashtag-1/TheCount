# Manual setup (things only YOU can do)

Dashboard screens and button names change. Treat the steps as a guide and check the current official docs when something looks different. Never paste secrets into the chat or the repo.

## 1. Tools
- Android Studio (current stable). Its embedded JDK is enough (Gradle JDK = "Embedded JDK" or "jbr").
- A phone with USB debugging (Xiaomi: also enable "Install via USB" and "USB debugging (Security settings)" in Developer options), or an emulator.
- Windows PowerShell: commands use `.\gradlew.bat`.

## 2. Base Android project (before Task 1)
See START-HERE.md step 3.

## 3. local.properties (never committed)
Create or edit `local.properties` in the project root (Android Studio already creates it with `sdk.dir`). Add:
```
SUPABASE_URL=https://YOUR-PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR-PUBLISHABLE-OR-ANON-KEY
GOOGLE_WEB_CLIENT_ID=YOUR-WEB-CLIENT-ID.apps.googleusercontent.com
```
Only the public URL, the anon/publishable key and the Google WEB client id go here. NEVER the service-role key, never the OAuth client secret.

## 4. Supabase project (before Task 5)
1. supabase.com, New project. Pick a region near you and a strong database password (store it in a password manager).
2. Project Settings, API: copy the Project URL and the anon/publishable key into local.properties.
3. SQL Editor: run the files from `supabase/migrations/` in numeric order after the agent writes them (5A, then 5B, then 006 in 7A). Run each file once.
4. Table Editor: confirm RLS is enabled on every table.
5. Free-plan limits (backups, pausing after inactivity) change; Task 8 documents the current ones.

## 5. Inviting and revoking users (after Task 5B)
The agent writes the exact SQL into `supabase/README.md`. You run it in the SQL editor: invite an email, list invitations, revoke, change max_users, count users. There is no admin screen in the app (docs/DECISIONS.md D7).

## 6. Google Cloud and Google sign-in (before Task 6A)
1. console.cloud.google.com: create a project.
2. OAuth consent screen: app name TheCount, your support email, scopes email and profile (openid).
3. Important: while the consent screen is in "Testing", only listed test users can sign in and there is a cap on test users. With up to 120 users you will need to set the publishing status to "In production". Check Google's current rules on user caps and verification for basic scopes.
4. Credentials, Create OAuth client ID, type **Web application**. Copy its Client ID into local.properties (GOOGLE_WEB_CLIENT_ID). Keep its Client Secret ONLY for the next step.
5. Credentials, Create OAuth client ID, type **Android**: package name = your app id (docs/DECISIONS.md D1) and the SHA-1 fingerprint. Get the debug SHA-1 with `.\gradlew.bat signingReport` (look for the debug variant). You need a SECOND Android client later for the release keystore SHA-1.
6. Supabase dashboard, Authentication, Providers, Google: enable it, paste the Web client ID and Web client secret. Follow Supabase's current instructions for native ID-token sign-in (nonce handling) when Task 6A starts.
7. Authentication, URL configuration: add what Supabase's docs ask for.

## 7. Testing with real accounts
You need at least: one invited Google account, one NOT invited, and a second invited account (to prove isolation). Use `.\gradlew.bat installDebug` or Run from Android Studio.

## 8. Release signing (before Task 9)
Create the keystore once and BACK IT UP (lose it and you cannot update the app):
```
keytool -genkey -v -keystore thecount-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias thecount
```
Keep the file outside the repo (or inside, since *.jks is gitignored, but also copy it somewhere safe). Create `keystore.properties` (gitignored):
```
storeFile=thecount-release.jks
storePassword=...
keyAlias=thecount
keyPassword=...
```
Get the release SHA-1 with `keytool -list -v -keystore thecount-release.jks -alias thecount` and register it as another Android OAuth client in Google Cloud (step 6.5). Without that, Google sign-in fails in the release APK.

## 9. Installing the release APK
Copy `app/build/outputs/apk/release/app-release.apk` to the phone and open it, allowing "install from unknown sources" when asked.
