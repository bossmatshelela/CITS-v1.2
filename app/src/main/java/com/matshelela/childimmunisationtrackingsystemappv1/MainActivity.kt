package com.matshelela.childimmunisationtrackingsystemappv1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.matshelela.childimmunisationtrackingsystemappv1.ui.ImmunisationTrackerApp
import com.matshelela.childimmunisationtrackingsystemappv1.ui.theme.MyApplicationTheme
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImmunisationTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}
