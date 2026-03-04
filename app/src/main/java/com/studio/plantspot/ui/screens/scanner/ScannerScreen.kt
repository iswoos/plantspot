package com.studio.plantspot.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studio.plantspot.R
import com.studio.plantspot.ui.screens.scanner.components.LightMeterOverlay

@Composable
fun ScannerScreen() {
    var isPreAdoptionMode by remember { mutableStateOf(true) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray) // Placeholder for Camera Preview
                .padding(innerPadding)
        ) {
            // Mode Toggle (Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isPreAdoptionMode = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreAdoptionMode) MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.scanner_mode_pre_adoption))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { isPreAdoptionMode = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isPreAdoptionMode) MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.scanner_mode_post_adoption))
                }
            }

            // Light Meter Overlay (Center)
            // Hardcoded Lux value for UI mockup, will be connected to sensor later.
            LightMeterOverlay(luxValue = 450f)
            
            // Camera capture action button will be at the bottom (handled later)
        }
    }
}
