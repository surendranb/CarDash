# CarDash Maintenance Ledger

This log tracks all architectural repairs and feature fixes to ensure they are properly "reflecting" in the build.

## Fix History

| Date | Issue | Component | Fix Description | Status | Commit |
|------|-------|-----------|-----------------|--------|--------|
| 2026-04-11 | HUD Space | HUD | Replaced APP_ICON with minimalist dot icon | Fixed | staged |
| 2026-04-11 | HUD Distraction | HUD | Increased throttle to 4s, stabilized header | Fixed | staged |
| 2026-04-11 | Storage Gap | Heartbeat | Initialized aggregator on connection, added stop-flush | Fixed | staged |
| 2026-04-11 | Assistant Data | AI | Migrated to getRecentHeartbeatsInstant | Verified | staged |
| 2026-04-14 | HUD Freeze | HUD | Fixed swallowed invalidate by decoupling state from redraw | Fixed | HEAD |
| 2026-04-14 | Missing Ledger | Backend | Refactored HeartbeatAggregator genesis to start unconditionally | Fixed | HEAD |
| 2026-04-14 | DB Crash | Backend | Wrapped Room Db insertion in try/catch to prevent coroutine death | Fixed | HEAD |
| 2026-04-14 | Release Bundle | CI/CD | Located isolated Brew JDK 17 to successfully forge app-release.aab | Verified | N/A |
| 2026-04-19 | Service Leak | Core | Consolidated OBDService and Diagnostics into single provider unit | Fixed | RECOVERY |
| 2026-04-19 | Telemetry Gap | Persistence| Hardened Aggregator with dynamic deltas and robust Flush-on-Exit | Fixed | RECOVERY |
| 2026-04-19 | HUD Lag | UI | Reduced refresh throttle to 2s and added priority invalidates | Fixed | RECOVERY |
| 2026-04-19 | History Void | UI | Restored Ledger flow and added 'Recording' status indicator | Fixed | RECOVERY |

## Current Verification Status
- [x] HUD minimalist UI verified (build successful with ic_dot)
- [x] AA Driving Error resolved via 4s throttle
- [x] Heartbeat DB initialization logic hardened
- [x] Assistant prompt parity verified via code audit
