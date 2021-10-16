package com.rainyseason.cj

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.rainyseason.cj.databinding.FragmentReleaseNoteBinding
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.image.ImageSize
import io.noties.markwon.image.ImageSizeResolverDef
import io.noties.markwon.image.glide.GlideImagesPlugin
import okio.buffer
import okio.source

class ReleaseNoteFragment : Fragment(R.layout.fragment_release_note) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentReleaseNoteBinding.bind(view)

        val content = resources.openRawResource(R.raw.release_note).source().buffer().readUtf8()
        val markwon = Markwon.builder(requireContext())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.imageSizeResolver(ImageSizeResolver(requireContext()))
                }
            })
            .usePlugin(GlideImagesPlugin.create(GlideApp.with(binding.content)))
            .build()

        markwon.setMarkdown(binding.content, content)
    }

    class ImageSizeResolver(val context: Context) : ImageSizeResolverDef() {
        private val density: Float by lazy(LazyThreadSafetyMode.NONE) {
            context.resources.displayMetrics.density
        }

        override fun resolveAbsolute(
            dimension: ImageSize.Dimension,
            original: Int,
            textSize: Float
        ): Int {
            if (dimension.unit == null) {
                return (dimension.value).toInt()
            }
            return super.resolveAbsolute(dimension, original, textSize)
        }
    }
}