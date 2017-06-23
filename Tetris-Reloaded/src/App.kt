import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.*
import java.util.*

fun main(args: Array<String>) {

    Application.launch(App::class.java, *args)

}

val GAME_DATA_FILE_NAME = "game_data"
var mainStage_nullable: Stage? = null
var mainStage: Stage
    get() = checkNotNull(mainStage_nullable);
    set(value) {
        mainStage_nullable = value
    }

class App : Application() {

    override fun start(primaryStage: Stage) {
        mainStage = primaryStage
        mainStage.title = APP_TITLE

        launchHomeScreen()
    }

    companion object {
        val APP_TITLE = "Tetris: Reloaded"
        val HOME_LAYOUT_NAME = "HomeLayout.fxml"
        val SINGLE_PLAYER_LAYOUT_NAME = "SinglePlayerLayout.fxml"
        val DUEL_LAYOUT_NAME = "DuelLayout.fxml"
        val HISTORY_LAYOUT_NAME = "HistoryLayout.fxml"
        val SINGLE_PLAYER_SAVE_LAYOUT_NAME = "SaveSinglePlayerLayout.fxml"
        val DUEL_SAVE_LAYOUT_NAME = "SaveDuelLayout.fxml"

        fun launchHomeScreen() {
            launchScreen(HOME_LAYOUT_NAME, mainStage)
        }

        fun launchSinglePlayerScreen(width: Int, height: Int, squaresInPiece: Int) {
            val controller = SinglePlayerController(width,height,squaresInPiece)
            launchScreen(SINGLE_PLAYER_LAYOUT_NAME, mainStage, controller)
        }

        fun launchDuelScreen(width: Int, height: Int, squaresInPiece: Int) {
            val controller = DuelController(width,height,squaresInPiece)
            launchScreen(DUEL_LAYOUT_NAME, mainStage, controller)

        }

        fun launchHistoryScreen() {
            launchScreen(HISTORY_LAYOUT_NAME, mainStage)
        }

        fun launchSinglePlayerSaveScreen(game: Game) {
            val controller = SaveSinglePlayerController(game)
            val stage = Stage()
            stage.title = ""
            launchPopup(SINGLE_PLAYER_SAVE_LAYOUT_NAME, stage, controller)
        }

        fun launchDuelSaveScreen(gameLeft: Game, gameRight:Game) {
            val controller = SaveDuelController(gameLeft,gameRight)
            val stage = Stage()
            stage.title = ""
            launchPopup(DUEL_SAVE_LAYOUT_NAME, stage, controller)
        }


        private fun launchScreen(layoutFileName: String, stage: Stage, controller: Any? = null) {
            loadLayout(layoutFileName, stage, controller)
            stage.show()
        }
        private fun launchPopup(layoutFileName: String, stage: Stage, controller: Any? = null) {
            loadLayout(layoutFileName, stage, controller)
            stage.initModality(Modality.APPLICATION_MODAL)
            stage.show()
        }
        private fun loadLayout(layoutFileName: String, stage: Stage, controller: Any? = null)
        {
            val loader = FXMLLoader(javaClass.getResource(layoutFileName))
            loader.setController(controller)
            val layout: Parent = loader.load()
            val scene = Scene(layout)
            stage.scene = scene
        }


    }

}

fun generateRandomId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz1234567890"
    var id = ""
    val rng = Random()
    repeat(30) {
        val index = rng.nextInt(chars.length)
        id += chars[index]
    }
    return id
}

class SerializableColor(val red: Double, val green: Double, val blue: Double, val opacity: Double) : Serializable {
    fun toJavaFxColor(): Color {
        return Color(red, green, blue, opacity)
    }
}

fun Color.toSerializableColor(): SerializableColor {
    return SerializableColor(red, green, blue, opacity)
}


 fun downloadSaveList(fileName: String): List<GameSave> {
    val file = File(fileName)
    if (file.exists()) {
        val ois = ObjectInputStream(FileInputStream(file))
        val obj = ois.readObject()
        if (obj is List<*> && !obj.isEmpty() && obj.first() is GameSave)
            return  obj as List<GameSave>
        ois.close()
    }
    return listOf()
}

 fun uploadSaveList(fileName: String,saveList:List<GameSave>) {
    val file = File(fileName)
    val oos = ObjectOutputStream(FileOutputStream(file,false))
    oos.writeObject(saveList)
    oos.close()
}
fun mergeSaveLists(list1: List<GameSave>, list2: List<GameSave>):List<GameSave>{
    val tempList = list1.toMutableList()
    list2.forEach{x ->
        if (!tempList.any {y -> y.id == x.id  })
            tempList.add(x)
    }
    return tempList.toList()
}

