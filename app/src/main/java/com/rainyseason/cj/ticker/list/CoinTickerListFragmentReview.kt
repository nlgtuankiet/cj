package com.rainyseason.cj.ticker.list

import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.databinding.CoinTickerListFragmentBinding
import com.rainyseason.cj.tracking.logKeyParamsEvent
import timber.log.Timber


@Suppress("unused")
fun CoinTickerListFragment.setupReview(binding: CoinTickerListFragmentBinding) {
    val askForReviewBackground = binding.askForReviewBackground
    val showReview = binding.showReview
    val askForReviewContainer = binding.askForReviewContainer
    val ratingBar = binding.ratingBar
    val tellWhyEditText = binding.tellWhyEditText
    val tellWhy = binding.tellWhy
    val askReviewTitle = binding.askReviewTitle
    val cancelButton = binding.cancelButton
    val submitButton = binding.submitReview
    val tracker = submitButton.coreComponent.tracker

    fun maybeShowGoogleInAppReview() {

    }

    fun moveToEndState() {
        askForReviewBackground.isGone = true
        askForReviewContainer.isGone = true
        submitButton.dismissKeyboard()
    }

    fun moveToStartState() {
        askForReviewBackground.isVisible = true
        askForReviewBackground.setOnClickListener { }

        cancelButton.setOnClickListener {
            moveToEndState()
            tracker.logKeyParamsEvent("app_review_cancel")
        }

        askForReviewContainer.isVisible = true
        askForReviewContainer.setOnClickListener {  }


        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val ratingInt = rating.toInt()
            Timber.d("ratingInt: $ratingInt")
            if (ratingInt != 5) {
                tellWhy.isVisible = true
                askReviewTitle.isGone = true
            }
        }


        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val why = tellWhyEditText.text?.toString().orEmpty()
            tracker.logKeyParamsEvent(
                "app_review",
                mapOf(
                    "rating" to rating,
                    "why" to why
                )
            )


            if (rating == 5) {
                maybeShowGoogleInAppReview()
            }
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