import java.util.*




fun generateBoolGridsToSharedList(blockCount: Int, sharedList: MutableList<BoolGrid>, onGridAdded: (BoolGrid) -> Unit ={}) {
    //the grid must be of size: blockCount X blockCount
    require(blockCount > 0)
    fun lockAndAdd(grid: BoolGrid) {
        synchronized(sharedList)
        {
            sharedList.add(grid)
        }
        onGridAdded(grid)
    }

    if (blockCount == 1) {
        lockAndAdd(BoolGrid(1, 1).apply { toggle(0, 0) })
        return
    }


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

    fun generateNewGridsFromPrevious(grid: BoolGrid) {
        grid.addBorderMargin(1)
        val truesLocations = grid.getTruesLocations()
        Collections.shuffle(truesLocations)
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
    generateBoolGridsToSharedList(blockCount - 1, previousGrids,
            onGridAdded = ::generateNewGridsFromPrevious

    )



}