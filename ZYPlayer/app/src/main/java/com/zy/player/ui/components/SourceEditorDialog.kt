package com.zy.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zy.player.ui.theme.AppColors
import com.zy.player.ui.theme.Dimens

@Composable
fun SourceEditorDialog(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            border = BorderStroke(1.dp, AppColors.Divider)
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.paddingLarge)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = editorTextFieldColors()
                )

                Spacer(modifier = Modifier.height(Dimens.paddingMedium))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("地址") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(4.dp),
                    colors = editorTextFieldColors()
                )

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                onConfirm(name.trim(), url.trim())
                            }
                        },
                        enabled = name.isNotBlank() && url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = AppColors.OnPrimary,
                            disabledContainerColor = AppColors.SurfaceRaised,
                            disabledContentColor = AppColors.TextTertiary
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun editorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedContainerColor = AppColors.SurfaceAlt,
    unfocusedContainerColor = AppColors.SurfaceAlt,
    focusedBorderColor = AppColors.Primary,
    unfocusedBorderColor = AppColors.Divider,
    cursorColor = AppColors.Primary,
    focusedLabelColor = AppColors.Primary,
    unfocusedLabelColor = AppColors.TextSecondary
)
