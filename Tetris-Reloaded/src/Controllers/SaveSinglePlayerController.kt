import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.net.URL
import java.util.*

class SaveSinglePlayerController(val game: Game) : Initializable {
    @FXML var tfPlayerName = TextField()
    @FXML var btnSave = Button()
    @FXML var lblRowsPopped = Label()


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        lblRowsPopped.text = game.rowsPopped.toString()
        tfPlayerName.textProperty().addListener { _, _, newValue ->
            btnSave.isDisable = newValue.isBlank()
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

    private fun generateGameSave(): SinglePlayerSave {
        return SinglePlayerSave(
                playerName = tfPlayerName.text,
                width = game.width,
                height = game.height,
                date = Date(),
                squaresInPiece = game.squaresInPiece,
                timeStamps = game.history,
                totalRowsPopped = game.rowsPopped,
                id = generateRandomId()
        )
    }

}

