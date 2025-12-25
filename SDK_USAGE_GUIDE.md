# InsideAdSdk - App Integration Guide

This guide shows how to properly integrate the InsideAdSdk with all recent improvements including callback lifecycle management, video player sizing, and debugging capabilities.

## Table of Contents

1. [Basic Setup](#basic-setup)
2. [Callback Lifecycle Management](#callback-lifecycle-management)
3. [Video Player Sizing](#video-player-sizing)
4. [Debug Mode](#debug-mode)
5. [Complete Examples](#complete-examples)
6. [Common Issues & Solutions](#common-issues--solutions)

---

## Basic Setup

### 1. Initialize the SDK

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            apiKey = "your-api-key",
            apiToken = "your-api-token",
            baseUrl = "https://your-ad-server.com",
            appDomain = "com.example.app",
            userBirthYear = 1990,
            userGender = "M"
        )
    }
}
```

### 2. Add InsideAdView to Layout

```xml
<com.streann.insidead.InsideAdView
    android:id="@+id/prerollAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone" />
```

---

## Callback Lifecycle Management

### Problem

Callbacks can fire after your Activity is destroyed, causing crashes or memory leaks.

### Solution

**Always clean up callbacks in `onDestroy()`:**

```kotlin
class VideoPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        setupPrerollAd()
    }

    private fun setupPrerollAd() {
        // Set callback
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.d(TAG, "Preroll ad received: ${insideAd.name}")
            }

            override fun insideAdLoaded() {
                Log.d(TAG, "Preroll ad loaded")
                binding.progressBar.visibility = View.GONE
                binding.prerollAdView.playAd()
            }

            override fun insideAdPlay() {
                Log.d(TAG, "Preroll ad playing")
            }

            override fun insideAdStop() {
                Log.d(TAG, "Preroll ad stopped - opening player")
                openVideoPlayer()
            }

            override fun insideAdSkipped() {
                Log.d(TAG, "Preroll ad skipped")
            }

            override fun insideAdClicked() {
                Log.d(TAG, "Preroll ad clicked")
            }

            override fun insideAdError(error: String) {
                Log.e(TAG, "Preroll ad error: $error")
                openVideoPlayer() // Continue to content on error
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.d(TAG, "Volume changed: $level")
            }
        })

        // Request ad
        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = binding.prerollAdView,
            screen = "Video Player",
            isAdMuted = true
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // CRITICAL: Clean up to prevent callbacks on destroyed activity
        InsideAdSdk.cancelPrerollAdRequest()  // Cancels ongoing request + removes callback
        InsideAdSdk.removeInsideAdCallback()   // Remove regular ad callback if used

        // Also cancel at view level
        binding.prerollAdView.cancelAdRequest()

        Log.d(TAG, "Callbacks cleaned up")
    }
}
```

### Available Cleanup Methods

| Method | When to Use | What It Does |
|--------|-------------|--------------|
| `cancelPrerollAdRequest()` | Activity `onDestroy()` | Cancels ongoing request, removes callback, stops handlers |
| `removePrerollAdCallback()` | When you want to stop receiving events | Only removes callback, doesn't cancel request |
| `removeInsideAdCallback()` | Activity `onDestroy()` | Removes regular ad callback |
| `cancelAdRequest()` (on view) | Activity `onDestroy()` | Cancels view-level handlers |

### Why This Is Important

**Without cleanup:**
```
09:03:49 - Activity destroyed
09:04:19 - Callback fires (30 seconds later!) ❌ CRASH
```

**With cleanup:**
```
09:03:49 - Activity destroyed
09:03:49 - Callbacks removed
09:04:19 - No callback fires ✅ Safe
```

---

## Video Player Sizing

### Problem

In landscape mode, video player doesn't fill the screen width - it's stuck on the left side with empty space.

### Solution

**Set resize mode BEFORE requesting ad:**

```kotlin
class PrerollActivity : AppCompatActivity() {

    private fun requestPrerollAd() {
        // IMPORTANT: Set resize mode BEFORE requesting ad
        binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)

        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = binding.prerollAdView,
            screen = "Video Player",
            isAdMuted = true
        )
    }
}
```

### Available Resize Modes

| Mode | Behavior | Best For |
|------|----------|----------|
| `FIT` | Maintains aspect ratio with letterboxing<br>Landscape: Uses half screen width | Split-screen ads (default) |
| `FILL` | Fills screen width, maintains aspect ratio | **Fullscreen landscape ads** (recommended) |
| `FIXED_WIDTH` | Uses full width, adjusts height | Landscape fullscreen ads |
| `FIXED_HEIGHT` | Uses full height, adjusts width | Portrait fullscreen ads |

### Examples

**Fullscreen Landscape Ads:**
```kotlin
binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)
```

**Dynamic Based on Orientation:**
```kotlin
private fun setResizeModeBasedOnOrientation() {
    val mode = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        InsideAdView.ResizeMode.FILL  // Fill width in landscape
    } else {
        InsideAdView.ResizeMode.FIT   // Default in portrait
    }
    binding.prerollAdView.setResizeMode(mode)
}

