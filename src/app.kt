import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.*
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import javafx.embed.swing.SwingFXUtils

class MyApp : Application(){
    var prev_x = 0.0
    var prev_y = 0.0
    val directorry_a = "directorry.cfg"
    val default_path = System.getenv("USERPROFILE") + "\\Documents\\"

    override fun start(primaryStage: Stage) {

        //-------------------------переменные
        val tools = HBox()
        val Checkbox_for_hide = CheckBox("Свернуть приложение")
        val delayVBox = VBox()
        val delayLabel = Label("Задерждка перед скрином")
        val delaySlider = Slider(0.0, 10.0, 3.0)
        //-------------------------кисточка
        val brushColorPicker = ColorPicker(Color.BLACK)
        val brush_size_VBox = VBox()
        val brush_size_label = Label("Размер кисти ")
        val brush_size_slider = Slider(1.0, 50.0, 20.0)
        val cut_CheckBox = CheckBox("Обрезать")
        //-------------------------боковые панели
        val scroll = ScrollPane()
        val stack = StackPane()
        //-------------------------канвасы для изменений
        val img_canvas = Canvas(stack.width, stack.height)
        val draw_canvas = Canvas(stack.width, stack.height)
        val cut_canvas = Canvas(stack.width, stack.height)
        scroll.content = stack
        val draw = draw_canvas.graphicsContext2D
        val cut = cut_canvas.graphicsContext2D
        //-------------------------менюбра и другое
        val screeenshot_btn = Button("Скрин")
        val menuBar = MenuBar()
        val menuFile = Menu("Фаил")
        val menuItemOpen = MenuItem("Открыть фаил")
        val menuItemSave = MenuItem("Сохранить")
        val menuItemSaveAs = MenuItem("Сохранить как")
        //-------------------------удаление области
        //-------------------------часть с выделением области
        cut_canvas.onMousePressed = EventHandler { holder ->
            if(cut_CheckBox.isSelected) {
                prev_x = holder.x
                prev_y = holder.y
                cut.fill = Color.rgb(0, 0, 0, 0.5)
                cut.fillRect(0.0, 0.0, draw_canvas.width, draw_canvas.height)
            }
            else {
                val size = brush_size_slider.value
                val x = holder.x - size / 2
                val y = holder.y - size / 2
                if (holder.button == MouseButton.SECONDARY) {
                    draw.clearRect(x, y, size, size)
                } else {
                    draw.fill = brushColorPicker.value
                    draw.fillOval(x, y, size, size)
                }
            }
        }
        //-------------------------часть с выделение области внутри канваса
        cut_canvas.onMouseDragged = EventHandler { holder ->
            if(cut_CheckBox.isSelected) {
                cut.clearRect(0.0, 0.0, draw_canvas.width, draw_canvas.height)
                cut.fill = Color.rgb(0, 0, 0, 0.5)
                cut.fillRect(0.0, 0.0, draw_canvas.width, draw_canvas.height)
                cut.clearRect(min(prev_x, holder.x), min(prev_y, holder.y), abs(holder.x - prev_x), abs(holder.y - prev_y))
            }
            else {
                val size = brush_size_slider.value
                val x = holder.x - size / 2
                val y = holder.y - size / 2
                if (holder.button == MouseButton.SECONDARY) {
                    draw.clearRect(x, y, size, size)
                } else {
                    draw.fill = brushColorPicker.value
                    draw.fillOval(x, y, size, size)
                }
            }
        }
        //-------------------------замена фотки
        cut_canvas.onMouseReleased = EventHandler { holder ->
            if (cut_CheckBox.isSelected && (prev_x != holder.x && prev_y != holder.y)) {
                cut.clearRect(0.0, 0.0, draw_canvas.width, draw_canvas.height)
                val params = SnapshotParameters()
                params.fill = Color.TRANSPARENT
                val snapImg = img_canvas.snapshot(params, null)
                val snapDraw = draw_canvas.snapshot(params, null)
                val x = max(min(prev_x, holder.x), 0.0)
                val y = max(min(prev_y, holder.y), 0.0)
                val cropWidth = min(abs(holder.x - prev_x), img_canvas.width - x)
                val cropHeight = min(abs(holder.y - prev_y), img_canvas.height - y)
                val croppedImg = WritableImage(snapImg.pixelReader, x.toInt(), y.toInt(), cropWidth.toInt(), cropHeight.toInt())
                val croppedDraw = WritableImage(snapDraw.pixelReader, x.toInt(), y.toInt(), cropWidth.toInt(), cropHeight.toInt())

                refreshCanvas(img_canvas, draw_canvas, cut_canvas, croppedImg)
                draw.drawImage(croppedDraw, x, y, cropWidth, cropHeight)
            }
        }
        screeenshot_btn.onAction = EventHandler {
            takeScreenshot(Checkbox_for_hide.isSelected, delaySlider.value.toLong(), img_canvas, draw_canvas, cut_canvas, primaryStage)
        }
        //-------------------------adds
        tools.children.addAll(screeenshot_btn, Checkbox_for_hide, delayVBox, brushColorPicker, brush_size_VBox, cut_CheckBox)
        stack.children.addAll(img_canvas, draw_canvas, cut_canvas)
        brush_size_VBox.children.addAll(brush_size_label, brush_size_slider)
        delayVBox.children.addAll(delayLabel, delaySlider)
        //-------------------------Меню
        menuItemOpen.onAction = EventHandler {
            openImage(img_canvas, draw_canvas, cut_canvas, primaryStage)
        }
        menuItemOpen.accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN)
        menuItemSave.onAction = EventHandler {
            saveImage(true, img_canvas, draw_canvas, primaryStage)
        }
        menuItemSave.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)

        menuItemSaveAs.onAction = EventHandler {
            saveImage(false, img_canvas, draw_canvas, primaryStage)
        }
        menuItemSaveAs.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
        menuFile.items.addAll(menuItemOpen, menuItemSave, menuItemSaveAs)
        menuBar.menus.addAll(menuFile)
        //-------------------------Оснвной бокс для всего
        val root = VBox(menuBar, tools, scroll)

        //-------------------------Сцена(окно)
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val scene = Scene(root, screenSize.getWidth() / 2, screenSize.getHeight() / 2)

        primaryStage.title = "MyApp"
        primaryStage.scene = scene
        primaryStage.show()
        primaryStage.isMaximized = true
    }
    //-------------------------Функция по скриншоту
    fun takeScreenshot(isHide: Boolean, delay: Long, imgC : Canvas, drawC : Canvas, cutC : Canvas, stage: Stage) {
        if (isHide) {
            stage.hide()
            Thread.sleep(400)
        }
        Thread.sleep(delay * 1000)
        try {
            val robot = Robot()
            val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            val image = SwingFXUtils.toFXImage(robot.createScreenCapture(screenRect), null)
            refreshCanvas(imgC, drawC, cutC, image)
        } catch (ex: IOException) {
            print(ex)
        }
        stage.show()
    }
    //-------------------------обновление для фотки
    fun refreshCanvas(canvas_1:Canvas, canvas_2: Canvas, canvas_3:Canvas, image: Image?) {
        if (image != null) {
            canvas_2.graphicsContext2D.clearRect(0.0, 0.0, canvas_2.width, canvas_2.height)
            image.height.also {
                canvas_1.height = it
                canvas_2.height = it
                canvas_3.height = it
            }
            image.width.also {
                canvas_1.width = it
                canvas_2.width = it
                canvas_3.width = it
            }
            canvas_1.graphicsContext2D.drawImage(image, 0.0, 0.0)
        }
    }
    //-------------------------открытие фотки
    fun openImage(imgC : Canvas, drawC : Canvas, cutC : Canvas, stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"))
        fileChooser.initialDirectory = File(getLastDir())
        val file = fileChooser.showOpenDialog(stage)
        if (file != null) {
            setLastDir(file)
            val image = Image(file.toURI().toString())
            refreshCanvas(imgC, drawC, cutC, image)
        }
    }
    //-------------------------сохранение
    fun saveImage(isQuick: Boolean, imgC : Canvas, drawC : Canvas, stage: Stage) {
        val fileName =  "screanshot.png"
        val file: File
        if (isQuick) {
            file = File(default_path + fileName)
        } else {
            val directoryChooser = DirectoryChooser()
            directoryChooser.initialDirectory = File(getLastDir())
            val dir = directoryChooser.showDialog(stage) ?: return
            file = File(dir.toString() + fileName)
        }
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        val snapImg = imgC.snapshot(params, null)
        val snapDraw = drawC.snapshot(params, null)
        val result = Canvas(imgC.width, drawC.height)
        val resultCtx = result.graphicsContext2D
        resultCtx.drawImage(snapImg, 0.0, 0.0)
        resultCtx.drawImage(snapDraw, 0.0, 0.0)
        ImageIO.write(SwingFXUtils.fromFXImage(result.snapshot(params, null), null), "png", file)
        if (!isQuick) {
            setLastDir(file)
        }
    }

    //-------------------------Работа с путем фаила для последующего открытия
    fun setLastDir(file: File) {
        try {
            BufferedWriter(FileWriter(default_path + directorry_a)).use { bw ->
                bw.write(file.parent)
            }
        } catch (ex: IOException) {
            print(ex)
        }
    }

    fun getLastDir(): String {
        return try {
            BufferedReader(FileReader(default_path + directorry_a)).readLine()
        } catch (ex: IOException) {
            print(ex)
            default_path
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MyApp::class.java)
        }
    }
}