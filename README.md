# PC DOQTAR 🩺💻
### *Smart, Lightweight System Health & Diagnostics Companion for Windows*

[![Version](https://img.shields.io/badge/version-1.1.2-blue.svg)](https://github.com/KishoreNMproject/pc-doqtar/releases)
[![Platform](https://img.shields.io/badge/platform-Windows%2010%20%7C%2011-0078D6.svg)](https://www.microsoft.com/windows)
[![Architecture](https://img.shields.io/badge/architecture-x64-brightgreen.svg)]()
[![Privacy](https://img.shields.io/badge/privacy-local--first-success.svg)]()
[![Cloud Sync](https://img.shields.io/badge/cloud%20sync-optional-orange.svg)]()

---

## 🌟 What is PC DOQTAR?

**PC DOQTAR** is your personal computer's digital physician. Designed from the ground up for **Windows 10 and 11**, PC DOQTAR provides crystal-clear visibility into your computer's health, hardware performance, and system stability—all packaged inside a sleek, modern desktop interface that uses virtually zero background resources.

Whether you are a **gamer** tracking resource bottlenecks, a **remote professional** ensuring system stability for work, or an **everyday PC user** wanting to know why your computer is slowing down, PC DOQTAR delivers instant, actionable health assessments without confusing jargon or heavy background bloat.

---

## ✨ Key Customer Benefits & Features

### 🩺 1. Instant PC Health Checkup
- **One-Glance Health Status:** Get immediate clarity on your PC's condition with intuitive status badges: **Healthy**, **Warning**, or **Critical**.
- **Plain-English Diagnosis:** Instead of confusing hex codes, PC DOQTAR tells you exactly what requires attention (e.g., *"RAM utilization is exceeding 85%"* or *"Storage drive C: has under 10% free space"*).
- **Proactive Warnings:** Configurable health thresholds give you early notice before memory exhaustion or disk bottlenecks cause application freezes.

### ⚡ 2. Real-Time Hardware Diagnostics
- **CPU Performance:** Track live multi-core CPU load, system idle capacity, and core frequencies in real time.
- **Memory (RAM) Health:** Monitor available memory vs. in-use memory with dynamic visual gauges.
- **Storage Drive Health:** Inspect all connected physical drives and partitions, reviewing total storage, used space, and free capacity.
- **High-Precision Telemetry:** Powered by the proven OSHI hardware abstraction library for fast, reliable hardware queries.

### 🌐 3. Network Health & Speed Testing
- **Smart Adapter Filtering:** Automatically detects and displays your active physical network interfaces (Wi-Fi and Ethernet) while filtering out confusing virtual adapters.
- **Live Bandwidth Counters:** View sent and received traffic counters and link speeds.
- **Built-in Cloudflare Speed Test:** Run latency and speed tests directly within the application to diagnose internet connectivity issues.

### 🔍 4. Process Explorer & Bottleneck Finder
- **Active Process Tracking:** See exactly which programs and background services are consuming your CPU cycles and memory.
- **PID & Resource Visibility:** Easily identify rogue or unneeded background processes that are draining your laptop's battery or slowing down your workflow.

### ☁️ 5. Optional Cloud Synchronization & Historical Trends
- **Track Health Over Time:** Connect to the optional **PC DOQTAR Cloud Service** to store diagnostic snapshots and observe your computer’s performance trends across days, weeks, and months.
- **Automatic Degradation Detection:** The cloud service analyzes trends between your diagnostic uploads to alert you if your system's performance is gradually degrading.
- **Offline-First Resilience:** No internet? No problem! PC DOQTAR queues diagnostic reports safely on your hard drive and seamlessly synchronizes them when you reconnect.

### 🎨 6. Sleek, Distraction-Free User Experience
- **Custom Modern Window Design:** Clean, frameless layout with a custom title bar, smooth navigation, and responsive cards.
- **Dark & Light Mode:** Toggle between Dark and Light themes with a single click in Settings. Your preference is automatically remembered across app restarts.
- **Lightweight Footprint:** Built to monitor your computer without impacting its performance.

---

## 🔒 Privacy & Data Ethics: Your Data Stays Yours

At PC DOQTAR, we believe your personal data belongs to you. Here is our customer privacy commitment:

| What PC DOQTAR Reads | What PC DOQTAR NEVER Touches |
| :--- | :--- |
| ✅ CPU, RAM, and Disk utilization percentages | ❌ Personal files, photos, and documents |
| ✅ Storage drive capacity and free space | ❌ Browsing history, cookies, or internet activity |
| ✅ Network adapter speeds and byte counters | ❌ Passwords, keystrokes, or screen contents |
| ✅ Active process names and memory footprint | ❌ Any sensitive or confidential user files |

- **Local-First by Default:** PC DOQTAR operates 100% locally. You do not need an account or internet access to use all core diagnostics.
- **Secure Cloud Sync:** When you choose to sync your reports to the cloud, all diagnostic payloads are transmitted over encrypted TLS/HTTPS and protected by Firebase Authentication.

---

## 🖥️ User Interface Tour

```
┌────────────────────────────────────────────────────────────────────────┐
│  PC DOQTAR                                      [👤 Sign In]  —  □  ✕  │
├──────────────┬─────────────────────────────────────────────────────────┤
│  📊 Overview │  System Health: 🟢 HEALTHY                              │
│              │                                                         │
│  🩺 Diags    │  [ CPU Load: 18% ]     [ Memory: 6.2 / 16.0 GB ]        │
│              │  [ Disk C: 68% Used ]  [ Network: 120 Mbps Down ]       │
│  🌐 Network  │                                                         │
│              │  Recent Alerts:                                         │
│  ⚙️ Processes│  • All hardware sensors operating within safe limits.   │
│              │  • Storage drive has 142 GB available.                  │
│  🔧 Settings │                                                         │
└──────────────┴─────────────────────────────────────────────────────────┘
```

1. **Overview Dashboard:** Your daily high-level summary. View overall health status and quick vital cards.
2. **Diagnostics Page:** Deep dive into CPU details, RAM allocation, and individual drive partitions.
3. **Network Page:** Live network adapter statuses and integrated connection speed testing.
4. **Processes Page:** Top resource-consuming processes sorted by CPU and memory impact.
5. **Settings Page:** Customize alert thresholds, switch between Dark/Light themes, and manage your cloud account.

---

## 📥 Installation & Getting Started

### System Requirements
* **Operating System:** Windows 10 (64-bit) or Windows 11 (64-bit)
* **Processor:** 64-bit Intel or AMD processor
* **Memory (RAM):** 4 GB minimum (8 GB recommended)
* **Disk Space:** ~150 MB of free storage space

### Quick Start Guide

1. **Download the Installer:**
   - Grab the latest `PCDOQTAR-Setup.exe` from the [Releases](https://github.com/KishoreNMproject/pc-doqtar/releases) page.
2. **Run Setup:**
   - Double-click `PCDOQTAR-Setup.exe` and follow the guided on-screen installation wizard.
3. **Launch PC DOQTAR:**
   - Open PC DOQTAR from your Start Menu or Desktop shortcut.
4. **Instant Diagnostic:**
   - The application immediately runs an initial diagnostic scan and presents your system health status.
5. **(Optional) Connect to the Cloud:**
   - Click the **Sign In** button in the top right corner.
   - Create a free account or log in with your email to start recording your PC's health history.

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>Do I need an active internet connection to use PC DOQTAR?</b></summary>
No. All core hardware monitoring, diagnostics, and process exploration run entirely offline on your local machine. An internet connection is only needed if you want to use the optional Cloudflare speed test or sync diagnostic reports to your cloud account.
</details>

<details>
<summary><b>Will PC DOQTAR slow down my games or demanding apps?</b></summary>
No. PC DOQTAR is designed to be ultra-lightweight. Diagnostic background updates run on low-overhead worker threads and consume minimal CPU and RAM resources.
</details>

<details>
<summary><b>How does cloud synchronization work?</b></summary>
When you log in with your account, clicking "Sync Now" (or enabling background reporting) uploads a compact JSON snapshot of your hardware statistics to the secure PC DOQTAR Connection Service. You can then compare snapshots over time to identify trends such as memory leaks or disk usage growth.
</details>

<details>
<summary><b>Can I reset my password if I forget it?</b></summary>
Yes. In the Sign-In window, click "Forgot Password?", enter your account email address, and a secure password reset link will be sent to your inbox immediately.
</details>

---

## 🛠️ For Developers & Builders

If you are interested in compiling PC DOQTAR from source or packaging your own installer:

```bash
# Clone the repository
git clone https://github.com/KishoreNMproject/pc-doqtar.git
cd pc-doqtar

# Build and run tests with Maven
mvn clean test

# Run the desktop application
mvn javafx:run
```

*Requires JDK 21+ and JavaFX 21.*

---

## 🤝 Support & Feedback

We want PC DOQTAR to be the most helpful, user-friendly PC health tool on Windows. If you encounter any bugs, have questions, or would like to request a new feature:

- 🐛 **Report an Issue:** [GitHub Issues](https://github.com/KishoreNMproject/pc-doqtar/issues)
- 💡 **Request a Feature:** [GitHub Discussions](https://github.com/KishoreNMproject/pc-doqtar/discussions)
- 🌐 **Backend Cloud Service:** [pc-doqtar-connection-service](https://github.com/KishoreNMproject/pc-doqtar-connection-service)

---

<p align="center">
  <b>PC DOQTAR</b> — <i>Keep your PC running at its best, every single day.</i>
</p>
