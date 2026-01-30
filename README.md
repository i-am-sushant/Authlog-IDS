# Auth Log IDS (Intrusion Detection System)

A **rule-based Intrusion Detection System** built in pure Java that analyzes Linux authentication logs (`/var/log/auth.log`) to detect suspicious login patterns and potential security threats. Designed for clarity, auditability, and extensibility.

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Detection Rules](#detection-rules)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Installation & Usage](#installation--usage)
- [Alert Format](#alert-format)
- [Sample Output](#sample-output)
- [Technical Deep Dive](#technical-deep-dive)
- [Limitations & Assumptions](#limitations--assumptions)
- [Future Enhancements](#future-enhancements)

---

## Features

- **Pure Java Implementation** - No external dependencies; JDK 8+ only
- **Rule-Based Detection** - Explainable, auditable security rules
- **Sliding Window Algorithm** - Efficient time-based pattern detection
- **Three Detection Rules**:
  - IP-based brute force detection
  - User-based distributed attack detection
  - Suspicious fail-to-success pattern detection
- **Machine-Parseable Output** - Structured alerts for log aggregators
- **Debug Mode** - Optional verbose parsing diagnostics

---

## Architecture

The system follows **clean architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      CLI Layer (App.java)                    │
│         Command-line parsing & orchestration                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Parser Layer (AuthLogParser.java)            │
│      Regex-based log parsing → LoginEvent domain objects     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               Detection Layer (DetectionEngine.java)         │
│     Sliding window tracking & threshold-based detection      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Alert Layer (AlertService.java)              │
│          Structured alert formatting & output                │
└─────────────────────────────────────────────────────────────┘
```

---

## Detection Rules

| Rule | Threshold | Time Window | Threat Detected |
|------|-----------|-------------|-----------------|
| **IP Brute Force** | 5 failures | 5 minutes | Automated password attacks from single source |
| **User Brute Force** | 10 failures | 10 minutes | Distributed botnet attacks targeting one account |
| **Fail-to-Success** | 3 failures → 1 success | 2 minutes | Successful password guessing after multiple attempts |

### Why These Thresholds?

- **IP Brute Force (5 in 5min)**: Catches automated tools like Hydra/Medusa that attempt rapid logins
- **User Brute Force (10 in 10min)**: Higher threshold accounts for legitimate typos; longer window catches distributed attacks
- **Fail-to-Success (3 in 2min)**: Normal users rarely fail 3+ times then succeed—this pattern suggests credential compromise

---

## Project Structure

```
src/main/java/ids/
├── cli/
│   └── App.java              # Entry point, CLI argument handling
├── parser/
│   └── AuthLogParser.java    # Regex-based log line parsing
├── detect/
│   └── DetectionEngine.java  # Sliding window detection logic
├── alert/
│   └── AlertService.java     # Alert formatting and output
├── config/
│   └── DetectionConfig.java  # Centralized threshold configuration
└── model/
    ├── LoginEvent.java       # Immutable domain object
    └── EventType.java        # FAIL/SUCCESS enum
```

---

## How It Works

### 1. Log Parsing

The parser uses regex to extract structured data from syslog-formatted SSH log lines:

```
Jan  3 10:00:01 server1 sshd[1001]: Failed password for admin from 203.0.113.10 port 55001
      │              │                    │                  │            │
      └─ Timestamp ──┘                    └── Outcome ───────┴── User ────┴── IP Address
```

### 2. Sliding Window Tracking

For each event, the engine maintains time-ordered deques (double-ended queues):

```java
Map<String, Deque<Instant>> ipFailures      // Track failures by source IP
Map<String, Deque<Instant>> userFailures    // Track failures by username
Map<String, Deque<Instant>> failSuccessByUser // Track pre-success failures
```

**Algorithm:**
1. Add new timestamp to the appropriate deque
2. Remove timestamps older than the window cutoff (sliding window trim)
3. Check if count meets threshold → trigger alert

### 3. Alert Generation

When thresholds are met, structured alerts are printed to stdout:

```
RULE=IP_BRUTE_FORCE time=2024-01-03T10:01:30Z ip=203.0.113.10 count=5 window=5m
```

---

## Installation & Usage

### Prerequisites

- Java Development Kit (JDK) 8 or higher

### Build

**Linux/macOS:**
```bash
javac -d out $(find src -name "*.java")
```

**Windows PowerShell:**
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### Run

```bash
# Analyze a log file
java -cp out ids.cli.App sample_auth.log

# Enable debug mode (shows skipped lines)
java -cp out ids.cli.App --debug-parse sample_auth.log
```

---

## Alert Format

Alerts use a structured, machine-parseable format:

```
RULE=<rule_name> time=<iso_timestamp> <context_fields> count=<n> window=<duration>
```

### Alert Types

| Rule | Format |
|------|--------|
| IP Brute Force | `RULE=IP_BRUTE_FORCE time=<ts> ip=<ip> count=<n> window=5m` |
| User Brute Force | `RULE=USER_BRUTE_FORCE time=<ts> user=<user> count=<n> window=10m` |
| Fail-to-Success | `RULE=FAIL_TO_SUCCESS time=<ts> user=<user> ip=<ip> count=<n> window=2m` |

---

## Sample Output

Running against `sample_auth.log`:

```
RULE=IP_BRUTE_FORCE time=2026-01-03T04:31:30Z ip=203.0.113.10 count=5 window=5m
RULE=USER_BRUTE_FORCE time=2026-01-03T04:43:35Z user=deploy count=10 window=10m
RULE=FAIL_TO_SUCCESS time=2026-01-03T05:36:20Z user=alice ip=198.51.100.45 count=3 window=2m
```

---

## Technical Deep Dive

### Data Structures

**Why `ArrayDeque`?**
- O(1) add/remove from both ends
- Memory-efficient (array-backed, no node overhead)
- Perfect for sliding windows: add at tail, remove stale entries from head

**Why Immutable `LoginEvent`?**
- Thread-safe without synchronization
- Prevents accidental state mutation
- Enables confident reasoning about data flow

### Algorithm Complexity

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Parse line | O(n) where n = line length | O(1) |
| Add event | O(1) amortized | O(k) where k = events in window |
| Trim window | O(m) where m = expired events | O(1) |

### Regex Pattern Explained

```regex
^(\w{3})\s+(\d{1,2})\s+(\d{2}:\d{2}:\d{2})\s+[^ ]+\s+sshd\[[^]]+\]:\s+(Failed|Accepted) password for (?:invalid user )?(\S+) from ([0-9]{1,3}(?:\.[0-9]{1,3}){3}).*$
```

| Group | Pattern | Captures |
|-------|---------|----------|
| 1 | `(\w{3})` | Month abbreviation |
| 2 | `(\d{1,2})` | Day of month |
| 3 | `(\d{2}:\d{2}:\d{2})` | Time (HH:MM:SS) |
| 4 | `(Failed\|Accepted)` | Login outcome |
| 5 | `(\S+)` | Username |
| 6 | `([0-9]{1,3}(?:\.[0-9]{1,3}){3})` | IPv4 address |

---

## Limitations & Assumptions

| Limitation | Description | Mitigation |
|------------|-------------|------------|
| **Year assumption** | Syslog lacks year; assumes current year | May misplace events at year boundaries |
| **Timezone** | Parses in system timezone, outputs UTC | Consistent output but input assumptions |
| **In-memory state** | No persistence; state lost on restart | Suitable for batch analysis only |
| **SSH only** | Regex targets OpenSSH `sshd` format | Other services need additional patterns |
| **IPv4 only** | Regex captures IPv4 addresses only | IPv6 support requires pattern extension |

---

## Future Enhancements

- [ ] **Real-time streaming** - `tail -f` style continuous monitoring
- [ ] **Persistent storage** - Redis/SQLite for sliding windows
- [ ] **Configurable thresholds** - YAML/properties file support
- [ ] **IPv6 support** - Extended regex patterns
- [ ] **Additional services** - Patterns for sudo, PAM, other daemons
- [ ] **Alert integrations** - Webhooks, PagerDuty, Slack notifications
- [ ] **GeoIP enrichment** - Add location context to IP addresses

---

## License

MIT License - See [LICENSE](LICENSE) for details.
