import javafx.collections.FXCollections
import javafx.collections.ObservableList

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.stage.FileChooser
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class HistoryController : Initializable {


    @FXML var tbSinglePlayer = ToggleButton()
    @FXML var tbDuel = ToggleButton()
    @FXML var btnBack = Button()
    @FXML var btnWatch = Button()
    @FXML var btnDelete = Button()
    @FXML var btnBackup = Button()
    @FXML var btnImport = Button()
    @FXML var tableSinglePlayer = TableView<SinglePlayerSave>()
    @FXML var tableDuel = TableView<DuelSave>()
    val toggleGroup = ToggleGroup()
    var saveList = downloadSaveList(GAME_DATA_FILE_PATH)
    //region Columns
    @FXML var columnNameSingle = TableColumn<SinglePlayerSave, String>()
    @FXML var columnRowsSingle = TableColumn<SinglePlayerSave, Int>()
    @FXML var columnDateSingle = TableColumn<SinglePlayerSave, Date>()
    @FXML var columnWidthSingle = TableColumn<SinglePlayerSave, Int>()
    @FXML var columnHeightSingle = TableColumn<SinglePlayerSave, Int>()
    @FXML var columnSquaresSingle = TableColumn<SinglePlayerSave, Int>()

    @FXML var columnLeftNameDuel = TableColumn<DuelSave, String>()
    @FXML var columnRightNameDuel = TableColumn<DuelSave, String>()
    @FXML var columnLeftRowsDuel = TableColumn<DuelSave, Int>()
    @FXML var columnRightRowsDuel = TableColumn<DuelSave, Int>()
    @FXML var columnDateDuel = TableColumn<DuelSave, Date>()
    @FXML var columnWidthDuel = TableColumn<DuelSave, Int>()
    @FXML var columnHeightDuel = TableColumn<DuelSave, Int>()
    @FXML var columnSquaresDuel = TableColumn<DuelSave, Int>()

    //endregion

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        toggleGroup.toggles.addAll(tbSinglePlayer, tbDuel)
        setupColumnsFactory()
        setupEvents()
        loadDataToTables()
        tableSinglePlayer.columns.forEach { it.style = "-fx-alignment: CENTER;" }
        tableDuel.columns.forEach { it.style = "-fx-alignment: CENTER;" }

    }

    private fun loadDataToTables() {
        tableSinglePlayer.items = saveList.getSinglePlayerSaves().toObservableList().apply { reverse() }
        tableDuel.items = saveList.getDuelSaves().toObservableList().apply { reverse() }
    }


    fun tbSinglePlayer_Action() {
        tableSinglePlayer.isVisible = true
        tableDuel.isVisible = false
        btnWatch.isDisable = tableSinglePlayer.selectionModel.isEmpty
        btnDelete.isDisable = tableSinglePlayer.selectionModel.isEmpty
    }

    fun tbDuel_Action() {
        tableDuel.isVisible = true
        tableSinglePlayer.isVisible = false
        btnWatch.isDisable = tableDuel.selectionModel.isEmpty
        btnDelete.isDisable = tableDuel.selectionModel.isEmpty
    }

    fun btnBack_Action() {
        App.launchHomeScreen()
    }

    fun btnWatch_Action() {
        val save = saveList.find { it.id == getSelectedSaveId() }
        when (save){
            is SinglePlayerSave -> App.launchWatchSinglePlayerScreen(save)
            is DuelSave -> App.launchWatchDuelScreen(save)
        }

    }

    fun btnDelete_Action() {
        val confirmation = App.launchConfirmationAlert(
                title = "Warning!",
                content = "Are you sure you want to permanently delete this game save?"
        )
        if (!confirmation) return
        val id = getSelectedSaveId()
        saveList = saveList.filter { it.id != id }
        uploadSaveList(GAME_DATA_FILE_PATH, saveList)
        loadDataToTables()
        btnWatch.isDisable = true
        btnDelete.isDisable = true

    }

    fun btnBackup_Action() {
        val fileChooser = FileChooser()
        val dateString = generateCurrentDateString()
        fileChooser.initialFileName = "tetris reloaded backup $dateString"
        val backupFile = fileChooser.showSaveDialog(btnBackup.scene.window) ?: return
        val saveFile = File(GAME_DATA_FILE_PATH)
        saveFile.copyTo(backupFile, true)
    }

    fun btnImport_Action() {
        val fileChooser = FileChooser()
        val importFile = fileChooser.showOpenDialog(btnImport.scene.window) ?: return
        val importSaveList = downloadSaveList(importFile.path)
        val oldSaveList = downloadSaveList(GAME_DATA_FILE_PATH)
        saveList = mergeSaveLists(oldSaveList, importSaveList)
        uploadSaveList(GAME_DATA_FILE_PATH, saveList)
        loadDataToTables()

    }


    private fun generateCurrentDateString(): String {
        val date = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yy")
        return dateFormat.format(date)
    }

    private fun setupEvents() {
        btnDelete.sceneProperty().addListener { _, oldValue, newValue ->
            if (oldValue == null && newValue != null)
                setupKeyPressedEvents(newValue)
        }
        tableSinglePlayer.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            btnWatch.isDisable = newVal == null
            btnDelete.isDisable = newVal == null
        }
        tableDuel.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            btnWatch.isDisable = newVal == null
            btnDelete.isDisable = newVal == null
        }
    }

    private fun setupKeyPressedEvents(scene: Scene) {
        scene.setOnKeyPressed { e ->
            when (e.code) {
                KeyCode.DELETE -> if (!btnDelete.isDisable) btnDelete.fire()
            }

        }
    }

    private fun setupColumnsFactory() {
        columnNameSingle.cellValueFactory = PropertyValueFactory("playerName")
        columnRowsSingle.cellValueFactory = PropertyValueFactory("totalRowsPopped")
        columnDateSingle.cellValueFactory = PropertyValueFactory("date")
        columnWidthSingle.cellValueFactory = PropertyValueFactory("width")
        columnHeightSingle.cellValueFactory = PropertyValueFactory("height")
        columnSquaresSingle.cellValueFactory = PropertyValueFactory("squaresInPiece")
        columnLeftNameDuel.cellValueFactory = PropertyValueFactory("playerNameLeft")
        columnRightNameDuel.cellValueFactory = PropertyValueFactory("playerNameRight")
        columnLeftRowsDuel.cellValueFactory = PropertyValueFactory("totalRowsPoppedLeft")
        columnRightRowsDuel.cellValueFactory = PropertyValueFactory("totalRowsPoppedRight")
        columnDateDuel.cellValueFactory = PropertyValueFactory("date")
        columnWidthDuel.cellValueFactory = PropertyValueFactory("width")
        columnHeightDuel.cellValueFactory = PropertyValueFactory("height")
        columnSquaresDuel.cellValueFactory = PropertyValueFactory("squaresInPiece")
    }

    private fun <T> List<T>.toObservableList(): ObservableList<T> {
        return FXCollections.observableList(this)
    }

    private fun List<GameSave>.getSinglePlayerSaves(): List<SinglePlayerSave> {
        val tempList = mutableListOf<SinglePlayerSave>()
        this.filter { it is SinglePlayerSave }.forEach { tempList.add(it as SinglePlayerSave) }
        return tempList.toList()
    }

    private fun List<GameSave>.getDuelSaves(): List<DuelSave> {
        val tempList = mutableListOf<DuelSave>()
        this.filter { it is DuelSave }.forEach { tempList.add(it as DuelSave) }
        return tempList.toList()
    }

    private fun getSelectedSaveId() = when {
        tableSinglePlayer.isVisible -> tableSinglePlayer.selectionModel.selectedItem.id
        tableDuel.isVisible -> tableDuel.selectionModel.selectedItem.id
        else -> ""
    }


}