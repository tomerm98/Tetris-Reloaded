import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.net.URL
import java.util.*
import kotlin.concurrent.timerTask

class DuelController(val width: Int, val height: Int, val squaresInPiece: Int) : Initializable {

    var gameLeft: Game
    var gameRight: Game
    @FXML var lblMessage = Label()
    @FXML var lblRowsPoppedLeft = Label()
    @FXML var lblRowsPoppedRight = Label()
    @FXML var lblPiecesGenerated = Label()
    @FXML var lblPossibleCombinations = Label()
    @FXML var canvasNextPieceLeft = Canvas()
    @FXML var canvasNextPieceRight = Canvas()
    @FXML var canvasGameLeft = Canvas()
    @FXML var canvasGameRight = Canvas()
    @FXML var gameContainerLeft = HBox()
    @FXML var gameContainerRight = HBox()
    @FXML var btnStart = Button()
    @FXML var btnRestart = Button()
    @FXML var btnSave = Button()
    @FXML var btnBack = Button()
    @FXML var splitPane = SplitPane()

    val gameSizeRatio = width.toDouble() / height.toDouble()
    val possibleCombinations = getAmountOfPossibleCombinations(squaresInPiece)

    var dividerChangedByUser = true

    init {
        val randomSeed = System.currentTimeMillis()
        gameLeft = Game(
                width = width,
                height = height,
                squaresInPiece = squaresInPiece,
                onChange = this::gameLeft_Change,
                onEnd = this::gameLeft_End,
                onRowsPopped = this::gameLeft_RowsPopped,
                onPieceChanged = this::gameLeft_PieceChanged,
                onPieceGenerated = this::games_pieceGenerated,
                randomSeed = randomSeed,
                runLater = Platform::runLater

        )
        gameRight = Game(
                width = width,
                height = height,
                squaresInPiece = squaresInPiece,
                onChange = this::gameRight_Change,
                onEnd = this::gameRight_End,
                onRowsPopped = this::gameRight_RowsPopped,
                onPieceChanged = this::gameRight_PieceChanged,
                onPieceGenerated = this::games_pieceGenerated,
                randomSeed = randomSeed,
                runLater = Platform::runLater

        )
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        gameLeft.load()
        gameRight.load()
        setupEvents()
        lblPossibleCombinations.text = possibleCombinations.toString()
        resizeGameCanvases()
    }


    private fun btnStart_Action(event: ActionEvent) {
        when {
            gameLeft.isPlaying || gameRight.isPlaying -> pauseGames()
            gameLeft.isPaused || gameRight.isPaused -> resumeGames()
            else -> startGames()
        }
    }

    private fun btnRestart_Action(event: ActionEvent) {

        btnStart.isVisible = true
        btnStart.text = "Pause"
        lblRowsPoppedLeft.text = "0"
        lblRowsPoppedRight.text = "0"
        lblMessage.text = ""
        val randomSeed = System.currentTimeMillis()
        gameLeft.resetRNG(randomSeed)
        gameRight.resetRNG(randomSeed)
        gameLeft.restart()
        gameRight.restart()
    }

    private fun btnBack_Action(event: ActionEvent) {
        if (gameLeft.isPlaying)
            gameLeft.pause()

        if (gameRight.isPlaying)
            gameRight.pause()

        App.launchHomeScreen()
    }

    private fun btnSave_Action(event: ActionEvent) {
        if (gameLeft.isPlaying || gameRight.isPlaying)
            btnStart.fire()
        App.launchSaveDuelScreen(gameLeft, gameRight)
    }

    private fun gameContainerLeft_MouseClicked(event: MouseEvent) {
        if (gameLeft.isPaused || gameLeft.isPlaying)
            btnStart.fire()
    }

    private fun gameContainerRight_MouseClicked(event: MouseEvent) {
        if (gameRight.isPaused || gameRight.isPlaying)
            btnStart.fire()
    }

    private fun splitPaneDividerLeft_PositionChanged(obs: ObservableValue<out Number>, oldValue: Number, newValue: Number) {
        if (dividerChangedByUser) {
            dividerChangedByUser = false
            val otherDividerPositionProperty = splitPane.dividers.component2().positionProperty()
            val change = newValue.toDouble() - oldValue.toDouble()
            otherDividerPositionProperty.value -= change
        } else dividerChangedByUser = true
    }

