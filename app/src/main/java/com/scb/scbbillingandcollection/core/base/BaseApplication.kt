package com.scb.scbbillingandcollection.core.base

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * A application class where we can define the variable scope to use through out the application.
 */

@HiltAndroidApp
class BaseApplication : Application(){

    companion object {
        private lateinit var instance: BaseApplication
        fun getInstance(): BaseApplication {
            return instance
        }

        fun get(context: Context): BaseApplication {
            return context.applicationContext as BaseApplication
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this@BaseApplication
    }
}
