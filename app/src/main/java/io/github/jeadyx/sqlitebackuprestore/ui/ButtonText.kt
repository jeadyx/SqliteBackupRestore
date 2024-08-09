package io.github.jeadyx.sqlitebackuprestore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun ButtonText(text: String, modifier: Modifier = Modifier, enabled: Boolean=true, onClick: () -> Unit) {
    Button(modifier = modifier
        .height(50.dp)
        .aspectRatio(3f),
        enabled = enabled,
        onClick=onClick
    ){
        Text(text)
    }
}