package com.rainyseason.cj.common.contact

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.databinding.FragmentContactBinding
import com.rainyseason.cj.tracking.logScreenEnter

class ContactFragment : Fragment(R.layout.fragment_contact) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentContactBinding.bind(view)
        binding.openTelegram.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("tg://resolve?domain=bwpapp")
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://t.me/bwpapp")
                startActivity(intent)
            }
        }

        binding.openReddit.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://www.reddit.com/r/bwp/")
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {

            }
        }

        binding.openMail.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:")
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("nlg.tuan.kiet@gmail.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hello!")
                startActivity(intent)
            } catch (ignored: Exception) {

            }

        }

        requireContext().coreComponent.tracker
            .logScreenEnter("contact")
    }
}
