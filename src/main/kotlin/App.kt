import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.*
import java.io.File
import java.lang.Math.abs
import java.lang.Math.random
import javax.swing.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime
fun main() {
    createWindow("Life")
}

