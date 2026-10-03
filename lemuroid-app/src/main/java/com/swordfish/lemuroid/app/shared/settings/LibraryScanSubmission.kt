package com.swordfish.lemuroid.app.shared.settings

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Retains the accepted import submission across picker-Activity configuration changes. */
class LibraryScanSubmission : ViewModel() {
    enum class State { IDLE, SUBMITTING, SUBMITTED, FAILED }

    private val mutableState = MutableStateFlow(State.IDLE)
    val state = mutableState.asStateFlow()
    val wasRequested: Boolean get() = state.value != State.IDLE

    fun start(context: Context) {
        if (wasRequested) return
        mutableState.value = State.SUBMITTING
        // This accepted command is process-owned, not cancelled with the picker Activity.
        val submitted = LibraryIndexScheduler.scheduleLibrarySync(context.applicationContext)
        viewModelScope.launch {
            try {
                submitted.await()
                mutableState.value = State.SUBMITTED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = State.FAILED
            }
        }
    }
}

fun FragmentActivity.finishAfterScanSubmission(submission: LibraryScanSubmission) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            submission.state.collect { state ->
                when (state) {
                    LibraryScanSubmission.State.SUBMITTED -> finish()
                    LibraryScanSubmission.State.FAILED ->
                        displayErrorDialog(
                            "The games folder was saved, but its scan could not be scheduled. " +
                                "Please try importing again.",
                            getString(android.R.string.ok),
                        ) { finish() }
                    else -> Unit
                }
            }
        }
    }
}
