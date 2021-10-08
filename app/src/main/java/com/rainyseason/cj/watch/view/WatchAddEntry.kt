package com.rainyseason.cj.watch.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ViewWatchAddEntryBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WatchAddEntry @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    val binding = ViewWatchAddEntryBinding.inflate(inflater, this, true)

    @ModelProp
    fun setName(value: String) {
        binding.name.text = value
    }

    @ModelProp
    fun setSymbol(value: String) {
        binding.symbol.text = value
    }

    @ModelProp
    fun setIsAdded(value: Boolean?) {
        binding.buttonLoading.isGone = value != null
        when (value) {
            true -> {
                binding.addedButton.setText(R.string.watch_added)
                binding.addedButton.isInvisible = false
                binding.addButton.isInvisible = true
                binding.buttonLoading.isGone = true
            }
            false -> {
                binding.addedButton.isInvisible = true
                binding.addButton.isInvisible = false
                binding.buttonLoading.isGone = true
            }
            else -> {
                binding.addedButton.text = ""
                binding.addedButton.isInvisible = false
                binding.addButton.isInvisible = true
                binding.buttonLoading.isGone = false
            }
        }
    }

    @CallbackProp
    fun setOnAddClickListener(l: OnClickListener?) {
        binding.addButton.setOnClickListener(l)
        binding.addedButton.setOnClickListener(l)
    }
}
