package com.streann.insidead

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class InsideAdView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private var mGoogleImaPlayer: GoogleImaPlayer? = null

    init {
        init()
    }

    private fun init() {
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)
    }

}