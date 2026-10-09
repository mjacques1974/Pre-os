package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PestoScreen
import com.example.ui.PestoViewModel
import com.example.ui.theme.PestoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PestoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PestoViewModel = viewModel()
                    PestoScreen(viewModel = viewModel)
                }
            }
        }
    }
}
