import javafx.scene.paint.Color
import java.io.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
* @property runLater A function that will execute code in the UI Thread
*/
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
        val runLater: (Runnable) -> Unit,
        var delayMillis: Long = GAME_DELAY_MILLIS_INITIAL,
        var randomSeed: Long = System.currentTimeMillis()
) {
    private var gameTask = GameTask(this)
    private var totalSuspendedTime: Long = 0
    private var lastPauseTime: Long = 0
    private val initialDelayMillis = delayMillis
    private val piecesCombinations = mutableListOf<BoolGrid>()
    private var currentPiece =GamePiece(BoolGrid(1,1))
    private val atomicIsPlaying: AtomicBoolean = AtomicBoolean(false)
    private val atomicIsPaused: AtomicBoolean = AtomicBoolean(false)
    private var rng = Random(randomSeed)
    private var timer = Timer()
    private val squareGrid= createSynchronizedSquareGrid(width, height)

    val history = mutableListOf<GameTimeStamp>()
    var timeStarted: Long = 0; private set
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
                        onGridAdded = {
                            runUI { onPieceGenerated(this) }
                        }
                )
            }
            thread {
                waitUntilReady()
                runUI { onReady(this) }
            }

    }

    operator fun get(x: Int, y: Int): GameSquare {
        return squareGrid[x][y]
    }

    operator fun set(x: Int, y: Int, value: GameSquare) {
        squareGrid[x][y] = value

    }

    fun resetRNG(randomSeed: Long = this.randomSeed) {
        this.randomSeed = randomSeed
        rng = Random(this.randomSeed)
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
        timeStarted = System.currentTimeMillis()
        currentPiece = getRandomPiece()
        nextPiece = getRandomPiece()
        onPieceChanged(this)
        addPieceToGrid(currentPiece)
        screenChanged()
        startTimer()


    }


    private fun end() {
        stopTimer()
        isPlaying = false
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
        val time = System.currentTimeMillis() - totalSuspendedTime - timeStarted
        history += GameTimeStamp(squareGrid, time, rowsPopped)
    }

    private fun resetProperties() {
        isPaused = false
        isPlaying = false
        totalSuspendedTime = 0
        timeStarted = 0
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
                if (running) {
                    synchronized(currentPiece) {
                        removePieceFromGrid(currentPiece)
                        currentPiece.moveDown()
                        if (isPieceLocationLegal(currentPiece)) {
                            addPieceToGrid(currentPiece)
                            runUI { screenChanged() }
                        }
                        else {
                            currentPiece.moveUp()
                            addPieceToGrid(currentPiece)
                            runUI { hitBottom() }
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

    private fun runUI(function: () -> Unit) = runLater(Runnable { function() })

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
        val time: Long,
        val rowsPopped: Int
) : Serializable

abstract class GameSave(
        val date: Date,
        val width: Int,
        val height: Int,
        val squaresInPiece: Int,
        val id:String

) : Serializable, Comparable<GameSave>


class SinglePlayerSave(
        date: Date,
        width: Int,
        height: Int,
        squaresInPiece: Int,
        id: String,
        val timeStamps: List<GameTimeStamp>,
        val playerName: String,
        val totalRowsPopped: Int

) : GameSave(date, width, height, squaresInPiece, id), Serializable {
    override fun compareTo(other: GameSave): Int {
        return this.date.compareTo(other.date)
    }
}


class DuelSave(
        date: Date,
        width: Int,
        height: Int,
        squaresInPiece: Int,
        id: String,
        val timeStampsLeft: List<GameTimeStamp>,
        val timeStampsRight: List<GameTimeStamp>,
        val playerNameLeft: String,
        val playerNameRight: String,
        val totalRowsPoppedLeft: Int,
        val totalRowsPoppedRight: Int

) : GameSave(date, width, height, squaresInPiece, id), Serializable {
    override fun compareTo(other: GameSave): Int {
        return this.date.compareTo(other.date)
    }
}


private val COLOR_LIST = listOf(
        Color.ORANGE,
        Color.web("#1C36FF"), //blue
        Color.web("#EF79FC"), //purple
        Color.YELLOW,
        Color.web("#FF2323"), //red
        Color.AQUA, //cyan
        Color.CHARTREUSE //green
)