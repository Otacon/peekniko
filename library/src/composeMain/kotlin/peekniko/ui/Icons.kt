package peekniko.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal object Icons {

    val ArrowBack by toolbarIcon(
        name = "arrow_back",
        pathData = "M7.83,13l5.6,5.6L12,20L4,12L12,4l1.43,1.4L7.83,11H20v2H7.83Z",
    )

    val Close by toolbarIcon(
        name = "close",
        pathData = "M6.4,19L5,17.6L10.6,12L5,6.4L6.4,5L12,10.6L17.6,5L19,6.4L13.4,12L19,17.6L17.6,19L12,13.4L6.4,19Z",
    )

    val Delete by toolbarIcon(
        name = "delete",
        pathData = "M7,21q-0.82,0-1.41-0.59T5,19V6H4V4H9V3h6V4h5V6H19V19q0,0.82-0.59,1.41T17,21H7ZM17,6H7V19H17V6ZM9,17h2V8H9v9Zm4,0h2V8H13v9ZM7,6V19V6Z",
    )

    val Filter by toolbarIcon(
        name = "filter",
        pathData = "M10,18V16h4v2H10ZM6,13V11H18v2H6ZM3,8V6H21V8H3Z"
    )

    val FilterApplied by toolbarIcon(
        name = "filter_applied",
        pathData = "M3,6H21V8H3ZM6,11H14V13H6ZM10,16H13V18H10ZM16.5,20L13.5,17L14.9,15.6L16.5,17.2L21.1,12.6L22.5,14Z",
    )

    val Search by toolbarIcon(
        name = "search",
        pathData = "M19.6,21l-6.3-6.3q-0.75,0.6-1.72,0.95T9.5,16q-2.72,0-4.61-1.89Q3,12.23 3,9.5Q3,6.77 4.89,4.89T9.5,3t4.61,1.89T16,9.5q0,1.1-0.35,2.07T14.7,13.3L21,19.6L19.6,21ZM9.5,14q1.88,0 3.19-1.31T14,9.5T12.69,6.31T9.5,5T6.31,6.31T5,9.5t1.31,3.19T9.5,14Z",
    )

}

private fun toolbarIcon(name: String, pathData: String): Lazy<ImageVector> = lazy {
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black)
    ).build()
}
