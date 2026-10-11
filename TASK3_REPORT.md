Task 3: Earnings engine (Sections 3 and 4)

What changed
- domain/EarningsResult.kt: data class with strokes, gross, tax, net, outstanding (BigDecimal)
- domain/EarningsCalculator.kt: pure Kotlin, BigDecimal only, HALF_UP, scale 2. Formulas: gross = strokes * RATE_PER_STROKE; tax = gross*TAX_RATE rounded HALF_UP scale 2 (0 if gross<=0); net = gross-tax (scale 2); outstanding = net-payments (scale 2, never clamped). Signature calculate(strokes: Long, payments: BigDecimal)
- util/MoneyFormat.kt: formats BigDecimal as "रु X,XXX.XX" (US grouping, 2 decimals)
- ui/viewmodel/HomeViewModel.kt: computes earnings for selected date using cumulative strokes up to selected date (payments=0 for now) via EarningsCalculator; exposes in uiState
- ui/screens/HomeScreen.kt: Section 4 shows real receivable (net) formatted with MoneyFormat
- ui/screens/EarningsScreen.kt: shows selected date and breakdown (4.1 Total Earning, 4.2 Tax 1%, 4.3 Receivable Income)
- app/src/test/java/com/sandesh/thecount/domain/EarningsCalculatorTest.kt: 9 unit tests as specified (0,1,100000 with/without payments, 12,13, 9B, multiple payments, payments>net)

Tests (real results)
- testDebugUnitTest (clean): BUILD SUCCESSFUL
- ValidatorsTest: 9 tests, 0 failures, 0 errors
- EarningsCalculatorTest: 9 tests, 0 failures, 0 errors  
- ExampleUnitTest: 1 test, 0 failures, 0 errors
- Total: 19 tests, all passed

Notes
- Section 3 (cumulative strokes) already shows real data via HomeViewModel; digit grouping via NumberFormat. Payments remain sample until Task 4 (payments passed as ZERO). No money computed in UI.

Files changed
- domain/EarningsResult.kt (new)
- domain/EarningsCalculator.kt (new)
- util/MoneyFormat.kt (new)
- ui/viewmodel/HomeViewModel.kt (modified)
- ui/screens/HomeScreen.kt (modified)
- ui/screens/EarningsScreen.kt (modified)
- app/src/test/java/com/sandesh/thecount/domain/EarningsCalculatorTest.kt (new)
- docs/STATE.md (modified)

STOP - Task 3 done. Await approval for Task 4.
