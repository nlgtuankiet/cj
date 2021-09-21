package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.google.android.material.slider.Slider
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SettingSliderView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    init {
        inflateAndAdd(R.layout.view_setting_slider)
    }

    private val title = findViewById<TextView>(R.id.title)
    private val slider = findViewById<Slider>(R.id.slider)

    @TextProp
    fun setTitle(value: CharSequence?) {
        title.text = value
    }

    @ModelProp
    fun setValueFrom(value: Int) {
        slider.valueFrom = value.toFloat()
    }

    @ModelProp
    fun setValueTo(value: Int) {
        slider.valueTo = value.toFloat()
    }

    @ModelProp
    fun setStepSize(value: Int) {
        slider.stepSize = value.toFloat()
    }

    @ModelProp
    fun setValue(value: Int) {
        slider.value = value.toFloat()
    }

    @CallbackProp
    fun setOnChangeListener(l: ((newValue: Int) -> Unit)?) {
        if (l != null) {
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    l.invoke(value.toInt())
                }
            }
        } else {
            slider.clearOnChangeListeners()
        }
    }

}