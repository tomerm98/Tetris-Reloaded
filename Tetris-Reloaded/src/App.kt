import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


fun main(args: Array<String>) {

    Application.launch(App::class.java, *args)

}

var mainStage: Stage? = null

class App : Application() {

    override fun start(primaryStage: Stage) {
        mainStage = primaryStage
        mainStage?.title = APP_TITLE

        launchHomeScreen()
    }

    companion object {
        val APP_TITLE = "Tetris: Reloaded"
        val HOME_LAYOUT_NAME = "HomeLayout.fxml"
        val SINGLE_PLAYER_LAYOUT_NAME = "SinglePlayerLayout.fxml"
        val DUEL_LAYOUT_NAME = "DuelLayout.fxml"
        val HISTORY_LAYOUT_NAME = "HistoryLayout.fxml"

        fun launchHomeScreen() {
            load(HOME_LAYOUT_NAME)
        }

        fun launchSinglePlayerScreen(width: Int, height: Int, squaresInPiece: Int) {
            val controller = SinglePlayerController(width,height,squaresInPiece)
            val scene = load(SINGLE_PLAYER_LAYOUT_NAME, controller)
            controller.setupScene(scene)
        }

        fun launchDuelScreen(width: Int, height: Int, squaresInPiece: Int) {
            val controller = DuelController(width,height,squaresInPiece)
            val scene = load(DUEL_LAYOUT_NAME, controller)
            controller.setupScene(scene)
        }

        fun launchHistoryScreen() {
            // load(HISTORY_LAYOUT_NAME)
        }

        private fun load(layoutFileName: String, controller: Any): Scene {
            val loader = FXMLLoader(javaClass.getResource(layoutFileName))
            loader.setController(controller)
            val layout: Parent = loader.load()
            val scene = Scene(layout)
            mainStage?.scene = scene
            mainStage?.show()
            return scene
        }
        private fun load(layoutFileName: String): Scene {
            val loader = FXMLLoader(javaClass.getResource(layoutFileName))
            val layout: Parent = loader.load()
            val scene  = Scene(layout)
            mainStage?.scene = scene
            mainStage?.show()
            return scene
        }
    }

}