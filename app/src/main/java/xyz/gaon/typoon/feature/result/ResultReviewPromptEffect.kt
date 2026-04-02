package xyz.gaon.typoon.feature.result

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
internal fun ResultReviewPromptEffect(viewModel: ResultViewModel) {
    val activity = LocalActivity.current ?: return

    LaunchedEffect(viewModel, activity) {
        viewModel.events.collect { event ->
            when (event) {
                ResultUiEvent.TriggerReview -> {
                    val manager = ReviewManagerFactory.create(activity)
                    manager.requestReviewFlow().addOnCompleteListener { requestTask ->
                        if (requestTask.isSuccessful) {
                            manager
                                .launchReviewFlow(activity, requestTask.result)
                                .addOnCompleteListener {
                                    viewModel.onReviewShown()
                                }
                        }
                    }
                }
            }
        }
    }
}
