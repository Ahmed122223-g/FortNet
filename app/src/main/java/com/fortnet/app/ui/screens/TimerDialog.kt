package com.fortnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortnet.app.ui.AppInfo
import com.fortnet.app.ui.theme.FortNetColors

@Composable
fun TimerDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }

    val presets = listOf(
        "30 دقيقة" to 30,
        "ساعة" to 60,
        "ساعتان" to 120,
        "3 ساعات" to 180
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FortNetColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = FortNetColors.TimerActive,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "مؤقت الحظر",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    app.name,
                    color = FortNetColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(20.dp))

                presets.forEach { (label, minutes) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FortNetColors.CardGlass)
                            .clickable { onSetTimer(minutes) }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = Color.White, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (!showCustom) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FortNetColors.Primary.copy(alpha = 0.2f))
                            .clickable { showCustom = true }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("مخصص", color = FortNetColors.Primary, fontSize = 16.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) customMinutes = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("دقيقة", color = FortNetColors.TextHint) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = FortNetColors.Primary,
                                unfocusedBorderColor = FortNetColors.Divider
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                customMinutes.toIntOrNull()?.let { if (it > 0) onSetTimer(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FortNetColors.Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تأكيد")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = FortNetColors.TextSecondary)
                }
            }
        }
    }
}
