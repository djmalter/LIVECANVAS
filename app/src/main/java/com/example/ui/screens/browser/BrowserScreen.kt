package com.example.ui.screens.browser

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "https://idverify.amazon/",
    var title: String = "New Tab",
    val webView: WebView
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var tabs by remember { mutableStateOf<List<BrowserTab>>(emptyList()) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    var urlInput by remember { mutableStateOf("https://idverify.amazon/") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Ensure native runtime permissions are requested up front
    val nativePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission response handled by OS */ }

    LaunchedEffect(Unit) {
        val ungranted = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            nativePermissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    // Standard File Chooser Launcher for <input type="file">
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    // Container FrameLayout that holds WebViews for all tabs to preserve DOM state
    val containerLayout = remember {
        FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Forward declaration of createTab helper
    lateinit var createTab: (String, Boolean) -> BrowserTab

    createTab = { initialUrl: String, autoLoad: Boolean ->
        val tabId = UUID.randomUUID().toString()
        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // High Performance & Compatibility Settings with Popup Support
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                // Support window.open, popups and multi-windows
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT

                val baseUa = userAgentString
                if (baseUa.contains("; wv")) {
                    userAgentString = baseUa.replace("; wv", "")
                }
            }

            // Enable Cookies & Third-Party Cookies (Essential for Amazon authentication & redirects)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (activeTabId == tabId) {
                        isLoading = true
                        url?.let { urlInput = it }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (activeTabId == tabId) {
                        isLoading = false
                        canGoBack = view?.canGoBack() == true
                        canGoForward = view?.canGoForward() == true
                        url?.let { urlInput = it }
                    }
                    // Update tab URL
                    url?.let { finishedUrl ->
                        tabs = tabs.map { tab ->
                            if (tab.id == tabId) tab.copy(url = finishedUrl) else tab
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true && activeTabId == tabId) {
                        isLoading = false
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (activeTabId == tabId) {
                        loadProgress = newProgress / 100f
                        isLoading = newProgress < 100
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) {
                        tabs = tabs.map { tab ->
                            if (tab.id == tabId) tab.copy(title = title) else tab
                        }
                    }
                }

                // Handle Popups and window.open by creating a new browser tab
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val popupTab = createTab("", false)
                    containerLayout.addView(popupTab.webView)
                    tabs = tabs + popupTab
                    activeTabId = popupTab.id

                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = popupTab.webView
                    resultMsg?.sendToTarget()
                    return true
                }

                override fun onCloseWindow(window: WebView?) {
                    val tabToClose = tabs.find { it.webView == window }
                    if (tabToClose != null && tabs.size > 1) {
                        val remaining = tabs.filter { it.id != tabToClose.id }
                        containerLayout.removeView(tabToClose.webView)
                        tabToClose.webView.destroy()
                        tabs = remaining
                        if (activeTabId == tabToClose.id) {
                            activeTabId = remaining.last().id
                        }
                    }
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    val req = request ?: return
                    post {
                        req.grant(req.resources)
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                    super.onPermissionRequestCanceled(request)
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
        }

        if (autoLoad && initialUrl.isNotBlank()) {
            webView.loadUrl(initialUrl)
        }

        BrowserTab(id = tabId, url = initialUrl, title = "New Tab", webView = webView)
    }

    // Initialize first tab
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val initialTab = createTab("https://idverify.amazon/", true)
            containerLayout.addView(initialTab.webView)
            tabs = listOf(initialTab)
            activeTabId = initialTab.id
        }
    }

    val activeTab = tabs.find { it.id == activeTabId }

    // Keep active webview state synchronized
    LaunchedEffect(activeTabId) {
        activeTab?.let { current ->
            urlInput = current.webView.url ?: current.url
            canGoBack = current.webView.canGoBack()
            canGoForward = current.webView.canGoForward()
        }
        // Update visibility of views in containerLayout
        for (i in 0 until containerLayout.childCount) {
            val child = containerLayout.getChildAt(i)
            child.visibility = if (child == activeTab?.webView) View.VISIBLE else View.GONE
        }
    }

    // Handle Back Press
    BackHandler {
        if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (activeTab?.webView?.canGoBack() == true) {
            activeTab.webView.goBack()
        } else if (tabs.size > 1) {
            // Close current tab and switch to previous
            activeTab?.let { closingTab ->
                val remaining = tabs.filter { it.id != closingTab.id }
                containerLayout.removeView(closingTab.webView)
                closingTab.webView.destroy()
                tabs = remaining
                activeTabId = remaining.last().id
            }
        } else {
            onNavigateBack()
        }
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
                // Top Address and Control Bar
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
                        placeholder = { Text("Enter URL...", color = TextSecondary) },
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

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            var formatted = urlInput.trim()
                            if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                formatted = "https://$formatted"
                            }
                            activeTab?.webView?.loadUrl(formatted)
                        },
                        modifier = Modifier.testTag("browser_go_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Load URL",
                            tint = TealAccent
                        )
                    }

                    // Tab Count Button
                    Surface(
                        onClick = { showTabSwitcher = !showTabSwitcher },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showTabSwitcher) ElectricIndigo else DarkSlateSurfaceVariant,
                        border = BorderStroke(1.dp, if (showTabSwitcher) ElectricIndigo else DarkSlateBorder),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tab_switcher_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tabs.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Quick New Tab (+) Button
                    IconButton(
                        onClick = {
                            val newTab = createTab("https://idverify.amazon/", true)
                            containerLayout.addView(newTab.webView)
                            tabs = tabs + newTab
                            activeTabId = newTab.id
                            showTabSwitcher = false
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("new_tab_button")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = Color.White
                        )
                    }
                }

                // Navigation Controls & Shortcuts Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { activeTab?.webView?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Web Back",
                                tint = if (canGoBack) Color.White else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = { activeTab?.webView?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Web Forward",
                                tint = if (canGoForward) Color.White else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = { activeTab?.webView?.reload() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Quick Shortcut Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = {
                                val target = "https://idverify.amazon/"
                                urlInput = target
                                activeTab?.webView?.loadUrl(target)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricIndigo.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, ElectricIndigo),
                            modifier = Modifier.testTag("shortcut_amazon_idverify")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(13.dp))
                                Text("Amazon ID", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }

                        Surface(
                            onClick = {
                                val target = "https://webcamtests.com"
                                urlInput = target
                                activeTab?.webView?.loadUrl(target)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSlateSurfaceVariant,
                            border = BorderStroke(1.dp, DarkSlateBorder),
                            modifier = Modifier.testTag("shortcut_webcamtests")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = TealAccent, modifier = Modifier.size(13.dp))
                                Text("Webcam", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Progress Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(top = 2.dp),
                        color = TealAccent,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // AndroidView embedding the FrameLayout container
            AndroidView(
                factory = { containerLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_webview_container")
            )

            // Multi-Tab Switcher Overlay
            AnimatedVisibility(
                visible = showTabSwitcher,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NearBlackCharcoal.copy(alpha = 0.96f))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Tabs (${tabs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val newTab = createTab("https://idverify.amazon/", true)
                                        containerLayout.addView(newTab.webView)
                                        tabs = tabs + newTab
                                        activeTabId = newTab.id
                                        showTabSwitcher = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Tab", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showTabSwitcher = false },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Done", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(tabs, key = { it.id }) { tab ->
                                val isSelected = tab.id == activeTabId
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) DarkSlateSurfaceVariant else DarkSlateSurface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TealAccent else DarkSlateBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clickable {
                                            activeTabId = tab.id
                                            showTabSwitcher = false
                                        }
                                        .testTag("tab_card_${tab.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tab.title.ifBlank { "Untitled" },
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (tabs.size > 1) {
                                                        val remaining = tabs.filter { it.id != tab.id }
                                                        containerLayout.removeView(tab.webView)
                                                        tab.webView.destroy()
                                                        tabs = remaining
                                                        if (activeTabId == tab.id) {
                                                            activeTabId = remaining.last().id
                                                        }
                                                    } else {
                                                        // Reset only tab to default
                                                        tab.webView.loadUrl("https://idverify.amazon/")
                                                        tab.title = "Amazon ID Verify"
                                                        tab.url = "https://idverify.amazon/"
                                                        showTabSwitcher = false
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close Tab",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = tab.url.ifBlank { "about:blank" },
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = TealAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Active Tab",
                                                        fontSize = 9.sp,
                                                        color = TealAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            }
        }
    }
}
