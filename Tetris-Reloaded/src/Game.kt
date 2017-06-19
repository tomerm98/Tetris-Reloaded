import javafx.scene.paint.Color
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


class Game(
        val width: Int,
        val height: Int,
        val squaresInPiece: Int,
        val onReady: (Game) -> Unit = {},
        val onEnd: (Game) -> Unit = {},
        val onChange: (Game) -> Unit = {},
        val onPieceChanged: (Game) -> Unit = {},
        val onRowsPopped: (Game, rowsPopped: Int) -> Unit = { _,_-> },
        val onPieceGenerated: (Game) -> Unit = {},
        val functionThatRunsCodeInUiThread: (Runnable) -> Unit,
        var delayMillis: Long = GAME_DELAY_MILLIS_INITIAL,
        val randomSeed: Long = System.currentTimeMillis()
) {
    private var gameTask = GameTask(this)
    private var totalSuspendedTime: Long = 0
    private var lastPauseTime: Long = 0
    private val initialDelayMillis = delayMillis
    private val piecesCombinations = mutableListOf<BoolGrid>()
    private var currentPiece =GamePiece(BoolGrid(1,1))
    private val atomicIsPlaying: AtomicBoolean = AtomicBoolean(false)
    private val atomicIsPaused: AtomicBoolean = AtomicBoolean(false)
    private val rng= Random(randomSeed)
    private var timer = Timer()
    private val squareGrid= createSynchronizedSquareGrid(width, height)

    val history = mutableListOf<GameTimeStamp>()
    val timePlayed: Long; get() {
        if (history.size > 0) {
            return history.last().time -
                    history.first().time -
                    totalSuspendedTime
        }
        return 0
    }
    var nextPiece= GamePiece(BoolGrid(1,1))
    var rowsPopped = 0; private set
    val piecesGenerated: Int get() = piecesCombinations.size


    var isPaused: Boolean private set(value) {
        atomicIsPaused.set(value)
    } get() {
        return atomicIsPaused.get()
    }

    var isPlaying: Boolean private set(value) {
            atomicIsPlaying.set(value)
    } get() {
            return atomicIsPlaying.get()
        }


    fun load() {

            thread {
                generatePieceCombinationsToList(
                        squaresInPiece = squaresInPiece,
                        sharedList = piecesCombinations,
                        randomSeed = randomSeed,
                        onGridAdded = {
                            runUI { onPieceGenerated(this) }
                        }
                )
            }
            thread {
                waitUntilReady()
                currentPiece = getRandomPiece()
                nextPiece = getRandomPiece()
                runUI { onReady(this) }
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
        synchronized(piecesCombinations)
        {
            return !piecesCombinations.isEmpty()
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
       hitBottom()
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
                currentPiece.moveRight()
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
            removePieceFromGrid(currentPiece)
            val isHitBottom = !isPieceLocationLegal(currentPiece.copy().apply { moveDown() })
            addPieceToGrid(currentPiece)

            if (isHitBottom) {
                hitBottom()
            } else screenChanged()

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
        synchronized(piecesCombinations) {
            require(!piecesCombinations.isEmpty())
            val randomIndex = rng.nextInt(piecesCombinations.size)
            val grid = piecesCombinations[randomIndex]

            val color = COLOR_LIST[randomIndex % COLOR_LIST.size]
            val left = width / 2 - grid.width / 2
            return GamePiece(
                    grid = grid,
                    color = color,
                    left = left
            )
        }
    }

   private fun setUpNewPiece() {
        currentPiece = nextPiece
        nextPiece = getRandomPiece()
        if (isPieceLocationLegal(currentPiece)) {
            addPieceToGrid(currentPiece)
            onPieceChanged(this)
            screenChanged()
        }
        else end()

    }

    private fun startTimer() {
        gameTask = GameTask(this)
        timer.schedule(gameTask, delayMillis)

    }
    private fun stopTimer()
    {
        gameTask.running = false
    }


    private class GameTask(val game: Game) : TimerTask() {
        var running = true
        override fun run() {
            with(game) {

                if ( running) {
                    removePieceFromGrid(currentPiece)
                    currentPiece.moveDown()
                    if (isPieceLocationLegal(currentPiece)) {
                        addPieceToGrid(currentPiece)
                        runUI { screenChanged() }
                    } else {
                        currentPiece.moveUp()
                        addPieceToGrid(currentPiece)

                        runUI {
                            hitBottom()
                        }
                    }

                        gameTask = GameTask(game)
                        timer.schedule(gameTask, delayMillis)

                }
            }
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
        if (count > 0) {
            rowsPopped += count
            onRowsPopped(this, count)
        }
    }

    private fun addPieceToGrid(piece: GamePiece) {
        for ((x, y) in piece.getSquaresLocations())
            this[x + piece.left, y + piece.top] = GameSquare(true, piece.color)
    }

    private fun removePieceFromGrid(piece: GamePiece) {
        for ((x, y) in piece.getSquaresLocations())
            this[x + piece.left, y + piece.top] = GameSquare()
    }

    private fun hitBottom(){
        popFullRows()
        screenChanged()
        setUpNewPiece()
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

    private fun runUI(function: () -> Unit) = functionThatRunsCodeInUiThread(Runnable { function() })

    private fun screenChanged() {
        updateHistory()
        onChange(this)
    }


}
val GAME_DELAY_MILLIS_INITIAL: Long = 450
val GAME_DELAY_MILLIS_MIN: Long = 50
val GAME_DELAY_MILLIS_DROP: Long = 5
data class GameTimeStamp(
        val squareGrid: SquareGrid,
        val nextPiece: GamePiece,
        val time: Long,
        val totalRowsPopped: Int
)

private val COLOR_LIST = listOf(
        Color.ORANGE,
        Color.web("#EF79FC"), //purple
        Color.web("#1C36FF"), //blue
        Color.YELLOW,
        Color.web("#FF2323"), //red
        Color.CHARTREUSE, //green
        Color.AQUA //cyan
)