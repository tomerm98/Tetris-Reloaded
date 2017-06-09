import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.VBox
import java.net.URL
import java.util.*

class HomeController : Initializable {
    @FXML var layout: VBox? = null
    @FXML var btnSinglePLayer: Button? = null
    @FXML var btnDuel: Button? = null
    @FXML var btnHistory: Button? = null
    @FXML var btnReset: Button? = null
    @FXML var lblWidth: Label? = null
    @FXML var lblHeight: Label? = null
    @FXML var lblPieceSize: Label? = null
    @FXML var sldrWidth: Slider? = null
    @FXML var sldrHeight: Slider? = null
    @FXML var sldrPieceSize: Slider? = null


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        //bind sliders min value
        sldrWidth?.minProperty()?.bind(sldrPieceSize?.valueProperty())
        sldrHeight?.minProperty()?.bind(sldrPieceSize?.valueProperty())

        //set labels text when sliders change value
        sldrWidth?.valueProperty()?.addListener { _, _, newValue ->
            lblWidth?.text = newValue.toInt().toString()
        }

        sldrHeight?.valueProperty()?.addListener { _, _, newValue ->
            lblHeight?.text = newValue.toInt().toString()
        }

        sldrPieceSize?.valueProperty()?.addListener { _, _, newValue ->
            lblPieceSize?.text = newValue.toInt().toString()
        }
        btnReset?.isFocusTraversable = false
        btnSinglePLayer?.isFocusTraversable = false
        btnDuel?.isFocusTraversable = false
        btnHistory?.isFocusTraversable = false

    }

    fun btnReset_Action() {
        sldrPieceSize?.value = 4.0
        sldrWidth?.value = 10.0
        sldrHeight?.value = 18.0
    }

    fun btnSinglePlayer_Action() {
        val (width, height, pieceSize) = getSliderValues()
        App.launchSinglePlayerScreen(width, height, pieceSize)
    }

    fun btnDuel_Action() {
        val (width, height, pieceSize) = getSliderValues()
        App.launchDuelScreen(width, height, pieceSize)
    }

    fun btnHistory_Action() {
        App.launchHistoryScreen()
    }

    private fun getSliderValues(): Triple<Int, Int, Int> {
        val width = checkNotNull(sldrWidth?.value?.toInt())
        val height = checkNotNull(sldrHeight?.value?.toInt())
        val pieceSize = checkNotNull(sldrPieceSize?.value?.toInt())
        return (Triple(width, height, pieceSize))
    }

}