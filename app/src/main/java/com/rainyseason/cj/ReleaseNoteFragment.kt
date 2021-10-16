package com.rainyseason.cj

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.databinding.FragmentReleaseNoteBinding
import com.rainyseason.cj.tracking.logScreenEnter
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

class ReleaseNoteFragment : Fragment(R.layout.fragment_release_note) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentReleaseNoteBinding.bind(view)

        val content = resources.openRawResource(R.raw.release_note).source().buffer().readUtf8()
        val markwon = Markwon.builder(requireContext())
            .build()

        markwon.setMarkdown(binding.content, content)

        lifecycleScope.launch(Dispatchers.IO) {
            requireContext().coreComponent.commonRepository
                .setReadReleaseNote()
        }

        requireContext().coreComponent.tracker
            .logScreenEnter("release_note")
    }
}
