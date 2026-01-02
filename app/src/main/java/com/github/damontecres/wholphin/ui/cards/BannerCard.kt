package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import org.jellyfin.sdk.model.api.ImageType

/**
 * Displays an image as a card. If no image is available, the name will be shown instead
 */
@Composable
fun BannerCard(
    name: String?,
    item: BaseItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerText: String? = null,
    played: Boolean = false,
    favorite: Boolean = false,
    playPercent: Double = 0.0,
    cornerImageItemId: java.util.UUID? = null,
    cornerImageType: ImageType = ImageType.LOGO,
    cardHeight: Dp = 120.dp,
    aspectRatio: Float = AspectRatios.WIDE,
    imageType: ImageType = ImageType.PRIMARY,
    overlayContent: (@Composable BoxScope.() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    focusedBorderShape: Shape = RoundedCornerShape(12.dp),
    isFocused: Boolean = false,
) {
    val imageUrlService = LocalImageUrlService.current
    val density = LocalDensity.current
    val cornerImageUrl =
        remember(cornerImageItemId, cornerImageType, density) {
            cornerImageItemId?.let { id ->
                val targetWidth = with(density) { 56.dp.roundToPx() }
                val targetHeight = with(density) { 32.dp.roundToPx() }
                imageUrlService.getItemImageUrl(
                    itemId = id,
                    imageType = cornerImageType,
                    maxWidth = targetWidth,
                    maxHeight = targetHeight,
                )
                    ?: imageUrlService.getItemImageUrl(
                        itemId = id,
                        imageType = ImageType.PRIMARY,
                        maxWidth = targetWidth,
                        maxHeight = targetHeight,
                    )
            }
        }
    val imageUrl =
        remember(item, cardHeight, density, imageType) {
            if (item != null) {
                val fillHeight =
                    if (cardHeight != Dp.Unspecified) {
                        with(density) { cardHeight.roundToPx() }
                    } else {
                        null
                    }
                imageUrlService.getItemImageUrl(
                    item,
                    imageType,
                    fillWidth = null,
                    fillHeight = fillHeight,
                )
                    ?: imageUrlService.getItemImageUrl(
                        item,
                        ImageType.PRIMARY,
                        fillWidth = null,
                        fillHeight = fillHeight,
                    )
            } else {
                null
            }
        }
    var imageError by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.size(cardHeight * aspectRatio, cardHeight),
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        colors =
            CardDefaults.colors(
//                containerColor = Color.Transparent,
            ),
        shape = CardDefaults.shape(focusedBorderShape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize(),
//                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!imageError && imageUrl.isNotNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = name ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .align(Alignment.Center),
                )
            }
            if (played || cornerText.isNotNullOrBlank() || cornerImageUrl.isNotNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                ) {
                    if (played && (playPercent <= 0 || playPercent >= 100)) {
                        WatchedIcon(Modifier.size(24.dp))
                    }
                    if (cornerImageUrl.isNotNullOrBlank()) {
                        Box(
                            modifier =
                                Modifier
                                    .size(56.dp, 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppColors.TransparentBlack50),
                        ) {
                            AsyncImage(
                                model = cornerImageUrl,
                                contentDescription = name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                            )
                        }
                    }
                    if (cornerText.isNotNullOrBlank()) {
                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        AppColors.TransparentBlack50,
                                        shape = RoundedCornerShape(25),
                                    ),
                        ) {
                            Text(
                                text = cornerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
            if (isFocused) {
                val baseColor = MaterialTheme.colorScheme.surface
                val highlight =
                    if (baseColor.luminance() > 0.5f) {
                        baseColor.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    }
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .border(
                                width = 2.dp,
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                highlight,
                                                highlight.copy(alpha = 0.1f),
                                                highlight,
                                            ),
                                        tileMode = TileMode.Clamp,
                                    ),
                                shape = focusedBorderShape,
                            ),
                )
            }
            if (favorite) {
                Text(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    color = colorResource(android.R.color.holo_red_light),
                    text = stringResource(R.string.fa_heart),
                    fontSize = 16.sp,
                    fontFamily = FontAwesome,
                )
            }
            if (playPercent > 0 && playPercent < 100) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .background(
                                MaterialTheme.colorScheme.tertiary,
                            ).clip(RectangleShape)
                            .height(Cards.playedPercentHeight)
                            .fillMaxWidth((playPercent / 100).toFloat()),
                )
            }
            overlayContent?.invoke(this)
        }
    }
}
