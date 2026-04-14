# 🚗 CarDash: Master State Ledger

## 🎯 Global Vision
CarDash is a "Safety-First" automotive dashboard that transforms Android devices into professional-grade vehicle telemetry tools. We provide real-time diagnostics (OBD-II) with a zero-distraction UI and local AI insights.

## 🛠 Active Technical Stack
- **Platform**: Android (Min SDK 26).
- **Languages**: Kotlin + Compose + Room DB.
- **Hardware**: ELM327 Bluetooth OBD-II Adapters.
- **Dev Pipeline**: Build App → Upload to Play Console (Internal/Testing) → Download to Device.
- **Standards**: Dashboard UI Standard, <200 LOC per file, Gradle-verified builds.

## 📂 Project Geography
- `app/`: Main Android Application.
- `METRIC_IMPLEMENTATION_GUIDE.md`: Core PID logic and threshold definitions.
- `MAINTENANCE_LOG.md`: Tracks architectural repairs and feature fixes.

## 🚦 Current Development State (Last Known Truth)
- **Status**: Stable Play Console Release Build (`app-release.aab` generated).
- **Recent Fixes**: Re-engineered HUD debounce logic to prevent UI freezes. Implemented unconditional genesis for the Master Ledger. Wrapped DB operations in try/catch to guarantee coroutine immunity. 
- **Current Objective**: Move to **Phase 4: AI & Visual Trends**. The deterministic 1-minute `VehicleHeartbeat` ledger is fully functional and safely recording without dropping points or crashing.
- **Critical Conflict**: Resolved. The structural divergence between frontend UI throttles and backend high-fidelity data hoarding is formally decoupled and performing properly.

---
*Status: Ontology Phase 2 Active | Location: Root Ledger*
