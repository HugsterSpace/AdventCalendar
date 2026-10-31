package space.hugster.calendar.utils.menu.task

import com.cjcrafter.foliascheduler.TaskImplementation
import space.hugster.calendar.AdventCalendar
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("LeakingThis")
abstract class TaskTicker(
    delay: Int,
    period: Int,
    async: Boolean
) {
    protected val plugin: AdventCalendar
        get() = AdventCalendar.INSTANCE

    companion object
    {
        @JvmStatic
        val tickers: MutableList<TaskTicker> = CopyOnWriteArrayList()

        @JvmStatic
        fun cancelAll()
        {
            for (ticker in tickers)
            {
                ticker.taskId?.cancel()
            }
            tickers.clear()
        }
    }

    var ticks: Int = 0
        private set
    private var finishPreRun: Boolean = false
    private var taskId: TaskImplementation<*>? = null

    init
    {
        val initialDelay = maxOf(1L, delay.toLong())
        val periodTicks = maxOf(1L, period.toLong())
        val scheduler = plugin.foliaScheduler
        val runner: (TaskImplementation<*>) -> Unit = { run() }

        taskId = if (async) {
            scheduler.async().runAtFixedRate(runner, initialDelay, periodTicks)
        } else {
            scheduler.global().runAtFixedRate(runner, initialDelay, periodTicks)
        }
        tickers.add(this)
    }

    private fun run()
    {
        if (!finishPreRun) {
            ticks = getStartTick()
            preRun()
            finishPreRun = true
        }
        onRun()
        when (getTickType()) {
            TickType.COUNT_UP -> ticks++
            TickType.COUNT_DOWN -> ticks--
            TickType.NONE -> {}
        }
    }

    abstract fun onRun()

    open fun preRun() {}

    fun cancel()
    {
        taskId?.cancel()
        tickers.remove(this)
    }

    abstract fun getTickType(): TickType
    abstract fun getStartTick(): Int

    enum class TickType
    {
        COUNT_UP,
        COUNT_DOWN,
        NONE
    }
}
