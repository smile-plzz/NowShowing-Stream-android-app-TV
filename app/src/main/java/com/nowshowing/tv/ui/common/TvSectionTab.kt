package com.nowshowing.tv.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.ui.theme.*

/**
 * TV Navigation Tab following Apple-like restraint (Section E5):
 * Clean text tab, selected primary text + 2dp accent underline, secondary text otherwise, no heavy pill backgrounds.
 */
@Composable
fun TvSectionTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tab_scale"
    )

    val textColor = when {
        isFocused -> NsAccent
        isSelected -> NsTextPrimary
        else -> NsTextSecondary
    }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) NsSurface2.copy(alpha = 0.5f) else Color.Transparent)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .focusable(interactionSource = interaction)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // 2dp accent underline when selected or focused
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (isSelected || isFocused) 20.dp else 0.dp)
                .background(if (isFocused) NsAccent else if (isSelected) NsTextPrimary else Color.Transparent)
        )
    }
}
