# Selenium QA Automation Framework

**Author:** Saimadhav Thottala
**LinkedIn:** [linkedin.com/in/saimadhav-thottala](https://www.linkedin.com/in/saimadhav-thottala)
**GitHub:** [github.com/saimadhav22](https://github.com/saimadhav22)

A production-grade Selenium 4 test automation framework built in Java 25, demonstrating three core QA engineering capabilities against a real deep-DOM stress-test page (`the-internet.herokuapp.com/large`):

1. **Locator Strategy Benchmarking** — quantifying the performance tradeoff between brittle XPath and Selenium 4's native Relative Locators.
2. **Hybrid API/UI Data Integrity Validation** — reconciling a 2,500-cell (50×50) grid between the rendered UI and a reference data source.
3. **Self-Healing Element Locator Engine** — automatically recovering from broken locators via sequential fallback resolution, without failing the test.

---

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 25 (records, `var`, text blocks) |
| Build tool | Maven |
| Browser automation | Selenium 4.x (native Relative Locators, Selenium Manager) |
| Test runner | TestNG |
| API testing | RestAssured |
| JSON serialization | Jackson |
| Logging | Log4j2 |
| IDE | IntelliJ IDEA |

No third-party driver-management dependency is used — Selenium 4.6+'s built-in **Selenium Manager** automatically resolves the correct ChromeDriver/GeckoDriver binary at runtime.

---

## Project Structure

```
selenium-qa-framework/
├── pom.xml
├── testng.xml
└── src/
    ├── main/
    │   ├── java/com/qa/framework/
    │   │   ├── config/
    │   │   │   ├── ConfigManager.java      # Thread-safe singleton config loader
    │   │   │   └── DriverFactory.java      # WebDriver lifecycle management
    │   │   ├── pages/
    │   │   │   ├── BrittleLargeDomPage.java    # Absolute XPath locators (control group)
    │   │   │   └── DynamicLargeDomPage.java    # Selenium 4 Relative Locators
    │   │   ├── model/
    │   │   │   ├── TableCell.java          # Record: single grid cell
    │   │   │   ├── TableSnapshot.java      # Record: full grid extraction
    │   │   │   ├── CellMismatch.java       # Record: a single diff result
    │   │   │   └── ElementProfile.java     # Record: self-healing locator metadata
    │   │   ├── util/
    │   │   │   ├── BenchmarkStats.java     # Mean/median/std-dev calculator
    │   │   │   ├── JsonTableExtractor.java # Single executeScript grid extraction
    │   │   │   └── TableDiffEngine.java    # O(n) coordinate-indexed deep diff
    │   │   ├── api/
    │   │   │   └── TableApiClient.java     # RestAssured reference-data client
    │   │   └── healing/
    │   │       ├── SelfHealingElementLocator.java  # Fallback resolution engine
    │   │       └── SmartDriver.java        # WebDriver facade exposing self-healing lookups
    │   └── resources/
    │       ├── config.properties
    │       └── log4j2.xml
    └── test/
        └── java/com/qa/framework/tests/
            ├── LocatorBenchmarkTest.java
            ├── DataIntegrityTest.java
            └── SelfHealingTest.java
```

---

## Setup & Running

### Prerequisites
- JDK 25 installed and set as your IntelliJ Project SDK
- Google Chrome installed (Selenium Manager auto-downloads the matching driver)
- IntelliJ IDEA (Community or Ultimate)

### Run via IntelliJ (no terminal required)
1. Open the project in IntelliJ and let Maven auto-import (or **Maven tool window → reload**).
2. Right-click `testng.xml` at the project root → **Run 'testng.xml'**.
3. Watch results in the TestNG panel, or the Run console for full log output.

### Run via Maven (terminal or IntelliJ's Maven Lifecycle panel)
```
mvn clean test
```

### Configuration
All environment settings live in `src/main/resources/config.properties`:

```properties
browser=chrome
headless=true
base.url=http://the-internet.herokuapp.com/large
implicit.wait.seconds=0
explicit.wait.seconds=10
benchmark.iterations=50
api.base.uri=http://localhost:8089
api.table.endpoint=/mock/large-table
grid.rows=50
grid.columns=50
```

Set `headless=false` if you want to visually watch the browser during a run — useful for debugging, but slower and unsuitable for CI/CD (headless is required on servers with no display attached).

---

## What Each Test Proves

### `LocatorBenchmarkTest`
Runs 50 iterations of the same cell lookup using both locator strategies and reports mean/median/standard-deviation timing. Typical result:

```
| Strategy             |  Samples |   Mean(ms) | Median(ms) | StdDev(ms) |
| Brittle XPath        |       50 |     20.077 |     18.642 |      4.047 |
| Relative Locator     |       50 |    346.301 |    334.052 |     61.909 |
```

**Takeaway:** Relative locators are significantly slower (~17x in this run) because they compute geometric bounding-box proximity across candidate elements client-side, versus a direct tree-traversal XPath lookup. This is a genuine tradeoff — resilience to DOM changes costs latency, it isn't a free upgrade.

### `DataIntegrityTest`
Extracts the full 2,500-cell grid via a **single** `executeScript` call (collapsing what would otherwise be 2,500 individual Selenium wire commands into one), fetches reference data via RestAssured (or a deterministic synthesized fallback if no reference API is reachable), and performs an O(n) hash-indexed deep diff. Reports exact `(row, col)` mismatches if any are found.

### `SelfHealingTest`
Deliberately breaks the primary locator for a target element and proves the framework recovers via sequential fallback evaluation — without ever failing the test. Every successful heal is logged with full detail on which fallback resolved it, giving the team an audit trail of locator drift to investigate.

---

## Key Design Decisions

- **Selenium Manager over third-party driver management** — fewer dependencies, less version-drift risk.
- **`SmartDriver` uses composition, not inheritance** — wraps `WebDriver` rather than re-implementing its full interface, in line with the Interface Segregation Principle.
- **`ConfigManager` is a thread-safe singleton** with defensive `.trim()`ing on every value, so stray whitespace in `config.properties` never breaks numeric/boolean parsing.
- **`TableDiffEngine` uses O(n) coordinate-indexed hashing**, not O(n²) nested-loop comparison, to stay efficient at scale.
- **RestAssured gracefully degrades** to a synthesized reference dataset when no live reference API is reachable, keeping the demo runnable end-to-end without external infrastructure.

---

## Known Limitations / Next Steps

- `TableApiClient` currently has no real backend to call in this demo context; wiring it to a real service or a WireMock stub would exercise the true API path instead of the synthesized fallback.
- Tests run sequentially; enabling TestNG's `parallel` and `thread-count` attributes in `testng.xml` would speed up the full suite.
- `ElementProfile`'s self-healing metadata (`dynamicIdPattern`, structural anchors) is defined but not yet exercised by a dedicated test — a good candidate for future coverage.