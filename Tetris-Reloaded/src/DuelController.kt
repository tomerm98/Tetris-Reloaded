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

class DuelController : Initializable{

    var gameLeft: Game? = null
    var gameRight: Game? = null
    @FXML var lblMessage = Label()
    @FXML var lblRowsPoppedLeft = Label()
    @FXML var lblRowsPoppedRight=Label()
    @FXML var lblPiecesGenerated =  Label()
    @FXML var lblPossibleCombinations= Label()
    @FXML var canvasNextPieceLeft = Canvas()
    @FXML var canvasNextPieceRight = Canvas()
    @FXML var canvasGameLeft= Canvas()
    @FXML var canvasGameRight=Canvas()
    @FXML var gameContainerLeft=HBox()
    @FXML var gameContainerRight=HBox()
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
        initiateGameObjects(width,height,pieceSize)
        lblPossibleCombinations.text = getAmountOfPossibleCombinations(pieceSize).toString()
        val gameSizeRatio = width.toDouble() / height.toDouble()
        resizeGameCanvases(gameSizeRatio)
        setupGameContainersEvents(gameSizeRatio)
        setupKeyPressedEvents(stage.scene)
    }
    fun btnStart_Action() {
        val gameLeft = checkNotNull(this.gameLeft)
        val gameRight = checkNotNull(this.gameRight)
        when {
            gameLeft.isPlaying -> pauseGameLeft()
            gameLeft.isPlaying -> pauseGameLeft()
            game.isPaused -> resumeGameRight()
            game.isPaused -> resumeGameRight()
            else -> startGames()
        }
    }

    fun btnRestart_Action() {
        val game = checkNotNull(this.game)
        btnStart?.isVisible = true
        btnStart?.text = "Pause"
        lblRowsPopped?.text = "0"
        lblMessage?.text = ""
        game.restart()
    }
    fun btnBack_Action(){
        if (game?.isPlaying ?: false)
            game?.pause()
        App.launchHomeScreen()
    }
    fun btnSave_Action() {

    }

    fun gameContainerLeft_MouseClicked() {
        if (isGameLeftStarted())
            btnStart?.fire()
    }
    fun gameContainerRight_MouseClicked() {
        if (isGameRightStarted())
            btnStart?.fire()
    }

    private fun resizeGameCanvases(ratio: Double) {
        resizeCanvas(
                checkNotNull(canvasGameLeft),
                checkNotNull(gameContainerLeft),
                ratio

        )
        resizeCanvas(
                checkNotNull(canvasGameRight),
                checkNotNull(gameContainerRight),
                ratio

        )

        if (gameLeft != null)
            updateCanvas(checkNotNull(canvasGameLeft), checkNotNull(gameLeft))
        if (gameRight != null)
            updateCanvas(checkNotNull(canvasGameRight), checkNotNull(gameRight))


    }

    private fun updateNextPieceCanvas(canvas:Canvas,game:Game) {
        val graphics = canvas.graphicsContext2D
        val canvasSize = canvas.width
        val squareSize = canvasSize /  game.squaresInPiece
        val piece = game.nextPiece
        val leftMargin = (canvasSize - piece.width*squareSize) /2
        val topMargin = (canvasSize - piece.height*squareSize) /2
        graphics.clearRect(0.0,0.0,canvasSize,canvasSize)
        for ((x, y) in piece.getSquaresLocations()) {
            val left = x * squareSize + leftMargin
            val top = y * squareSize + topMargin
            drawSquare(graphics, piece.color, squareSize, left,top)
        }
    }

    private fun resizeCanvas(canvas: Canvas, container: Pane, sizeRatio: Double) {

        val reversedRatio = 1.0 / sizeRatio
        val conWidth = container.width
        val conHeight = container.height
        canvas.width = Math.min(conWidth, conHeight * sizeRatio)
        canvas.height = Math.min(conHeight, conWidth * reversedRatio)
    }

    private fun updateCanvas(canvas:Canvas,game:Game) {
        val graphics = canvas.graphicsContext2D
        val squareSize = canvas.width / game.width
        graphics.clearRect(0.0, 0.0, canvas.width, canvas.height)
        drawCanvasBorder(canvas)
        for ((x,y) in game.getSquaresLocations()) {
            val color = game[x,y].color
            val left = x*squareSize
            val top = y*squareSize
            drawSquare(graphics,color,squareSize,left,top)
        }

    }

    private fun drawSquare(graphics: GraphicsContext, color: Color, squareSize: Double, left:Double, top: Double, border:Int=2)
    {
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
    private fun drawCanvasBorder(canvas: Canvas)
    {
        val graphics = canvas.graphicsContext2D
        val width = canvas.width
        val height = canvas.height
        graphics.strokeLine(0.0,0.0,width,0.0)
        graphics.strokeLine(0.0,0.0,0.0,height)
        graphics.strokeLine(width,height,width,0.0)
        graphics.strokeLine(width,height,0.0,height)
    }

    private fun isGameLeftStarted(): Boolean {
        val paused = gameLeft?.isPaused ?: false
        val playing = gameLeft?.isPlaying ?: false
        return paused || playing
    }
    private fun isGameRightStarted(): Boolean {
        val paused = gameRight?.isPaused ?: false
        val playing = gameRight?.isPlaying ?: false
        return paused || playing
    }
    private fun setupGameContainersEvents(gameSizeRatio: Double) {
        gameContainerLeft.heightProperty().addListener { _, _, _ -> resizeGameCanvases(gameSizeRatio) }
        gameContainerLeft.widthProperty().addListener { _, _, _ -> resizeGameCanvases(gameSizeRatio) }
        gameContainerRight.heightProperty().addListener { _, _, _ -> resizeGameCanvases(gameSizeRatio) }
        gameContainerRight.widthProperty().addListener { _, _, _ -> resizeGameCanvases(gameSizeRatio) }
    }
    private fun initiateGameObjects(width: Int, height:Int, pieceSize:Int){
        val randomSeed = System.currentTimeMillis()
        gameLeft = Game(
                width = width,
                height = height,
                squaresInPiece = pieceSize,
                onReady = this::gameLeft_Ready,
                onChange = this::gameLeft_Change,
                onEnd = this::gameLeft_End,
                onRowsPopped = this::gameLeft_RowsPopped,
                onPieceChanged = this::gameLeft_PieceChanged,
                onPieceGenerated = this::gameLeft_PieceGenerated,
                randomSeed = randomSeed,
                functionThatRunsCodeInUiThread = Platform::runLater

        )
        gameRight = Game(
                width = width,
                height = height,
                squaresInPiece = pieceSize,
                onReady = this::gameRight_Ready,
                onChange = this::gameRight_Change,
                onEnd = this::gameRight_End,
                onRowsPopped = this::gameRight_RowsPopped,
                onPieceChanged = this::gameRight_PieceChanged,
                onPieceGenerated = this::gameRight_PieceGenerated,
                randomSeed = randomSeed,
                functionThatRunsCodeInUiThread = Platform::runLater

        )
    }
    private fun setupKeyPressedEvents(scene: Scene) {

        var downPressedTimerLeft = Timer()
        var downPressedTimerRight = Timer()
        var timerLeftRunning = false

        fun startTimerLeft() {
            downPressedTimerLeft = Timer()
            val task = timerTask { Platform.runLater {
                if (gameLeft?.isPlaying ?: false)
                    gameLeft?.movePieceDown() } }
            downPressedTimerLeft.scheduleAtFixedRate(task, 0, 45)
        }

        fun stopTimerLeft() {
            downPressedTimer.cancel()
            downPressedTimer.purge()
        }
        fun startTimerRight() {
            downPressedTimer = Timer()
            val downTimerTask = timerTask { Platform.runLater {
                if (gameLeft?.isPlaying ?: false)
                    gameLeft?.movePieceDown() } }
            downPressedTimer.scheduleAtFixedRate(downTimerTask, 0, 45)
        }

        fun stopTimerRight() {
            downPressedTimer.cancel()
            downPressedTimer.purge()
        }
        scene.setOnKeyPressed { e ->
            if (game?.isPlaying ?: false) {
                when (e.code) {
                    KeyCode.UP -> game?.rotatePiece()
                    KeyCode.LEFT -> game?.movePieceLeft()
                    KeyCode.RIGHT -> game?.movePieceRight()
                    KeyCode.SPACE -> game?.dropPiece()
                    KeyCode.DOWN -> {
                        if (!timerRunning) {
                            startTimer()
                            timerRunning = true
                        }
                    }
                }
            }
            when (e.code){
                KeyCode.F11 -> stage.isFullScreen = !stage.isFullScreen
                KeyCode.ENTER -> btnStart.fire()
                KeyCode.R -> {
                    if (!btnRestart.isDisabled)
                        btnRestart.fire()
                }
            }

        }
        scene.setOnKeyReleased { e ->
            if (e.code == KeyCode.DOWN) {
                stopTimer()
                timerRunning = false
            }
        }

    }

}
















