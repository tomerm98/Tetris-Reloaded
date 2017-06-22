import javafx.scene.paint.Color
import java.io.Serializable
import java.util.*

 class GameSquare(var visible: Boolean = false, color: Color = Color.BLACK) : Serializable {
    val color = color.toSerializableColor()
    fun toggle() {
        visible = !visible
    }
}
typealias SquareGrid = MutableList<MutableList<GameSquare>>
fun createSynchronizedSquareGrid(width: Int, height: Int, visible: Boolean = false, color: Color = Color.BLACK): SquareGrid {
    return Collections.synchronizedList(
            MutableList(width, {
                Collections.synchronizedList(
                        MutableList(height, {
                            GameSquare(
                                    visible = visible,
                                    color = color
                            )
                        }))
            }))
}