import java.util.*


fun generateBoolGridsToSharedList(
        squareCount: Int,
        sharedList: MutableList<BoolGrid>,
        onGridAdded: (BoolGrid) -> Unit = {},
        randomSeed: Long = System.currentTimeMillis()
) {
    fun lockAndAdd(grid: BoolGrid) {
        synchronized(sharedList)
        {
            sharedList.add(grid)
        }
        onGridAdded(grid)
    }
    require(squareCount > 0)
    if (squareCount == 1) {
        lockAndAdd(BoolGrid(1, 1).apply { toggle(0, 0) })
        return
    }

    fun generateNewGridsFromPrevious(grid: BoolGrid) {
        fun addToListIfNotDuplicate(grid: BoolGrid) {

            grid.removeAllMargin()
            for (gridFromList in sharedList) {
                val compare = gridFromList.copy()
                compare.removeAllMargin()
                if (grid == compare)
                    return
                grid.rotateRight()
                if (grid == compare)
                    return
                grid.rotateRight()
                if (grid == compare)
                    return
                grid.rotateRight()
                if (grid == compare)
                    return
            }
            lockAndAdd(grid)

        }
        grid.addBorderMargin(1)
        val truesLocations = grid.getTruesLocations()
        if (squareCount > 4)
            Collections.shuffle(truesLocations, Random(randomSeed))
        for ((x, y) in truesLocations) {
            if (!grid[x - 1, y]) {
                val newGrid = grid.copy()
                newGrid.toggle(x - 1, y)
                addToListIfNotDuplicate(newGrid)
            }
            if (!grid[x + 1, y]) {
                val newGrid = grid.copy()
                newGrid.toggle(x + 1, y)
                addToListIfNotDuplicate(newGrid)
            }
            if (!grid[x, y - 1]) {
                val newGrid = grid.copy()
                newGrid.toggle(x, y - 1)
                addToListIfNotDuplicate(newGrid)
            }
            if (!grid[x, y + 1]) {
                val newGrid = grid.copy()
                newGrid.toggle(x, y + 1)
                addToListIfNotDuplicate(newGrid)
            }
        }
    }

    val previousGrids = mutableListOf<BoolGrid>()
    generateBoolGridsToSharedList(
            squareCount = squareCount - 1,
            sharedList = previousGrids,
            onGridAdded = ::generateNewGridsFromPrevious,
            randomSeed = randomSeed
    )


}

fun getAmountOfPossibleCombinations(squareCount: Int): Long {
    if (squareCount > 30 || squareCount < 1)
        return -1
    return possibleCombinationsAmounts[squareCount - 1]
}

private val possibleCombinationsAmounts = listOf(
        1,
        1,
        2,
        7,
        18,
        60,
        196,
        704,
        2500,
        9189,
        33896,
        126759,
        476270,
        1802312,
        6849777,
        26152418,
        100203194,
        385221143,
        1485200848,
        5741256764,
        22245940545,
        86383382827,
        336093325058,
        1309998125640,
        5114451441106,
        19998172734786,
        78306011677182,
        307022182222506,
        1205243866707468,
        4736694001644862

)
