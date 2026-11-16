package dev.minimal.launcher.app

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dev.minimal.launcher.core.designsystem.LauncherTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }
    private val requestHomeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                LauncherApp(viewModel)
            }
        }
        if (savedInstanceState == null) requestHomeRoleIfNeeded()
    }

    // Pressing Home while the launcher is already in front arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
    }

    // Whenever another app takes over (or the screen turns off), come back to a clean home screen.
    override fun onStop() {
        super.onStop()
        viewModel.goHome()
    }

    private fun requestHomeRoleIfNeeded() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            requestHomeRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    }
}
