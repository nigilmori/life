import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.*
import java.awt.event.MouseWheelListener
import java.io.File
import java.lang.Math.*
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
    window.layer.addMouseMotionListener(MouseMotionListener)
    window.layer.addMouseWheelListener(MouseWheelListener)
    window.layer.addKeyListener(KeyboardListener)


    val menuBar = JMenuBar()
    val menuF = JMenu("File")
    val openItem = JMenuItem("Open")
    openItem.addActionListener(OpenActionListener)
    val saveItem = JMenuItem("Save")
    saveItem.addActionListener(SaveActionListener)
    menuF.add(saveItem)
    menuF.add(openItem)
    val menuO = JMenu("Options")
    val sizeItem = JMenuItem("Field size")
    sizeItem.addActionListener(SizeActionListener)
    val ruleItem = JMenuItem("Rules")
    ruleItem.addActionListener(RuleActionListener)
    val setNumberOfMovesItem = JMenuItem("Set number of moves")
    setNumberOfMovesItem.addActionListener(SetNumberOfMovesListener)
    menuO.add(sizeItem)
    menuO.add(ruleItem)
    menuO.add(setNumberOfMovesItem)
    menuBar.add(menuF)
    menuBar.add(menuO)
    window.layer.add(JTextField(5), "Pray")
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
        saver.write(listOf(field.size.toString(), field.bornRule.map{if(it) "1" else "0"}.joinToString(" "),
            field.liveRule.map{if(it) "1" else "0"}.joinToString(" "),
            State.cellSize.toInt().toString())
            .joinToString(" "))
        saver.newLine()
        saver.write(field.cells.map { if (it.isAlive) "1" else "0" }.joinToString(" "))
        saver.close()
        State.saveFile = File("")
    }
}
fun openField(): Field{
    return Field(State.openFile.path.toString())
}

