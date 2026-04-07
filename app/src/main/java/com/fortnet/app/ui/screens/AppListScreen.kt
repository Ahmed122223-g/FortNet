package com.fortnet.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.fortnet.app.ui.*
import com.fortnet.app.ui.theme.FortNetColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppViewModel,
    onToggle: (AppInfo) -> Unit,
    onTimerSet: (AppInfo, Int) -> Unit,
    onTimerCancel: (AppInfo) -> Unit,
    onBatteryOptimClick: () -> Unit
) {
    val apps by viewModel.apps.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showTimerDialog by remember { mutableStateOf<AppInfo?>(null) }
    var showScheduleSheet by remember { mutableStateOf<AppInfo?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FortNetColors.GradientTop, FortNetColors.GradientBottom)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = FortNetColors.Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("FortNet", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    val blockedCount = apps.count { it.isBlocked }
                    if (blockedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FortNetColors.BlockedBg)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("$blockedCount محظور", color = FortNetColors.Blocked, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text("حماية تطبيقاتك", color = FortNetColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 42.dp))
            }

            // ── Update Notification ──
            val updateInfo by viewModel.updateAvailable.collectAsState()
            updateInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FortNetColors.Primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = FortNetColors.Primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تحديث جديد متوفر (v${info.versionName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (info.releaseNotes.isNotEmpty()) {
                                Text(info.releaseNotes, color = FortNetColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Button(
                            onClick = { viewModel.startUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = FortNetColors.Primary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("تحديث", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Battery Optimization ──
            if (isBatteryOptimized) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FortNetColors.Blocked.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = FortNetColors.Blocked)
                            Spacer(Modifier.width(12.dp))
                            Text("تحسين البطارية نشط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "قد يتم إيقاف الخدمة في الخلفية. يرجى استثناء التطبيق من تحسين البطارية لضمان استقرار الحماية.",
                            color = FortNetColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onBatteryOptimClick,
                            colors = ButtonDefaults.buttonColors(containerColor = FortNetColors.Blocked),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("إيقاف التحسين", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Search ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("ابحث عن تطبيق…", color = FortNetColors.TextHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FortNetColors.TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = FortNetColors.TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = FortNetColors.SurfaceGlass, unfocusedContainerColor = FortNetColors.SurfaceGlass,
                    focusedBorderColor = FortNetColors.Primary.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent,
                    cursorColor = FortNetColors.Primary
                )
            )

            // ── Categories ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FortNetColors.ChipSelected,
                            selectedLabelColor = Color.White,
                            containerColor = FortNetColors.ChipUnselected,
                            labelColor = FortNetColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = null
                    )
                }
            }

            // ── Sort & Filter ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    AssistChip(
                        onClick = { showSortMenu = true },
                        label = { Text(sortOption.label, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = FortNetColors.CardGlass,
                            labelColor = FortNetColors.TextSecondary,
                            leadingIconContentColor = FortNetColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(FortNetColors.Surface)) {
                        SortOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, color = if (sortOption == opt) FortNetColors.Primary else Color.White) },
                                onClick = { viewModel.setSortOption(opt); showSortMenu = false }
                            )
                        }
                    }
                }
                Box {
                    AssistChip(
                        onClick = { showFilterMenu = true },
                        label = { Text(filterOption.label, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = FortNetColors.CardGlass,
                            labelColor = FortNetColors.TextSecondary,
                            leadingIconContentColor = FortNetColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.background(FortNetColors.Surface)) {
                        FilterOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, color = if (filterOption == opt) FortNetColors.Primary else Color.White) },
                                onClick = { viewModel.setFilterOption(opt); showFilterMenu = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${apps.size} تطبيق", color = FortNetColors.TextHint, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }

            // ── App List ──
            if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = FortNetColors.TextHint, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد تطبيقات", color = FortNetColors.TextHint, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            onToggle = { onToggle(app) },
                            onTimerClick = { showTimerDialog = app },
                            onScheduleClick = { showScheduleSheet = app }
                        )
                    }
                }
            }
        }
    }

    // ── Timer Dialog ──
    showTimerDialog?.let { app ->
        if (app.timerEndTime > System.currentTimeMillis()) {
            // Timer is active - show cancel option
            AlertDialog(
                onDismissRequest = { showTimerDialog = null },
                containerColor = FortNetColors.Surface,
                title = { Text("إلغاء المؤقت", color = Color.White) },
                text = { Text("هل تريد إلغاء المؤقت وفتح ${app.name}؟", color = FortNetColors.TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { onTimerCancel(app); showTimerDialog = null }) {
                        Text("إلغاء المؤقت", color = FortNetColors.Blocked)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimerDialog = null }) {
                        Text("رجوع", color = FortNetColors.TextSecondary)
                    }
                }
            )
        } else {
            TimerDialog(
                app = app,
                onDismiss = { showTimerDialog = null },
                onSetTimer = { minutes -> onTimerSet(app, minutes); showTimerDialog = null }
            )
        }
    }

    // ── Schedule Sheet ──
    showScheduleSheet?.let { app ->
        ScheduleBottomSheet(
            app = app,
            viewModel = viewModel,
            onDismiss = { showScheduleSheet = null }
        )
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    onToggle: () -> Unit,
    onTimerClick: () -> Unit,
    onScheduleClick: () -> Unit
) {
    val isTimerActive = app.timerEndTime > System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked) FortNetColors.BlockedBg.copy(alpha = 0.08f)
            else FortNetColors.CardGlass
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── App Icon ──
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FortNetColors.SurfaceGlass),
                    contentAlignment = Alignment.Center
                ) {
                    app.icon?.let { drawable ->
                        Image(
                            bitmap = drawable.toBitmap(48, 48, null).asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ── App Info ──
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.name, color = Color.White,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.packageName, color = FortNetColors.TextHint,
                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                // ── Actions ──
                IconButton(onClick = onTimerClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isTimerActive) Icons.Default.Timer else Icons.Outlined.Timer,
                        contentDescription = "مؤقت",
                        tint = if (isTimerActive) FortNetColors.TimerActive else FortNetColors.TextHint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onScheduleClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "جدول",
                        tint = if (app.hasSchedule) FortNetColors.ScheduleActive else FortNetColors.TextHint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FortNetColors.Blocked,
                        uncheckedThumbColor = FortNetColors.TextSecondary,
                        uncheckedTrackColor = FortNetColors.ChipUnselected
                    )
                )
            }

            // ── Timer Countdown ──
            if (isTimerActive) {
                Spacer(Modifier.height(6.dp))
                TimerCountdown(app.timerEndTime)
            }
        }
    }
}

@Composable
fun TimerCountdown(endTime: Long) {
    var remaining by remember(endTime) { mutableStateOf(endTime - System.currentTimeMillis()) }

    LaunchedEffect(endTime) {
        while (remaining > 0) {
            delay(1000)
            remaining = endTime - System.currentTimeMillis()
        }
    }

    if (remaining > 0) {
        val h = remaining / 3600000
        val m = (remaining % 3600000) / 60000
        val s = (remaining % 60000) / 1000

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(FortNetColors.TimerBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = FortNetColors.TimerActive, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("متبقي: ", color = FortNetColors.TimerActive, fontSize = 13.sp)
            Text(
                String.format("%02d:%02d:%02d", h, m, s),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
