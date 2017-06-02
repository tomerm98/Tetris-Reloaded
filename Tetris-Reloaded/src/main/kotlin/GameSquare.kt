import javafx.scene.paint.Color

class GameSquare(var visible: Boolean = false, val color: Color = Color.BLACK){
    fun toggle() {visible = !visible}
}