override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    setResizeModeBasedOnOrientation()
}
```

**Different Modes for Different Screens:**
```kotlin
when (screenName) {
    "Video Player" -> {
        // Fullscreen experience
        binding.adView.setResizeMode(InsideAdView.ResizeMode.FILL)
    }
    "Reels" -> {
        // Fill height for vertical video
        binding.adView.setResizeMode(InsideAdView.ResizeMode.FIXED_HEIGHT)
    }
    else -> {
        // Default behavior
        binding.adView.setResizeMode(InsideAdView.ResizeMode.FIT)
    }
}
```

---

## Debug Mode

### When to Use

Enable debug mode when:
- Skip button not appearing
- Close button not appearing
- Callbacks firing at wrong times
- Need to understand which ad type is playing

### How to Enable

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable debug mode (only in debug builds!)
        if (BuildConfig.DEBUG) {
            InsideAdSdk.debugMode = true
        }

        InsideAdSdk.initializeSdk(...)
    }
}
```

**Or enable per-Activity:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    InsideAdSdk.debugMode = true  // Enable

    // ... your code
}

override fun onDestroy() {
    super.onDestroy()

    InsideAdSdk.debugMode = false  // Disable
}
```

### What Debug Mode Shows

**1. Ad Type Identification:**
```
InsideAdSdk-InsideAdView: Showing VAST ad: Sample Ad | Skip Button Support: YES - Google IMA SDK controls skip button based on VAST XML skipoffset
```

**2. Skip Button Details (VAST ads):**
```
╔═══════════════════════════════════════════════════════════╗
║          SKIP BUTTON DEBUG - LOADED (VAST Ad Details)
╠═══════════════════════════════════════════════════════════╣
║ Ad Name: Sample VAST Ad
║ Ad Type: VAST
║ Is Skippable: true
║ Skip Offset: 5s
║ Ad Duration: 30.0s
║ Skip Button Note: Skip button will appear after 5.0 seconds
╚═══════════════════════════════════════════════════════════╝
```

**3. Close Button Details (Local ads):**
```
╔═══════════════════════════════════════════════════════════╗
║          SKIP BUTTON DEBUG - LOCAL AD STARTED
╠═══════════════════════════════════════════════════════════╣
║ Ad Type: LOCAL_VIDEO
║ Is Skippable: false
║ Skip Button Support: NOT SUPPORTED - Local ads only have close button
║ Close Button Delay: 5s
╚═══════════════════════════════════════════════════════════╝
```

**4. Close Button Events:**
```
InsideAdSdk-InsideAdPlayer: Close button created - Will appear after 5s
InsideAdSdk-InsideAdPlayer: Close button NOW VISIBLE after 5s
InsideAdSdk-InsideAdPlayer: Close button clicked
```

### Filtering Logcat

To see only SDK debug logs:

```bash
# All SDK logs
adb logcat | grep InsideAdSdk

# Only skip button logs
adb logcat | grep "InsideAdSdk-SkipButton"

# Only close button logs
adb logcat | grep "InsideAdSdk-InsideAdPlayer"

