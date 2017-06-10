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
    @FXML var lblMessage: Label? = null
    @FXML var lblRowsPopped: Label? = null
    @FXML var canvasNextPiece: Canvas? = null
    @FXML var canvasGame: Canvas? = null
    @FXML var nextPieceContainer: HBox? = null
    @FXML var gameContainer: HBox? = null
    @FXML var btnStart: Button? = null
    @FXML var btnRestart: Button? = null
    @FXML var btnSave: Button? = null
    @FXML var btnBack: Button? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        //resize the next-piece canvas when the container is resized
        nextPieceContainer?.heightProperty()?.addListener { _, _, _ -> resizeNextPieceCanvas() }
        nextPieceContainer?.widthProperty()?.addListener { _, _, _ -> resizeNextPieceCanvas() }
        btnStart?.isFocusTraversable = false
        btnRestart?.isFocusTraversable = false
        btnSave?.isFocusTraversable = false
        btnBack?.isFocusTraversable = false

    }

    fun loadGame(width: Int, height: Int, pieceSize: Int) {
        val gameSizeRatio = width.toDouble() / height.toDouble()
        val container = checkNotNull(gameContainer)
        //resize the game canvas when the container is resized
        container.heightProperty().addListener { _, _, _ -> resizeGameCanvas(gameSizeRatio) }
        container.widthProperty().addListener { _, _, _ -> resizeGameCanvas(gameSizeRatio) }
        setupKeyPressedEvents(container.scene)

        game = Game(
                width = width,
                height = height,
                pieceSize = pieceSize,
                onReady = this::game_Ready,
                onChange = this::game_Change,
                onEnd = this::game_End,
                onRowsPopped = this::game_RowsPopped,
                onPieceChanged = this::game_PieceChanged
        )

    }


    fun btnStart_Action() {
        val game = checkNotNull(this.game)
        when {
            game.isPlaying && !game.isPaused -> pauseGame()
            !game.isPlaying && game.isPaused -> resumeGame()
            !game.isPlaying && !game.isPaused -> startGame()
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

    fun canvasGame_MouseClicked() {

    }


    private fun game_End(game: Game) {
        lblMessage?.text = "Game Over"
        btnStart?.isVisible = false


    }

    private fun game_Change(game: Game) {
        updateGameCanvas()

    }

    private fun game_Ready(game: Game) {
        btnStart?.isDisable= false
        lblMessage?.text = "Ready"

    }

    private fun game_PieceChanged(game: Game) {
        updateNextPieceCanvas()
    }

    private fun game_RowsPopped(game: Game, rowsPopped: Int) {
        lblRowsPopped?.text = game.rowsPopped.toString()
        game.delayMillis -= 20 * rowsPopped
        if (game.delayMillis < 100)
            game.delayMillis = 100
    }


    private fun startGame() {
        lblMessage?.text = ""
        btnStart?.text = "Pause"
        btnRestart?.isDisable= false
        btnSave?.isDisable = false
        updateNextPieceCanvas()
        game?.startIfReady()
    }

    private fun resumeGame() {
        game?.resume()
        lblMessage?.text = ""
        btnStart?.text = "Pause"
    }

    private fun pauseGame() {
        game?.pause()
        lblMessage?.text = "Paused"
        btnStart?.text = "Resume"
    }


    private fun resizeNextPieceCanvas() {
        resizeCanvas(
                checkNotNull(canvasNextPiece),
                checkNotNull(nextPieceContainer),
                1.0
        )
        if (game != null)
        updateNextPieceCanvas()
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
        val option1Width = Math.min(conWidth, conHeight * sizeRatio)
        val option1Height = Math.min(conHeight, conWidth * reversedRatio)
        val option2Width = Math.min(conHeight, conWidth * sizeRatio)
        val option2Height = Math.min(conWidth, conHeight * reversedRatio)
        val option1Area = option1Width * option1Height
        val option2Area = option2Width * option2Height
        if (option1Area > option2Area)
            {
                canvas.width = option1Width
                canvas.height = option1Height
            }
        else
        {
            canvas.width = option2Width
            canvas.height = option2Height
        }


    }


    private fun setupKeyPressedEvents(scene: Scene) {

        var timer = Timer()
        var timerRunning = false
        fun startTimer() {
            timer = Timer()
            val downTimerTask = timerTask { game?.movePieceDown() }
            timer.scheduleAtFixedRate(downTimerTask, 0, 60)
        }

        fun stopTimer() {
            timer.cancel()
            timer.purge()
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
        }
        scene.setOnKeyReleased { e ->
            if (e.code == KeyCode.DOWN) {
                stopTimer()
                timerRunning = false
            }
        }

    }



    private fun updateNextPieceCanvas() {
        val canvas = checkNotNull(canvasNextPiece)
        require(canvas.width == canvas.height)

        val graphics = canvas.graphicsContext2D
        val game = checkNotNull(game)
        val canvasSize = canvas.width
        val squareSize = canvasSize /  game.pieceSize
        val piece = game.nextPiece
        graphics.clearRect(0.0,0.0,canvasSize,canvasSize)

        for ((x, y) in piece.getSquaresLocations())
            drawSquare(graphics,piece.color,squareSize,x,y)
    }

    private fun updateGameCanvas() {
        val canvas = checkNotNull(canvasGame)
        val graphics = canvas.graphicsContext2D
        val game = checkNotNull(game)
        val squareSize = canvas.width / game.width
        graphics.clearRect(0.0,0.0,canvas.width,canvas.height)
        drawCanvasBorder(canvas)
        for ((x,y) in game.getSquaresLocations()) {
            val color = game[x,y].color
            drawSquare(graphics,color,squareSize,x,y)
        }

    }
    private fun drawSquare(graphics: GraphicsContext, color:Color, squareSize: Double, left:Int, top: Int, border:Int=2)
    {
        graphics.fill = Color.BLACK
        graphics.fillRect(left*squareSize, top*squareSize, squareSize, squareSize)
        graphics.fill = color
        graphics.fillRect(
                left*squareSize + border,
                top*squareSize + border,
                squareSize - 2 * border,
                squareSize - 2 * border
        )
    }
    private fun drawCanvasBorder(canvas: Canvas, border:Int = 2)
    {
        val graphics = canvas.graphicsContext2D
        val width = canvas.width
        val height = canvas.height
        graphics.strokeLine(0.0,0.0,width,0.0)
        graphics.strokeLine(0.0,0.0,0.0,height)
        graphics.strokeLine(width,height,width,0.0)
        graphics.strokeLine(width,height,0.0,height)
    }



}
























