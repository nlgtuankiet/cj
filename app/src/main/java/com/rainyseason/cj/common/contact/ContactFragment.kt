package com.rainyseason.cj.common.contact

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.FragmentContactBinding
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.tracking.logScreenEnter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactFragment : Fragment(R.layout.fragment_contact) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentContactBinding.bind(view)
        val tracker = view.context.coreComponent.tracker
        binding.openTelegram.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("tg://resolve?domain=bwpapp")
                startActivity(intent)
                tracker.logClick(
                    screenName = SCREEN_NAME,
                    target = "open_telegram"
                )
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
                tracker.logClick(
                    screenName = SCREEN_NAME,
                    target = "open_reddit"
                )
            } catch (ex: ActivityNotFoundException) {
            }
        }

        binding.openMail.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val appHash = requireContext().coreComponent.commonRepository.getAppHash()
                withContext(Dispatchers.Main) {
                    try {
                        val email = "rainyseasonstudio@gmail.com"
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("mailto:")
                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Hello!")
                        intent.putExtra(Intent.EXTRA_TEXT, "App hash: $appHash \n")
                        startActivity(intent)
                        tracker.logClick(
                            screenName = SCREEN_NAME,
                            target = "open_email"
                        )
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        requireContext().coreComponent.tracker
            .logScreenEnter("contact")
        setupSystemWindows()
    }

    companion object {
        const val SCREEN_NAME = "contact"
    }
}
