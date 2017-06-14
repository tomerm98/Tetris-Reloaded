class BoolGrid(width: Int, height: Int) {
    val width: Int get() = values.size
    val height: Int get() = values.first().size

    init {
        require(width > 0 && height > 0)
    }

    private var values: Array<Array<Boolean>> =
            Array(width, { Array(height, { false }) })


    fun getTruesLocations(): List<Pair<Int, Int>> {
        val locations = mutableListOf<Pair<Int, Int>>()
        forEach { x, y ->
            if (this[x, y])
                locations.add(Pair(x, y))
        }
        return locations
    }


    operator fun get(x: Int, y: Int) = values[x][y]
    operator fun set(x: Int, y: Int, value: Boolean) {
        values[x][y] = value
    }

    fun toggle(x: Int, y: Int) {
        values[x][y] = !values[x][y]
    }

    fun rotateRight() {
        val newGrid = BoolGrid(height, width)
        forEach { x, y ->
            newGrid[height - 1 - y, x] = this[x, y]
        }

        replaceGrid(newGrid)

    }

    fun rotateLeft() {
        val newGrid = BoolGrid(height, width)
        forEach { x, y ->
            newGrid[y, width - 1 - x] = this[x, y]
        }

        replaceGrid(newGrid)

    }

    fun copyTo(other: BoolGrid) {

        forEach { x, y ->
            other[x, y] = this[x, y]
        }

    }

    fun copy(): BoolGrid {
        val newGrid = BoolGrid(width, height)
        copyTo(newGrid)
        return newGrid
    }


    fun forEach(operation: (Int, Int) -> Unit) {
        for (x in 0 until width)
            for (y in 0 until height)
                operation(x, y)
    }

    operator override fun equals(other: Any?): Boolean {
        if (other !is BoolGrid)
            return false
        if (other.width != width || other.height != height)
            return false
        var b = true
        forEach { x, y ->
            if (other[x, y] != this[x, y])
                b = false
        }
        return b
    }

    fun addBorderMargin(amount: Int) {
        val newGrid = BoolGrid(width + 2 * amount, height + 2 * amount)
        forEach { x, y ->
            newGrid[x + amount, y + amount] = this[x, y]
        }
        replaceGrid(newGrid)
    }


    fun removeAllMargin() {
        val newWidth = width - leftMargin - rightMargin
        val newHeight = height - topMargin - bottomMargin
        val newGrid = BoolGrid(newWidth, newHeight)
        newGrid.forEach { x, y ->
            newGrid[x, y] = this[x + leftMargin, y + topMargin]
        }
        replaceGrid(newGrid)
    }

    private fun replaceGrid(newGrid: BoolGrid) {
        values = Array(newGrid.width, { Array(newGrid.height, { false }) })
        newGrid.copyTo(this)
    }

    val topMargin: Int get() {
        var margin = 0
        var truesInCurrentRow: Boolean
        for (y in 0 until height) {
            truesInCurrentRow = (0 until width).any { x -> this[x, y] }
            if (!truesInCurrentRow) margin++
            else return margin
        }
        return margin
    }

    val leftMargin: Int get() {
        var margin = 0
        var truesInCurrentColumn: Boolean
        for (x in 0 until width) {
            truesInCurrentColumn = (0 until height).any { y -> this[x, y] }
            if (!truesInCurrentColumn) margin++
            else return margin
        }
        return margin
    }

    val rightMargin: Int get() {
        var margin = 0
        var truesInCurrentColumn: Boolean
        for (x in width - 1 downTo 0) {
            truesInCurrentColumn = (0 until height).any { y -> this[x, y] }
            if (!truesInCurrentColumn) margin++
            else return margin

        }
        return margin
    }

    val bottomMargin: Int get() {
        var margin = 0
        var truesInCurrentRow: Boolean
        for (y in height - 1 downTo 0) {
            truesInCurrentRow = (0 until width).any { x -> this[x, y] }
            if (!truesInCurrentRow) margin++
            else return margin
        }
        return margin
    }


    override fun toString(): String {
        var s = ""
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = if (this[x, y]) 'O' else '*'
                s += "$c "
            }

            s += "\n"
        }
        return s.removeSuffix("\n")

    }


}
