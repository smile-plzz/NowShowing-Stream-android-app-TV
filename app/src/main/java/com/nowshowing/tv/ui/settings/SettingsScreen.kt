package com.nowshowing.tv.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowshowing.tv.BuildConfig
import com.nowshowing.tv.data.repository.MetadataHealth
import com.nowshowing.tv.ui.common.TvSectionTab
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import com.nowshowing.tv.ui.theme.*
import kotlinx.coroutines.launch

private enum class SettingsSection(val label: String, val description: String) {
    PLAYBACK("Playback", "Playback behavior, episode advancement, and pointer speed."),
    LIBRARY("Library & Data", "Backup, restore, cache clearing, and local data reset."),
    PRIVACY("Privacy & Metadata", "OMDb API keys and ad-blocking filter protection."),
    ABOUT("About & Support", "App health diagnostics, remote help, updates, and repository.")
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateFeedback by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }

    var sectionName by rememberSaveable {
        mutableStateOf(if (MetadataHealth.failure.value != null) SettingsSection.PRIVACY.name else SettingsSection.PLAYBACK.name)
    }
    val section = SettingsSection.valueOf(sectionName)

    var metadataKey by remember { mutableStateOf("") }
    var testKeyFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedBackupJson by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    var reportMessage by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    var photoReport by remember { mutableStateOf<HealthReport?>(null) }
    var tipsReset by remember { mutableStateOf(false) }

    val entryFocus = remember { FocusRequester() }
    val reportFocus = remember { FocusRequester() }
    var restoreReportFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { withFrameNanos { }; entryFocus.requestFocus() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var live by remember { mutableStateOf("Live readings unavailable") }
    val recording by com.nowshowing.tv.diagnostics.BrowsingRecorder.recording.collectAsStateWithLifecycle()

    LaunchedEffect(section, lifecycleOwner, showReport) {
        if (section == SettingsSection.ABOUT && !showReport) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                while (true) { live = liveMemory(context); kotlinx.coroutines.delay(2000) }
            }
        }
    }
    LaunchedEffect(restoreReportFocus) {
        if (restoreReportFocus) { withFrameNanos { }; reportFocus.requestFocus(); restoreReportFocus = false }
    }

    if (showReport) photoReport?.let { HealthPhotoDialog(it) { showReport = false; restoreReportFocus = true } }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Library Backup (JSON)", color = NsTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your library data has been copied to your clipboard.", color = NsTextSecondary, fontSize = 15.sp)
                    Text("Characters: ${exportedBackupJson.length}", color = NsTextTertiary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TvPlayerActionButton("Done", null) { showExportDialog = false }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore Library from JSON", color = NsTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paste or enter a valid NowShowing TV backup JSON below:", color = NsTextSecondary, fontSize = 15.sp)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        label = { Text("Backup JSON") },
                        placeholder = { Text("{\"version\":1,\"watchlist\":[...],\"watchedEpisodes\":[...]}") }
                    )
                }
            },
            confirmButton = {
                TvPlayerActionButton("Import", null, enabled = importJsonText.isNotBlank()) {
                    viewModel.importLibrary(importJsonText) { success, _ ->
                        if (success) {
                            showImportDialog = false
                            importJsonText = ""
                        }
                    }
                }
            },
            dismissButton = {
                TvPlayerActionButton("Cancel", null) { showImportDialog = false }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Application Data", color = NsDestructive, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will clear your browsing history, cached metadata, and watched episode records.",
                        color = NsTextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        "Choose whether you would like to keep your saved Watchlist or wipe everything completely.",
                        color = NsTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvPlayerActionButton("Reset (Keep Watchlist)", null) {
                        viewModel.resetAppData(keepWatchlist = true) {
                            showResetDialog = false
                        }
                    }
                    TvPlayerActionButton("Wipe All (Include Watchlist)", null) {
                        viewModel.resetAppData(keepWatchlist = false) {
                            showResetDialog = false
                        }
                    }
                    TvPlayerActionButton("Cancel", null) { showResetDialog = false }
                }
            }
        )
    }

    if (showUpdateDialog) {
        availableRelease?.let { release ->
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Update available: ${release.tagName}", color = NsTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = release.name ?: release.tagName,
                            fontWeight = FontWeight.Bold,
                            color = NsAccent
                        )
                        val notes = release.body?.take(300)?.trim().orEmpty()
                        if (notes.isNotBlank()) {
                            Text(
                                text = if (notes.length >= 300) "$notes…" else notes,
                                color = NsTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Visit the GitHub release page to view details and download the update.",
                            color = NsTextTertiary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TvPlayerActionButton("Open release page", null) {
                        showUpdateDialog = false
                        runCatching {
                            val url = release.htmlUrl.ifBlank { "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases" }
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }
                },
                dismissButton = {
                    TvPlayerActionButton("Dismiss", null) { showUpdateDialog = false }
                }
            )
        }
    }

    Column(Modifier.fillMaxSize().background(NsBackground)) {
        TvTopBar("settings", onNavigate)
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", color = NsTextPrimary, style = NsTypography.Title, fontWeight = FontWeight.Bold)
            Text("${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", color = NsTextSecondary, style = NsTypography.Caption)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 40.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SettingsSection.entries, key = { it.name }) { tab ->
                TvSectionTab(tab.label, tab == section,
                    modifier = if (tab == section) Modifier.focusRequester(entryFocus) else Modifier) { sectionName = tab.name }
            }
        }
        key(section) {
            val scroll = rememberLazyListState()
            LazyColumn(state = scroll, modifier = Modifier.weight(1f).testTag("settings-column"),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item(key = "intro") { Text(section.description, color = NsTextSecondary, fontSize = 16.sp) }

                when (section) {
                    // =========================================================================
                    // 1. PLAYBACK GROUP
                    // =========================================================================
                    SettingsSection.PLAYBACK -> {
                        item(key = "autoplay") {
                            SettingToggleItem(
                                "Autoplay next episode",
                                "Continue automatically when the source supports verified completion. Web players currently use manual Next episode.",
                                state.autoplay,
                                viewModel::toggleAutoplay
                            )
                        }
                        item(key = "pointer_speed") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NsShape.cardRadius))
                                    .background(NsSurface1)
                                    .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Remote Pointer Speed", color = NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Controls D-pad cursor velocity across web player controls: Precision (0.5x), Slow (0.75x), Normal (1.0x), Fast (1.35x).",
                                        color = NsTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                TvPlayerActionButton(
                                    "Speed: ${state.pointerSpeed}",
                                    null,
                                    onClick = { viewModel.cyclePointerSpeed() }
                                )
                            }
                        }
                        item(key = "playback_help") {
                            InfoCard(
                                "Watching a series",
                                "Press Back during web playback to open player controls. Next episode is ready to select; Episodes lets you change the season or episode. Your last episode and source are remembered on this TV."
                            )
                        }
                    }

                    // =========================================================================
                    // 2. LIBRARY & DATA GROUP
                    // =========================================================================
                    SettingsSection.LIBRARY -> {
                        item(key = "backup_library") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NsShape.cardRadius))
                                    .background(NsSurface1)
                                    .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Export Library to JSON (Backup)", color = NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Export your Watchlist and watched episodes history to standard JSON. The backup is automatically copied to your clipboard.",
                                        color = NsTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                TvPlayerActionButton(
                                    if (state.isExportingLibrary) "Exporting…" else "Export Backup",
                                    null,
                                    enabled = !state.isExportingLibrary,
                                    onClick = {
                                        viewModel.exportLibrary { json ->
                                            if (json != null) {
                                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                                    .setPrimaryClip(ClipData.newPlainText("NowShowing TV Backup", json))
                                                exportedBackupJson = json
                                                showExportDialog = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        item(key = "restore_library") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NsShape.cardRadius))
                                    .background(NsSurface1)
                                    .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Import Library from JSON (Restore)", color = NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Restore a previously exported JSON backup. Restored titles and episode progress are merged into your current collection.",
                                        color = NsTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                TvPlayerActionButton(
                                    if (state.isImportingLibrary) "Importing…" else "Import Backup",
                                    null,
                                    enabled = !state.isImportingLibrary,
                                    onClick = { showImportDialog = true }
                                )
                            }
                        }
                        item(key = "cache_clear") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NsShape.cardRadius))
                                    .background(NsSurface1)
                                    .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Clear Metadata & Artwork Caches", color = NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Clear downloaded OMDb title metadata and poster artwork to free storage. Watchlist, history, and watched episode records are preserved.",
                                        color = NsTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                TvPlayerActionButton(
                                    if (state.isClearingCache) "Clearing…" else "Clear Caches",
                                    null,
                                    enabled = !state.isClearingCache,
                                    onClick = { viewModel.clearCaches() }
                                )
                            }
                        }
                        item(key = "reset_data_row") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NsShape.cardRadius))
                                    .background(NsSurface1)
                                    .border(1.dp, NsDestructive.copy(alpha = 0.3f), RoundedCornerShape(NsShape.cardRadius))
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Reset Application Data", color = NsDestructive, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Wipe recently opened history, watched episode records, and cache. You will be prompted to choose whether to keep your Watchlist.",
                                        color = NsTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                TvPlayerActionButton(
                                    "Reset Data…",
                                    null,
                                    onClick = { showResetDialog = true }
                                )
                            }
                        }
                        state.maintenanceMessage?.let { message -> item(key = "library_feedback") { Feedback(message) } }
                    }

                    // =========================================================================
                    // 3. PRIVACY & METADATA GROUP
                    // =========================================================================
                    SettingsSection.PRIVACY -> {
                        item(key = "filtering") {
                            SettingToggleItem(
                                "Ad request filtering",
                                "Block requests matched by the app's built-in EasyList rules.",
                                state.isDomainValidationEnabled,
                                viewModel::toggleDomainValidation
                            )
                        }
                        item(key = "tracking") {
                            SettingToggleItem(
                                "Tracker blocking",
                                "Filter telemetry and tracking scripts using EasyPrivacy rules.",
                                state.isTrackerBlockingEnabled,
                                viewModel::toggleTrackerBlocking
                            )
                        }
                        item(key = "navigation") {
                            SettingToggleItem(
                                "Keep player page contained",
                                "Restrict web player navigations to the original provider domain.",
                                state.isRedirectIsolationEnabled,
                                viewModel::toggleRedirectIsolation
                            )
                        }
                        item(key = "filter_update") {
                            TvPlayerActionButton(
                                if (state.isUpdatingFilters) "Updating filter lists…" else "Update filter lists online",
                                null,
                                enabled = !state.isUpdatingFilters,
                                onClick = { viewModel.updateFiltersOnline() }
                            )
                            state.updateStatusMessage?.let { Feedback(it) }
                        }
                        item(key = "metadata_header") {
                            InfoCard("Metadata Status", state.metadataStatus)
                            Text(
                                if (state.hasPersonalKey) "${state.personalKeyCount} personal OMDb key(s) active" else "Using shared demo metadata key",
                                color = NsAccent, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        item(key = "metadata_keys") {
                            Text("Personal OMDb API Keys", color = NsTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Enter authorized keys separated by commas or spaces. Stored securely on this TV. Multiple keys rotate in order when daily limits are reached.",
                                color = NsTextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = metadataKey,
                                onValueChange = { metadataKey = it },
                                singleLine = true,
                                label = { Text("Enter OMDb keys") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TvPlayerActionButton("Save personal keys", null, enabled = metadataKey.isNotBlank()) {
                                    if (viewModel.saveMetadataKey(metadataKey)) metadataKey = ""
                                }
                                TvPlayerActionButton(
                                    if (isTestingKey) "Testing…" else "Test Key",
                                    null,
                                    enabled = !isTestingKey && (metadataKey.isNotBlank() || state.hasPersonalKey),
                                    onClick = {
                                        isTestingKey = true
                                        testKeyFeedback = null
                                        val keyToTest = if (metadataKey.isNotBlank()) metadataKey else {
                                            context.getSharedPreferences("metadata", Context.MODE_PRIVATE).getString("key", "").orEmpty()
                                        }
                                        viewModel.testMetadataKey(keyToTest) { success, msg ->
                                            isTestingKey = false
                                            testKeyFeedback = if (success) "✓ $msg" else "✗ $msg"
                                        }
                                    }
                                )
                                if (state.hasPersonalKey) {
                                    TvPlayerActionButton("Use Demo Key", null) { viewModel.saveMetadataKey("") }
                                }
                            }
                            testKeyFeedback?.let { feedback ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = feedback,
                                    color = if (feedback.startsWith("✓")) NsAccent else NsDestructive,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        item(key = "stats") {
                            InfoCard(
                                "Requests Blocked",
                                "Ads: ${state.totalThreatsBlocked}    Trackers: ${state.totalTrackersBlocked}    Redirects: ${state.totalRedirectsBlocked}"
                            )
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton("Reset Blocking Counts", null) { viewModel.resetStatistics() }
                        }
                        if (state.exemptSources.isNotEmpty()) {
                            item(key = "exceptions_header") { Text("Source Protection Exceptions", color = NsTextPrimary, fontSize = 20.sp) }
                            items(state.exemptSources.sorted(), key = { "exception:$it" }) { source ->
                                TvPlayerActionButton("Restore protection · $source", null) { viewModel.removeSourceExemption(source) }
                            }
                            item(key = "clear_exceptions") { TvPlayerActionButton("Restore protection for all sources", null) { viewModel.clearSourceExceptions() } }
                        }
                        state.maintenanceMessage?.let { message -> item(key = "metadata_feedback") { Feedback(message) } }
                    }

                    // =========================================================================
                    // 4. ABOUT & SUPPORT GROUP
                    // =========================================================================
                    SettingsSection.ABOUT -> {
                        item(key = "about_card") {
                            InfoCard(
                                "NowShowing TV",
                                "An independent Android TV application for comfortable streaming.\n" +
                                "Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n" +
                                "Variant: ${BuildConfig.BUILD_TYPE}\n" +
                                "Commit: ${BuildConfig.BUILD_COMMIT}"
                            )
                        }
                        item(key = "update_check") {
                            InfoCard("Application Updates", "Check if a newer release candidate or release of NowShowing TV is available on GitHub.")
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton(
                                if (checkingUpdate) "Checking for updates…" else "Check for updates",
                                null,
                                enabled = !checkingUpdate
                            ) {
                                checkingUpdate = true
                                updateFeedback = null
                                coroutineScope.launch {
                                    when (val res = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
                                        is UpdateCheckResult.UpdateAvailable -> {
                                            availableRelease = res.release
                                            showUpdateDialog = true
                                        }
                                        is UpdateCheckResult.UpToDate -> {
                                            updateFeedback = "You are running the latest version (${BuildConfig.VERSION_NAME})."
                                            android.widget.Toast.makeText(context, updateFeedback, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        is UpdateCheckResult.Error -> {
                                            updateFeedback = res.message
                                            android.widget.Toast.makeText(context, updateFeedback, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    checkingUpdate = false
                                }
                            }
                            if (updateFeedback != null) {
                                Feedback(updateFeedback.orEmpty())
                            }
                        }
                        item(key = "remote_help") {
                            InfoCard(
                                "Remote Control Navigation",
                                "Browse: Directional arrows navigate cards and tabs; OK selects.\n" +
                                "Player: Arrows move the translucent pointer; OK clicks; hold an arrow to accelerate cursor.\n" +
                                "Back: Exits provider fullscreen first, opens app controls overlay next, and returns to title.\n" +
                                "Menu: Opens playback controls directly."
                            )
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton(
                                if (tipsReset) "First-use tips will appear next time" else "Show first-use tips again",
                                null
                            ) {
                                context.getSharedPreferences("tv_remote_help", Context.MODE_PRIVATE).edit().putBoolean("seen", false).apply()
                                tipsReset = true
                            }
                        }
                        item(key = "health_diagnostics") {
                            InfoCard("System Diagnostics & Memory", live)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TvPlayerActionButton(
                                    if (recording) "Stop Recording" else "Record Browsing (FPS)",
                                    null
                                ) {
                                    if (recording) com.nowshowing.tv.diagnostics.BrowsingRecorder.stop()
                                    else {
                                        val activity = context.findActivity()
                                        if (activity != null) {
                                            com.nowshowing.tv.diagnostics.BrowsingRecorder.start(activity.window)
                                            reportMessage = "Recording started. Navigate between screens, then return here."
                                        } else reportMessage = "Frame recording unavailable."
                                    }
                                }
                                TvPlayerActionButton(
                                    if (state.isRefreshingDiagnostics) "Capturing…" else "Run Health Check",
                                    null,
                                    enabled = !state.isRefreshingDiagnostics
                                ) {
                                    com.nowshowing.tv.diagnostics.BrowsingRecorder.stop()
                                    viewModel.refreshDiagnostics()
                                }
                            }
                        }
                        if (state.healthReport != null) {
                            item(key = "health_report_actions") {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TvPlayerActionButton("View Frozen Report", null, modifier = Modifier.focusRequester(reportFocus)) {
                                        photoReport = state.healthReport
                                        showReport = true
                                    }
                                    TvPlayerActionButton("Copy Report Text", null) {
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                            .setPrimaryClip(ClipData.newPlainText("NowShowing diagnostics", state.diagnosticText))
                                        reportMessage = "Diagnostics copied to clipboard."
                                    }
                                    TvPlayerActionButton("Delete Report", null) {
                                        viewModel.deleteReport()
                                    }
                                }
                            }
                        }
                        if (reportMessage.isNotBlank()) item(key = "report_msg") { Feedback(reportMessage) }

                        // Developer & GitHub Links
                        listOf(
                            "Source Code (GitHub)" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV",
                            "Releases & Downloads" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases",
                            "Report an Issue" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/issues",
                            "Developer Profile" to "https://github.com/smile-plzz",
                            "LinkedIn" to "https://www.linkedin.com/in/md-ismail-hossain-2938541b1/"
                        ).forEach { (label, url) ->
                            item(key = label) {
                                InfoCard(label, url)
                                Spacer(Modifier.height(4.dp))
                                TvPlayerActionButton("Open $label", null) {
                                    reportMessage = runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                        "Opened $label."
                                    }.getOrElse { "No external browser available. The URL remains visible above." }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NsShape.cardRadius))
            .background(NsSurface1)
            .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text, color = NsTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun Feedback(message: String) {
    Text(message, color = NsAccent, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun SettingToggleItem(title: String, description: String, enabled: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NsShape.cardRadius))
            .background(if (focused) NsSurface2 else NsSurface1)
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) NsAccent else NsSeparator,
                RoundedCornerShape(NsShape.cardRadius)
            )
            .semantics { stateDescription = if (enabled) "On" else "Off" }
            .clickable(interactionSource = interaction, indication = null, role = Role.Switch, onClick = onToggle)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = if (focused) NsAccent else NsTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(description, color = NsTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Spacer(Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) NsAccent.copy(alpha = 0.25f) else NsSurface2)
                .border(
                    1.dp,
                    if (enabled) NsAccent else NsSeparator,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (enabled) "ON" else "OFF",
                color = if (enabled) NsAccent else NsTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
