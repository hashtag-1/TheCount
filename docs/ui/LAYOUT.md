# Screen layout (text version of the sketches)

If image files exist in docs/ui/ (sketches), they WIN over this text. Look at them first.

## Home (single scrolling column, mobile first)
1. Top bar: "TheCount" title, settings icon.
2. Status row: today's date, account chip (email or "Not signed in"), sync chip (Synced / N pending / Error / Offline).
3. Six compact cards, numbered, in this order. The number is shown in a small badge on each card.
   - 1 "Enter today's stroke count": today's date, numeric field, Save/Update button, "Saved today: N" line.
   - 2 "Daily stroke count": selected date, its saved count (or "No entry"), previous/next date arrows, tap opens the calendar screen.
   - 3 "Cumulative total strokes": big grouped number (100,000 style) with "through <selected date>".
   - 4 "Total earning": prominent receivable amount, small line "Gross / Tax". Tap opens the Earnings breakdown.
   - 5 "Payment received": total received, "+ Add payment" button, tap opens payment history.
   - 6 "Payment yet to be received": outstanding amount. Positive = owed. Zero = "Fully settled". Negative = "Overpaid / credit" in a clearly different style.

## Other screens
- Login: app name, "Sign in with Google" button, error text area.
- Daily entry/edit: date, numeric field, save, delete (with confirmation).
- Calendar / history: month view or date list from 2026-09-02 up to today. Dates with an entry show the count. Dates without an entry look clearly different (no zero is implied).
- Earnings breakdown: selected date, then exactly: 4.1 Total Earning, 4.2 Tax = 1%, 4.3 Receivable Income. Read-only.
- Payments: list newest first, each row amount/date/note, edit and delete (confirm). "Add payment" dialog or screen: amount, date received, optional note, Save, Cancel.
- Settings: account info, sync status and "Sync now", export/import backup (Task 8), sign out.
- Access denied: explains that the account is not invited and how to request access.

## Style
Material 3, restrained neutral palette, light and dark follow the system. Money is the most prominent text. Use "रु" everywhere. No charts, no animations, no decorative widgets. Every screen has loading, empty, error and offline states. Confirm destructive actions.
