
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
        mainStage?.title = "Tetris: Reloaded"

        launchHomeScreen()
    }

    companion object {
        fun launchHomeScreen() {
            simpleLoad("HomeLayout.fxml")
        }

        fun launchSinglePlayerScreen(width: Int, height: Int, pieceSize: Int) {
            val loader = FXMLLoader(javaClass.getResource("SinglePlayerLayout.fxml"))
            val layout: Parent = loader.load()
            mainStage?.scene = Scene(layout)
            mainStage?.show()
            val controller: SinglePlayerController = loader.getController()
            controller.loadGame(width, height, pieceSize)

        }

        fun launchDuelScreen(width: Int, height: Int, pieceSize: Int) {

        }

        fun launchHistoryScreen() {
           // simpleLoad("HistoryLayout.fxml")
        }

        private fun simpleLoad(layoutFileName: String) {
            val layout: Parent = FXMLLoader.load(javaClass.getResource(layoutFileName))
            mainStage?.scene = Scene(layout)
            mainStage?.show()
        }
    }

}