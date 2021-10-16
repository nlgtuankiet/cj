package com.rainyseason.cj.common.contact

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.rainyseason.cj.R

class ContactFragment : Fragment(R.layout.fragment_contact) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FrlagmentContactBinding.bind(view)
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
    }
}
