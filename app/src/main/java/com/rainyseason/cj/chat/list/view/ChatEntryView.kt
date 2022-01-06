package com.rainyseason.cj.chat.list.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isGone
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ChatListEntryViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class ChatEntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    private val binding = ChatListEntryViewBinding.inflate(inflater, this, true)

    @ModelProp
    fun setTitle(text: String) {
        binding.title.text = text
    }

    @ModelProp
    fun setContent(text: String) {
        binding.text.text = text
    }

    @ModelProp
    fun isSeen(seen: Boolean) {
        binding.readIndicator.isGone = seen
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}
