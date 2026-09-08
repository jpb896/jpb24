package com.jpb.jpb24x

import android.annotation.SuppressLint
import android.opengl.EGL14
import android.opengl.GLES20
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jpb.jpb24x.viewmodels.SocUiState
import com.jpb.jpb24x.viewmodels.SystemInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    val socViewModel: SystemInfoViewModel = viewModel {
        SystemInfoViewModel(
            repository,
            hardwareProvider
        )
    }

    var gpuRenderer by remember { mutableStateOf("Fetching...") }
    var gpuVendor by remember { mutableStateOf("Fetching...") }

    LaunchedEffect(Unit) {
        val info = getGpuHardwareSpecsAsync()
        gpuRenderer = info.first
        gpuVendor = info.second
    }

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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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

                        Text(
                            text = "Processor",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
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
                                            text = SoCHelper.calcCpuCoreCount().toString() + " cores",
                                            style = Typography.headlineSmallEmphasized
                                        )
                                        Text(
                                            text = "Process: ${state.info.processNode} (${state.info.foundry})",
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
                                            text = SoCHelper.calcCpuCoreCount().toString() + " cores",
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

                        Text(
                            text = "Graphics Processing Unit",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = gpuRenderer,
                                    style = Typography.displaySmallEmphasized
                                )
                                Text(
                                    text = "Vendor: $gpuVendor",
                                    style = Typography.bodyLargeEmphasized
                                )
                            }
                        }
                    }

                    else -> { // Software Tab
                        val customFirmware = remember { com.jpb.jpb24x.helpers.CustomFirmwareDetectionHelper.detectAdvancedFirmware() }

                        // Base System Card
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Android " + Build.VERSION.RELEASE,
                                    style = Typography.displaySmallEmphasized
                                )
                                Text(
                                    text = "API Level: ${Build.VERSION.SDK_INT}",
                                    style = Typography.headlineSmallEmphasized
                                )
                                Text(
                                    text = "Security Patch: ${Build.VERSION.SECURITY_PATCH}",
                                    style = Typography.bodyLargeEmphasized
                                )
                            }
                        }

                        // Custom Firmware Card (Conditional Rendering)
                        if (customFirmware != null) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = customFirmware.title,
                                        style = Typography.headlineSmallEmphasized
                                    )
                                    Text(
                                        text = customFirmware.details,
                                        style = Typography.displaySmallEmphasized
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun getGpuHardwareSpecsAsync(): Pair<String, String> = withContext(Dispatchers.Default) {
    val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    val vers = IntArray(2)
    EGL14.eglInitialize(dpy, vers, 0, vers, 1)

    val configAttr = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_NONE
    )
    val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
    val numConfig = IntArray(1)
    EGL14.eglChooseConfig(dpy, configAttr, 0, configs, 0, 1, numConfig, 0)
    val config = configs[0]

    val surfAttr = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
    val surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0)
    val ctxAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttr, 0)

    EGL14.eglMakeCurrent(dpy, surf, surf, ctx)
    val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown GPU"
    val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown Vendor"

    EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    EGL14.eglDestroyContext(dpy, ctx)
    EGL14.eglDestroySurface(dpy, surf)
    EGL14.eglTerminate(dpy)

    Pair(renderer, vendor)
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    FAVORITES("Hardware", Icons.Filled.Memory),
    PROFILE("Software", Icons.Filled.Apps),
}