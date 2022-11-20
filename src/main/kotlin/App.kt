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
fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title
    window.addWindowListener(WindowCloser)


    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseListener(MouseListener)

    val menuBar = JMenuBar()
    val menu = JMenu("File")
    val openItem = JMenuItem("Open")
    openItem.addActionListener(OpenActionListener)
    val saveItem = JMenuItem("Save")
    saveItem.addActionListener(SaveActionListener)
    menu.add(saveItem)
    menu.add(openItem)
    menuBar.add(menu)
    window.jMenuBar = menuBar

    window.preferredSize = Dimension(700, 800)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}
fun saveField(field: Field){
    if(State.saveFile.path != "") {
        State.saveFile.createNewFile()
        val saver = State.saveFile.bufferedWriter()
        saver.write(field.size.toString())
        saver.newLine()
        saver.write(field.cells.map { if (it.isAlive) "1" else "0" }.joinToString(" "))
        saver.close()
        State.saveFile = File("")
    }
}
fun openField(): Field{
    return Field(State.openFile.path.toString())
}

class Cell(val x: Int, val y: Int, var isAlive: Boolean){
    fun choose(){
        isAlive = !isAlive
    }
}
class Field(val path: String = "./data/CommonField.txt", var size: Int = 20, var cells: List<Cell> = listOf()){
    init{
        try{
        var iterator = 0
        val file = File(path)
        val fileReader = file.bufferedReader()
        size = fileReader.readLine().toInt()
        cells = fileReader.readLine().split(" ").map{Cell(iterator++/size+1, (iterator-1)%size+1, it != "0")}.slice(0 until size*size)
        fileReader.close()}
        catch(e: Exception){
            JOptionPane("This file not available", JOptionPane.ERROR_MESSAGE)
        }
    }
    fun transform(x: Int, y: Int) { cells[((x - 10) / 21 - 1) * size + (y - 10) / 21 - 1].choose() }
    fun mouseIn(): Boolean = (State.mouseX > 10+21 && State.mouseX < 10+(size+1)*21) && (State.mouseY > 10+21 && State.mouseY < 10+(size+1)*21)
    fun aliveNeighbours(cell: Cell): Int =  cells.filter { abs((it.x - cell.x)%20) <= 1 && abs((it.y - cell.y)%20) <= 1 && (it.x != cell.x || it.y != cell.y)}.count{it.isAlive}
    fun nextGeneration(){
        cells = cells.map{
            if(it.isAlive && (this.aliveNeighbours(it) < 2 || this.aliveNeighbours(it) >= 4)) Cell(it.x, it.y, false)
            else if(!it.isAlive && this.aliveNeighbours(it) == 3) Cell(it.x, it.y, true)
            else Cell(it.x, it.y, it.isAlive)
        }
    }
    fun randomField(){
        cells = cells.map{Cell(it.x, it.y, Random.nextBoolean())}
    }
}
class Button(val x: Int, val y: Int, val w: Int, val h: Int){
    fun mouseIn(): Boolean{
        return (State.mouseX > x && State.mouseX < x+w) && (State.mouseY > y && State.mouseY < h+y)
    }
}
class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 14f)
    val alivePaint = Paint().apply{color = 0xFFFF4500.toInt()}
    val deadPaint = Paint().apply{color = 0xFF228B22.toInt()}

    val nextButton = Button(30, 50+21*State.field.size, 50, 30)
    val randomButton = Button(100, 50+21*State.field.size, 50, 30)

    @ExperimentalTime
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        if(State.isClicked) {
            if (State.field.mouseIn()) {
                State.field.transform(State.mouseX, State.mouseY)
            }
            if (nextButton.mouseIn()) {
                State.field.nextGeneration()
            }
            if(randomButton.mouseIn()) {
                State.field.randomField()
            }
            State.isClicked = false
        }
        if(State.openFile.path != ""){
            State.field = openField()
            State.openFile = File("")
        }
        saveField(State.field)
        drawPlayField(canvas, State.field)
        drawButtons(canvas, State.field)
        layer.needRedraw()
    }
    private fun drawPlayField(canvas: Canvas, field: Field){
        field.cells.forEach{canvas.drawRect(Rect.makeXYWH(10f+21f*it.x, 10f+21f*it.y, 20f, 20f), if(it.isAlive) alivePaint else deadPaint)}
    }
    private fun drawButtons(canvas: Canvas, field: Field){
        canvas.drawRect(Rect.makeXYWH(30f, 50f+21f*field.size, 50f, 30f), Paint().apply { color = 0x55FF4500.toInt()})
        canvas.drawString("Next", 38f, 70f+21f*field.size, font, deadPaint)
        canvas.drawRect(Rect.makeXYWH(100f, 50f+21f*field.size, 70f, 30f), Paint().apply { color = 0x55FF4500.toInt()})
        canvas.drawString("Random", 108f, 70f+21f*field.size, font, deadPaint)
    }
}
object State {
    var mouseX = 0
    var mouseY = 0
    var isClicked  = false
    var saveFile = File("")
    var openFile = File("")
    var field = Field()
}
object MouseListener : MouseAdapter() {
    override fun mousePressed(event: MouseEvent) {
        State.mouseX = event.x
        State.mouseY = event.y
        State.isClicked = true
    }
}
object SaveActionListener : ActionListener {
    override fun actionPerformed(event: ActionEvent?) {
        var fileSaver = JFileChooser()
        val returnVal = fileSaver.showSaveDialog(null)
        if(returnVal == JFileChooser.APPROVE_OPTION){
            State.saveFile = fileSaver.getSelectedFile()
        }
    }
}
object OpenActionListener : ActionListener {
    override fun actionPerformed(event: ActionEvent?) {
        var fileSaver = JFileChooser()
        val returnVal = fileSaver.showOpenDialog(null)
        if(returnVal == JFileChooser.APPROVE_OPTION){
            State.openFile = fileSaver.getSelectedFile()
        }
    }
}
object WindowCloser: WindowAdapter() {
    override fun windowClosing(event: WindowEvent?) {
        val result = JOptionPane.showConfirmDialog(null, "Save the state of the field?", "Autosave", JOptionPane.YES_NO_OPTION)
        if(result == 0){
            State.saveFile = File("./data/CommonField.txt")
            saveField(State.field)
        }
    }
}