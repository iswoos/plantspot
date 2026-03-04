package com.studio.plantspot.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio.plantspot.ui.screens.home.components.GreetingHeader
import com.studio.plantspot.ui.screens.home.components.PlantCard
import com.studio.plantspot.ui.screens.home.components.QuickCareRow
import com.studio.plantspot.ui.screens.home.components.profile.UserProfileEditSheet
import com.studio.plantspot.ui.theme.PlantSpotTheme

@Composable
fun HomeScreen() {
    var userName by remember { mutableStateOf("민수") }
    var showUserProfileSheet by remember { mutableStateOf(false) }

    if (showUserProfileSheet) {
        UserProfileEditSheet(
            currentName = userName,
            onDismissRequest = { showUserProfileSheet = false },
            onSaveRequest = { newName ->
                userName = newName
                showUserProfileSheet = false
            }
        )
    }

    Scaffold { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    GreetingHeader(
                        userName = userName,
                        onEditProfileClick = { showUserProfileSheet = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    QuickCareRow()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Dummy items for grid
            items(4) {
                PlantCard()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PlantSpotTheme {
        HomeScreen()
    }
}
