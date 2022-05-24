package org.kafka.common.widgets

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DismissableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    SnackbarHost(
        hostState = hostState,
        snackbar = {
            SwipeDismissSnackbar(
                data = it,
                onDismiss = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Wrapper around [Snackbar] to make it swipe-dismissable, using [SwipeToDismiss].
 */
@Composable
fun SwipeDismissSnackbar(
    data: SnackbarData,
    onDismiss: () -> Unit,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) }
) {
    Dismissable(onDismiss = onDismiss) {
        snackbar(data)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Dismissable(
    onDismiss: () -> Unit,
    directions: Set<DismissDirection> = setOf(
        DismissDirection.StartToEnd,
        DismissDirection.EndToStart
    ),
    content: @Composable () -> Unit
) {
    val dismissState = rememberDismissState {
        if (it != DismissValue.Default) {
            onDismiss.invoke()
        }
        true
    }
    SwipeToDismiss(
        state = dismissState,
        directions = directions,
        background = {},
        dismissContent = { content() }
    )
}