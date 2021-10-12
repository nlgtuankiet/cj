package com.rainyseason.cj.widget.watch

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.rainyseason.cj.R
import com.rainyseason.cj.databinding.WatchPreviewFragmentBinding

class WatchPreviewFragment : Fragment(R.layout.watch_preview_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = WatchPreviewFragmentBinding.bind(view)
    }
}