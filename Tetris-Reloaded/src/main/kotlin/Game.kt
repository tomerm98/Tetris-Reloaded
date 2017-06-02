import javafx.scene.paint.Color
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


private val COLOR_LIST = listOf(
        Color.ORANGE,
        Color.PURPLE,
        Color.BLUE,
        Color.YELLOW,
        Color.RED,
        Color.GREEN,
        Color.CYAN
)
typealias SquareGrid = MutableList<MutableList<GameSquare>>


fun createSquareGrid(width: Int, height: Int, visible: Boolean = false, color: Color = Color.BLACK): SquareGrid {
    return Collections.synchronizedList(
            MutableList(width, {
                Collections.synchronizedList(
                        MutableList(height, {
                            GameSquare(
                                    visible = visible,
                                    color = color
                            )
                        }))
            }))
}


class Game(
        val width: Int,
        val height: Int,
        val pieceSize: Int,
        var delayMillis: Long = 1000,
        val onReady: (Game) -> Unit = {},
        val onStart: (Game) -> Unit = {},
        val onEnd: (Game, totalRowsPopped: Int) -> Unit = { _, _ -> },
        val onChange: (Game) -> Unit = {},
        val onRowsPop: (Game, rowsPopped: Int) -> Unit = { _, _ -> },
        val onPieceChanged: (Game) -> Unit = {}
) {

    private val boolGrids: MutableList<BoolGrid>
    var squareGrid: SquareGrid private set
    private var currentPiece: GamePiece = GamePiece(BoolGrid(1, 1))
    var nextPiece: GamePiece = GamePiece(BoolGrid(1, 1)); private set
    var rowsPopped = 0; private set
    private val atomicIsPlaying: AtomicBoolean = AtomicBoolean(false)
    var isPlaying: Boolean
        private set(value) {
            atomicIsPlaying.set(value)
        }
        get() {
            return atomicIsPlaying.get()
        }


    init {
        require(width > 0 && height > 0 && delayMillis > 0 && pieceSize > 0)
        boolGrids = mutableListOf<BoolGrid>()
        squareGrid = createSquareGrid(width, height)
        thread {
            generateBoolGridsToSharedList(pieceSize, boolGrids)
        }
        thread{
            waitUntilReady()
            onReady(this)
        }
    }

    operator fun get(x: Int, y: Int): GameSquare {
        return squareGrid[x][y]
    }

    operator fun set(x: Int, y: Int, value: GameSquare) {
        squareGrid[x][y] = value

    }


    fun isGameReady(): Boolean {
        synchronized(boolGrids)
        {
            return !boolGrids.isEmpty()
        }
    }

    fun moveRight() {
        require(isPlaying)
        synchronized(currentPiece)
        {
            removePieceFromGrid(currentPiece)
            currentPiece.moveRight()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.moveLeft()
            addPieceToGrid(currentPiece)
        }
        onChange(this)
    }

    fun moveLeft() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.moveLeft()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.rotateRight()
            addPieceToGrid(currentPiece)
        }
        onChange(this)
    }

    fun moveDown() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.moveDown()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.moveUp()
            addPieceToGrid(currentPiece)
        }
        onChange(this)
    }

    fun rotate() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.rotateRight()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.rotateLeft()
            addPieceToGrid(currentPiece)
        }
        onChange(this)
    }

    fun startIfReady() {
        require(!isPlaying)
        if (!isGameReady()) return
        isPlaying = true
        currentPiece = getRandomPiece()
        nextPiece = getRandomPiece()
        addPieceToGrid(currentPiece)
        onStart(this)
        startTimerActions()
    }

    fun end() {
        require(isPlaying)
        isPlaying = false
        val tempRowsPopped = rowsPopped
        delayMillis = 1000
        rowsPopped = 0
        squareGrid = createSquareGrid(width, height)
        onEnd(this, tempRowsPopped)


    }

    fun pasue() {
        require(isPlaying)
        isPlaying = false
    }

    fun resume() {
        require(!isPlaying)
        isPlaying = true
    }


    override fun toString(): String {
        var s = ""
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = if (this[x, y].visible) "O" else "*"
                s += "$c "
            }

            s += "\n"
        }
        return s.removeSuffix("\n")
    }

    private fun getRandomPiece(): GamePiece {
        synchronized(boolGrids) {
            require(!boolGrids.isEmpty())
            val index = Random().nextInt(boolGrids.size)
            val grid = boolGrids[index]

            val color = COLOR_LIST[index % COLOR_LIST.size]
            val left = width / 2 - grid.width / 2
            return GamePiece(
                    grid = grid,
                    color = color,
                    left = left
            )
        }
    }

    private fun startTimerActions() {

        fun setUpNewPiece() {
            currentPiece = nextPiece
            nextPiece = getRandomPiece()
            if (isPieceLocationLegal(currentPiece)) {
                addPieceToGrid(currentPiece)
                popFullRows()
                onPieceChanged(this)
                onChange(this)
            }
            else end()

        }

        fun moveCurrentPieceDown() {
            currentPiece.moveDown()
            addPieceToGrid(currentPiece)
            onChange(this)
        }

        while (isPlaying) {
            removePieceFromGrid(currentPiece)
            val tempPiece = currentPiece.copy().apply { moveDown() }
            if (isPieceLocationLegal(tempPiece))
                moveCurrentPieceDown()
            else
            {
                addPieceToGrid(currentPiece)
                setUpNewPiece()
            }

            Thread.sleep(delayMillis)

        }


    }

    private fun popFullRows() {
        fun isRowFull(top: Int): Boolean {
            return (0 until width).all { this[it, top].visible }
        }

        fun popTopRow() {
            for (x in 0 until width)
                this[x, 0] = GameSquare()
        }

        fun popRow(top: Int) {
            require(top >= 0)
            if (top != 0)
                for (y in top - 1 downTo 0)
                    for (x in 0 until width)
                        this[x, y + 1] = this[x, y]
            popTopRow()
        }

        var count = 0
        synchronized(squareGrid)
        {
            for (y in 0 until height)
                if (isRowFull(y)) {
                    popRow(y)
                    count++
                }
        }
        onRowsPop(this, count)
    }

    private fun addPieceToGrid(piece: GamePiece) {
        for ((x, y) in piece.getLocations())
            this[x + piece.left, y + piece.top] = GameSquare(true, piece.color)
    }

    private fun removePieceFromGrid(piece: GamePiece) {
        for ((x, y) in piece.getLocations())
            this[x + piece.left, y + piece.top] = GameSquare()
    }

    private fun isPieceLocationLegal(piece: GamePiece): Boolean {

        for ((x, y) in piece.getLocations()) {
            val left = x+ piece.left
            val top = y + piece.top
            if (left < 0 || top < 0 || left > width-1 || top > height-1)
                return false
            if (this[left, top].visible)
                return false
        }
        return true
    }

    private fun waitUntilReady(checkDelay: Long = 100) {
        while (!isGameReady())
            Thread.sleep(checkDelay)

    }


}
