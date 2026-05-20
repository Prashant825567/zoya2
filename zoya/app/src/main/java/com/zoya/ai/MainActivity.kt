package com.zoya.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.zoya.ai.ui.MainScreen
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.viewmodel.ZoyaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ZoyaViewModel by viewModels()

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZoyaTheme {
                Surface(Modifier.fillMaxSize(), color = ZoyaTheme.colors.background) {
                    MainScreen(viewModel) { requestMic() }
                }
            }
        }
    }

    private fun requestMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) viewModel.onPermissionGranted()
        else requestPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onDestroy() { super.onDestroy(); viewModel.disconnect() }
}
