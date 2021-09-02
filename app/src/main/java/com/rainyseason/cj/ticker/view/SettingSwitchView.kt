package com.rainyseason.cj.ticker.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SettingSwitchView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val title: TextView by lazy { findViewById(R.id.title) }
    private val checkBox: SwitchMaterial by lazy { findViewById(R.id.check_box) }
    private val clickOverlay: View by lazy { findViewById(R.id.click_overlay) }

    init {
        inflateAndAdd(R.layout.setting_switch_view)
    }

    @TextProp
    fun setTitle(value: CharSequence) {
        title.text = value
    }

    @ModelProp
    fun setChecked(value: Boolean) {
        checkBox.isChecked = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        clickOverlay.setOnClickListener(l)
    }
}