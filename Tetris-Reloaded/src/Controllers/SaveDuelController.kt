import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.net.URL
import java.util.*

/**
 * Created by Tomer on 22/06/2017.
 */
class SaveDuelController(val gameLeft: Game, val gameRight: Game) : Initializable {
    @FXML var tfPlayerNameLeft = TextField()
    @FXML var tfPlayerNameRight = TextField()
    @FXML var lblRowsPoppedLeft = Label()
    @FXML var lblRowsPoppedRight = Label()
    @FXML var btnSave = Button()


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        lblRowsPoppedLeft.text = gameLeft.rowsPopped.toString()
        lblRowsPoppedRight.text = gameRight.rowsPopped.toString()
        tfPlayerNameLeft.textProperty().addListener { _, _, newValue ->
            btnSave.isDisable = newValue.isBlank() || tfPlayerNameRight.text.isBlank()
        }
        tfPlayerNameRight.textProperty().addListener { _, _, newValue ->
            btnSave.isDisable = newValue.isBlank() || tfPlayerNameLeft.text.isBlank()
        }
        btnSave.setOnAction(this::btnSave_Action)

    }

    private fun btnSave_Action(event: ActionEvent) {
        saveGame()
        val stage = btnSave.scene.window as Stage
        stage.close()
    }

    private fun saveGame() {
        val gameSave = generateGameSave()
        val oldSaveList = downloadSaveList(GAME_DATA_FILE_PATH)
        val newSaveList = mergeSaveLists(oldSaveList, listOf(gameSave))
        uploadSaveList(GAME_DATA_FILE_PATH, newSaveList)
    }

    private fun generateGameSave(): DuelSave {
        return DuelSave(
                playerNameLeft = tfPlayerNameLeft.text,
                playerNameRight = tfPlayerNameRight.text,
                width = gameLeft.width,
                height = gameLeft.height,
                date = Date(),
                squaresInPiece = gameLeft.squaresInPiece,
                timeStampsLeft = gameLeft.history,
                timeStampsRight = gameRight.history,
                totalRowsPoppedLeft = gameLeft.rowsPopped,
                totalRowsPoppedRight = gameRight.rowsPopped,
                id = generateRandomId()
        )
    }


}
