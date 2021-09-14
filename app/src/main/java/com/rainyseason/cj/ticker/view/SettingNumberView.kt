package com.rainyseason.cj.ticker.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rainyseason.cj.R

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SettingNumberView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.setting_number_view, this, true)
    }

    private val textInputLayout: TextInputLayout = findViewById(R.id.text_input_layout)
    private val editText: TextInputEditText = findViewById(R.id.edit_text)


    @TextProp
    fun setHint(value: CharSequence) {
        textInputLayout.hint = value
    }

    @ModelProp
    fun setValue(value: String) {
        if (editText.text?.toString() != value) {
            editText.setText(value)
        }
    }

    private var watcher: TextWatcher? = null

    @CallbackProp
    fun setTextChangeListener(block: ((String) -> Unit)?) {
        if (block == null) {
            watcher?.let { editText.removeTextChangedListener(block) }
        } else {
            val newWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    s?.let { block.invoke(it.toString()) }
                }
            }
            watcher = newWatcher
            editText.addTextChangedListener(newWatcher)
        }
    }
}