    private fun splitPaneDividerRight_PositionChanged(obs: ObservableValue<out Number>, oldValue: Number, newValue: Number) {
        if (dividerChangedByUser) {
            dividerChangedByUser = false
            val otherDividerPositionProperty = splitPane.dividers.component1().positionProperty()
            val change = newValue.toDouble() - oldValue.toDouble()
            otherDividerPositionProperty.value -= change
        } else dividerChangedByUser = true
    }

    private fun setupEvents() {
        //when scene is initialized
        gameContainerLeft.sceneProperty().addListener { _, oldValue, newValue ->
            if (oldValue == null && newValue != null)
                setupKeyPressedEvents(newValue)
        }
        btnStart.setOnAction(this::btnStart_Action)
        btnRestart.setOnAction(this::btnRestart_Action)
        btnBack.setOnAction(this::btnBack_Action)
        btnSave.setOnAction(this::btnSave_Action)

        gameContainerLeft.setOnMouseClicked(this::gameContainerLeft_MouseClicked)
        gameContainerLeft.heightProperty().addListener { _, _, _ -> resizeGameCanvases() }
        gameContainerLeft.widthProperty().addListener { _, _, _ -> resizeGameCanvases() }

        gameContainerRight.setOnMouseClicked(this::gameContainerRight_MouseClicked)
        gameContainerRight.heightProperty().addListener { _, _, _ -> resizeGameCanvases() }
        gameContainerRight.widthProperty().addListener { _, _, _ -> resizeGameCanvases() }


        splitPane.dividers.component1().positionProperty().addListener(this::splitPaneDividerLeft_PositionChanged)
        splitPane.dividers.component2().positionProperty().addListener(this::splitPaneDividerRight_PositionChanged)


    }

    private fun gameLeft_Change(game: Game) {
        updateGameCanvas(canvasGameLeft, gameLeft)
    }

    private fun gameRight_Change(game: Game) {
        updateGameCanvas(canvasGameRight, gameRight)
    }

    private fun gameLeft_End(game: Game) {
        if (!gameRight.isPaused && !gameRight.isPlaying)
            endGames()
    }

    private fun gameRight_End(game: Game) {
        if (!gameLeft.isPaused && !gameLeft.isPlaying)
            endGames()
    }

    private fun gameLeft_RowsPopped(game: Game, rowsPopped: Int) {
        lblRowsPoppedLeft.text = gameLeft.rowsPopped.toString()
        gameLeft.delayMillis -= GAME_DELAY_MILLIS_DROP * rowsPopped
        if (gameLeft.delayMillis < GAME_DELAY_MILLIS_MIN)
            gameLeft.delayMillis = GAME_DELAY_MILLIS_MIN
    }

    private fun gameRight_RowsPopped(game: Game, rowsPopped: Int) {
        lblRowsPoppedRight.text = gameRight.rowsPopped.toString()
        gameRight.delayMillis -= GAME_DELAY_MILLIS_DROP * rowsPopped
        if (gameRight.delayMillis < GAME_DELAY_MILLIS_MIN)
            gameRight.delayMillis = GAME_DELAY_MILLIS_MIN
    }

    private fun gameLeft_PieceChanged(game: Game) {
        updateNextPieceCanvas(canvasNextPieceLeft, gameLeft)
    }

    private fun gameRight_PieceChanged(game: Game) {
        updateNextPieceCanvas(canvasNextPieceRight, gameRight)
    }

    private fun games_pieceGenerated(game: Game) {
        val combinations = Math.min(gameLeft.piecesGenerated, gameRight.piecesGenerated).toLong()
        lblPiecesGenerated.text = combinations.toString()
        if (combinations == possibleCombinations && gameLeft.isGameReady() && gameRight.isGameReady()) {
            btnStart.isDisable = false
            lblMessage.text = "Ready"
        }
    }


    private fun startGames() {
        lblMessage.text = ""
        btnStart.text = "Pause"
        btnRestart.isDisable = false
        btnSave.isDisable = false
        updateNextPieceCanvas(canvasNextPieceRight, gameRight)
        updateNextPieceCanvas(canvasNextPieceLeft, gameLeft)
        gameLeft.startIfReady()
        gameRight.startIfReady()
    }

