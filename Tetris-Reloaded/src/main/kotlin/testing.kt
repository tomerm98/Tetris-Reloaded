import javafx.scene.layout.*
import javafx.scene.control.*
import tornadofx.*

class MyApp: App(MyView::class)
class MyView: View() {
    override val root = VBox()

    init {
        root += Button("Press Me")
        root += Label("hello")
    }
}
