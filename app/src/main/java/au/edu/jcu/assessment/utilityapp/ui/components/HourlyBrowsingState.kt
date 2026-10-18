package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod

/** Lives with the weather page, including while its hourly section is off screen. */
class HourlyBrowsingState(
    selected: Long? = null,
    anchor: Long? = null,
    index: Int = 0,
    offset: Int = 0
) {
    var selectedStart by mutableStateOf(selected)
    internal var anchorStart: Long? = anchor
    internal var anchorOffset: Int = offset
    val listState = LazyListState(index, offset)

    companion object {
        val Saver = listSaver<HourlyBrowsingState, Long>(
            save = {
                listOf(it.selectedStart ?: Long.MIN_VALUE, it.anchorStart ?: Long.MIN_VALUE,
                    it.listState.firstVisibleItemIndex.toLong(), it.anchorOffset.toLong())
            },
            restore = {
                HourlyBrowsingState(it[0].takeUnless { v -> v == Long.MIN_VALUE },
                    it[1].takeUnless { v -> v == Long.MIN_VALUE }, it[2].toInt(), it[3].toInt())
            }
        )
    }
}

@Composable
fun rememberHourlyBrowsingState(periods: List<RainPeriod>, timezoneId: String): HourlyBrowsingState {
    val state = rememberSaveable(timezoneId, saver = HourlyBrowsingState.Saver) { HourlyBrowsingState() }
    val starts = periods.map { it.start.epochSecond }
    LaunchedEffect(starts) {
        val selectedExpired = state.selectedStart != null && state.selectedStart !in starts
        if (selectedExpired) {
            state.selectedStart = starts.firstOrNull()
            state.anchorStart = starts.firstOrNull()
            state.anchorOffset = 0
            state.listState.scrollToItem(0)
        } else {
            // LazyListState normally retains keys while laid out; the timestamp also covers disposal.
            val anchorIndex = starts.indexOf(state.anchorStart)
            if (anchorIndex >= 0) state.listState.scrollToItem(anchorIndex, state.anchorOffset)
            else if (state.anchorStart != null) state.listState.scrollToItem(0)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            val first = state.listState.layoutInfo.visibleItemsInfo.firstOrNull()
            (first?.key as? Long) to state.listState.firstVisibleItemScrollOffset
        }.collect { (start, offset) ->
            if (start != null) {
                state.anchorStart = start
                state.anchorOffset = offset
            }
        }
    }
    return state
}
