# AI Card Detector

AI Card Detector is a real-time card detection library designed to detect and process cards using TensorFlow Lite and CameraX. It allows seamless integration of card detection capabilities into your applications for KYC (Know Your Customer) purposes and other identity verification scenarios. The library is lightweight, fast, and optimized for mobile environments.

## Features

- Real-time card detection using TensorFlow Lite.
- Supports integration with CameraX for capturing real-time images.
- Detects various types of cards, such as IDs, passports, and driver’s licenses.
- Optimized for Android applications.

## Installation

Follow these steps to integrate the AI Card Detector library into your Android project.

### Step 1: Add the JitPack Repository

In your root `build.gradle` file, add the JitPack repository at the end of the `repositories` section:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

 This will allow you to access dependencies hosted on JitPack.

### Step2: Add the Dependency

In your app's build.gradle file, add the following dependency:

```
dependencies {
    implementation 'com.github.shahrukhahmed94:AICardDetector:1.0.3'
}
```

Make sure you sync your project with Gradle after adding the dependency to complete the integration.


