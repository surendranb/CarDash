# Developer Guide: Android Auto Integration

This document contains technical details for developers looking to modify or understand the Android Auto integration in CarDash.

## Manifest Configuration

The following manifest configurations are crucial for the app to be recognized and run correctly on an Android Auto unit:

### 1. Core Components

*   **Uses Library:** Declare `androidx.car.app`.
    ```xml
    <application ...>
        <uses-library android:name="androidx.car.app" android:required="false"/>
    </application>
    ```

*   **Car App Service:** The entry point for Android Auto.
    ```xml
    <service
        android:name="com.fuseforge.cardash.services.auto.CarDashCarAppService"  
        android:exported="true"
        android:label="@string/app_name">
        <intent-filter>
            <action android:name="androidx.car.app.CarAppService" />
            <category android:name="androidx.car.app.category.IOT" />
        </intent-filter>
    </service>
    ```

*   **Meta-Data:**
    ```xml
    <meta-data
        android:name="com.google.android.gms.car.application"
        android:resource="@xml/automotive_app_desc" />
    <meta-data
        android:name="androidx.car.app.minCarApiLevel"
        android:value="1" />
    ```

### 2. Desktop Head Unit (DHU) Testing

The DHU allows you to simulate an Android Auto head unit on your development machine.

1.  **Install DHU:** Download from Android Studio SDK Manager ("SDK Tools" -> "Android Auto Desktop Head Unit emulator").
2.  **Enable Developer Mode:** On your phone, go to Android Auto settings and tap **Version** 10 times.
3.  **Start Server:** In Developer Settings, enable "Start head unit server".
4.  **Forward Port:** `adb forward tcp:5277 tcp:5277`
5.  **Run DHU:** Execute `desktop-head-unit` from the Android SDK `extras/google/auto/` directory.

## App Signing & Play Store

For production releases, CarDash must be signed with a valid release key. 

*   **Upload Key Restoration:** If you lose your keys, use the **Key Reset** feature in the Play Console. 
*   **AAB Build:** Run `./gradlew bundleRelease` to generate the production bundle.
