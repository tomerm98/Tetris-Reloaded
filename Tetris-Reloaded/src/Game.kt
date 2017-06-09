import javafx.application.Platform
import javafx.scene.paint.Color
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask

private val COLOR_LIST = listOf(
        Color.ORANGE,
        Color.PURPLE,
        Color.BLUE,
        Color.YELLOW,
        Color.RED,
        Color.GREEN,
        Color.CYAN
)

data class GameTimeStamp(
        val squareGrid: SquareGrid,
        val nextPiece: GamePiece,
        val time: Long,
        val totalRowsPopped: Int
)


class Game(
        val width: Int,
        val height: Int,
        val pieceSize: Int,
        var delayMillis: Long = 1000,
        val onReady: (Game) -> Unit = {},
        val onEnd: (Game) -> Unit = {},
        val onChange: (Game) -> Unit = {},
        val onPieceChanged: (Game) -> Unit = {},
        val onRowsPopped: (Game, rowsPopped: Int) -> Unit = { _,_-> },
        randomSeed: Long = System.currentTimeMillis()
) {
    private var totalSuspendedTime: Long = 0
    private var lastPauseTime: Long = 0
    private val initialDelayMillis = delayMillis
    private val boolGrids: MutableList<BoolGrid>
    private var currentPiece =GamePiece(BoolGrid(1,1))
    private val atomicIsPlaying: AtomicBoolean = AtomicBoolean(false)
    private val rng: Random
    private var timer = Timer()
    private val squareGrid: SquareGrid

    val history = mutableListOf<GameTimeStamp>()
    var nextPiece= GamePiece(BoolGrid(1,1))
    var rowsPopped = 0; private set
    var isPaused = false; private set
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
        squareGrid = createSynchronizedSquareGrid(width, height)
        rng = Random(randomSeed)

        thread {
            generateBoolGridsToSharedList(pieceSize, boolGrids)
        }
        thread {
            waitUntilReady()
            currentPiece = getRandomPiece()
            nextPiece = getRandomPiece()
            Platform.runLater { onReady(this) }
        }
    }

    operator fun get(x: Int, y: Int): GameSquare {
        return squareGrid[x][y]
    }

    operator fun set(x: Int, y: Int, value: GameSquare) {
        squareGrid[x][y] = value

    }

    fun getSquaresLocations(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width)
            for (y in 0 until height)
                if (this[x, y].visible)
                    list.add(Pair(x, y))
        return list
    }

    fun isGameReady(): Boolean {
        synchronized(boolGrids)
        {
            return !boolGrids.isEmpty()
        }
    }

    fun dropPiece() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            while (isPieceLocationLegal(currentPiece))
                currentPiece.moveDown()
            currentPiece.moveUp()
            addPieceToGrid(currentPiece)
        }
    }

    fun movePieceRight() {
        require(isPlaying)
        synchronized(currentPiece)
        {
            removePieceFromGrid(currentPiece)
            currentPiece.moveRight()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.moveLeft()
            addPieceToGrid(currentPiece)
        }
        screenChanged()
    }

    fun movePieceLeft() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.moveLeft()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.rotateRight()
            addPieceToGrid(currentPiece)
        }
        screenChanged()
    }

    fun movePieceDown() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.moveDown()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.moveUp()
            addPieceToGrid(currentPiece)
        }
        screenChanged()
    }

    fun rotatePiece() {
        require(isPlaying)
        synchronized(currentPiece) {
            removePieceFromGrid(currentPiece)
            currentPiece.rotateRight()
            if (!isPieceLocationLegal(currentPiece))
                currentPiece.rotateLeft()
            addPieceToGrid(currentPiece)
        }
        screenChanged()
    }

    fun startIfReady() {
        require(!isPlaying)
        if (!isGameReady()) return
        isPlaying = true
        addPieceToGrid(currentPiece)
        screenChanged()
        startTimer()

    }


    private fun end() {
        require(isPlaying)
        stopTimer()
        resetProperties()
        onEnd(this)
    }

    fun pause() {
        require(isPlaying)
        isPlaying = false
        isPaused = true
        lastPauseTime = System.currentTimeMillis()
        stopTimer()
    }

    fun resume() {
        require(!isPlaying && isPaused)
        isPlaying = true
        isPaused = false
        val currentTime = System.currentTimeMillis()
        totalSuspendedTime += currentTime - lastPauseTime
        startTimer()
    }

    fun restart() {
        stopTimer()
        resetProperties()
        onPieceChanged(this)
        startIfReady()
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

    private fun updateHistory() {
        val time = System.currentTimeMillis() - totalSuspendedTime
        history += GameTimeStamp(squareGrid, nextPiece, time, rowsPopped)
    }

    private fun resetProperties() {
        isPaused = false
        isPlaying = false
        totalSuspendedTime = 0
        delayMillis = initialDelayMillis
        currentPiece = getRandomPiece()
        nextPiece = getRandomPiece()
        history.clear()
        rowsPopped = 0
        for (x in 0 until width)
            for (y in 0 until height)
                this[x, y] = GameSquare()

    }

    private fun getRandomPiece(): GamePiece {
        synchronized(boolGrids) {
            require(!boolGrids.isEmpty())
            val randomIndex = rng.nextInt(boolGrids.size)
            val grid = boolGrids[randomIndex]

            val color = COLOR_LIST[randomIndex % COLOR_LIST.size]
            val left = width / 2 - grid.width / 2
            return GamePiece(
                    grid = grid,
                    color = color,
                    left = left
            )
        }
    }

    private fun startTimer() {
        timer = Timer()

        fun setUpNewPiece() {
            currentPiece = nextPiece
            nextPiece = getRandomPiece()
            if (isPieceLocationLegal(currentPiece)) {
                addPieceToGrid(currentPiece)
                popFullRows()
                onPieceChanged(this)
                screenChanged()
            }
            else {
                isPlaying = false
                Platform.runLater { onEnd(this) }
            }

        }

        fun moveCurrentPieceDown() {
            currentPiece.moveDown()
            addPieceToGrid(currentPiece)
            screenChanged()
        }

        val task = timerTask {
            if (isPlaying) {
                removePieceFromGrid(currentPiece)
                val tempPiece = currentPiece.copy().apply { moveDown() }
                if (isPieceLocationLegal(tempPiece))
                    moveCurrentPieceDown()
                else {
                    addPieceToGrid(currentPiece)
                    setUpNewPiece()
                }
            }
        }
        timer.scheduleAtFixedRate(task, 0, delayMillis)

    }

    private fun stopTimer() {
        timer.cancel()
        timer.purge()
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
        onRowsPopped(this, count)
    }

    private fun addPieceToGrid(piece: GamePiece) {
        for ((x, y) in piece.getSquaresLocations())
            this[x + piece.left, y + piece.top] = GameSquare(true, piece.color)
    }

    private fun removePieceFromGrid(piece: GamePiece) {
        for ((x, y) in piece.getSquaresLocations())
            this[x + piece.left, y + piece.top] = GameSquare()
    }

    private fun isPieceLocationLegal(piece: GamePiece): Boolean {

        for ((x, y) in piece.getSquaresLocations()) {
            val left = x + piece.left
            val top = y + piece.top
            if (left < 0 || top < 0 || left > width - 1 || top > height - 1)
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


    private fun screenChanged() {
        updateHistory()
        onChange(this)
    }


}
