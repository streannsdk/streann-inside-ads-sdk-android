# streann-inside-ads-sdk-android

Streann Inside Ad library is designed to incorporate playback functionality
seamlessly of diverse ad formats into your Android applications. One standout feature of this
library is its split-screen option, enabling you to effortlessly display ads side by side with your
content, allowing developers to create immersive and engaging user experiences. This library
supports the playback of VAST ads, playback of banner and full-screen native ads, various video
ad formats, and the presentation of image-based ads within your app's content.tag

## Features

- Effortlessly integrate ad playback within your Android app.
- Split Screen Option: Easily integrate a split-screen layout to display ads alongside your content.
- Elevate user engagement with native full-screen ads for an immersive advertising experience.
- Support VAST video ads through the Google IMA SDK, ensuring smooth ad delivery.
- Seamlessly integrate banner ads into your app's interface for enhanced monetization.
- Enjoy full compatibility with various video types, including MP4, m3u8, and more.
- Display image-based advertisements seamlessly within your app, enhancing visual appeal.
- **NEW**: Callback lifecycle management to prevent memory leaks
- **NEW**: Video player resize modes for fullscreen landscape ads
- **NEW**: Debug mode for troubleshooting skip/close button issues

## Documentation

📚 **For detailed integration guide with best practices, see [SDK_USAGE_GUIDE.md](SDK_USAGE_GUIDE.md)**

This comprehensive guide covers:
- ✅ Proper callback lifecycle management (prevent crashes and memory leaks)
- ✅ Video player sizing for fullscreen landscape ads
- ✅ Debug mode for troubleshooting
- ✅ Complete working examples
- ✅ Common issues and solutions

**Quick links:**
- [Skip/Close Button Behavior](SKIP_BUTTON_BEHAVIOR.md) - Why buttons appear inconsistently
- [Developer Guide](CLAUDE.md) - SDK architecture and implementation details

## Installation

To integrate the Streann Inside Ad library into your Android project, add the following dependency
to your app-level build.gradle file:

```gradle
dependencies {
    implementation 'com.github.streannsdk:streann-inside-ads-sdk-android:1.0.23'
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

- If you want to use banner or native ads add your Ad Manager app ID to your app's AndroidManifest.xml file.
    - To do so, add a <meta-data> tag with android:name="com.google.android.gms.ads.APPLICATION_ID".
    - For android:value, insert your own Ad Manager app ID.

      ```xml
      <manifest>
        <application>
            <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID"
                android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" tools:replace="android:value" />
        </application>
      </manifest>
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
        userView = yourContentView, ,
        parentView = findViewById(android.R.id.content),
        screen = "",
        isAdMuted = false,
        isInsideAdAbove = false,
        insideAdCallback = object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {}
    
            override fun insideAdLoaded() {}
    
            override fun insideAdPlay() {}
    
            override fun insideAdStop() {}
    
            override fun insideAdSkipped() {}
    
            override fun insideAdClicked() {}
    
            override fun insideAdError(error: String) {}
    
            override fun insideAdVolumeChanged(level: Int) {}
        })
    
    -userView - this parameter represents the content or view that the user wants to display in the split screen
    -screen - enter one of the following screens: Splash or Video Player
    -isAdMuted - choose if you want your ad to be muted or not(optional parameter, default value: false)
    -isInsideAdAbove - set to true if you want the ad above the content(optional parameter, default value: false)
    
    -InsideAdCallback - implement our interface to receive events from the ads ' progress
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
  ```

- In your activity or fragment, setup the Inside Ad Sdk callback:
    - InsideAdCallback - implement our interface to receive events from the ads' progress
    - By overriding the event listeners, handle ad events such as ads received, loaded, played,
      stopped, ad volume changed, ad errors, etc.

      ```kotlin
      InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
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
      ```

- Afterwards, request an ad:

  ```js
    insideAdView?.requestAd(
      screen = "",
      isAdMuted = true
    )
  
  - screen - enter one of the following screens: Splash or Video Player
  - isAdMuted - choose if you want your ad to be muted or not (optional parameter, default value: false)
    ```

- To stop the ad while it's playing:
  ```js
  insideAdView?.stopAd()
  ```

**Preroll Ad**

Preroll ads are ads that play before your main content starts (typically before a video player). Unlike regular ads, preroll ads:
- Display immediately with no delay
- Play only once (no automatic repetition)
- Are requested manually by the app when opening a player

To use preroll ads in your project, follow these steps:

- Add the InsideAdView to your layout XML file:
  ```xml
  <com.streann.insidead.InsideAdView
    android:id="@+id/prerollAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
  ```

- In your activity or fragment, initialize the InsideAdView:
  ```kotlin
  val prerollAdView = findViewById(R.id.prerollAdView)
  ```

- Setup the Preroll Ad callback (separate from regular ad callback):
  ```kotlin
  InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
    override fun insideAdReceived(insideAd: InsideAd) { }

    override fun insideAdLoaded() {
        // Call the InsideAdView playAd method to start playing the preroll ad
        prerollAdView?.playAd()
    }

    override fun insideAdPlay() { }

    override fun insideAdStop() {
        // Preroll completed, start your main content here
    }

    override fun insideAdSkipped() { }

    override fun insideAdClicked() { }

    override fun insideAdError(error: String) {
        // No preroll ad available, start your main content
    }

    override fun insideAdVolumeChanged(level: Int) { }
  })
  ```

- Request a preroll ad (call this once when opening your player):
  ```kotlin
  InsideAdSdk.requestPrerollAd(
    context = this,
    adContainer = prerollAdView,
    screen = "Video Player",
    isAdMuted = false,  // Optional, default: false
    targetingFilters = null  // Optional, for content targeting
  )

  - context - your activity or context
  - adContainer - the InsideAdView that will display the preroll ad
  - screen - enter one of the following screens: Splash or Video Player
  - isAdMuted - choose if you want your ad to be muted or not (optional parameter, default: false)
  - targetingFilters - optional targeting filters for content-specific ads
  ```

**Using Both Preroll and Regular Ads**

You can use preroll ads and regular ads together in the same screen:

1. Request and play the preroll ad when opening the player
2. After the preroll completes (in `insideAdStop()` callback), start your main content
3. Request regular ads during content playback using `requestAd()` for mid-roll ads

Regular ads requested with `requestAd()` will automatically exclude preroll ads and work with intervals as usual.

## Sample App

You can check out our InsideAdDemo App to see the InsideAdView and PrerollActivity in action.
