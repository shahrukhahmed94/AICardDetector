package com.shahrukh.aicarddetector.libexposer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun CropImageBox(
    uri: Uri,
    onCropDone: (Uri) -> Unit,
    onNotAbleToCrop: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val imageBitmap: Bitmap? = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    var tl = Offset.Zero
    var br = Offset.Zero
    var sw by remember {
        mutableStateOf(1f)
    }

    var initTL by remember { mutableStateOf(Offset.Zero) }
    var initBR by remember { mutableStateOf(Offset.Zero) }

    if (imageBitmap != null) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black
                ),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(title = { },
                    actions = {
                        IconButton(
                            onClick = {

                                val ar = imageBitmap.width / sw

                                val x = ((tl.x - initTL.x) * ar).toInt()
                                val y = ((tl.y - initTL.y) * ar).toInt()
                                val cropWidth = ((br.x - tl.x) * ar).toInt()
                                val cropHeight = ((br.y - tl.y) * ar).toInt()

                                if (x >= 0 &&
                                    y >= 0 &&
                                    cropHeight >= 0 &&
                                    cropWidth >= 0 &&
                                    cropWidth + x <= imageBitmap.width &&
                                    cropHeight + y <= imageBitmap.height
                                ) {
                                    val croppedImage = Bitmap.createBitmap(
                                        imageBitmap,
                                        x,
                                        y,
                                        cropWidth,
                                        cropHeight
                                    )
                                    onCropDone(getImageUri(context, croppedImage))
                                } else {
                                    onNotAbleToCrop()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                null
                            )
                        }
                    }
                )
            }
        ) { paddingVal ->
            Box(
                modifier = Modifier
                    .padding(top = paddingVal.calculateTopPadding())
                    .background(Color.Black)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val d = LocalDensity.current
                var firstTime by remember {
                    mutableStateOf(true)
                }

                var initialOffsetTL by remember { mutableStateOf(Offset.Zero) }
                var initialOffsetBR by remember { mutableStateOf(Offset.Zero) }

                var topLeft by remember { mutableStateOf(initialOffsetTL) }
                var bottomRight by remember { mutableStateOf(initialOffsetBR) }

                val minimumDistance = 400f

                tl = topLeft
                br = bottomRight

                Image(
                    imageBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .onGloballyPositioned { layoutCoordinates ->
                            if (firstTime) {
                                firstTime = false
                                var rect = layoutCoordinates.boundsInRoot()
                                rect = rect.copy(
                                    top = rect.top - (2*(paddingVal.calculateTopPadding().value+paddingVal.calculateBottomPadding().value))
                                )
                                topLeft = rect.topLeft
                                bottomRight = rect.bottomRight
                                initialOffsetTL = rect.topLeft
                                initialOffsetBR = rect.bottomRight
                                initTL = initialOffsetTL
                                initBR = initialOffsetBR
                                sw = rect.bottomRight.x - rect.topLeft.x
                            }
                        },
                )
                val stateTL = rememberTransformableState { _, panChange, _ ->
                    if (bottomRight.x - topLeft.x >= minimumDistance &&
                        bottomRight.y - topLeft.y >= minimumDistance
                    ) {
                        topLeft = Offset(
                            x = max(initialOffsetTL.x, topLeft.x + panChange.x),
                            y = max(initialOffsetTL.y, topLeft.y + panChange.y)
                        )
                    } else {
                        if (panChange.x < 0) {
                            topLeft = Offset(
                                x = max(initialOffsetTL.x, topLeft.x + panChange.x),
                                y = max(initialOffsetTL.y, topLeft.y)
                            )
                        }
                        if (panChange.y < 0) {
                            topLeft = Offset(
                                x = max(initialOffsetTL.x, topLeft.x),
                                y = max(initialOffsetTL.y, topLeft.y + panChange.y)
                            )
                        }
                    }
                }
                val stateBR = rememberTransformableState { _, panChange, _ ->
                    if (bottomRight.x - topLeft.x >= minimumDistance &&
                        bottomRight.y - topLeft.y >= minimumDistance
                    ) {
                        bottomRight = Offset(
                            x = min(initialOffsetBR.x, bottomRight.x + panChange.x),
                            y = min(initialOffsetBR.y, bottomRight.y + panChange.y)
                        )
                    } else {
                        if (panChange.x > 0) {
                            bottomRight = Offset(
                                x = min(initialOffsetBR.x, bottomRight.x + panChange.x),
                                y = min(initialOffsetBR.y, bottomRight.y)
                            )
                        }
                        if (panChange.y > 0) {
                            bottomRight = Offset(
                                x = min(initialOffsetBR.x, bottomRight.x),
                                y = min(initialOffsetBR.y, bottomRight.y + panChange.y)
                            )
                        }
                    }
                }
                val stateTR = rememberTransformableState { _, panChange, _ ->
                    if (bottomRight.x - topLeft.x >= minimumDistance &&
                        bottomRight.y - topLeft.y >= minimumDistance
                    ) {
                        bottomRight = Offset(
                            x = min(initialOffsetBR.x, bottomRight.x + panChange.x),
                            y = min(initialOffsetBR.y, bottomRight.y)
                        )
                        topLeft = Offset(
                            x = max(initialOffsetTL.x, topLeft.x),
                            y = max(initialOffsetTL.y, topLeft.y + panChange.y)
                        )
                    } else {
                        if (panChange.x > 0) {
                            bottomRight = Offset(
                                x = min(initialOffsetBR.x, bottomRight.x + panChange.x),
                                y = min(initialOffsetBR.y, bottomRight.y)
                            )
                        }
                        if (panChange.y < 0) {
                            topLeft = Offset(
                                x = max(initialOffsetTL.x, topLeft.x),
                                y = max(initialOffsetTL.y, topLeft.y + panChange.y)
                            )
                        }
                    }
                }
                val stateBL = rememberTransformableState { _, panChange, _ ->
                    if (bottomRight.x - topLeft.x >= minimumDistance &&
                        bottomRight.y - topLeft.y >= minimumDistance
                    ) {
                        bottomRight = Offset(
                            x = min(initialOffsetBR.x, bottomRight.x),
                            y = min(initialOffsetBR.y, bottomRight.y + panChange.y)
                        )
                        topLeft = Offset(
                            x = max(initialOffsetTL.x, topLeft.x + panChange.x),
                            y = max(initialOffsetTL.y, topLeft.y)
                        )
                    } else {
                        if (panChange.y > 0) {
                            bottomRight = Offset(
                                x = min(initialOffsetBR.x, bottomRight.x),
                                y = min(initialOffsetBR.y, bottomRight.y + panChange.y)
                            )
                        }
                        if (panChange.x < 0) {
                            topLeft = Offset(
                                x = max(initialOffsetTL.x, topLeft.x + panChange.x),
                                y = max(initialOffsetTL.y, topLeft.y)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color.Transparent)
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = .99f
                        }
                    ) {
                        // Adding scrim to whole screen
                        drawRect(
                            color = Color(0xAA000000),
                            size = size
                        )

                        // Transparent area
                        drawRect(
                            topLeft = topLeft,
                            size = Size(
                                bottomRight.x - topLeft.x,
                                bottomRight.y - topLeft.y
                            ),
                            color = Color.Transparent,
                            blendMode = BlendMode.Clear,
                            style = Fill
                        )
                        // Border
                        drawRect(
                            topLeft = topLeft,
                            size = Size(
                                bottomRight.x - topLeft.x,
                                bottomRight.y - topLeft.y
                            ),
                            color = Color.White,
                            blendMode = BlendMode.Color,
                            style = Stroke(5f)
                        )
                    }
                    val cornerBoxSize = 30.dp
                    // Top Left Corner Box
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (topLeft.x / d.density).dp,
                                y = (topLeft.y / d.density).dp
                            )
                            .size(cornerBoxSize)
                            .transformable(stateTL)
                            .sideBorder(2.dp, Color.White, Side.LEFT)
                            .sideBorder(2.dp, Color.White, Side.TOP)
                    )
                    // Top
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((((((bottomRight.x - topLeft.x) / 2) + topLeft.x) / d.density).dp)) - cornerBoxSize / 2,
                                y = (topLeft.y / d.density).dp
                            )
                            .size(cornerBoxSize)
                            .transformable(stateTL)
                            .sideBorder(2.dp, Color.White, Side.TOP)
                    )
                    // Top Right Corner Box
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (bottomRight.x / d.density).dp - cornerBoxSize,
                                y = (topLeft.y / d.density).dp
                            )
                            .size(cornerBoxSize)
                            .transformable(stateTR)
                            .sideBorder(side = Side.RIGHT)
                            .sideBorder(side = Side.TOP)
                    )
                    // Left
                    Box(
                        modifier = Modifier
                            .offset(
                                y = ((((((bottomRight.y - topLeft.y) / 2) + topLeft.y) / d.density).dp)) - cornerBoxSize / 2,
                                x = (topLeft.x / d.density).dp
                            )
                            .size(cornerBoxSize)
                            .transformable(stateTL)
                            .sideBorder(side = Side.LEFT)
                    )
                    // Bottom Left Corner Box
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (topLeft.x / d.density).dp,
                                y = (bottomRight.y / d.density).dp - cornerBoxSize
                            )
                            .size(cornerBoxSize)
                            .transformable(stateBL)
                            .sideBorder(side = Side.LEFT)
                            .sideBorder(side = Side.BOTTOM)
                    )
                    // Bottom
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((((((bottomRight.x - topLeft.x) / 2) + topLeft.x) / d.density).dp)) - cornerBoxSize / 2,
                                y = (bottomRight.y / d.density).dp - cornerBoxSize
                            )
                            .size(cornerBoxSize)
                            .transformable(stateBR)
                            .sideBorder(side = Side.BOTTOM)
                    )
                    // Bottom-right Corner Box
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (bottomRight.x / d.density).dp - cornerBoxSize,
                                y = (bottomRight.y / d.density).dp - cornerBoxSize
                            )
                            .size(cornerBoxSize)
                            .transformable(stateBR)
                            .sideBorder(side = Side.RIGHT)
                            .sideBorder(side = Side.BOTTOM)
                    )
                    // Right
                    Box(
                        modifier = Modifier
                            .offset(
                                y = ((((((bottomRight.y - topLeft.y) / 2) + topLeft.y) / d.density).dp)) - cornerBoxSize / 2,
                                x = (bottomRight.x / d.density).dp - cornerBoxSize
                            )
                            .size(cornerBoxSize)
                            .transformable(stateBR)
                            .sideBorder(side = Side.RIGHT)
                    )
                }
            }
        }
    } else {
        onBackPressed()
    }
}

