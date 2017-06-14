
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


fun main(args: Array<String>) {

    Application.launch(App::class.java, *args)

}

var stage: Stage? = null

class App : Application() {

    override fun start(primaryStage: Stage) {
        stage = primaryStage
        stage?.title = "Tetris: Reloaded"
        launchHomeScreen()
    }

    companion object {
        fun launchHomeScreen() {
            simpleLoad("HomeLayout.fxml")
        }

        fun launchSinglePlayerScreen(width: Int, height: Int, pieceSize: Int) {
            val loader = FXMLLoader(javaClass.getResource("SinglePlayerLayout.fxml"))
            val layout: Parent = loader.load()
            stage?.scene = Scene(layout)
            stage?.show()
            val controller: SinglePlayerController = loader.getController()
            controller.loadGame(width, height, pieceSize)
        }

        fun launchDuelScreen(width: Int, height: Int, pieceSize: Int) {

        }

        fun launchHistoryScreen() {
            simpleLoad("HistoryLayout.fxml")
        }

        private fun simpleLoad(layoutFileName: String) {
            val layout: Parent = FXMLLoader.load(javaClass.getResource(layoutFileName))
            stage?.scene = Scene(layout)
            stage?.show()
        }
    }

}