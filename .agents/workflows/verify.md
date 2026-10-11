---
description: Quick health check of the Android project
---

1. Run .\gradlew.bat assembleDebug and report errors and warnings.
2. Run .\gradlew.bat testDebugUnitTest and report pass/fail counts.
3. Search src for Double or Float used with money (rate, tax, gross, net, amount, balance) and report any.
4. Search the repo for anything that looks like a secret (service_role, client_secret, private keys). Report matches.
5. Report pass/fail per item. Fix only what failed, then re-check.
