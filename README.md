# 🩺 PC DOQTAR

> **Your Intelligent, Lightweight Personal Computer Health & Diagnostic Companion**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Cross--Platform-blue.svg)](https://github.com/KishoreNMproject/pc-doqtar)
[![UI Framework](https://img.shields.io/badge/UI-JavaFX-green.svg)](https://openjfx.io/)
[![Cloud Sync](https://img.shields.io/badge/Cloud%20Sync-Firebase%20%2B%20Spring%20Boot-red.svg)](https://github.com/KishoreNMproject/pc-doqtar-connection-service)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](#)

---

## 🌟 Overview

**PC DOQTAR** is a modern, responsive desktop application built to take the guesswork out of computer performance and maintenance. Designed with a clean, user-friendly interface, PC DOQTAR continuously monitors your system hardware and operating system metrics, performs in-depth diagnostics, alerts you to emerging bottlenecks, and synchronizes health snapshots with the cloud for long-term health tracking.

Whether you are a casual user wanting a smooth, fast PC, a power user monitoring demanding workloads, or an IT professional maintaining client machines, **PC DOQTAR acts as your personal system physician**.

---

## 🚀 Key Features for Users & Customers

### 1. 📊 Real-Time System Vitals
- **Live Health Overview**: Get an instant summary score of your computer's vital signs (CPU, RAM, Disks, Network, and active tasks) directly from the primary dashboard.
- **Intuitive Status Badges**: Clear, color-coded health states (**Healthy**, **Warning**, **Critical**) replace cryptic system error codes.
- **Ultra-Low Overhead**: Engineered with lightweight architecture to ensure monitoring never drags down your gaming, streaming, or work productivity.

### 2. 🔍 Comprehensive Diagnostic Suites
| Diagnostic Module | What It Checks | Why It Matters To You |
| :--- | :--- | :--- |
| **CPU Diagnostics** | Core utilization, workload distribution, clock speeds, and thermal spikes | Prevents overheating and identifies applications causing performance stutter. |
| **Memory (RAM)** | Active consumption, available RAM, swap file pressure, and leak heuristics | Ensures your system has enough memory for smooth multitasking without freezing. |
| **Storage & Disks** | Partition capacities, read/write throughput, drive wear, and space alerts | Alerts you before drives run out of storage space or experience read/write failures. |
| **Network Performance**| Latency (ping), active network interfaces, connection stability, and bandwidth | Diagnoses Wi-Fi lag, dropped connections, and apps hogging bandwidth. |
| **Process Inspection** | High-consumption background tasks and unresponsive services | Pinpoints background software slowing down your startup or everyday workflow. |

### 3. ☁️ Secure Cloud Sync & Historical Trends
- **Track Health Over Time**: Connect your PC DOQTAR account to securely synchronize your diagnostic snapshots to the **PC DOQTAR Connection Service**.
- **Trend & Degradation Analysis**: Compare your current system performance against historical baselines to identify gradual slowdowns, memory leaks, or thermal deterioration before they cause hardware damage.
- **Offline Resilient**: Take diagnostic snapshots while offline; PC DOQTAR automatically syncs them to the cloud once you reconnect.

### 4. 🎨 Modern & Accessible Experience
- **Dark & Light Themes**: Easily toggle between sleek Dark Mode and high-contrast Light Mode to suit your environment.
- **Personalized Alerts**: Configure notification thresholds so you're only alerted when it matters to you.
- **Dedicated Account Control**: Simple sign-in and session management powered by secure industry-standard authentication.

---

## 🖥️ User Interface Tour

```
┌────────────────────────────────────────────────────────────────────────┐
│  🩺 PC DOQTAR  [ Dashboard ]                          -  □  ×          │
├─────────────┬──────────────────────────────────────────────────────────┤
│             │  System Health: 96% [ EXCELLENT ]                        │
│  📊 Overview│  ──────────────────────────────────────────────────────  │
│  🔬 Diags   │  [ CPU: 18% ]     [ RAM: 4.2 / 16 GB ]   [ Disk: 62% ]   │
│  🌐 Network │  Intel Core i7    DDR4 Dual Channel      NVMe SSD        │
│  ⚙️ Tasks   │                                                          │
│  ⚙️ Settings│  Recent Alerts:                                          │
│             │  • No critical issues detected. System is running cool.  │
│             │                                                          │
│  [ Sign In ]│  [ Run Full Diagnostic Scan ]   [ Sync to Cloud ]        │
└─────────────┴──────────────────────────────────────────────────────────┘
```

- **Overview Page**: High-level telemetry cards for quick glances at CPU load, RAM usage, and drive capacity.
- **Diagnostics Page**: Deep dive into hardware sub-systems with automated recommendations.
- **Network Page**: Test ping, latency, and interface throughput in real-time.
- **Processes Page**: Sort and inspect running tasks by CPU and memory footprint.
- **Settings Page**: Theme configuration, auto-sync intervals, and connection preferences.

---

## 🔒 Privacy & Data Security

Your privacy and peace of mind are paramount:
- **No Personal File Scanning**: PC DOQTAR only reads hardware and operating system telemetry (temperatures, percentages, speeds). It **never** scans or reads your personal documents, browsing history, or private data.
- **Encrypted Transmission**: All communication with the PC DOQTAR cloud is encrypted end-to-end via TLS/HTTPS.
- **Secure Authentication**: User sign-in and cloud sessions are managed through Firebase Authentication with cryptographically verified identity tokens.

---

## 📥 Getting Started

### System Requirements
- **Operating System**: Windows 10 / Windows 11 (64-bit) or compatible Linux/macOS environment
- **Runtime**: Java 21 or higher (or the bundled standalone installer)
- **Memory**: Minimum 512 MB RAM free for the application
- **Disk Space**: ~100 MB free space

### Quick Launch (For Users)
1. Download the latest release installer or packaged executable from the [Releases](https://github.com/KishoreNMproject/pc-doqtar/releases) tab.
2. Run the application installer or executable (`PCDOQTAR.exe`).
3. (Optional) Sign in with your PC DOQTAR account in the bottom-left corner to enable cloud sync and historical reports.
4. Click **Run Diagnostics** on the main dashboard to evaluate your system's health.

### Running from Source (For Developers & Advanced Users)
If you are building from source:
```bash
# Clone the repository
git clone https://github.com/KishoreNMproject/pc-doqtar.git
cd pc-doqtar

# Launch the desktop UI via Maven JavaFX plugin
mvn clean javafx:run
```

---

## 🌐 Connecting to Cloud Services

PC DOQTAR works entirely offline out-of-the-box. When you choose to connect your account:
- By default, it communicates with the secure cloud endpoint:  
  `https://pc-doqtar-connection-service.vercel.app`
- For private enterprise deployments or local network test setups, you can override the target server by setting:
  ```bash
  PC_DOQTAR_SERVER_URL=http://your-server-address:8080
  ```

---

## 💬 Frequently Asked Questions (FAQ)

<details>
<summary><b>Does PC DOQTAR slow down my computer while running?</b></summary>
No. PC DOQTAR is engineered to be extremely lightweight. Background telemetry polling consumes less than 1% of modern CPU capacity and stays dormant when not actively viewing real-time charts.
</details>

<details>
<summary><b>Can I use PC DOQTAR without creating an account?</b></summary>
Yes! All local diagnostic scans, real-time monitors, and hardware health checks work 100% offline without requiring any login or account creation. An account is only needed if you want cloud backup and cross-scan trend analytics.
</details>

<details>
<summary><b>What should I do if PC DOQTAR shows a "Warning" or "Critical" status?</b></summary>
Click directly on the warning card on the dashboard or navigate to the Diagnostics tab. PC DOQTAR displays a clear explanation of what triggered the threshold (e.g., low disk space, high memory usage, or thermal throttling) alongside actionable recommendations to resolve it.
</details>

---

## 🤝 Support & Feedback

If you encounter issues or have feature suggestions:
- Report a bug or request a feature via [GitHub Issues](https://github.com/KishoreNMproject/pc-doqtar/issues).
- Check the companion cloud service at [pc-doqtar-connection-service](https://github.com/KishoreNMproject/pc-doqtar-connection-service).

---

<p align="center">
  <b>PC DOQTAR — Proactive Care for Peak PC Performance.</b>
</p>
