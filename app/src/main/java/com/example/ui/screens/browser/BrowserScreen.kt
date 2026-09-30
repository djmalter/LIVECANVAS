package com.example.ui.screens.browser

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://webcamtests.com") }
    var currentUrl by remember { mutableStateOf("https://webcamtests.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Native Permission Launcher for WebKit Permission Coordination
    val nativePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        pendingPermissionRequest?.let { req ->
            if (allGranted) {
                req.grant(req.resources)
            } else {
                req.deny()
                Toast.makeText(context, "Camera or Microphone permission denied by user", Toast.LENGTH_SHORT).show()
            }
        }
        pendingPermissionRequest = null
    }

    // Standard File Chooser Launcher for <input type="file">
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NearBlackCharcoal,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSlateSurface)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_url_input"),
                        singleLine = true,
                        placeholder = { Text("Enter website URL...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSlateSurfaceVariant,
                            unfocusedContainerColor = DarkSlateSurfaceVariant,
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = DarkSlateBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            var formatted = urlInput.trim()
                            if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                formatted = "https://$formatted"
                            }
                            currentUrl = formatted
                            webViewInstance?.loadUrl(formatted)
                        },
                        modifier = Modifier.testTag("browser_go_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Load URL",
                            tint = TealAccent
                        )
                    }
                }

                // Security & Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Web Back",
                                tint = if (canGoBack) Color.White else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Web Forward",
                                tint = if (canGoForward) Color.White else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Secure WebKit Capture",
                        fontSize = 11.sp,
                        color = TealAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                url?.let { urlInput = it }
                            }
                        }

                        // WebChromeClient with universal domain support and native permission coordination
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                val req = request ?: return
                                post {
                                    // 1. Works across all domains - origin is parsed for auditability
                                    val originUri = req.origin
                                    
                                    // 2. Map WebKit resources to native Manifest permissions
                                    val requestedResources = req.resources
                                    val standardPermissions = mutableListOf<String>()

                                    if (requestedResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                        standardPermissions.add(Manifest.permission.CAMERA)
                                    }
                                    if (requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                        standardPermissions.add(Manifest.permission.RECORD_AUDIO)
                                    }

                                    // 3. Coordinate web grant with native runtime permissions
                                    val hasNative = standardPermissions.all { perm ->
                                        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
                                    }

                                    if (hasNative) {
                                        // Grant the requested resources for this domain
                                        req.grant(requestedResources)
                                    } else {
                                        // Request native OS permissions from the user first
                                        pendingPermissionRequest = req
                                        nativePermissionLauncher.launch(standardPermissions.toTypedArray())
                                    }
                                }
                            }

                            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                                post {
                                    if (pendingPermissionRequest == request) {
                                        pendingPermissionRequest = null
                                    }
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = filePathCallback
                                
                                val mimeType = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                                fileChooserLauncher.launch(mimeType)
                                return true
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_webview")
            )
        }
    }
}
