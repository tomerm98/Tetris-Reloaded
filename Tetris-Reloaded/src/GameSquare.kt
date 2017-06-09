import javafx.scene.paint.Color
import java.util.*

data class GameSquare(var visible: Boolean = false, val color: Color = Color.BLACK) {
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