package com.tokinote.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class TokiNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}