# Auth Log IDS (Java CLI)

A small, auditable, rule-based intrusion detection helper that scans Linux authentication logs (`/var/log/auth.log` style) from a file and emits human-readable alerts to stdout. It favors clarity, explainable rules, and simple in-memory state.

## How it works
- Parses auth log lines via regex to extract timestamp, username, IP, and outcome (success/fail).
- Converts syslog timestamps using the **current year** and the **system timezone**, then renders alerts in ISO-8601 UTC (Instant) for consistency.
- Maintains in-memory `Deque` windows per IP and per user to count recent failures and detect suspicious patterns.
- Applies rule-based thresholds and prints one-line alerts to stdout. Parsing failures are skipped silently unless `--debug-parse` is enabled.

## Detection rules and defaults
- **IP brute force**: 5 failed logins from the same IP within 5 minutes.
- **User brute force**: 10 failed logins for the same user within 10 minutes.
- **Fail → success pattern**: ≥3 failures followed by a success (same user) within 2 minutes.

Alert format (one line, structured plain text):
- `RULE=IP_BRUTE_FORCE time=<iso_in_utc> ip=<ip> count=<n> window=5m`
- `RULE=USER_BRUTE_FORCE time=<iso_in_utc> user=<user> count=<n> window=10m`
- `RULE=FAIL_TO_SUCCESS time=<iso_in_utc> user=<user> ip=<ip> count=<n> window=2m`

## Assumptions and limitations
- **Timestamps**: Syslog entries lack year; we assume the **current year** and **system timezone** when parsing. Around New Year boundaries, this can misplace events by a year if logs span years.
- **Timezone**: Parsing uses the system zone; alerts render in ISO-8601 UTC.
- **State**: All tracking is in-memory only (no persistence). Suitable for batch/offline analysis; extension to streaming would need external state.
- **Parsing scope**: Regex targets common OpenSSH `sshd` lines. Other formats may be skipped.
- **Warnings**: Parse failures are skipped silently by default. When `--debug-parse` is provided, warnings emit to stderr as `WARN parse-skip: <reason> line="<original line>"`.

## Usage
1. Build and run (JDK 8+):
   ```sh
   # From project root
   javac -d out $(find src -name "*.java")
   java -cp out ids.cli.App sample_auth.log
   ```
   On Windows PowerShell:
   ```powershell
   # From project root
   javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
   java -cp out ids.cli.App sample_auth.log
   ```
2. Optional debug for parse skips:
   ```sh
   java -cp out ids.cli.App --debug-parse sample_auth.log
   ```

## Sample log cases
See [sample_auth.log](sample_auth.log) for crafted scenarios covering IP brute force, user brute force, and fail→success patterns.

## Extending
- Add more regex patterns for other services.
- Swap in persistent/stateful storage for real-time monitoring.
- Tune thresholds in `ids.config.DetectionConfig`.
