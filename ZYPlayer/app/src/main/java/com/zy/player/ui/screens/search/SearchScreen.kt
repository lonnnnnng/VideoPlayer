package com.zy.player.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zy.player.ui.components.CinemaBackground
import com.zy.player.ui.components.PageHeader
import com.zy.player.ui.theme.AppColors
import com.zy.player.ui.theme.Dimens

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearchResult: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var searchText by remember { mutableStateOf("") }
    val searchHistory by viewModel.searchHistory.collectAsState()

    CinemaBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "搜索",
            onBackClick = onNavigateBack
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium),
            placeholder = { Text("输入关键词搜索") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.Primary)
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空", tint = AppColors.TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = Color.White.copy(alpha = 0.045f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.045f),
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Divider,
                cursorColor = AppColors.Primary,
                focusedPlaceholderColor = AppColors.TextSecondary,
                unfocusedPlaceholderColor = AppColors.TextSecondary
            )
        )

        Button(
            onClick = {
                if (searchText.isNotBlank()) {
                    viewModel.addSearchHistory(searchText.trim())
                    onNavigateToSearchResult(searchText.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.paddingMedium),
            enabled = searchText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                contentColor = AppColors.Background,
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = AppColors.TextTertiary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("搜索")
        }

        if (searchHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.paddingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索历史",
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black
                )
                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                    Text("清空")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.paddingMedium)
            ) {
                items(searchHistory) { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(1.dp, AppColors.Divider), RoundedCornerShape(16.dp))
                            .clickable {
                                onNavigateToSearchResult(keyword)
                            }
                            .padding(Dimens.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AppColors.Primary
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingMedium))
                        Text(
                            text = keyword,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    }
}
