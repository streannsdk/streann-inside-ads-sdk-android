# streann-inside-ad-sdk-android

Streann Inside Ad is an Android library that provides a simple way to integrate ad playback functionality into your Android applications. With Streann Inside Ad library, you can seamlessly incorporate ads into your projects and monetize your content.

## Features

- Easily integrate ad playback into your Android app.
- Support vast ad formats.
- Customizable ad display options.

## Installation

To integrate AdPlayerView into your Android project, add the following dependency to your app-level build.gradle file:

```gradle
dependencies {
    implementation 'com.example:adplayerview:1.0.0'
}
```

In order to use our library you need to add the JitPack maven repository to the list of repositories in your settings.gradle file:

```
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

To use InsideAdView in your project, follow these steps:
- Implement the initializeSdk method in your main Application class in order to initialize our SDK:
  ```
  InsideAdSdk.initializeSdk(
            apiKey = "api_key",
            baseUrl = "https://inside-ads.services.c1.streann.com/")
  ```
  
   
   - The apiKey and baseUrl are mandatory parameters in order to initialize our SDK.
        You could also implement the optional parameters: appDomain, siteUrl, storeUrl, descriptionUrl, userBirthYear and userGender. Ex:
       ```
           InsideAdSdk.initializeSdk(
                  apiKey = "559ff7ade4b0d0aff40888dd",
                  baseUrl = "https://inside-ads.services.c1.streann.com/",
                  userGender = "Female"
              )
       ```
   
- Add the InsideAdView to your layout XML file and customize its size (width, height) as needed:
  ```
  <com.streann.insidead.InsideAdView
        android:id="@+id/insideAdView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
- In your activity or fragment, initialize the InsideAdView:

  ```val insideAdView = findViewById(R.id.insideAdView);```

- In your activity or fragment, request an ad:
  
```
   mInsideAdView?.requestAd("screen", object : InsideAdCallback {
        override fun insideAdReceived(insideAd: InsideAd) { }

        override fun insideAdLoaded() { }

        override fun insideAdPlay() { }

        override fun insideAdStop() { }

        override fun insideAdError(error: String) { }

        override fun insideAdVolumeChanged(level: Float) { }
    })

 - screen - enter the screen where you wish to load the ad 
    ex. Login, Splash screen, etc.
 - InsideAdCallback - implement our callback in order to receive events from the ads' progress
```

- Handle ad events such as ads received, loaded, played, stopped, ad volume changed, and ad errors by overriding the event listeners.

## Sample App
Check out our InsideAdDemo App to see the InsideAdView in action.
