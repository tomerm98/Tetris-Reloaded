import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


fun main(args: Array<String>) {

    Application.launch(App::class.java, *args)

}

var mainStage = Stage()

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

        fun launchHomeScreen() {
            load(HOME_LAYOUT_NAME)
        }

        fun launchSinglePlayerScreen(width: Int, height: Int, pieceSize: Int) {
            val loader = load(SINGLE_PLAYER_LAYOUT_NAME)
            val controller: SinglePlayerController = loader.getController()
            controller.loadGame(width, height, pieceSize)

        }

        fun launchDuelScreen(width: Int, height: Int, pieceSize: Int) {
            val loader = load(DUEL_LAYOUT_NAME)
            val controller: DuelController = loader.getController()
            controller.loadGame(width, height, pieceSize)
        }

        fun launchHistoryScreen() {
            // load(HISTORY_LAYOUT_NAME)
        }

        private fun load(layoutFileName: String): FXMLLoader {
            val loader = FXMLLoader(javaClass.getResource(layoutFileName))
            val layout: Parent = loader.load()
            mainStage.scene = Scene(layout)
            mainStage.show()
            return loader
        }
    }

}