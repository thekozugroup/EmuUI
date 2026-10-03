package com.swordfish.lemuroid.app.shared.library

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.swordfish.lemuroid.app.shared.startup.BackgroundWork

class LibraryIndexBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        val appContext = context?.applicationContext ?: return
        val pendingResult = goAsync()
        BackgroundWork.finishBroadcastWhenSubmitted(pendingResult, LibraryIndexScheduler.cancelLibrarySync(appContext))
    }
}
