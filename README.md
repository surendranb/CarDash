# CarDash

<div align="center">
  <img src="docs/images/cardash_logo.png" alt="CarDash Logo" width="200"/>
  <br>
  <h3>Your Vehicle's Digital Dashboard & Diagnostics Companion</h3>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
  [![Android Version](https://img.shields.io/badge/Android-8.0%2B-green)](https://www.android.com/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.8-orange)](https://kotlinlang.org/)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-purple)](https://developer.android.com/jetpack/compose)
  
</div>

## 🚗 About CarDash

CarDash transforms your Android device into a powerful automotive dashboard, connecting to your vehicle's OBD-II port to provide real-time metrics and basic diagnostics. Whether you're a car enthusiast wanting to monitor your engine's vital statistics or a DIY mechanic diagnosing issues, CarDash offers an intuitive interface for viewing your vehicle's data.

<div align="center">
  <img src="docs/images/mockup_dashboard.png" alt="CarDash App Dashboard" width="80%"/>
</div>

## ✨ Key Features

### 📊 Comprehensive Metrics Dashboard
* **Real-time Engine Stats**: RPM, speed, engine load, and throttle position
* **Temperature Monitoring**: Coolant and intake air temperature
* **System Status**: Fuel level, fuel pressure, barometric pressure, and battery voltage
* **Customizable Layout**: Arrange metrics in your preferred dashboard configuration

### 📈 Data Visualization
* **Basic Graphs**: View trends of your vehicle's parameters
* **Historical Data**: See how metrics change over time

### 🔧 Basic Diagnostics
* **OBD-II Error Codes**: Read diagnostic trouble codes
* **Log Viewer**: View basic communication logs with your vehicle's ECU
* **Data Recording**: Save session data for later analysis

### 📱 Modern Android Experience
* **Material Design**: Clean, intuitive interface
* **Jetpack Compose UI**: Responsive layouts for various screen sizes
* **Tab-based Navigation**: Easy access to different functionality

<div align="center">
  <table>
    <tr>
      <td><img src="docs/images/metrics_screen.png" width="250" alt="Metrics Dashboard"/></td>
      <td><img src="docs/images/graphs_screen.png" width="250" alt="Performance Graphs"/></td>
      <td><img src="docs/images/diagnostics_screen.png" width="250" alt="Diagnostics"/></td>
    </tr>
    <tr>
      <td align="center"><b>Real-time Metrics</b></td>
      <td align="center"><b>Data Graphs</b></td>
      <td align="center"><b>Diagnostic Tools</b></td>
    </tr>
  </table>
</div>

## 🛠️ Requirements

* Android 8.0 (API level 26) or higher
* Bluetooth-enabled Android device
* OBD-II compatible vehicle (generally all cars made after 1996 in the US, 2001 in the EU, and 2008 globally)
* ELM327-based Bluetooth OBD-II adapter (widely available for $10-30)

## 📱 Installation

### Google Play Store
<a href="#"><img src="docs/images/google-play-badge.png" height="50" alt="Get it on Google Play"/></a>
*(Coming Soon)*

### Direct APK Download
* Download the latest release APK from our [Releases page](https://github.com/surendranb/cardash/releases)
* Enable "Install from Unknown Sources" in your device settings
* Open the downloaded APK to install

### Building from Source
```bash
# Clone the repository
git clone https://github.com/surendranb/cardash.git

# Change to project directory
cd cardash

# Build with Gradle
./gradlew assembleDebug

# The APK will be available at:
# app/build/outputs/apk/debug/app-debug.apk
```

## 🚀 Getting Started

1. **Connect your OBD-II adapter**
   * Plug the adapter into your vehicle's OBD-II port (typically under the dashboard)
   * Turn on vehicle ignition (engine doesn't need to be running)

2. **Pair with Bluetooth**
   * Pair your Android device with the OBD-II adapter in your Bluetooth settings
   * Default PIN is often `0000` or `1234`

3. **Launch CarDash**
   * Open the app and tap the connection button in the top right
   * Select your paired OBD-II adapter
   * Accept the requested permissions

4. **Start monitoring**
   * Once connected, your vehicle's metrics will appear on the dashboard
   * Customize your view using the settings menu

<div align="center">
  <img src="docs/images/connection_flow.png" alt="Connection Flow" width="90%"/>
</div>

## 🔄 Current Status

### What's Working
✅ Bluetooth connection to most standard ELM327 OBD-II adapters  
✅ Real-time display of vehicle metrics with customizable dashboard  
✅ Basic graph visualization of metrics over time  
✅ Basic diagnostic logging and session history  
✅ Multiple tabs for different functionality (Metrics, Trends, Diagnostics, History)  
✅ Settings customization for tab visibility and data collection frequency  

### What's In Development
🔄 **Android Auto Integration** - View your vehicle metrics directly on your car's infotainment display  
🔄 **Gemini AI Integration** - Advanced diagnostics and predictive maintenance using Google's Gemini AI  
🔄 Enhanced diagnostic trouble code (DTC) interpretation  
🔄 Trip logging and analysis  
🔄 Fuel economy calculations  
🔄 Expanded vehicle compatibility testing  

### Known Limitations
⚠️ Fuel pressure readings may be unavailable on some vehicles  
⚠️ Battery voltage reading accuracy depends on OBD-II adapter quality  
⚠️ Not all metrics are supported by all vehicles (manufacturer dependent)  
⚠️ Performance may vary based on vehicle's OBD-II implementation  

## 🤝 Contributing

We welcome contributions of all kinds from the community! Whether you're fixing bugs, adding features, improving documentation, or spreading the word, your help makes CarDash better for everyone.

* **Code Contributions**: See our [CONTRIBUTING.md](CONTRIBUTING.md) (coming soon) for guidelines
* **Bug Reports & Feature Requests**: Submit through our [Issues page](https://github.com/surendranb/cardash/issues)
* **Discussion**: Join the community to discuss ideas and get help

### Development Environment
* Android Studio Arctic Fox or newer
* Kotlin 1.8+
* Java 11+
* Android SDK 33+

## 📚 Documentation

* User Guide - Coming soon
* Developer Guide - Coming soon
* OBD-II Command Reference - Coming soon
* Troubleshooting - Coming soon

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgements

* [ELM327 Documentation](https://www.elmelectronics.com/products/ecutool/obdic/) for protocol specifications
* [OBD-II PIDs Reference](https://en.wikipedia.org/wiki/OBD-II_PIDs) for parameter definitions
* Android Jetpack libraries for modern Android development
* All our open source contributors and testers

## 📬 Contact & Support

* [GitHub Issues](https://github.com/surendranb/cardash/issues) for bug reports and feature requests
* Email: support@cardash-app.com (for private inquiries)

---

<div align="center">
  <i>CarDash is an independent project not affiliated with any automotive manufacturer.</i><br>
  <i>Use of this application is at your own risk and discretion.</i>
</div>
