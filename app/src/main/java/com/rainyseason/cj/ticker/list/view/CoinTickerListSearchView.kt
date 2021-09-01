package com.rainyseason.cj.ticker.list.view

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
import com.rainyseason.cj.common.debounced
import com.rainyseason.cj.common.setTextIfDifferent
import timber.log.Timber

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinTickerListSearchView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.coin_ticker_list_search_view, this, true)
    }

    private val textInputLayout: TextInputLayout = findViewById(R.id.text_input_layout)
    private val editText: TextInputEditText =
        findViewById<TextInputEditText>(R.id.edit_text).apply {
            post {
                addTextChangedListener(intervalWatcher)
            }
        }


    @TextProp
    fun setHint(value: CharSequence) {
        textInputLayout.hint = value
    }

    @ModelProp
    fun setValue(value: String) {
        editText.debounced { editText.setTextIfDifferent(value) }
    }

    private var textChangeListener: ((String) -> Unit)? = null


    private var intervalWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable) {
            Timber.d("afterTextChanged: $s")
            textChangeListener?.invoke(s.toString())
        }
    }


    @CallbackProp
    fun setTextChangeListener(block: ((String) -> Unit)?) {
        textChangeListener = block
    }
}