@Composable
fun Modifier.sideBorder(
    thickness: Dp = 2.dp,
    color: Color = Color.White,
    side: Side
): Modifier {
    val density = LocalDensity.current
    val strokeWidthPx = density.run { thickness.toPx() }
    return this then Modifier.drawBehind {
        val width = size.width
        val height = size.height

        if (side == Side.TOP) {
            drawLine(
                color = color,
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = width, y = 0f),
                strokeWidth = strokeWidthPx
            )
        }
        if (side == Side.LEFT) {
            drawLine(
                color = color,
                start = Offset(x = 2f, y = 0f),
                end = Offset(x = 2f, y = height),
                strokeWidth = strokeWidthPx
            )
        }
        if (side == Side.RIGHT) {
            drawLine(
                color = color,
                start = Offset(x = width - 2, y = 0f),
                end = Offset(x = width - 2, y = height),
                strokeWidth = strokeWidthPx
            )
        }
        if (side == Side.BOTTOM) {
            drawLine(
                color = color,
                start = Offset(x = 0f, y = height),
                end = Offset(x = width, y = height),
                strokeWidth = strokeWidthPx
            )
        }
    }
}

enum class Side {
    RIGHT,
    LEFT,
    TOP,
    BOTTOM
}

public fun getImageUri(inContext: Context?, inImage: Bitmap): Uri {

    val tempFile = File.createTempFile("tempImage", ".png")
    val bytes = ByteArrayOutputStream()
    inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes)
    val bitmapData = bytes.toByteArray()

    val fileOutPut = FileOutputStream(tempFile)
    fileOutPut.write(bitmapData)
    fileOutPut.flush()
    fileOutPut.close()
    return Uri.fromFile(tempFile)
}