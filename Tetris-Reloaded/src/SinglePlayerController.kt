import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.net.URL
import java.util.*
import kotlin.concurrent.timerTask

class SinglePlayerController : Initializable {

    var game: Game? = null
    @FXML var lblMessage = Label()
    @FXML var lblRowsPopped = Label()
    @FXML var lblPiecesGenerated = Label()
    @FXML var lblPossibleCombinations = Label()
    @FXML var canvasNextPiece = Canvas()
    @FXML var canvasGame = Canvas()
    @FXML var gameContainer = HBox()
    @FXML var btnStart = Button()
    @FXML var btnRestart = Button()
    @FXML var btnSave = Button()
    @FXML var btnBack = Button()
    val stage = checkNotNull(mainStage)

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        btnStart.isFocusTraversable = false
        btnRestart.isFocusTraversable = false
        btnSave.isFocusTraversable = false
        btnBack.isFocusTraversable = false


    }

    fun loadGame(width: Int, height: Int, pieceSize: Int) {
        initiateGameObject(width, height, pieceSize)
        lblPossibleCombinations.text = getAmountOfPossibleCombinations(pieceSize).toString()
        val gameSizeRatio = width.toDouble() / height.toDouble()
        resizeGameCanvas(gameSizeRatio)
        setupGameContainerEvents(gameSizeRatio)
        setupKeyPressedEvents(stage.scene)
    }

    private fun setupGameContainerEvents(gameSizeRatio: Double) {
        gameContainer.heightProperty()?.addListener { _, _, _ -> resizeGameCanvas(gameSizeRatio) }
        gameContainer.widthProperty()?.addListener { _, _, _ -> resizeGameCanvas(gameSizeRatio) }
    }


    fun btnStart_Action() {
        val game = checkNotNull(this.game)
        when {
            game.isPlaying -> pauseGame()
            game.isPaused -> resumeGame()
            else -> startGame()
        }
    }

    fun btnRestart_Action() {
        val game = checkNotNull(this.game)
        btnStart.isVisible = true
        btnStart.text = "Pause"
        lblRowsPopped.text = "0"
        lblMessage.text = ""
        game.restart()
    }

    fun btnBack_Action() {
        if (game?.isPlaying ?: false)
            game?.pause()
        App.launchHomeScreen()
    }

    fun btnSave_Action() {

    }

    fun gameContainer_MouseClicked() {
        if (isGameStarted())
            btnStart.fire()
    }


    private fun game_End(game: Game) {
        lblMessage.text = "Game Over"
        btnStart.isVisible = false


    }

    private fun game_Change(game: Game) {
        updateGameCanvas()

    }

    private fun game_Ready(game: Game) {
        btnStart.isDisable = false
        lblMessage.text = "Ready"
    }

    private fun game_PieceChanged(game: Game) {
        updateNextPieceCanvas()
    }

    private fun game_RowsPopped(game: Game, rowsPopped: Int) {
        lblRowsPopped.text = game.rowsPopped.toString()
        game.delayMillis -= 5 * rowsPopped
        if (game.delayMillis < 50)
            game.delayMillis = 50
    }

    private fun game_PieceGenerated(game: Game) {
        lblPiecesGenerated.text = game.piecesGenerated.toString()

    }

    private fun startGame() {
        lblMessage.text = ""
        btnStart.text = "Pause"
        btnRestart.isDisable = false
        btnSave.isDisable = false
        updateNextPieceCanvas()
        game?.startIfReady()
    }

    private fun resumeGame() {
        game?.resume()
        lblMessage.text = ""
        btnStart.text = "Pause"
    }

    private fun pauseGame() {
        game?.pause()
        lblMessage.text = "Paused"
        btnStart.text = "Resume"
    }

    private fun resizeGameCanvas(ratio: Double) {
        resizeCanvas(
                checkNotNull(canvasGame),
                checkNotNull(gameContainer),
                ratio

        )
        if (game != null)
            updateGameCanvas()
    }

    private fun resizeCanvas(canvas: Canvas, container: Pane, sizeRatio: Double) {

        val reversedRatio = 1.0 / sizeRatio
        val conWidth = container.width
        val conHeight = container.height
        canvas.width = Math.min(conWidth, conHeight * sizeRatio)
        canvas.height = Math.min(conHeight, conWidth * reversedRatio)
    }


    private fun setupKeyPressedEvents(scene: Scene) {

        var downPressedTimer = Timer()
        var timerRunning = false
        fun startTimer() {
            downPressedTimer = Timer()
            val downTimerTask = timerTask {
                Platform.runLater {
                    if (game?.isPlaying ?: false)
                        game?.movePieceDown()
                }
            }
            downPressedTimer.scheduleAtFixedRate(downTimerTask, 0, 45)
            timerRunning = true
        }

        fun stopTimer() {
            downPressedTimer.cancel()
            downPressedTimer.purge()
            timerRunning = false
        }
        scene.setOnKeyPressed { e ->
            val code = e.code
            val gamePlaying = game?.isPlaying ?: false
            if (game?.isPlaying ?: false) {
                when (e.code) {
                    KeyCode.UP -> game?.rotatePiece()
                    KeyCode.LEFT -> game?.movePieceLeft()
                    KeyCode.RIGHT -> game?.movePieceRight()
                    KeyCode.SPACE -> game?.dropPiece()
                    KeyCode.DOWN -> {
                        if (!timerRunning)
                            startTimer()


                    }
                }
                when{
                    e.code == KeyCode.T && true-> {}
                }
            }

            when (e.code) {
                KeyCode.F11 -> stage.isFullScreen = !stage.isFullScreen
                KeyCode.ENTER -> btnStart.fire()
                KeyCode.R -> {
                    if (!btnRestart.isDisabled)
                        btnRestart.fire()
                }
            }

        }
        scene.setOnKeyReleased { e ->
            if (e.code == KeyCode.DOWN)
                stopTimer()


        }

    }

    private fun isGameStarted(): Boolean {
        val paused = game?.isPaused ?: false
        val playing = game?.isPlaying ?: false
        return paused || playing
    }


    private fun updateNextPieceCanvas() {
        val canvas = checkNotNull(canvasNextPiece)
        val graphics = canvas.graphicsContext2D
        val game = checkNotNull(game)
        val canvasSize = canvas.width
        val squareSize = canvasSize / game.squaresInPiece
        val piece = game.nextPiece
        val leftMargin = (canvasSize - piece.width * squareSize) / 2
        val topMargin = (canvasSize - piece.height * squareSize) / 2
        graphics.clearRect(0.0, 0.0, canvasSize, canvasSize)
        for ((x, y) in piece.getSquaresLocations()) {
            val left = x * squareSize + leftMargin
            val top = y * squareSize + topMargin
            drawSquare(graphics, piece.color, squareSize, left, top)
        }
    }

    private fun updateGameCanvas() {
        val canvas = checkNotNull(canvasGame)
        val graphics = canvas.graphicsContext2D
        val game = checkNotNull(game)
        val squareSize = canvas.width / game.width
        graphics.clearRect(0.0, 0.0, canvas.width, canvas.height)
        drawCanvasBorder(canvas)
        for ((x, y) in game.getSquaresLocations()) {
            val color = game[x, y].color
            val left = x * squareSize
            val top = y * squareSize
            drawSquare(graphics, color, squareSize, left, top)
        }

    }

    private fun drawSquare(graphics: GraphicsContext, color: Color, squareSize: Double, left: Double, top: Double, border: Int = 2) {
        graphics.fill = Color.BLACK
        graphics.fillRect(left, top, squareSize, squareSize)
        graphics.fill = color
        graphics.fillRect(
                left + border,
                top + border,
                squareSize - 2 * border,
                squareSize - 2 * border
        )
    }

    private fun drawCanvasBorder(canvas: Canvas) {
        val graphics = canvas.graphicsContext2D
        val width = canvas.width
        val height = canvas.height
        graphics.strokeLine(0.0, 0.0, width, 0.0)
        graphics.strokeLine(0.0, 0.0, 0.0, height)
        graphics.strokeLine(width, height, width, 0.0)
        graphics.strokeLine(width, height, 0.0, height)
    }

    private fun initiateGameObject(width: Int, height: Int, pieceSize: Int) {
        game = Game(
                width = width,
                height = height,
                squaresInPiece = pieceSize,
                onReady = this::game_Ready,
                onChange = this::game_Change,
                onEnd = this::game_End,
                onRowsPopped = this::game_RowsPopped,
                onPieceChanged = this::game_PieceChanged,
                onPieceGenerated = this::game_PieceGenerated,
                functionThatRunsCodeInUiThread = Platform::runLater

        )
    }


}
























