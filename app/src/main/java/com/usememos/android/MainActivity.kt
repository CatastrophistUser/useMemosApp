package com.usememos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.usememos.android.core.AppContainer
import com.usememos.android.ui.MemosApp
import com.usememos.android.ui.theme.UseMemosTheme

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MemosApplication).container
        setContent {
            UseMemosTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    MemosApp()
                }
            }
        }
    }
}
