# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Streann Inside Ad SDK for Android** - an advertising SDK library that integrates various ad formats (VAST, banner, native, video, image) into Android applications with unique split-screen capabilities.

- **Library Module**: `insidead/` - the main SDK library published to JitPack
- **Demo App**: `app/` - sample application demonstrating SDK usage
- **Package**: `com.streann.insidead`
- **Published As**: `com.github.streannsdk:streann-inside-ads-sdk-android`

## Build Commands

```bash
# Build the entire project
./gradlew build

# Build only the SDK library
./gradlew :insidead:build

# Build the demo app
./gradlew :app:build

# Assemble release AAR (for library publishing)
./gradlew :insidead:assembleRelease

# Run unit tests
./gradlew :insidead:test

# Run instrumented tests (requires connected device/emulator)
./gradlew :insidead:connectedAndroidTest

# Clean build artifacts
./gradlew clean
```

## Architecture

### Core Components

**InsideAdSdk (Singleton)**
- Entry point for SDK initialization via `initializeSdk()`
- Manages global configuration: API credentials, user targeting, geo-IP data
- Fetches campaigns from backend on initialization
- Stores campaigns list and provides callback interface
- Provides separate callback management for preroll ads via `setPrerollAdCallback()`
- Includes `requestPrerollAd()` public API for one-time preroll ad requests
- Uses parameter isolation mechanism to prevent interference between preroll and regular ads

**InsideAdView (Custom View)**
- Main ad display component that apps integrate into layouts
- Manages four different player implementations internally
- Handles two ad request flows:
  - `requestAd()` - Regular ads with delays and intervals (excludes PREROLL ads)
  - `requestPrerollAd()` - Internal method for preroll ads (immediate display, no repetition)
- Supports retry mechanism (max 3 retries) when campaigns aren't ready
- Manages ad lifecycle callbacks and intervals
- Implements parameter save/restore mechanism to isolate preroll and regular ad settings

**SplitInsideAdView (Custom View)**
- Specialized view for split-screen ad display
- Dynamically positions user content and ads side-by-side or stacked
- Handles orientation changes (portrait: vertical split, landscape: horizontal split)
- Wraps an `InsideAdView` internally

### Player Implementations

Located in `insidead/src/main/java/com/streann/insidead/players/`:

1. **GoogleImaPlayer** - VAST ads via Google IMA SDK
2. **InsideAdPlayer** - Local video/image ads (MP4, m3u8, images)
3. **BannerAdsPlayer** - Google Ad Manager banner ads
4. **NativeAdsPlayer** - Full-screen native ads

The SDK selects the appropriate player based on the `adType` field in the campaign data.

### Campaign Filtering Logic

The `CampaignsFilterUtil` implements sophisticated ad selection:

1. **Time Period Filtering** - Checks current time/day against campaign time windows
2. **Placement Filtering** - Matches requested screen ("Splash", "Video Player", "Reels") to placement tags AND viewType
   - **viewType Filtering** (NEW):
     - When `viewType = null` (regular ad request): Excludes placements with `viewType = "PREROLL"`
     - When `viewType = "PREROLL"` (preroll request): Only includes placements with `viewType = "PREROLL"`
     - Ensures complete separation between preroll and regular ad flows
3. **Content Targeting** - Filters by VOD ID, channel ID, series ID, category IDs, content provider
4. **Weight-Based Selection** - Randomly selects from multiple eligible campaigns/ads using weighted probability

Flow: All Campaigns → Time Period Filter → Placement Filter (screen tags + viewType) → Content Targeting Filter → Weight Selection → Return InsideAd

### Data Models

Key models in `insidead/src/main/java/com/streann/insidead/models/`:

- **Campaign** - Contains placements, targeting rules, time periods, weight
- **Placement** - Groups ads with specific tags (screens), viewType, and properties
- **InsideAd** - Individual ad with type, URL, fallback, properties
- **TargetingFilters** - Client-side targeting (vodId, channelId, categoryIds, etc.)
- **Targeting** - Server-side campaign targeting configuration

Key enums in `insidead/src/main/java/com/streann/insidead/utils/enums/`:

- **ViewType** - Placement slots reserved for a dedicated ad request:
  - `PREROLL` - Ads shown before content starts
  - `MULTIVIEW_CANVAS` - Ads shown over the multiview player grid
  - `MULTIVIEW_RIGHT_BAR` - Ads shown in the multiview streams selector

  Matching is case/separator-insensitive via `ViewType.fromRaw()`, so backend casing drift cannot
  break delivery. `ViewType.DEDICATED_SLOTS` is the set that regular `requestAd()` calls must never
  serve - any new slot MUST be added there or it leaks into regular ad traffic.

