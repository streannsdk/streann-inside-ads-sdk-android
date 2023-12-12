# streann-inside-ads-sdk-android

Streann Inside Ad is an Android library designed to seamlessly incorporate playback functionality
of diverse ad formats into your Android applications.
This library supports the playback of VAST ads using the Interactive Media Ads (IMA) SDK by Google,
along with various video ad formats and the presentation of image-based ads.

## Features

- Effortless integration of ad playback within your Android app.
- Seamless support for VAST video ads through the Google IMA SDK.
- Full compatibility with different video types, including MP4, m3u8, etc.
- Capability to seamlessly display image-based advertisements.

## Installation

To integrate the Streann Inside Ad library into your Android project, add the following dependency
to your app-level build.gradle file:

```gradle
dependencies {
    implementation 'com.github.streannsdk:streann-inside-ads-sdk-android:1.0.4
}
```

To use our library you need to add the JitPack maven repository to the list of repositories
in your settings.gradle file:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

## Usage

To use the InsideAdView in your project, follow these steps:

- Implement the initializeSdk method in your application to initialize our SDK.
- The apiKey, apiToken and baseUrl are mandatory parameters to initialize our SDK, and they will be
  provided:
  ```js
  InsideAdSdk.initializeSdk(
        apiKey = "api_key",
        apiToken = "api_token",
        baseUrl = "base_url"
  )

  ```

- You could also implement the optional parameters: appDomain, siteUrl, storeUrl, descriptionUrl,
  userBirthYear and userGender. Ex:
   ```js
   InsideAdSdk.initializeSdk(
        apiKey = "api_key",
        apiToken = "api_token",
        baseUrl = "base_url",
        userGender = "Female"
   )
   ```

- Add the InsideAdView to your layout XML file:
  ```xml
  <com.streann.insidead.InsideAdView
    android:id="@+id/insideAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

- In your activity or fragment, initialize the InsideAdView:

  ```js
  val insideAdView = findViewById(R.id.insideAdView);

- In your activity or fragment, request an ad:

  ```kotlin
  mInsideAdView?.requestAd(
    screen = "",
    isAdMuted = true,
    insideAdCallback = object : InsideAdCallback {
        override fun insideAdReceived(insideAd: InsideAd) { }

        override fun insideAdLoaded() { }

        override fun insideAdPlay() { }

        override fun insideAdStop() { }

        override fun insideAdSkipped() { }

        override fun insideAdClicked() { }

        override fun insideAdError(error: String) { }

        override fun insideAdVolumeChanged(level: Int) { }
      })
  
   - screen - enter the screen where you wish to load the ad (ex. Login, Splash screen, etc.)
   - isAdMuted - choose if you want your ad to be muted or not (optional parameter, default value: false)
   - InsideAdCallback - implement our interface to receive events from the ads' progress
  ```

- By overriding the event listeners, handle ad events such as ads received, loaded, played, stopped,
  ad volume changed, ad errors, etc.

- If you want to manually stop the ad while it's playing:
  ```js
  mInsideAdView?.stopAd()
  ```

## Sample App

You can check out our InsideAdDemo App to see the InsideAdView in action.
