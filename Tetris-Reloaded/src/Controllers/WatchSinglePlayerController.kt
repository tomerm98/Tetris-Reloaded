import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.stage.Stage
import java.net.URL
import java.util.*

class WatchSinglePlayerController (val gameSave: SinglePlayerSave): Initializable {
    @FXML var btnForward = Button()
    @FXML var btnBackward = Button()
    @FXML var btnPlay = Button()
    @FXML var btnBack = Button()
    @FXML var lblTotalRows = Label()
    @FXML var lblCurrentRows = Label()
    @FXML var lblTotalTime = Label()
    @FXML var lblCurrentTime = Label()
    @FXML var lblDate = Label()
    @FXML var lblPlayerName = Label()
    @FXML var gameContainer = HBox()
    @FXML var rowsContainer = HBox()
    @FXML var canvasGame = Canvas()
    @FXML var sldrTime = Slider()
    val gameSizeRatio = gameSave.width.toDouble() / gameSave.height.toDouble()
    override fun initialize(location: URL?, resources: ResourceBundle?) {

    }
    private fun btnForward_Action(event: ActionEvent) {

    }

    private fun btnBackward_Action(event: ActionEvent) {

    }

    private fun btnPlay_Action(event: ActionEvent) {

    }

    private fun btnBack_Action(event: ActionEvent) {

    }
    private fun gameContainer_MouseClicked(event: MouseEvent) {

    }
    private fun setupEvents() {
        //when scene is initialized
        gameContainer.sceneProperty().addListener { _, oldValue, newValue ->
            if (oldValue == null && newValue != null)
                setupKeyPressedEvents(newValue)
        }
        btnForward.setOnAction(this::btnForward_Action)
        btnBackward.setOnAction(this::btnBack_Action)
        btnPlay.setOnAction(this::btnPlay_Action)
        btnBack.setOnAction(this::btnBack_Action)
        gameContainer.setOnMouseClicked(this::gameContainer_MouseClicked)
      //  gameContainer.heightProperty()?.addListener { _, _, _ -> resizeGameCanvas() }
       // gameContainer.widthProperty()?.addListener { _, _, _ -> resizeGameCanvas() }


    }
    private fun setupKeyPressedEvents(scene: Scene){
        val stage = scene.window as Stage
        scene.setOnKeyPressed { e ->

            when (e.code) {
                KeyCode.F11 -> stage.isFullScreen = !stage.isFullScreen
            }
        }
    }

    private fun resizeCanvas(canvas: Canvas, container: Pane, sizeRatio: Double) {

        val reversedRatio = 1.0 / sizeRatio
        val conWidth = container.width
        val conHeight = container.height
        canvas.width = Math.min(conWidth, conHeight * sizeRatio)
        canvas.height = Math.min(conHeight, conWidth * reversedRatio)
    }

}