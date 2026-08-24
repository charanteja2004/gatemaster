package com.gatemaster.app.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml

/**
 * Renders the light inline markup that appears in question text — <b>, <i>,
 * <sub>, <sup>, and entities like &nbsp;.
 *
 * Some GATE questions depend on this: "Select the statement in which the
 * underlined word is used correctly" is meaningless if the tags are shown
 * literally or stripped.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = LocalContentColor.current,
) {
    val annotated = remember(html) {
        runCatching { androidx.compose.ui.text.AnnotatedString.fromHtml(html) }
            .getOrElse { androidx.compose.ui.text.AnnotatedString(html) }
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
    )
}