# Only VAST ad logs
adb logcat | grep "InsideAdSdk-GoogleIma"
```

---

## Complete Examples

### Example 1: Simple Preroll Ad

```kotlin
class SimplePrerollActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimplePrerollBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimplePrerollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Enable debug mode
        InsideAdSdk.debugMode = BuildConfig.DEBUG

        // Optional: Set resize mode for fullscreen
        binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)

        // Set callback
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                // Ad received from server
            }

            override fun insideAdLoaded() {
                // Hide progress bar, play ad
                binding.progressBar.visibility = View.GONE
                binding.prerollAdView.playAd()
            }

            override fun insideAdPlay() {
                // Ad started playing
            }

            override fun insideAdStop() {
                // Ad finished - open player
                openVideoPlayer()
            }

            override fun insideAdSkipped() {
                // User skipped (VAST ads only)
            }

            override fun insideAdClicked() {
                // User clicked ad
            }

            override fun insideAdError(error: String) {
                // Error - continue to player
                openVideoPlayer()
            }

            override fun insideAdVolumeChanged(level: Int) {
                // Volume changed
            }
        })

        // Request ad
        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = binding.prerollAdView,
            screen = "Video Player",
            isAdMuted = true
        )
    }

    private fun openVideoPlayer() {
        // Your video player logic
        binding.prerollAdView.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

        // IMPORTANT: Clean up
        InsideAdSdk.cancelPrerollAdRequest()
        binding.prerollAdView.cancelAdRequest()
    }
}
```

### Example 2: Preroll + Regular Ads

```kotlin
class FullAdFlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullAdFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullAdFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set resize mode for both ad views
        binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)
        binding.regularAdView.setResizeMode(InsideAdView.ResizeMode.FILL)

        setupPrerollAd()
        setupRegularAd()

        // Start with preroll
        requestPreroll()
    }

    private fun setupPrerollAd() {
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.d(TAG, "Preroll received: ${insideAd.name}")
            }

            override fun insideAdLoaded() {
                binding.prerollAdView.playAd()
            }

            override fun insideAdStop() {
                // Preroll done - show content and start regular ads
                binding.prerollAdView.visibility = View.GONE
                binding.contentView.visibility = View.VISIBLE
                requestRegularAd()
            }

            override fun insideAdError(error: String) {
                // Skip to content on error
                binding.prerollAdView.visibility = View.GONE
                binding.contentView.visibility = View.VISIBLE
                requestRegularAd()
            }

            // ... other callbacks
        })
    }

    private fun setupRegularAd() {
        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.d(TAG, "Regular ad received: ${insideAd.name}")
            }

            override fun insideAdLoaded() {
                binding.regularAdView.visibility = View.VISIBLE
                binding.regularAdView.playAd()
            }

            override fun insideAdStop() {
                // Hide ad, keep content playing
                binding.regularAdView.visibility = View.GONE
                // SDK will automatically show next ad after interval
            }

            override fun insideAdError(error: String) {
                binding.regularAdView.visibility = View.GONE
            }

            // ... other callbacks
        })
    }

    private fun requestPreroll() {
        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = binding.prerollAdView,
            screen = "Video Player",
            isAdMuted = true
        )
    }

    private fun requestRegularAd() {
        binding.regularAdView.requestAd(
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = TargetingFilters(
                vodId = "12345",
                channelId = null
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up both
        InsideAdSdk.cancelPrerollAdRequest()
        InsideAdSdk.removeInsideAdCallback()

        binding.prerollAdView.cancelAdRequest()
        binding.regularAdView.cancelAdRequest()
    }
}
```

### Example 3: With Debug Mode & Error Handling

```kotlin
class DebugPrerollActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugPrerollBinding
    private var adAttempts = 0
    private val maxAdAttempts = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugPrerollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable debug mode
        InsideAdSdk.debugMode = true

        // Set resize mode
        binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)

        setupPrerollWithRetry()
        requestPreroll()
    }

    private fun setupPrerollWithRetry() {
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.d(TAG, "✅ Ad received: ${insideAd.name} (type: ${insideAd.adType})")
                adAttempts = 0  // Reset on success
            }

            override fun insideAdLoaded() {
                Log.d(TAG, "✅ Ad loaded - playing")
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Playing ad..."
                binding.prerollAdView.playAd()
            }

            override fun insideAdPlay() {
                Log.d(TAG, "▶️ Ad playing")
            }

            override fun insideAdStop() {
                Log.d(TAG, "⏹️ Ad completed - opening player")
                openVideoPlayer()
            }

            override fun insideAdSkipped() {
                Log.d(TAG, "⏭️ Ad skipped by user")
                openVideoPlayer()
            }

            override fun insideAdError(error: String) {
                Log.e(TAG, "❌ Ad error: $error")

                adAttempts++
                if (adAttempts < maxAdAttempts) {
                    Log.d(TAG, "🔄 Retrying ad request ($adAttempts/$maxAdAttempts)")
                    binding.statusText.text = "Ad error, retrying..."

                    Handler(Looper.getMainLooper()).postDelayed({
                        requestPreroll()
                    }, 2000)
                } else {
                    Log.d(TAG, "⏭️ Max retries reached, skipping to player")
                    binding.statusText.text = "No ad available, continuing..."
                    openVideoPlayer()
                }
            }

            override fun insideAdClicked() {
                Log.d(TAG, "👆 Ad clicked")
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.d(TAG, "🔊 Volume: $level")
            }
        })
    }

    private fun requestPreroll() {
        binding.statusText.text = "Loading ad..."
        binding.progressBar.visibility = View.VISIBLE

        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = binding.prerollAdView,
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = TargetingFilters(
                vodId = intent.getStringExtra("vodId"),
                channelId = intent.getStringExtra("channelId")
            )
        )
    }

    private fun openVideoPlayer() {
        binding.prerollAdView.visibility = View.GONE
        binding.statusText.visibility = View.GONE
        binding.progressBar.visibility = View.GONE

        // Open your player
        startActivity(Intent(this, PlayerActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "🧹 Cleaning up callbacks")
        InsideAdSdk.cancelPrerollAdRequest()
        binding.prerollAdView.cancelAdRequest()
        InsideAdSdk.debugMode = false
    }
}
```

---

## Common Issues & Solutions

### Issue 1: Callbacks Fire After Activity Destroyed

**Symptoms:**
- Crashes after navigating away from ad screen
- Logs show callbacks 30+ seconds after activity destroyed

**Solution:**
```kotlin
override fun onDestroy() {
    super.onDestroy()
    InsideAdSdk.cancelPrerollAdRequest()  // ✅ Always call this
    binding.prerollAdView.cancelAdRequest()
}
```

### Issue 2: Video Doesn't Fill Screen in Landscape

**Symptoms:**
- Video stuck on left side in landscape
- Empty space on right side
- Overlay controls span full width but video doesn't

**Solution:**
```kotlin
// Set BEFORE requesting ad
binding.prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)
```

### Issue 3: Skip Button Doesn't Appear

**Symptoms:**
- Skip button never shows up
- Works sometimes but not always

**Root Cause:**
- VAST ads: Skip button depends on VAST XML having `skipoffset` attribute
- Local ads: Don't have skip buttons (only close buttons)

**Debug:**
```kotlin
InsideAdSdk.debugMode = true
// Check logs to see if ad is skippable
```

**Solution:**
1. For VAST ads: Ensure VAST XML has `<Linear skipoffset="00:00:05">`
2. For local ads: Accept that only close button is available
3. See `SKIP_BUTTON_BEHAVIOR.md` for details

### Issue 4: Close Button Doesn't Appear

**Symptoms:**
- Close button never shows on local video ads

**Common Causes:**
1. ProgressBar covering the button
2. `showCloseButtonAfterSeconds` not set in campaign

**Solution:**
```kotlin
override fun insideAdLoaded() {
    // IMPORTANT: Hide ProgressBar!
    binding.progressBar.visibility = View.GONE
    binding.prerollAdView.playAd()
}
```

**Debug:**
```kotlin
InsideAdSdk.debugMode = true
// Look for "Close button NOW VISIBLE" in logs
```

### Issue 5: Ad Doesn't Load

**Symptoms:**
- `insideAdReceived` never called
- Stuck on loading screen

**Solution:**
```kotlin
// Add timeout
Handler(Looper.getMainLooper()).postDelayed({
    if (/* ad not loaded */) {
        Log.e(TAG, "Ad timeout - continuing without ad")
        openVideoPlayer()
    }
}, 30000)  // 30 second timeout
```

### Issue 6: Memory Leaks

**Symptoms:**
- App memory grows over time
- Activity not garbage collected

**Solution:**
```kotlin
override fun onDestroy() {
    super.onDestroy()

    // Clean up everything
    InsideAdSdk.cancelPrerollAdRequest()
    InsideAdSdk.removeInsideAdCallback()
    binding.prerollAdView.cancelAdRequest()

    // Clear references
    binding.prerollAdView.visibility = View.GONE
}
```

---

## Best Practices

### ✅ DO:
- Always call `cancelPrerollAdRequest()` in `onDestroy()`
- Set resize mode BEFORE requesting ad
- Hide ProgressBar in `insideAdLoaded()` callback
- Enable debug mode during development
- Handle ad errors gracefully (continue to content)
- Use appropriate resize mode for your layout

### ❌ DON'T:
- Forget to clean up callbacks
- Set resize mode after ad is loaded
- Leave ProgressBar visible during ad playback
- Enable debug mode in production builds
- Block user experience if ad fails
- Assume all ads have skip buttons

---

## Quick Reference

### Cleanup Methods
```kotlin
InsideAdSdk.cancelPrerollAdRequest()  // Cancel preroll + remove callback
InsideAdSdk.removePrerollAdCallback()  // Just remove callback
InsideAdSdk.removeInsideAdCallback()   // Remove regular ad callback
view.cancelAdRequest()                 // Cancel view-level handlers
```

### Resize Modes
```kotlin
setResizeMode(InsideAdView.ResizeMode.FIT)          // Default, letterbox
setResizeMode(InsideAdView.ResizeMode.FILL)         // Fill width (recommended)
setResizeMode(InsideAdView.ResizeMode.FIXED_WIDTH)  // Full width
setResizeMode(InsideAdView.ResizeMode.FIXED_HEIGHT) // Full height
```

### Debug Mode
```kotlin
InsideAdSdk.debugMode = true   // Enable
InsideAdSdk.debugMode = false  // Disable
```

---

## Additional Resources

- **Skip Button Behavior**: See `SKIP_BUTTON_BEHAVIOR.md`
- **README**: See main `README.md`
- **Demo App**: Check `app/` module for working examples

---

## Support

If you encounter issues not covered here:
1. Enable `InsideAdSdk.debugMode = true`
2. Check logcat for detailed logs
3. Review `SKIP_BUTTON_BEHAVIOR.md` for skip/close button issues
4. Check demo app for working examples
