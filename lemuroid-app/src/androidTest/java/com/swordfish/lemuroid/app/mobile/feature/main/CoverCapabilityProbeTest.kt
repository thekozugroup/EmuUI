package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/** Read-only probe. Does not start a session or claim that a cover display receives touch. */
@OptIn(ExperimentalWindowApi::class)
class CoverCapabilityProbeTest {
    @Test fun recordActualDualDisplayAvailabilityWithoutActivatingIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val infos = runBlocking {
            withContext(Dispatchers.Main) {
                withTimeoutOrNull(5000) { WindowAreaController.getOrCreate().windowAreaInfos.first() }
            }
        }
        val report = JSONObject().put("model", android.os.Build.MODEL)
            .put("api", android.os.Build.VERSION.SDK_INT)
            .put("receivedCapabilityReport", infos != null)
            .put("areas", JSONArray().apply {
                infos?.forEach { info -> put(JSONObject().put("type", info.type.toString())
                    .put("presentStatus", info.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA).status.toString())) }
            })
            .put("simultaneousTouchVerified", false)
        val dir = File(context.getExternalFilesDir(null), "polish-qa").apply { mkdirs() }
        File(dir, "cover-capability.json").writeText(report.toString(2))
    }
}
