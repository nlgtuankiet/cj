package com.rainyseason.cj.chat.admin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

@Module
interface ChatAdminActivityModule {
    @ContributesAndroidInjector
    fun activity(): ChatAdminActivity
}

// admin user id: uJIQWCKAsLMf3AlY1bTjVhmJlVM2
class ChatAdminActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_admin_activity)
        firebaseAuth.addAuthStateListener {
            Handler(Looper.getMainLooper()).post {
                invalidate()
            }
        }
        findViewById<Button>(R.id.logout).setOnClickListener {
            firebaseAuth.signOut()
        }
        findViewById<Button>(R.id.go_to_chat_list).setOnClickListener {
            val intent = MainActivity.chatListIntent(this)
            startActivity(intent)
        }
        findViewById<Button>(R.id.login).setOnClickListener {
            val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.Theme_CryptoJet)
                .setAvailableProviders(
                    listOf(AuthUI.IdpConfig.GoogleBuilder().build())
                )
                .build()
            startActivity(intent)
        }
    }

    private fun invalidate() {
        val currentUser = firebaseAuth.currentUser
        val info = buildString {
            appendLine("uid: ${currentUser?.uid}")
            appendLine("isAnonymous: ${currentUser?.isAnonymous}")
            appendLine("displayName: ${currentUser?.displayName}")
            appendLine("email: ${currentUser?.email}")
        }
        findViewById<TextView>(R.id.info).text = info
    }
}
