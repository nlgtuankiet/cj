package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import com.airbnb.epoxy.Carousel
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper

class SnapCarousel @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : Carousel(context, attributeSet) {

    // call this only once
    fun setSnap(gravity: Int) {
        GravitySnapHelper(gravity).attachToRecyclerView(this)
    }

    override fun getSnapHelperFactory(): SnapHelperFactory? {
        return null
    }
}
