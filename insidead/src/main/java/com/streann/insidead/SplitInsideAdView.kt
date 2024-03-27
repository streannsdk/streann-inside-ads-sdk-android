package com.streann.insidead

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.Helper

@SuppressLint("ViewConstructor")
class SplitInsideAdView(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "InsideAdSdk.SplitView"

    private var mInsideAdView: InsideAdView? = null
    private var insideAdCallback: InsideAdCallback? = null

    private var userViewContainer: RelativeLayout? = null

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.split_ad_player, this)
    }

    fun showSplitScreen(
        userView: View,
        parentView: ViewGroup,
        screen: String,
        isAdMuted: Boolean? = false,
        isInsideAdAbove: Boolean? = false,
        insideAdCallback: InsideAdCallback
    ) {
        removeAllViews()
        userViewContainer = null
        Helper.setBannerAdHeight(null)

        this.insideAdCallback = insideAdCallback
        setupInsideAdView(userView, parentView, screen, isAdMuted, isInsideAdAbove)
    }

    private fun setupInsideAdView(
        userView: View,
        parentView: ViewGroup,
        screen: String,
        isAdMuted: Boolean? = false,
        isInsideAdAbove: Boolean? = false
    ) {
        mInsideAdView = InsideAdView(context)
        mInsideAdView?.visibility = View.GONE
        requestAd(userView, parentView, screen, isAdMuted, isInsideAdAbove)
    }

    private fun requestAd(
        userView: View,
        parentView: ViewGroup,
        screen: String, isAdMuted: Boolean? = false, isInsideAdAbove: Boolean? = false
    ) {
        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "insideAdReceived: $insideAd")
                insideAdCallback?.insideAdReceived(insideAd)
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "insideAdLoaded")
                insideAdCallback?.insideAdLoaded()
                mInsideAdView?.visibility = View.VISIBLE
                mInsideAdView?.playAd()

                val isLandscape =
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    showSplitScreenLandscape(userView, parentView, isInsideAdAbove ?: false)
                } else {
                    showSplitScreenPortrait(userView, parentView, isInsideAdAbove ?: false)
                }
            }

            override fun insideAdPlay() {
                Log.i(TAG, "insideAdPlay")
                insideAdCallback?.insideAdPlay()
            }

            override fun insideAdStop() {
                Log.i(TAG, "insideAdStop")
                insideAdCallback?.insideAdStop()

                mInsideAdView?.visibility = View.GONE

                val userViewParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )

                userView.layoutParams = userViewParams
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "insideAdSkipped")
                insideAdCallback?.insideAdSkipped()
                mInsideAdView?.stopAd()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "insideAdClicked")
                insideAdCallback?.insideAdClicked()
            }

            override fun insideAdError(error: String) {
                Log.i(TAG, "insideAdError: $error")
                insideAdCallback?.insideAdError(error)
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "insideAdVolumeChanged: $level")
                insideAdCallback?.insideAdVolumeChanged(level)
            }
        })

        mInsideAdView?.requestAd(
            screen = screen,
            isAdMuted = isAdMuted
        )
    }

    private fun showSplitScreenPortrait(
        userView: View,
        parentView: ViewGroup,
        isInsideAdAbove: Boolean
    ) {
        this.userViewContainer = RelativeLayout(context)

        val userViewParent = userView.parent as? ViewGroup
        userViewParent?.removeView(userView)

        val insideAdViewParent = mInsideAdView?.parent as? ViewGroup
        insideAdViewParent?.removeView(mInsideAdView)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = if (context is Activity) {
            Helper.getScreenHeight(context as Activity, resources)
        } else {
            resources.displayMetrics.heightPixels
        }
        val adHeight = Helper.getBannerAdHeight(context) ?: ((screenWidth * 9) / 16)

        val insideAdParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            adHeight
        )

        val userViewHeight = screenHeight - adHeight
        val userViewParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            userViewHeight
        )

        if (isInsideAdAbove) {
            insideAdParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            userViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        } else {
            insideAdParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            userViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }

        userViewContainer?.addView(userView, userViewParams)
        userViewContainer?.addView(mInsideAdView, insideAdParams)
        addView(userViewContainer)

        parentView.removeView(this)
        parentView.addView(this)
    }

    private fun showSplitScreenLandscape(
        userView: View,
        parentView: ViewGroup,
        isInsideAdAbove: Boolean
    ) {
        this.userViewContainer = RelativeLayout(context)

        val userViewParent = userView.parent as? ViewGroup
        userViewParent?.removeView(userView)

        val insideAdViewParent = mInsideAdView?.parent as? ViewGroup
        insideAdViewParent?.removeView(mInsideAdView)

        val screenWidth = resources.displayMetrics.widthPixels
        val adWidth = screenWidth / 2
        val adHeight = (adWidth * 9 / 16)

        val insideAdParams = RelativeLayout.LayoutParams(
            adWidth,
            adHeight
        )

        insideAdParams.addRule(RelativeLayout.CENTER_IN_PARENT)

        val userViewParams = RelativeLayout.LayoutParams(
            adWidth,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        if (isInsideAdAbove) {
            insideAdParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            userViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        } else {
            insideAdParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            userViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        }

        userViewContainer?.addView(userView, userViewParams)
        userViewContainer?.addView(mInsideAdView, insideAdParams)
        addView(userViewContainer)

        parentView.removeView(this)
        parentView.addView(this)
    }

}