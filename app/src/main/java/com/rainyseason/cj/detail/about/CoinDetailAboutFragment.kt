package com.rainyseason.cj.detail.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.rainyseason.cj.R
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.setOnClickToNavigateBack
import com.rainyseason.cj.databinding.CoinDetailAboutFragmentBinding

class CoinDetailAboutFragment : Fragment(R.layout.coin_detail_about_fragment) {
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArgs<CoinDetailAboutArgs>()
        val binding = CoinDetailAboutFragmentBinding.bind(view)
        binding.title.text = "About ${args.coinName}"
        binding.content.text = HtmlCompat.fromHtml(args.content, HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.content.movementMethod = LinkMovementMethod.getInstance()
        binding.backButton.setOnClickToNavigateBack()
    }
}