package com.example.arialtv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.example.arialtv.ui.theme.ArialTvTheme
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.viewmodel.AppDestination
import com.example.arialtv.ui.viewmodel.ArialTvViewModel
import com.example.arialtv.ui.viewmodel.TvNavTab

class MainActivity : ComponentActivity() {
    private val viewModel: ArialTvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArialTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = CanvasBlack),
                ) {
                    ArialTvApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val state = viewModel.uiState.value
        if (state.destination !is AppDestination.Player) {
            when (keyCode) {
                KeyEvent.KEYCODE_MENU -> {
                    viewModel.toggleQuickMenu()
                    return true
                }
                KeyEvent.KEYCODE_SEARCH -> {
                    viewModel.dismissQuickMenu()
                    viewModel.selectTab(TvNavTab.SEARCH)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