### Ad Request Flow

**Regular Ad Flow (requestAd)**
```
1. App calls InsideAdSdk.initializeSdk() → fetches campaigns from backend
2. App calls InsideAdView.requestAd(screen, isAdMuted, targetingFilters)
3. InsideAdView waits for campaigns (retry mechanism)
4. CampaignsFilterUtil.getInsideAd(campaigns, screen, viewType=null) filters and selects ad
   - Automatically excludes PREROLL ads
5. Callback: insideAdReceived(insideAd)
6. Show ad via appropriate player after startAfterSeconds delay
7. Callback: insideAdLoaded() → app calls playAd()
8. Ad plays with callbacks (play, stop, skip, click, error, volumeChanged)
9. After ad stops, schedule next ad based on intervalInMinutes
```

**Preroll Ad Flow (requestPrerollAd)** (NEW)
```
1. App calls InsideAdSdk.initializeSdk() → fetches campaigns from backend
2. App calls InsideAdSdk.requestPrerollAd(context, adContainer, screen, isAdMuted, targetingFilters)
3. SDK saves current global parameters (isAdMuted, targetingFilters) → sets preroll parameters
4. InsideAdView.requestPrerollAd() waits for campaigns (retry mechanism)
5. CampaignsFilterUtil.getInsideAd(campaigns, screen, viewType="PREROLL") filters and selects ad
   - Only includes PREROLL ads
6. Callback: prerollAdCallback.insideAdReceived(insideAd)
7. Show ad immediately (no startAfterSeconds delay)
8. Callback: prerollAdCallback.insideAdLoaded() → app calls playAd()
9. Ad plays with callbacks (play, stop, skip, click, error, volumeChanged)
10. After ad stops/errors, restore original global parameters
11. No automatic repetition (intervalInMinutes ignored)
```

**Parameter Isolation Mechanism**
- When preroll is requested, current `isAdMuted` and `targetingFilters` are saved to `savedIsAdMuted` and `savedTargetingFilters`
- Preroll-specific values are set to global parameters temporarily
- All players and filtering logic read from the same global parameters (no code duplication)
- On completion/error, original values are restored via `restoreRegularAdParameters()`
- Ensures preroll requests don't interfere with regular ad requests

## SDK Configuration Properties

Campaigns include properties that control behavior:
- `intervalInMinutes` - Time between ad displays
- `startAfterSeconds` - Delay before showing ad
- `showCloseButtonAfterSeconds` - When close button appears
- `durationInSeconds` - Ad duration
- `intervalForReels` - Special interval for Reels screen

## Testing Notes

- Unit tests are minimal (example test in `insidead/src/test/`)
- Instrumented tests in `insidead/src/androidTest/`
- Testing requires valid API credentials in SDK initialization
- Demo app in `app/` module shows integration examples:
  - **MainActivity** - Basic regular ad flow with intervals
  - **PrerollActivity** (NEW) - Complete preroll + regular ad flow test:
    1. Preroll ad plays immediately on activity start
    2. After preroll completes, simulated content appears
    3. Regular ad is requested automatically (with delays and intervals)
    4. Demonstrates parameter isolation between preroll and regular ads
  - **SplitActivity** - Split-screen ad display

## Important SDK Requirements

From README.md:
- **Minimum SDK**: 23 (Android 6.0). `play-services-ads` sets this floor; it cannot go lower.
  `java.time` is used by the campaign models, so the library enables core library desugaring -
  **consuming apps below minSdk 26 must enable it too**:
  ```gradle
  compileOptions { coreLibraryDesugaringEnabled true }
  dependencies { coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.2.2' }
  ```
- **Target SDK**: 34
- **Required**: JitPack repository in `settings.gradle`
- **For Banner/Native Ads**: Must add Google Ad Manager App ID to AndroidManifest.xml

## Development Notes

- SDK uses Kotlin 2.1.0, AGP 7.3.1, Java 8 compatibility
- Dependencies: Google IMA SDK 3.33.0, Google Play Services Ads 24.3.0
- Uses `SharedPreferences` to store advertising ID and tracking settings
- Async operations via `ExecutorService` for API calls and ad ID retrieval
- UI operations use `Handler` with `Looper.getMainLooper()`
- Published via maven-publish plugin to JitPack

