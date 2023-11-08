# streann-inside-ads-sdk-android

Streann Inside Ad is an Android library that provides a simple way to integrate ad playback
functionality into your Android applications using the Interactive Media Ads (IMA) SDK by Google. With Streann Inside Ad library, you can seamlessly
incorporate ads into your projects.


## Features

- Easily integrate ad playback into your Android app.
- Seamless integration of VAST video ads into your Android app.
- Customizable ad display options.
  

## Installation

To integrate Streann Inside Ad library into your Android project, add the following dependency to
your app-level build.gradle file:

```gradle
dependencies {
    implementation 'com.github.streannsdk:streann-inside-ads-sdk-android:1.0.1
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
- The apiKey and baseUrl are mandatory parameters to initialize our SDK, and they will be provided:
  ```js
  InsideAdSdk.initializeSdk(
        apiKey = "api_key",
        baseUrl = "base_url"
  )

  ```

- You could also implement the optional parameters: appDomain, siteUrl, storeUrl, descriptionUrl,
  userBirthYear and userGender. Ex:
   ```js
   InsideAdSdk.initializeSdk(
        apiKey = "api_key",
        baseUrl = "base_url",
        userGender = "Female"
   )
   ```

- Add the InsideAdView to your layout XML file:
  ```xml
  <com.streann.insidead.InsideAdView
        android:id="@+id/insideAdView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
  
- In your activity or fragment, initialize the InsideAdView:

  ```val insideAdView = findViewById(R.id.insideAdView);```

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
  
          override fun insideAdError(error: String) { }
  
          override fun insideAdVolumeChanged(level: Float) { }
      })
  
   - screen - enter the screen where you wish to load the ad
      ex. Login, Splash screen, etc.
   - isAdMuted - choose if you want your ad to be muted or not (default value: false)
   - InsideAdCallback - implement our callback to receive events from the ads' progress
  ```

- Handle ad events such as ads received, loaded, played, stopped, ad volume changed, and ad errors
  by overriding the event listeners.

## Sample App

You can check out our InsideAdDemo App to see the InsideAdView in action.
