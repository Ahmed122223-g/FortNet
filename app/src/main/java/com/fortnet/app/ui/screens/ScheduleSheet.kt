package com.fortnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortnet.app.data.ScheduleEntry
import com.fortnet.app.ui.AppInfo
import com.fortnet.app.ui.AppViewModel
import com.fortnet.app.ui.theme.FortNetColors
import java.util.*

private val DAY_NAMES = listOf(
    Calendar.SATURDAY to "السبت",
    Calendar.SUNDAY to "الأحد",
    Calendar.MONDAY to "الاثنين",
    Calendar.TUESDAY to "الثلاثاء",
    Calendar.WEDNESDAY to "الأربعاء",
    Calendar.THURSDAY to "الخميس",
    Calendar.FRIDAY to "الجمعة"
)

data class DayScheduleState(
    val dayOfWeek: Int,
    val dayName: String,
    val enabled: Boolean = false,
    val startHour: Int = 6,
    val startMinute: Int = 0,
    val endHour: Int = 8,
    val endMinute: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    app: AppInfo,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val existingSchedule by viewModel.getScheduleForApp(app.packageName)
        .collectAsState(initial = emptyList())

    var dayStates by remember { mutableStateOf<List<DayScheduleState>>(emptyList()) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(existingSchedule) {
        if (!initialized || existingSchedule.isNotEmpty()) {
            dayStates = DAY_NAMES.map { (day, name) ->
                val existing = existingSchedule.find { it.dayOfWeek == day }
                DayScheduleState(
                    dayOfWeek = day,
                    dayName = name,
                    enabled = existing?.enabled ?: false,
                    startHour = existing?.startHour ?: 6,
                    startMinute = existing?.startMinute ?: 0,
                    endHour = existing?.endHour ?: 8,
                    endMinute = existing?.endMinute ?: 0
                )
            }
            initialized = true
        }
    }

    var showTimePicker by remember { mutableStateOf<Triple<Int, Boolean, DayScheduleState>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FortNetColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = FortNetColors.ScheduleActive,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("جدول الحظر", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(app.name, color = FortNetColors.TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            dayStates.forEachIndexed { index, state ->
                DayScheduleRow(
                    state = state,
                    onToggle = {
                        dayStates = dayStates.toMutableList().also {
                            it[index] = state.copy(enabled = !state.enabled)
                        }
                    },
                    onStartTimeClick = { showTimePicker = Triple(index, true, state) },
                    onEndTimeClick = { showTimePicker = Triple(index, false, state) }
                )
                if (index < dayStates.lastIndex) {
                    Divider(
                        color = FortNetColors.Divider,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val entries = dayStates.map {
                        ScheduleEntry(
                            packageName = app.packageName,
                            dayOfWeek = it.dayOfWeek,
                            enabled = it.enabled,
                            startHour = it.startHour,
                            startMinute = it.startMinute,
                            endHour = it.endHour,
                            endMinute = it.endMinute
                        )
                    }
                    viewModel.saveSchedule(app.packageName, entries)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FortNetColors.Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("حفظ الجدول", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    showTimePicker?.let { (index, isStart, state) ->
        TimePickerDialog(
            initialHour = if (isStart) state.startHour else state.endHour,
            initialMinute = if (isStart) state.startMinute else state.endMinute,
            onConfirm = { h, m ->
                dayStates = dayStates.toMutableList().also {
                    it[index] = if (isStart) state.copy(startHour = h, startMinute = m)
                    else state.copy(endHour = h, endMinute = m)
                }
                showTimePicker = null
            },
            onDismiss = { showTimePicker = null }
        )
    }
}

@Composable
fun DayScheduleRow(
    state: DayScheduleState,
    onToggle: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (state.enabled) FortNetColors.ScheduleActive.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.enabled,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = FortNetColors.ScheduleActive,
                uncheckedColor = FortNetColors.TextSecondary
            )
        )
        Text(
            state.dayName,
            color = if (state.enabled) Color.White else FortNetColors.TextSecondary,
            fontWeight = if (state.enabled) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(70.dp),
            fontSize = 15.sp
        )
        Spacer(Modifier.weight(1f))
        if (state.enabled) {
            TimeChip(
                hour = state.startHour,
                minute = state.startMinute,
                onClick = onStartTimeClick
            )
            Text("→", color = FortNetColors.TextSecondary, modifier = Modifier.padding(horizontal = 6.dp))
            TimeChip(
                hour = state.endHour,
                minute = state.endMinute,
                onClick = onEndTimeClick
            )
        } else {
            Text("--:-- → --:--", color = FortNetColors.TextHint, fontSize = 14.sp)
        }
    }
}

@Composable
fun TimeChip(hour: Int, minute: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FortNetColors.CardGlass)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = FortNetColors.ScheduleActive,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                String.format("%02d:%02d", hour, minute),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FortNetColors.Surface,
        title = { Text("اختر الوقت", color = Color.White) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = FortNetColors.CardGlass,
                        selectorColor = FortNetColors.Primary,
                        containerColor = FortNetColors.Surface,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = FortNetColors.TextSecondary,
                        timeSelectorSelectedContainerColor = FortNetColors.Primary,
                        timeSelectorUnselectedContainerColor = FortNetColors.CardGlass,
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = FortNetColors.TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("تأكيد", color = FortNetColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = FortNetColors.TextSecondary)
            }
        }
    )
}
