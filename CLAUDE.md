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

**InsideAdView (Custom View)**
- Main ad display component that apps integrate into layouts
- Manages four different player implementations internally
- Handles ad request flow: `requestAd()` → campaigns filtering → player selection → `playAd()`
- Supports retry mechanism (max 3 retries) when campaigns aren't ready
- Manages ad lifecycle callbacks and intervals

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
2. **Placement Filtering** - Matches requested screen ("Splash", "Video Player", "Reels") to placement tags
3. **Content Targeting** - Filters by VOD ID, channel ID, series ID, category IDs, content provider
4. **Weight-Based Selection** - Randomly selects from multiple eligible campaigns/ads using weighted probability

Flow: All Campaigns → Time Period Filter → Placement Filter → Content Targeting Filter → Weight Selection → Return InsideAd

### Data Models

Key models in `insidead/src/main/java/com/streann/insidead/models/`:

- **Campaign** - Contains placements, targeting rules, time periods, weight
- **Placement** - Groups ads with specific tags (screens) and properties
- **InsideAd** - Individual ad with type, URL, fallback, properties
- **TargetingFilters** - Client-side targeting (vodId, channelId, categoryIds, etc.)
- **Targeting** - Server-side campaign targeting configuration

### Ad Request Flow

```
1. App calls InsideAdSdk.initializeSdk() → fetches campaigns from backend
2. App calls InsideAdView.requestAd(screen, isAdMuted, targetingFilters)
3. InsideAdView waits for campaigns (retry mechanism)
4. CampaignsFilterUtil.getInsideAd() filters and selects ad
5. Callback: insideAdReceived(insideAd)
6. Show ad via appropriate player after startAfterSeconds delay
7. Callback: insideAdLoaded() → app calls playAd()
8. Ad plays with callbacks (play, stop, skip, click, error, volumeChanged)
9. After ad stops, schedule next ad based on intervalInMinutes
```

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
- Demo app in `app/` module shows integration examples

## Important SDK Requirements

From README.md:
- **Minimum SDK**: 26 (Android 8.0)
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
