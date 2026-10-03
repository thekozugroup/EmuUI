package com.swordfish.lemuroid.app.tv.folderpicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.swordfish.lemuroid.app.shared.ImmersiveActivity
import com.swordfish.lemuroid.app.shared.settings.LibraryScanSubmission
import com.swordfish.lemuroid.app.shared.settings.finishAfterScanSubmission
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

class TVFolderPickerLauncher : ImmersiveActivity() {
    private val scanSubmission: LibraryScanSubmission by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finishAfterScanSubmission(scanSubmission)
        if (savedInstanceState?.getBoolean(SCAN_REQUESTED) == true) {
            scanSubmission.start(applicationContext)
        }

        if (savedInstanceState == null) {
            startActivityForResult(Intent(this, TVFolderPickerActivity::class.java), REQUEST_CODE_PICK_FOLDER)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            val sharedPreferences = SharedPreferencesHelper.getLegacySharedPreferences(this)
            val preferenceKey = getString(com.swordfish.lemuroid.lib.R.string.pref_key_legacy_external_folder)

            val currentValue: String? = sharedPreferences.getString(preferenceKey, null)
            val newValue = resultData?.extras?.getString(TVFolderPickerActivity.RESULT_DIRECTORY_PATH)

            if (newValue.toString() != currentValue) {
                sharedPreferences.edit().apply {
                    this.putString(preferenceKey, newValue.toString())
                    this.commit()
                }
            }

            startLibraryIndexWork()
            return
        }
        finish()
    }

    private fun startLibraryIndexWork() {
        scanSubmission.start(applicationContext)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SCAN_REQUESTED, scanSubmission.wasRequested)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1
        private const val SCAN_REQUESTED = "library_scan_requested"

        fun pickFolder(context: Context) {
            context.startActivity(Intent(context, TVFolderPickerLauncher::class.java))
        }
    }
}
