import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.io.*
import java.net.URL
import java.util.*

class SinglePlayerSaveController(val game:Game):Initializable {
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
        var saveList = listOf<GameSave>(gameSave)

        val file = File(GAME_DATA_FILE_NAME)
        if (file.exists()) {
            val ois = ObjectInputStream(FileInputStream(file))
            val oldList = ois.readObject() as List<GameSave>
            saveList += oldList
        }

        val oos = ObjectOutputStream(FileOutputStream(file, false))
        oos.writeObject(saveList)
        oos.close()
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

