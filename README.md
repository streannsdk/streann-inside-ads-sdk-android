# streann-inside-ads-sdk-android

Streann Inside Ad library is designed to incorporate playback functionality
seamlessly of diverse ad formats into your Android applications. One standout feature of this
library is its split-screen option, enabling you to effortlessly display ads side by side with your
content which allows developers to create immersive and engaging user experiences. This library
supports the playback of VAST ads using the Interactive Media Ads (IMA) SDK by Google, along with
various video ad formats and the presentation of image-based ads.

## Features

- Effortless integration of ad playback within your Android app.
- Split Screen Option: Easily integrate a split-screen layout to display ads alongside your content.
- Seamless support for VAST video ads through the Google IMA SDK.
- Full compatibility with different video types, including MP4, m3u8, etc.
- Capability to seamlessly display image-based advertisements.

## Installation

To integrate the Streann Inside Ad library into your Android project, add the following dependency
to your app-level build.gradle file:

```gradle
dependencies {
    implementation 'com.github.streannsdk:streann-inside-ads-sdk-android:1.0.7
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

To use the Streann Inside Ad library in your project, follow these steps:

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
            appDomain = "app_domain",
            siteUrl = "site_url",
            storeUrl = "store_url",
            descriptionUrl = "description_url",
            userBirthYear = 0,
            userGender = "user_gender"
        )
   ```

**Split Screen View**

To use the SplitInsideAdView in your project, follow these steps:

- Create a SplitInsideAdView instance:

```js
  val splitInsideAdView = SplitInsideAdView(context)
```

- Show split screen with your content and ad view:

```kotlin 
splitInsideAdView.showSplitScreen(
    userView = yourContentView,,
    parentView = findViewById(android.R.id.content),
    screen = "",
    isAdMuted = false,
    isInsideAdAbove = false,
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
    
    - userView - this parameter represents the content or view that the user wants to display in the split screen
    - screen - enter one of the following screens: Splash, Login, Register, Video Player or Main
    - isAdMuted - choose if you want your ad to be muted or not (optional parameter, default value: false)
    - isInsideAdAbove - set to true if you want the ad above the content (optional  parameter, default value: false)
    - InsideAdCallback - implement our interface to receive events from the ads' progress
```

**Inside Ad View**

To use InsideAdView in your project, follow these steps:

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
  insideAdView?.requestAd(
    screen = "",
    isAdMuted = true,
    insideAdCallback = object : InsideAdCallback {
        override fun insideAdReceived(insideAd: InsideAd) { }

        override fun insideAdLoaded() {
            // Call the InsideAdView playAd method to start playing the ad
            mInsideAdView?.playAd()
        }

        override fun insideAdPlay() { }

        override fun insideAdStop() { }

        override fun insideAdSkipped() { }

        override fun insideAdClicked() { }

        override fun insideAdError(error: String) { }

        override fun insideAdVolumeChanged(level: Int) { }
      })
  
   - screen - enter one of the following screens: Splash, Login, Register, Video Player or Main
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
