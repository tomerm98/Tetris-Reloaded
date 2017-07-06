import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.net.URL
import java.util.*
import kotlin.concurrent.timerTask

class SinglePlayerController(val width: Int, val height: Int, val squaresInPiece: Int) : Initializable {

    var game: Game
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
    val gameSizeRatio = width.toDouble() / height.toDouble()
    val possibleCombinations =  getAmountOfPossibleCombinations(squaresInPiece)
    init {
        game = Game(
                width = width,
                height = height,
                squaresInPiece = squaresInPiece,
                onReady = this::game_Ready,
                onChange = this::game_Change,
                onEnd = this::game_End,
                onRowsPopped = this::game_RowsPopped,
                onPieceChanged = this::game_PieceChanged,
                onPieceGenerated = this::game_PieceGenerated,
                runLater = Platform::runLater

        )

    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        game.load()
        setupEvents()
        lblPossibleCombinations.text = possibleCombinations.toString()
        resizeGameCanvas()
    }


    private fun btnStart_Action(event: ActionEvent) {
        when {
            game.isPlaying -> pauseGame()
            game.isPaused -> resumeGame()
            else -> startGame()
        }
    }

    private fun btnRestart_Action(event: ActionEvent) {
        btnStart.isVisible = true
        btnStart.text = "Pause"
        lblRowsPopped.text = "0"
        lblMessage.text = ""
        game.restart()
    }

    private fun btnBack_Action(event: ActionEvent) {
        if (game.isPlaying)
            game.pause()
        App.launchHomeScreen()
    }

    private fun btnSave_Action(event: ActionEvent) {
        if (game.isPlaying)
            btnStart.fire()
        App.launchSaveSinglePlayerScreen(game)
    }

    private fun gameContainer_MouseClicked(event: MouseEvent) {
        if (game.isPlaying|| game.isPaused)
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
        game.delayMillis -= GAME_DELAY_MILLIS_DROP * rowsPopped
        if (game.delayMillis < GAME_DELAY_MILLIS_MIN)
            game.delayMillis = GAME_DELAY_MILLIS_MIN
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
        game.startIfReady()
    }

    private fun resumeGame() {
        game.resume()
        lblMessage.text = ""
        btnStart.text = "Pause"
    }

    private fun pauseGame() {
        game.pause()
        lblMessage.text = "Paused"
        btnStart.text = "Resume"
    }

    private fun resizeGameCanvas() {
        resizeCanvas(canvasGame, gameContainer, gameSizeRatio)
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


        val stage = scene.window as Stage
        var downPressedTimer = Timer()
        var timerRunning = false
        fun startTimer() {
            downPressedTimer = Timer()
            val downTimerTask = timerTask {
                Platform.runLater {
                    if (game.isPlaying)
                        game.movePieceDown()
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
            val gamePlaying = game.isPlaying

            when (e.code) {
                KeyCode.UP -> if (gamePlaying) game.rotatePiece()
                KeyCode.LEFT -> if (gamePlaying) game.movePieceLeft()
                KeyCode.RIGHT -> if (gamePlaying) game.movePieceRight()
                KeyCode.SPACE -> if (gamePlaying) game.dropPiece()
                KeyCode.DOWN -> if (gamePlaying && !timerRunning) startTimer()
                KeyCode.F11 -> stage.isFullScreen = !stage.isFullScreen
                KeyCode.ENTER -> btnStart.fire()
                KeyCode.R -> if (!btnRestart.isDisabled) btnRestart.fire()

            }


        }
        scene.setOnKeyReleased { e ->
            when (e.code) {
                KeyCode.DOWN -> stopTimer()
            }
        }
    }


    private fun updateNextPieceCanvas() {
        val graphics = canvasNextPiece.graphicsContext2D
        val canvasSize = canvasNextPiece.width
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
        val graphics = canvasGame.graphicsContext2D
        val squareSize = canvasGame.width / game.width
        graphics.clearRect(0.0, 0.0, canvasGame.width, canvasGame.height)
        drawCanvasBorder(canvasGame)
        for ((x, y) in game.getSquaresLocations()) {
            val color = game[x, y].color.toJavaFxColor()
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


    private fun setupEvents() {
        //when scene is initialized
        gameContainer.sceneProperty().addListener { _, oldValue, newValue ->
            if (oldValue == null && newValue != null)
                setupKeyPressedEvents(newValue)
        }

        btnStart.setOnAction(this::btnStart_Action)
        btnRestart.setOnAction(this::btnRestart_Action)
        btnBack.setOnAction(this::btnBack_Action)
        btnSave.setOnAction(this::btnSave_Action)
        gameContainer.setOnMouseClicked(this::gameContainer_MouseClicked)
        gameContainer.heightProperty()?.addListener { _, _, _ -> resizeGameCanvas() }
        gameContainer.widthProperty()?.addListener { _, _, _ -> resizeGameCanvas() }
    }



}
























