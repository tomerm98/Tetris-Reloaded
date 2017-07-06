import javafx.scene.paint.Color

data class GamePiece(val grid: BoolGrid, var top: Int = 0, var left: Int = 0, val color: Color = Color.BLACK){


    val width: Int
        get() {
            return grid.width
        }

    val height: Int
        get() {
            return grid.height
        }

    fun getSquaresLocations(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width)
            for (y in 0 until height)
                if (grid[x, y])
                    list.add(Pair(x, y))
        return list
    }

    override fun toString(): String {
        return grid.toString()
    }

    fun rotateRight() {
        grid.rotateRight()
    }

    fun rotateLeft() {
        grid.rotateLeft()
    }

    fun moveRight() {
        left++
    }

    fun moveLeft() {
        left--
    }

    fun moveDown() {
        top++
    }

    fun moveUp() {
        top--
    }


}