class Cell(val x: Int, val y: Int, var isAlive: Boolean, var liveTime: Int){
    fun choose(){
        isAlive = !isAlive
    }
}
class Field(val path: String = "./data/CommonField.txt", var size: Int = 20, var cells: List<Cell> = listOf(),
            var bornRule: List<Boolean> = listOf(false, false, false, false, false, false, false, false, false, false),
            var liveRule: List<Boolean> = listOf(false, false, false, false, false, false, false, false, false, false)){
    init{
        try {
            var iterator = 0
            val file = File(path)
            val fileReader = file.bufferedReader()
            val settings = fileReader.readLine().split(" ")
            size = settings[0].toInt()
            bornRule = settings.slice(1..9).map{"1" == it}
            liveRule = settings.slice(10..18).map{"1" == it}
            State.cellSize = settings[19].toFloat()
            cells = fileReader.readLine().split(" ")
                .map { Cell(iterator++ / size + 1, (iterator - 1) % size + 1, it != "0", it.toInt()) }
                .slice(0 until size * size)
        }
        catch(e: Exception){
            JOptionPane("This file not available", JOptionPane.ERROR_MESSAGE)
        }
    }
    fun transform(x: Int, y: Int) { cells[(x / State.cellSize.toInt() - 1) * size + y / State.cellSize.toInt() - 1].choose() }
    fun liveTime(x:Int, y: Int): Int = cells[(x / State.cellSize.toInt() - 1) * size + y / State.cellSize.toInt() - 1].liveTime
    fun mouseIn(): Boolean = (State.mouseX - State.offsetX > State.cellSize &&
            State.mouseX - State.offsetX < min(1200-State.offsetX.toInt(), (size+1)*State.cellSize.toInt()) &&
           (State.mouseY - State.offsetY > State.cellSize &&
            State.mouseY - State.offsetY < (size+1)*State.cellSize))
    fun aliveNeighbours(cell: Cell): Int{
        val coordinat = (cell.x-1)*size + (cell.y-1)
        val squareSize = size*size
        return listOf(cells[(coordinat+1 + squareSize)%squareSize],
            cells[(coordinat-1 + squareSize)%squareSize],
            cells[(coordinat+1 + size + squareSize)%squareSize],
            cells[(coordinat-1 + size + squareSize)%squareSize],
            cells[(coordinat+1 - size + squareSize)%squareSize],
            cells[(coordinat-1 - size + squareSize)%squareSize],
            cells[(coordinat + size + squareSize)%squareSize],
            cells[(coordinat- size + squareSize)%squareSize]).count{it.isAlive}
    }
    fun nextGeneration(){
        cells = cells.map{
            if(it.isAlive && (!liveRule[this.aliveNeighbours(it)])) Cell(it.x, it.y, false, 0)
            else if(!it.isAlive && bornRule[this.aliveNeighbours(it)]) Cell(it.x, it.y, true, 1)
            else Cell(it.x, it.y, it.isAlive, if(it.isAlive) it.liveTime + 1 else it.liveTime)
        }
    }
    fun emptyField(_size: Int){
        size = _size
        cells = (0 until size*size).map{Cell(it/size+1, it%size+1, false, 0)}
    }
    fun randomField(){
        cells = cells.map{Cell(it.x, it.y, Random.nextBoolean(), 0)}.map{
            if(it.isAlive) Cell(it.x, it.y, it.isAlive, 1) else it}
    }
}
class Button(val x: Int, val y: Int, val w: Int, val h: Int, var name: String){
    fun mouseIn(): Boolean{
        return (State.mouseX > x && State.mouseX < x+w) && (State.mouseY > y && State.mouseY < h+y)
    }
}
class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 14f)
    val alivePaint = Paint().apply{color = 0xFFFF4500.toInt()}
    val deadPaint = Paint().apply{color = 0xFF228B22.toInt()}

    var buttons = listOf(Button(1250, 10, 200, 50, "Next"),
        Button(1250, 80, 200, 50, "Random"),
        Button(1250, 150, 200, 50, "Start"))

    @ExperimentalTime
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        if(State.isClicked) {
            if (State.field.mouseIn()) {
                State.field.transform(State.mouseX - State.offsetX.toInt(),
                    State.mouseY - State.offsetY.toInt())
            }
            if (buttons[0].mouseIn()) {
                State.field.nextGeneration()
            }
            if(buttons[1].mouseIn()) {
                State.field.randomField()
            }
            if(buttons[2].mouseIn()) {
                if(buttons[2].name == "Start")
                    buttons[2].name = "Stop"
                else
                    buttons[2].name = "Start"
            }
            State.isClicked = false
        }

        if(State.openFile.path != ""){
            State.field = openField()
            State.openFile = File("")
        }
        if(buttons[2].name == "Stop") {
            if(State.numberOfMoves > 0) {
                State.field.nextGeneration()
                State.numberOfMoves--
                if (State.numberOfMoves <= 0)
                    buttons[2].name = "Start"
            }
            else{
                State.field.nextGeneration()
            }
        }
        saveField(State.field)
        drawPlayField(canvas, State.field, State.offsetX, State.offsetY, State.cellSize)
        drawButtons(canvas, buttons)
        if(State.field.mouseIn()){
            drawLiveTime(canvas, State.mouseX, State.mouseY,
                State.field.liveTime(State.mouseX - State.offsetX.toInt(), State.mouseY - State.offsetY.toInt()))
        }
        layer.needRedraw()
    }
    private fun drawPlayField(canvas: Canvas, field: Field, offsetX: Float, offsetY: Float, cellSize: Float){
        canvas.drawRect(Rect.makeXYWH(offsetX + cellSize, offsetY + cellSize, cellSize*(field.size),
            cellSize*(field.size)), deadPaint)
        field.cells.forEach{
            if(it.x + offsetX/cellSize >= 0 && it.x + offsetX/cellSize < 1200/cellSize.toInt() &&
                it.y + offsetY/cellSize < 2000/cellSize.toInt() && it.y + offsetY/cellSize >= 0 && it.isAlive){
                canvas.drawRect(Rect.makeXYWH(offsetX + cellSize*it.x, offsetY + cellSize*it.y,
                                cellSize, cellSize), alivePaint)
            }
        }
        canvas.drawRect(Rect.makeXYWH(1200f, 0f, 600f, 1500f), Paint().apply {color = 0xFFFFFFFF.toInt()})
    }
    private fun drawButtons(canvas: Canvas, buttons: List<Button>){
        buttons.forEach {
            canvas.drawRect(Rect.makeXYWH(it.x.toFloat(), it.y.toFloat(), it.w.toFloat(),
                it.h.toFloat()), Paint().apply { color = 0x55FF4500.toInt() })
            canvas.drawString(it.name, it.x.toFloat() + it.w / 2 - 20, it.y.toFloat() + it.h/2 + 5, font, deadPaint)
        }
    }
    private fun drawLiveTime(canvas: Canvas, x: Int, y: Int, liveTime: Int){
        canvas.drawString("LiveTime: $liveTime", x.toFloat(), y.toFloat(), font, Paint().apply { color = 0xFFFFFF00.toInt() })
    }
}
object State {
    var mouseX = 0
    var mouseY = 0
    var isClicked  = false
    var saveFile = File("")
    var openFile = File("")
    var cellSize = 0f
    var field = Field()
    var offsetX = 10f
    var offsetY = 10f
    var numberOfMoves = 0
}
object MouseListener : MouseAdapter() {
    override fun mousePressed(event: MouseEvent) {
        State.mouseX = event.x
        State.mouseY = event.y
        State.isClicked = true
    }
}
object MouseMotionListener: MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent) {
        State.mouseX = event.x
        State.mouseY = event.y
    }
}
object MouseWheelListener: MouseWheelListener {
    override fun mouseWheelMoved(event: MouseWheelEvent) {
        State.cellSize -= event.wheelRotation
        if(State.cellSize < 1) State.cellSize = 1f
    }
}

