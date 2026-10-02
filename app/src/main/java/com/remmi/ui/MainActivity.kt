package com.remmi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.remmi.ui.theme.RemmiTheme

class MainActivity : ComponentActivity() {

    private val homeRequestedTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    // Home launcher activity should remain active on Back press
                    moveTaskToBack(true)
                }
            },
        )

        setContent {
            RemmiTheme {
                RemmiApp(homeRequestedTrigger = homeRequestedTrigger.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homeRequestedTrigger.intValue += 1
    }
}
