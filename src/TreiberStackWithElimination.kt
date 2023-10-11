import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * @author Belousov Timofey
 */
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()
    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val eliminationCellIndex = randomCellIndex()

        if (eliminationArray.compareAndSet(eliminationCellIndex, CELL_STATE_EMPTY, element)) {
            for (i in 0..<ELIMINATION_WAIT_CYCLES) {
                if (eliminationArray.compareAndSet(eliminationCellIndex, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                    return true
                }
            }
            while (eliminationArray[eliminationCellIndex] == element) {
                if (eliminationArray.compareAndSet(eliminationCellIndex, element, CELL_STATE_EMPTY)) {
                    return false
                }
            }
            if (eliminationArray[eliminationCellIndex] == CELL_STATE_RETRIEVED) {
                eliminationArray.set(eliminationCellIndex, CELL_STATE_EMPTY)
                return true
            }
        }
        return false
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        val eliminationCellIndex = randomCellIndex()
        val eliminationCellValue = eliminationArray[eliminationCellIndex]
        if (eliminationCellValue != CELL_STATE_RETRIEVED && eliminationCellValue != CELL_STATE_EMPTY) {
            if (eliminationArray.compareAndSet(eliminationCellIndex, eliminationCellValue, CELL_STATE_RETRIEVED)) {
                @Suppress("UNCHECKED_CAST")
                return eliminationCellValue as? E  // Unsafe cast to a generic type ㄟ( ▔, ▔ )ㄏ
            }
        }
        return null
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}
