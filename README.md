# 🌟 Diamonds Secure Operator Terminal

An offline-first, highly secure multi-tenant operator platform built for elite private excursion enterprises, utilizing Jetpack Compose and Material Design 3.

---

## 📖 Table of Contents
1. [Overview](#-overview)
2. [Key Features](#-key-features)
3. [Technology Stack](#-technology-stack)
4. [System Architecture](#%EF%B8%8F-system-architecture)
5. [Setup & Build Instructions](#-setup--build-instructions)
6. [Pre-Seeded Credentials](#-pre-seeded-credentials)
7. [Directory Structure](#-directory-structure)

---

## 🌌 Overview

The **Diamonds Secure Operator Terminal** is a premium, client-side offline-first enterprise dashboard crafted for high-end hospitality operators. Built around the concept of "exclusive luxury travel," it bridges the gap between secure multi-tenant client requirements and modern mobile workflows. 

The terminal allows captains, managers, guides, and drivers to isolate corporate database streams, sign in securely using a combination of passwords, MFA OTPs, and biometric sensors, view fleet availabilities, track client bookings, generate encrypted boarding tickets, and monitor terminal activities via an interactive cryptographic ledger.

---

## 🎨 Design Theme: Cosmic Slate & Gold Premium

The application adheres to an custom-crafted **Slate Dark Theme** styled for eye comfort during maritime or late-night operations:
- **Primary Canvas (`SlateDarkBg`)**: `#0B0F19` — Deep, immersive slate background.
- **Glassmorphic Containers (`SurfaceGlass`)**: `#162032` with varying translucent opacities (`0.4f` to `0.8f`).
- **Accents (`GoldPremium`)**: `#D4AF37` — A bright, premium metallic gold for highlights, focus cues, and primary branding.
- **Typography**: Paired display typography with spacious padding, elegant horizontal lines, and strict 8dp layout alignments.

---

## 🚀 Key Features

### 1. Stage A: Multi-Tenant Gateway & Subdomain Lookup
- **Secure Domain Binding**: Before entering credentials, operators verify their corporate tenant subdomain (e.g., `exclusive-yachts`, `amalfi-charters`, `elite-transfers`).
- **TLS Handshake Simulation**: Performs simulated queries to `/companies?subdomain=eq.{tenant_subdomain}`. Upon success, binds headers to the specific company schema, caches the `company_id`, and initiates the terminal schema.

### 2. Triple-Lock Employee Authentication
- **Multi-Factor Auth (MFA)**: Secure PIN/Password authentication paired with custom-generated 4-digit OTP handshakes.
- **Biometric Integration**: Simulation of fingerprint and facial recognition biometric protocol exchanges.
- **Fallback Verification**: Fully featured password recovery using encrypted security question matching.
- **Live Security Log**: A rolling, terminal-grade cryptographic event stream logging all logins, MFA toggles, tenant handshakes, and biometric authorizations.

### 3. Excursion Explorer & Active Fleet Status
- **Dynamic Catalog**: Full detailed view of luxury yachts, private helicopters, and luxury land transports.
- **Availability State**: Real-time indicators of fleet statuses (e.g., *Active*, *In Maintenance*, *Chartered*).
- **Favorites Registry**: Star priority vehicles for quick-access operations.

### 4. 100% User-Focused Bookings Engine
- **Universal Booking Registry**: Streamlined list of active, completed, and canceled vip bookings.
- **Secure Boarding Pass Generator**: Every booking features a custom-rendered boarding card with a dynamically generated secure QR Code matching the boarding token.
- **Interactive Trip Milestones**: Timeline flow detailing dispatch confirmation, passenger boarding, underway status, and dock completion.

### 5. Centralized Settings & Biometric Enrollment
- **Floating VIP Header Dropdown**: Tap the circular profile icon in the top-right header to access account options, app preferences, and security settings instantly.
- **MFA Management**: Toggle Multi-Factor Authentication on/off directly from the profile workspace, adding or removing authentication checkpoints on subsequent logins.

### 6. Interactive System Architecture Explorer (Diamonds Hub)
- **Schema Visualizer**: Directly browse active PostgreSQL tables and structures matching the Supabase-inspired backend specification.
- **Active Endpoints Logger**: View mock HTTP status codes, latencies, and payload responses from core operational endpoints.

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.x — 100% Type-safe code.
- **UI Framework**: Jetpack Compose — Modern declarative layouts.
- **Design System**: Material Design 3 (M3) — Standard containers, fluid shapes, and Edge-to-Edge window inset bindings.
- **State Architecture**: MVVM (Model-View-ViewModel) utilizing `StateFlow` and Compose Lifecycle bindings.
- **Data Persistence**: Room Database — SQLite-backed local cache layer for high-speed offline capabilities.
- **Concurrency**: Kotlin Coroutines & Flow — For asynchronous, non-blocking operations.

---

## ⚙️ System Architecture

The application is structured into two primary core layers:

1. **`MainActivity.kt` (UI Layer)**:
   - Houses the `DiamondsApp` root entry point.
   - Controls top-level navigation routes (`Explore`, `Bookings`, `Favorites`, `Profile`).
   - Implements the multi-step `LoginScreen` (handling subdomains, login, registration, recovery, and MFA dialogs).
   - Draws complex composables for fleet cards, ticket passes, live charts, and terminal logs.

2. **`DiamondsViewModel.kt` (Business Logic & State)**:
   - Governs all centralized states including authenticated user profiles, lists of fleet/bookings, security ledgers, and notifications.
   - Manages CRUD operations against the Room database.
   - Processes cryptographic actions like MFA state changes, domain lookup validations, and credential matching.

---

## 📦 Setup & Build Instructions

This is a standard Gradle Android project. All dependencies are configured via Version Catalogs (`libs.versions.toml`) and integrated using Gradle.

### Prerequisites
- Android Studio Ladybug (2024.2.1) or higher.
- JDK 17 or higher.
- Gradle 8.4+

### Build Process
To compile the application and generate a debug APK:

```bash
# Clean previous builds (optional)
gradle clean

# Compile application and verify type-safety
gradle assembleDebug
```

To run the local unit and integration tests (including Robolectric support):

```bash
gradle :app:testDebugUnitTest
```

---

## 🔐 Pre-Seeded Credentials

For testing and evaluation of different operator security profiles, the following employee records are pre-seeded into the local SQLite database on launch:

| Staff Member | Email Address | Access PIN | Auth Requirement | Role |
| :--- | :--- | :--- | :--- | :--- |
| **Captain Marco Rossi** | `captain@diamonds.com` | `1111` | Password + **MFA OTP** | Boat Captain |
| **Elena Moretti** | `guide@diamonds.com` | `2222` | PIN Verification | Excursion Guide |
| **Giovanni Bianchi** | `driver@diamonds.com` | `3333` | PIN Verification | Luxury Driver |
| **Demo Manager** | `manager@diamonds.com` | `8888` | Password + **MFA OTP** | Operations Manager |

> ℹ️ **Default MFA OTP Code**: For profiles with Multi-Factor Authentication active, the secure authentication gateway will prompt for a 4-digit code. Use **`5555`** to complete the verification handshake.

---

## 📁 Directory Structure

```text
.
├── app
│   ├── build.gradle.kts           # App-level build configurations
│   └── src
│       └── main
│           ├── AndroidManifest.xml # Core application manifest and permissions
│           └── java
│               └── com
│                   └── example
│                       ├── MainActivity.kt        # Primary UI & Navigation Hub
│                       └── ui
│                           └── viewmodel
│                               └── DiamondsViewModel.kt # Central VM, Security Logic & Room Repositories
├── build.gradle.kts               # Project-level build configurations
├── settings.gradle.kts            # Project-level plugin and repository settings
├── auth_architecture_spec.md      # Multi-tenant security specifications
└── metadata.json                  # AI Studio metadata
```
