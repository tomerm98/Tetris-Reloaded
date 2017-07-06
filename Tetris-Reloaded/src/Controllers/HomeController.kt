import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import java.net.URL
import java.util.*


class HomeController : Initializable {
    @FXML var btnSinglePLayer = Button()
    @FXML var btnDuel = Button()
    @FXML var btnHistory = Button()
    @FXML var btnReset = Button()
    @FXML var lblWidth = Label()
    @FXML var lblHeight = Label()
    @FXML var lblSquaresInPiece = Label()
    @FXML var sldrWidth = Slider()
    @FXML var sldrHeight = Slider()
    @FXML var sldrSquaresInPiece = Slider()


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        //bind sliders min value
        sldrWidth.minProperty()?.bind(sldrSquaresInPiece.valueProperty())
        sldrHeight.minProperty()?.bind(sldrSquaresInPiece.valueProperty())

        //set labels text when sliders change value
        sldrWidth.valueProperty()?.addListener { _, _, newValue ->
            lblWidth.text = newValue.toInt().toString()
        }
        sldrHeight.valueProperty()?.addListener { _, _, newValue ->
            lblHeight.text = newValue.toInt().toString()
        }
        sldrSquaresInPiece.valueProperty()?.addListener { _, _, newValue ->
            lblSquaresInPiece.text = newValue.toInt().toString()
        }


    }

    fun btnReset_Action() {
        sldrSquaresInPiece.value = 4.0
        sldrWidth.value = 10.0
        sldrHeight.value = 18.0
    }

    fun btnSinglePlayer_Action() {
        val (width, height, squaresInPiece) = getSliderValues()
        App.launchSinglePlayerScreen(width, height, squaresInPiece)
    }

    fun btnDuel_Action() {
        val (width, height, squaresInPiece) = getSliderValues()
        App.launchDuelScreen(width, height, squaresInPiece)
    }

    fun btnHistory_Action() {
        App.launchHistoryScreen()
    }

    fun btnWidthLeft_Action() = sldrWidth.value--
    fun btnWidthRight_Action() = sldrWidth.value++
    fun btnHeightLeft_Action() = sldrHeight.value--
    fun btnHeightRight_Action() = sldrHeight.value++
    fun btnSquaresInPieceLeft_Action() = sldrSquaresInPiece.value--
    fun btnSquaresInPieceRight_Action() = sldrSquaresInPiece.value++


    private fun getSliderValues(): Triple<Int, Int, Int> {
        val width = sldrWidth.value.toInt()
        val height = sldrHeight.value.toInt()
        val squaresInPiece = sldrSquaresInPiece.value.toInt()
        return (Triple(width, height, squaresInPiece))
    }

}