    private fun resumeGames() {
        if (gameRight.isPaused)
            gameRight.resume()
        if (gameLeft.isPaused)
            gameLeft.resume()
        lblMessage.text = ""
        btnStart.text = "Pause"
    }

    private fun pauseGames() {
        if (gameLeft.isPlaying)
            gameLeft.pause()
        if (gameRight.isPlaying)
            gameRight.pause()
        lblMessage.text = "Paused"
        btnStart.text = "Resume"
    }

    private fun endGames() {
        lblMessage.text = "Game Over"
        btnStart.isVisible = false
    }


    private fun resizeGameCanvases() {

        resizeCanvas(canvasGameLeft, gameContainerLeft, gameSizeRatio)
        resizeCanvas(canvasGameRight, gameContainerRight, gameSizeRatio)
        updateGameCanvas(canvasGameLeft, gameLeft)
        updateGameCanvas(canvasGameRight, gameRight)
    }


    private fun updateNextPieceCanvas(canvas: Canvas, game: Game) {
        val graphics = canvas.graphicsContext2D
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

    private fun resizeCanvas(canvas: Canvas, container: Pane, sizeRatio: Double) {

        val reversedRatio = 1.0 / sizeRatio
        val conWidth = container.width
        val conHeight = container.height
        canvas.width = Math.min(conWidth, conHeight * sizeRatio)
        canvas.height = Math.min(conHeight, conWidth * reversedRatio)
    }

    private fun updateGameCanvas(canvas: Canvas, game: Game) {
        val graphics = canvas.graphicsContext2D
        val squareSize = canvas.width / game.width
        graphics.clearRect(0.0, 0.0, canvas.width, canvas.height)
        drawCanvasBorder(canvas)
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


    private fun setupKeyPressedEvents(scene: Scene) {
        val stage = scene.window as Stage
        var downPressedTimerLeft = Timer()
        var downPressedTimerRight = Timer()
        var timerLeftRunning = false
        var timerRightRunning = false

        fun startTimerLeft() {
            downPressedTimerLeft = Timer()
            val task = timerTask {
                Platform.runLater {
                    if (gameLeft.isPlaying)
                        gameLeft.movePieceDown()
                }
            }

            downPressedTimerLeft.scheduleAtFixedRate(task, 0, 45)
            timerLeftRunning = true
        }

        fun startTimerRight() {
            downPressedTimerRight = Timer()
            val task = timerTask {
                Platform.runLater {
                    if (gameRight.isPlaying)
                        gameRight.movePieceDown()
                }
            }

            downPressedTimerRight.scheduleAtFixedRate(task, 0, 45)
            timerRightRunning = true
        }

        fun stopTimerLeft() {
            downPressedTimerLeft.cancel()
            downPressedTimerLeft.purge()
            timerLeftRunning = false
        }

        fun stopTimerRight() {
            downPressedTimerRight.cancel()
            downPressedTimerRight.purge()
            timerRightRunning = false
        }
        scene.setOnKeyPressed { e ->

            when (e.code) {
                KeyCode.UP -> if (gameRight.isPlaying) gameRight.rotatePiece()
                KeyCode.LEFT -> if (gameRight.isPlaying) gameRight.movePieceLeft()
                KeyCode.RIGHT -> if (gameRight.isPlaying) gameRight.movePieceRight()
                KeyCode.SPACE -> if (gameRight.isPlaying) gameRight.dropPiece()
                KeyCode.DOWN -> if (gameRight.isPlaying && !timerRightRunning) startTimerRight()
                KeyCode.W -> if (gameLeft.isPlaying) gameLeft.rotatePiece()
                KeyCode.A -> if (gameLeft.isPlaying) gameLeft.movePieceLeft()
                KeyCode.D -> if (gameLeft.isPlaying) gameLeft.movePieceRight()
                KeyCode.SHIFT -> if (gameLeft.isPlaying) gameLeft.dropPiece()
                KeyCode.S -> if (gameLeft.isPlaying && !timerLeftRunning) startTimerLeft()
                KeyCode.F11 -> stage.isFullScreen = !stage.isFullScreen
                KeyCode.ENTER -> btnStart.fire()
            }


        }
        scene.setOnKeyReleased { e ->
            when (e.code) {
                KeyCode.DOWN -> stopTimerRight()
                KeyCode.S -> stopTimerLeft()

            }
        }
    }


}
