object KeyboardListener: KeyListener{
    override fun keyTyped(event: KeyEvent) {
        if(event.keyCode == KeyEvent.VK_S){
            State.offsetY -= 10
        }
        if(event.keyCode == KeyEvent.VK_W){
            State.offsetY += 10
        }
        if(event.keyCode == KeyEvent.VK_A){
            State.offsetX += 10
        }
        if(event.keyCode == KeyEvent.VK_D){
            State.offsetX -= 10
        }
    }

    override fun keyPressed(event: KeyEvent) {
        if(event.keyCode == KeyEvent.VK_S){
            State.offsetY -= 10
        }
        if(event.keyCode == KeyEvent.VK_W){
            State.offsetY += 10
        }
        if(event.keyCode == KeyEvent.VK_A){
            State.offsetX += 10
        }
        if(event.keyCode == KeyEvent.VK_D){
            State.offsetX -= 10
        }
    }
    override fun keyReleased(event: KeyEvent) {}
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
object SizeActionListener : ActionListener {
    override fun actionPerformed(event: ActionEvent?) {
        val returnVal = JOptionPane.showInputDialog(null, "Size", "Set field size",
            JOptionPane.YES_NO_OPTION)
        if(returnVal != null){
            if(returnVal.toInt() <= 0)
                JOptionPane.showMessageDialog(null, "Invalid field size", "Error",
                    JOptionPane.ERROR_MESSAGE)
            else
                State.field.emptyField(returnVal.toInt())
        }
    }
}
object RuleActionListener : ActionListener {
    override fun actionPerformed(event: ActionEvent?) {
        val bornRules = (0..8).map {JCheckBox(it.toString(), State.field.bornRule[it])}
        val liveRules = (0..8).map {JCheckBox(it.toString(), State.field.liveRule[it])}
        val bornRulePanel = JPanel()
        bornRules.forEach{bornRulePanel.add(it)}
        val liveRulePanel = JPanel()
        liveRules.forEach{liveRulePanel.add(it)}
        val bornResult = JOptionPane.showConfirmDialog(
            null, bornRulePanel,
            "Set born rules", JOptionPane.OK_CANCEL_OPTION
        )
        if(bornResult == JOptionPane.OK_OPTION){
            val liveResult = JOptionPane.showConfirmDialog(
                null, liveRulePanel,
                "Set live rules", JOptionPane.OK_CANCEL_OPTION
            )
            if(liveResult == JOptionPane.OK_OPTION){
                State.field.bornRule = bornRules.map{it.isSelected}
                State.field.liveRule = liveRules.map{it.isSelected}
            }
        }
    }
}
object SetNumberOfMovesListener: ActionListener{
    override fun actionPerformed(event: ActionEvent) {
        val numberOfMovesPanel = JPanel()
        val isInfinity = JCheckBox("Infinity", State.numberOfMoves <= 0)
        val numberOfMoves = JTextField(10)
        if(!isInfinity.isSelected)
            numberOfMoves.setText(State.numberOfMoves.toString())
        numberOfMovesPanel.add(isInfinity)
        numberOfMovesPanel.add(JLabel("Moves: "))
        numberOfMovesPanel.add(numberOfMoves)
        val result = JOptionPane.showConfirmDialog(null, numberOfMovesPanel, "Set number of moves",
            JOptionPane.OK_CANCEL_OPTION)
        if(result == JOptionPane.OK_OPTION) {
            if (isInfinity.isSelected)
                State.numberOfMoves = -1
            else{
                try {
                    if(numberOfMoves.text.toInt() > 0)
                        State.numberOfMoves = numberOfMoves.text.toInt()
                    else
                        "Error".toInt()
                } catch(e: Exception){
                    JOptionPane.showMessageDialog(null, "Invalid number of moves", "Error",
                        JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }
}

object WindowCloser: WindowAdapter() {
    override fun windowClosing(event: WindowEvent?) {
        val result = JOptionPane.showConfirmDialog(null, "Save the state of the field?", "Autosave",
            JOptionPane.YES_NO_OPTION)
        if(result == 0){
            State.saveFile = File("./data/CommonField.txt")
            saveField(State.field)
        }
    }
}
