package com.rainyseason.cj.ticker.list

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.databinding.CoinTickerListFragmentBinding
import com.rainyseason.cj.tracking.logKeyParamsEvent


@Suppress("unused")
@SuppressLint("SetTextI18n")
fun CoinTickerListFragment.setupReview(binding: CoinTickerListFragmentBinding) {
    val askForReviewBackground = binding.askForReviewBackground
    val showReview = binding.showReview
    val askForReviewContainer = binding.askForReviewContainer
    val tellWhyEditText = binding.tellWhyEditText
    val tellWhy = binding.tellWhy
    val askReviewTitle = binding.askReviewTitle
    val leftButton = binding.leftButton
    val rightButton = binding.rightButton
    val tracker = rightButton.coreComponent.tracker
    val reviewIcon = binding.reviewIcon


    fun maybeShowGoogleInAppReview() {

    }

    fun moveToEndState() {
        askForReviewBackground.isGone = true
        askForReviewContainer.isGone = true
        rightButton.dismissKeyboard()
    }

    fun moveToTellWhyState() {
        tellWhy.isVisible = true
        reviewIcon.isGone = true
        askReviewTitle.text = "Tell us what's wrong : ("

        rightButton.text = "Submit"
        rightButton.setOnClickListener {
            moveToEndState()
            tracker.logKeyParamsEvent(
                "app_review_negative_why",
                mapOf(
                    "content" to tellWhyEditText.text.toString()
                )
            )
        }

        leftButton.text = "Maybe later"
        leftButton.setOnClickListener {
            moveToEndState()
        }
    }

    fun moveToStartState() {
        askForReviewBackground.isVisible = true
        askForReviewBackground.setOnClickListener { }

        leftButton.setOnClickListener {
            moveToTellWhyState()
            tracker.logKeyParamsEvent("app_review_negative")
            // save to local storage
        }

        askForReviewContainer.isVisible = true
        askForReviewContainer.setOnClickListener { }

        rightButton.setOnClickListener {
            tracker.logKeyParamsEvent("app_review_positive")
            Toast.makeText(rightButton.context, "Thank you for your feedback!", Toast.LENGTH_SHORT)
                .show()
            maybeShowGoogleInAppReview()
            moveToEndState()
        }

    }





    if (BuildConfig.DEBUG) {
        showReview.isVisible = true
        showReview.setOnClickListener {
            moveToStartState()
        }
    }
}