package com.rainyseason.cj.chat.history.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.chat.history.MessageEntry
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ChatMessageMeEntryViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class MessageMeEntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet), MessageViewView {
    private val binding = ChatMessageMeEntryViewBinding.inflate(inflater, this, true)
    override val content: TextView
        get() = binding.content

    @ModelProp
    override fun setText(text: String) {
        super.setText(text)
    }

    @ModelProp
    override fun setMessageEntry(messageEntry: MessageEntry) {
        super.setMessageEntry(messageEntry)
    }
}

interface MessageViewView {
    val content: TextView

    fun setText(text: String) {
        content.text = text
        content.requestLayout()
    }

    fun setMessageEntry(messageEntry: MessageEntry) {
    }
}