## Preroll Ad Implementation Files (NEW)

Key files modified/created for preroll ad support:

1. **insidead/src/main/java/com/streann/insidead/utils/enums/ViewType.kt**
   - Enum defining placement viewType values (PREROLL, MULTIVIEW_CANVAS, MULTIVIEW_RIGHT_BAR)

2. **insidead/src/main/java/com/streann/insidead/InsideAdSdk.kt** (MODIFIED)
   - Added `prerollAdCallback`, `isPrerollMode` flags
   - Added `savedIsAdMuted`, `savedTargetingFilters` for parameter isolation
   - Added `setPrerollAdCallback()`, `getPrerollAdCallback()` methods
   - Added public `requestPrerollAd()` API method

3. **insidead/src/main/java/com/streann/insidead/InsideAdView.kt** (MODIFIED)
   - Added internal `requestPrerollAd()` method
   - Added `restoreRegularAdParameters()` helper for parameter cleanup
   - Modified `getInsideAd()` to pass viewType parameter
   - Modified `showAd()` to bypass delays for preroll (sets delayMillis = 0)
   - Modified `insideAdStopped()`, `insideAdError()` to restore parameters
   - All exit points call `restoreRegularAdParameters()` when in preroll mode

4. **insidead/src/main/java/com/streann/insidead/utils/CampaignsFilterUtil.kt** (MODIFIED)
   - Updated 7 method signatures to accept optional `viewType` parameter
   - Modified `getFilteredPlacements()` to filter by both screen tags AND viewType:
     - `viewType = null`: Excludes PREROLL placements (regular ads)
     - `viewType = "PREROLL"`: Only includes PREROLL placements
   - Ensures backward compatibility (existing requestAd calls work unchanged)

5. **app/src/main/java/com/streann/insidead/demo/PrerollActivity.kt** (NEW)
   - Demo activity showing complete preroll → content → regular ad flow
   - Separate callbacks for preroll and regular ads with distinct logging
   - Two InsideAdView instances to demonstrate parameter isolation

6. **app/src/main/res/layout/activity_preroll.xml** (MODIFIED)
   - Added second InsideAdView for regular ads during content playback

## Multiview Ad Slots

Two placement slots serve the Multiview SDK, and unlike preroll they can be **on screen at the same
time**:

| ViewType | Renders in |
|---|---|
| `MULTIVIEW_CANVAS` | the max-4-player grid |
| `MULTIVIEW_RIGHT_BAR` | the streams selector |

```kotlin
InsideAdSdk.setAdCallback(ViewType.MULTIVIEW_CANVAS, callback)
InsideAdSdk.requestMultiviewCanvasAd(adContainer = canvasAdView, screen = "", isAdMuted = true)
InsideAdSdk.requestMultiviewRightBarAd(adContainer = rightBarAdView, screen = "", isAdMuted = false)
// in onDestroy()
InsideAdSdk.cancelAllDedicatedAdRequests()
```

Behaviour: repeats on the placement's `intervalInMinutes` (like a regular ad, unlike preroll),
honours `startAfterSeconds`, supports all five `AdType`s, and supports fallback ads.

`InsideAdView.setAdMuted(muted)` mutes or unmutes the ad that is currently playing. `isAdMuted` is
fixed when the ad is requested, so this is the only way to hand audio over mid-ad - the multiview
grid uses it when the user unmutes a stream while an ad is running. It dispatches to the player for
the current `adType`; banner and native have no audio control and ignore it.

Use `InsideAdView.ResizeMode.MATCH_CONTAINER` for these slots - every other mode sizes against the
screen, which is wrong for an ad embedded in a panel.

**`screen` must match the placement's tags.** Placements with no `tags` (the current multiview
inventory) match **only** an empty `screen` string.

### Per-request state

`AdRequestContext` holds everything that belongs to one request (mute state, targeting, timings,
geometry). It exists because `InsideAdSdk` used to keep this as global mutable state, which allowed
only one ad in flight - preroll worked around it by saving and restoring the globals, which does not
generalise to two concurrent slots.

Players read `requestContext?.X ?: InsideAdSdk.X`, so the globals survive as deprecated mirrors of
the most recent request and any unmigrated path behaves exactly as before.

`CampaignsFilterUtil.selectAd()` is the pure selection entry point and returns the chosen ad plus
its timings. The older `getInsideAd()` is deprecated: it still writes the globals as a side effect.
