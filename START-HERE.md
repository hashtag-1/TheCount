# START HERE: TheCount

## What this folder is
Instructions for building TheCount (Android, Kotlin, Compose, Room, Supabase) one task at a time in Google Antigravity.

```
TheCount-kit/
├── START-HERE.md          this file
├── AGENTS.md              rules the agent reads every session (keep it short)
├── .gitignore             keeps secrets and build files out of git
├── .agents/workflows/     /next-task and /verify
├── docs/
│   ├── prompts.md         the task prompts (paste one at a time)
│   ├── STATE.md           progress log, updated by the agent
│   ├── SPEC.md            your original requirements, unchanged
│   ├── ARCHITECTURE.md    how all the parts connect
│   ├── DECISIONS.md       choices and assumptions. READ THIS, edit before the task that uses it
│   ├── SETUP-MANUAL.md    Supabase, Google Cloud, signing: what only you can do
│   └── ui/                LAYOUT.md (+ put your sketches here)
└── supabase/              SQL migrations and tests (written later)
```

## Setup (about 15 minutes)

1. **Unzip** so that `AGENTS.md` is directly inside the folder you open (not one folder deeper).
2. **Read docs/DECISIONS.md.** Change anything you disagree with. Set the package name (D1) now.
3. **Create the base Android project** (the agent must not hand-write Gradle files):
   1. In Android Studio: New Project, **Empty Activity** (Compose), Name `TheCount`, package from D1, Language Kotlin, Minimum SDK 26, Build configuration Kotlin DSL. Save it in a TEMPORARY folder.
   2. Let Gradle sync finish, and press Run once to prove it builds.
   3. Copy everything from that temporary folder INTO this kit folder (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`). If asked to overwrite `.gitignore`, keep this kit's version.
4. **Add your sketches** (if you have them) to `docs/ui/`.
5. **Open the folder in Antigravity** (File, Open Folder).
6. **Set a fast model** in OmniRoute (thinking off or low) for Tasks 1 to 4. Use a stronger model for the security and sync tasks (see the table in docs/prompts.md).
7. `git init`, then `git add .` and `git commit -m "kit"`. Commit after every approved task.

## START COMMAND (paste for every task)

Read AGENTS.md, then docs/STATE.md, then docs/prompts.md. Find the first task in docs/prompts.md that is not marked done in docs/STATE.md and do ONLY that task. Follow every rule in AGENTS.md. Write each file in its own write call with filePath and full content. If a step takes more than about 10 minutes or fails 3 times, stop and ask me. Do not research or scrape outside the task scope. Build and test what can really be tested, update docs/STATE.md, report the result to me, and STOP. Do not start the next task until I approve.

(The workflow `/next-task` does the same if your Antigravity shows it as a slash command.)

## After each task
1. Do the **You check** list under that task in docs/prompts.md on your phone.
2. If it passes, tell the agent "Approved, mark Task N done", then commit.
3. If it fails, tell the agent exactly what you saw. Use the recovery prompts at the bottom of docs/prompts.md.

## Rules of thumb (learned the hard way)
- The agent cannot see your screen or reach your Supabase project. YOU run the app and the SQL, then report real results.
- Never paste the service-role key, database password or OAuth client secret anywhere.
- Before Task 4 and Task 6B, write down your stroke and payment totals so you can compare after.
- Security tasks (5A to 6B) are only "done" after YOU run the tests in Task 5C and report the results.
- If the agent's write tool fails repeatedly, split the work into smaller files, or bring the file to me to write.
