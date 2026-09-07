package com.jpb.jpb24x

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaredrummler.android.device.DeviceName
import com.jpb.jpb24x.helpers.SoCHelper
import com.jpb.jpb24x.helpers.SocRepository
import com.jpb.jpb24x.providers.AdvancedSocHardwareProvider
import com.jpb.jpb24x.ui.theme.Jpb24Theme
import com.jpb.jpb24x.ui.theme.Typography
import com.jpb.jpb24x.viewmodels.SystemInfoViewModel
import com.jpb.jpb24x.viewmodels.SocUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Jpb24Theme {
                Jpb24App()
            }
        }
    }
}

@SuppressLint("NewApi")
@PreviewScreenSizes
@Composable
fun Jpb24App() {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val hardwareProvider = remember { AdvancedSocHardwareProvider() }
    val repository = remember { SocRepository(context.applicationContext, hardwareProvider) }

    val socViewModel: SystemInfoViewModel = viewModel { SystemInfoViewModel(
        repository,
        hardwareProvider
    ) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        // Fix: Nesting a clean Scaffold inside the Adaptive layout acts as a dedicated inset provider.
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Consumes the correct top/bottom dimensions safely from the system framework
                    .padding(innerPadding)
                    // Restores layout margin breathing room for all child cards
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentDestination.label) {
                    "Home" -> {
                        DeviceName.init(LocalContext.current)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = DeviceName.getDeviceName(),
                                    style = Typography.displaySmallEmphasized
                                )
                                Text(
                                    text = "Android " + Build.VERSION.RELEASE,
                                    style = Typography.headlineSmallEmphasized
                                )
                                Text(
                                    text = Build.VERSION.SECURITY_PATCH,
                                    style = Typography.bodyLargeEmphasized
                                )
                            }
                        }
                    }
                    "Hardware" -> {
                        val uiState by socViewModel.uiState.collectAsState()

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (val state = uiState) {
                                    is SocUiState.Loading -> {
                                        CircularProgressIndicator()
                                    }

                                    is SocUiState.Success -> {
                                        if (state.info.marketingName.contains("Tensor") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            Text(
                                                text = Build.SOC_MODEL,
                                                style = Typography.displaySmallEmphasized
                                            )
                                        } else {
                                            Text(
                                                text = state.info.marketingName,
                                                style = Typography.displaySmallEmphasized
                                            )
                                        }
                                        Text(
                                            text = SoCHelper.calcCpuCoreCount()
                                                .toString() + " cores",
                                            style = Typography.headlineSmallEmphasized
                                        )
                                        Text(
                                            text = state.info.processNode,
                                            style = Typography.bodyLargeEmphasized
                                        )
                                    }

                                    is SocUiState.Unknown -> {
                                        val fallbackName =
                                            if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE
                                        Text(
                                            text = fallbackName,
                                            style = Typography.displaySmallEmphasized
                                        )
                                        Text(
                                            text = SoCHelper.calcCpuCoreCount()
                                                .toString() + " cores",
                                            style = Typography.headlineSmallEmphasized
                                        )
                                        Text(
                                            text = "Unknown Process Node",
                                            style = Typography.bodyLargeEmphasized
                                        )
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column {
                                    Text(
                                        text = "Processor",
                                        style = Typography.bodyLargeEmphasized
                                    )

                                    Text(
                                        text = "Core count",
                                        style = Typography.bodyLargeEmphasized
                                    )
                                    Text(
                                        text = "Process",
                                        style = Typography.bodyLargeEmphasized
                                    )
                                    Text(
                                        text = "Vendor",
                                        style = Typography.bodyLargeEmphasized
                                    )

                                    Text(
                                        text = "Foundry (manufacturer)",
                                        style = Typography.bodyLargeEmphasized
                                    )
                                    Text(
                                        text = "Fab",
                                        style = Typography.bodyLargeEmphasized
                                    )
                                }
                                Column {
                                    when (val state = uiState) {
                                        is SocUiState.Loading -> {
                                            CircularProgressIndicator()
                                        }

                                        is SocUiState.Success -> {
                                            if (state.info.marketingName.contains("Tensor") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                Text(
                                                    text = Build.SOC_MODEL,
                                                )
                                            } else {
                                                Text(
                                                    text = state.info.marketingName,
                                                )
                                            }
                                            Text(
                                                text = SoCHelper.calcCpuCoreCount()
                                                    .toString(),
                                            )
                                            Text(
                                                text = state.info.processNode,
                                            )
                                            Text(
                                                text = state.info.vendor,
                                            )
                                            Text(
                                                text = state.info.foundry,
                                            )
                                            Text(
                                                text = state.info.fab,
                                            )
                                        }

                                        is SocUiState.Unknown -> {
                                            val fallbackName =
                                                if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE
                                            Text(
                                                text = fallbackName,
                                                style = Typography.displaySmallEmphasized
                                            )
                                            Text(
                                                text = SoCHelper.calcCpuCoreCount()
                                                    .toString() + " cores",
                                                style = Typography.headlineSmallEmphasized
                                            )
                                            Text(
                                                text = "Unknown Process Node",
                                                style = Typography.bodyLargeEmphasized
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Architecture Details",
                                    style = Typography.headlineSmallEmphasized
                                )
                                Text(
                                    text = "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}",
                                    style = Typography.bodyLargeEmphasized
                                )
                            }
                        }
                    }
                    else -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Android " + Build.VERSION.RELEASE, style = Typography.displaySmallEmphasized)
                                Text(text = "custom firmware version (if one exists)", style = Typography.headlineSmallEmphasized)
                                Text(text = "SPL or API level", style = Typography.bodyLargeEmphasized)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    FAVORITES("Hardware", Icons.Filled.Memory),
    PROFILE("Software", Icons.Filled.Apps),
}