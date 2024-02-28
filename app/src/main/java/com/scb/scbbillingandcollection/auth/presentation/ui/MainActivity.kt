package com.scb.scbbillingandcollection.auth.presentation.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.scb.scbbillingandcollection.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}