import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Холст редактора.
 * Класс обрабатывает мышь и отображает документ, а хранение изображения
 * и история действий находятся в DrawingDocument.
 */
public class Canvas extends JComponent {
    public enum Tool {
        PENCIL, BRUSH, ERASER,
        LINE, RECT, ROUND_RECT, OVAL, TRIANGLE, DIAMOND,
        SPRAY, FILL, EYEDROPPER, TEXT
    }

    private static final Color CHECKER_LIGHT = new Color(245, 245, 245);
    private static final Color CHECKER_DARK = new Color(225, 225, 225);
    private static final int CHECKER_SIZE = 16;

    private final DrawingDocument document = new DrawingDocument();
    private final Random random = new Random();

    private Color drawingColor = new Color(35, 105, 190);
    private int strokeWidth = 4;
    private Tool selectedTool = Tool.PENCIL;

    private Point drawingStart;
    private Point currentPoint;
    private Point previousPoint;

    private boolean drawingShape;
    private boolean showGrid;
    private boolean filledShapes;

    public Canvas() {
        setPreferredSize(new Dimension(
                DrawingDocument.DEFAULT_WIDTH,
                DrawingDocument.DEFAULT_HEIGHT
        ));
        setOpaque(true);
        installMouseHandlers();
    }

    /** Подключает обработчики нажатия, перемещения и отпускания мыши. */
    private void installMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                beginDrawing(event.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                continueDrawing(event.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                finishDrawing(event.getPoint());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void beginDrawing(Point point) {
        if (!isInsideCanvas(point)) {
            return;
        }

        drawingStart = point;
        currentPoint = point;
        previousPoint = point;

        if (selectedTool == Tool.EYEDROPPER) {
            pickColor(point);
            resetMouseState();
            return;
        }

        if (selectedTool == Tool.TEXT) {
            addText(point);
            resetMouseState();
            return;
        }

        document.saveUndoState();

        if (selectedTool == Tool.FILL) {
            document.floodFill(point.x, point.y, drawingColor);
            resetMouseState();
            repaint();
            return;
        }

        if (isFreehandTool()) {
            drawDot(point);
        } else if (selectedTool == Tool.SPRAY) {
            sprayPaint(point);
        } else {
            drawingShape = true;
        }

        repaint();
    }

    private void continueDrawing(Point point) {
        if (drawingStart == null) {
            return;
        }

        currentPoint = limitToCanvas(point);

        if (isFreehandTool()) {
            drawLineSegment(previousPoint, currentPoint);
            previousPoint = currentPoint;
        } else if (selectedTool == Tool.SPRAY) {
            sprayPaint(currentPoint);
        }

        repaint();
    }

    private void finishDrawing(Point point) {
        if (drawingStart == null) {
            return;
        }

        currentPoint = limitToCanvas(point);

        if (drawingShape) {
            Graphics2D graphics = document.getImage().createGraphics();
            configureGraphics(graphics);
            ShapeDrawer.draw(
                    graphics,
                    selectedTool,
                    drawingStart,
                    currentPoint,
                    strokeWidth,
                    filledShapes
            );
            graphics.dispose();
        }

        resetMouseState();
        repaint();
    }

    private void resetMouseState() {
        drawingStart = null;
        currentPoint = null;
        previousPoint = null;
        drawingShape = false;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D canvasGraphics = (Graphics2D) graphics.create();
        drawTransparencyBackground(canvasGraphics);
        canvasGraphics.drawImage(document.getImage(), 0, 0, null);
        drawGrid(canvasGraphics);
        drawShapePreview(canvasGraphics);
        canvasGraphics.dispose();
    }

    /** Шахматная подложка помогает видеть прозрачные участки изображения. */
    private void drawTransparencyBackground(Graphics2D graphics) {
        graphics.setColor(CHECKER_DARK);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        int imageWidth = document.getImage().getWidth();
        int imageHeight = document.getImage().getHeight();

        for (int y = 0; y < imageHeight; y += CHECKER_SIZE) {
            for (int x = 0; x < imageWidth; x += CHECKER_SIZE) {
                boolean lightCell = ((x / CHECKER_SIZE) + (y / CHECKER_SIZE)) % 2 == 0;
                if (lightCell) {
                    graphics.setColor(CHECKER_LIGHT);
                    graphics.fillRect(x, y, CHECKER_SIZE, CHECKER_SIZE);
                }
            }
        }
    }

    private void drawGrid(Graphics2D graphics) {
        if (!showGrid) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 35));
        for (int x = 0; x < DrawingDocument.DEFAULT_WIDTH; x += 25) {
            graphics.drawLine(x, 0, x, document.getImage().getHeight());
        }
        for (int y = 0; y < DrawingDocument.DEFAULT_HEIGHT; y += 25) {
            graphics.drawLine(0, y, document.getImage().getWidth(), y);
        }
    }

    /** Показывает контур фигуры во время перетаскивания мыши. */
    private void drawShapePreview(Graphics2D graphics) {
        if (!drawingShape || drawingStart == null || currentPoint == null) {
            return;
        }

        graphics.setColor(drawingColor);
        ShapeDrawer.draw(
                graphics,
                selectedTool,
                drawingStart,
                currentPoint,
                strokeWidth,
                filledShapes
        );
    }

    private boolean isFreehandTool() {
        return selectedTool == Tool.PENCIL
                || selectedTool == Tool.BRUSH
                || selectedTool == Tool.ERASER;
    }

    private boolean isInsideCanvas(Point point) {
        return point.x >= 0
                && point.y >= 0
                && point.x < document.getImage().getWidth()
                && point.y < document.getImage().getHeight();
    }

    private Point limitToCanvas(Point point) {
        int x = Math.max(0, Math.min(document.getImage().getWidth() - 1, point.x));
        int y = Math.max(0, Math.min(document.getImage().getHeight() - 1, point.y));
        return new Point(x, y);
    }

    private void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics.setColor(drawingColor);
        graphics.setStroke(new BasicStroke(
                strokeWidth,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        if (selectedTool == Tool.ERASER) {
            graphics.setComposite(AlphaComposite.Clear);
        }
    }

    private void drawDot(Point point) {
        Graphics2D graphics = document.getImage().createGraphics();
        configureGraphics(graphics);
        graphics.fillOval(
                point.x - strokeWidth / 2,
                point.y - strokeWidth / 2,
                strokeWidth,
                strokeWidth
        );
        graphics.dispose();
    }

    private void drawLineSegment(Point start, Point end) {
        Graphics2D graphics = document.getImage().createGraphics();
        configureGraphics(graphics);
        graphics.drawLine(start.x, start.y, end.x, end.y);
        graphics.dispose();
    }

    /** Рисует небольшие случайные точки вокруг курсора, имитируя распылитель. */
    private void sprayPaint(Point center) {
        Graphics2D graphics = document.getImage().createGraphics();
        graphics.setColor(drawingColor);

        int radius = Math.max(5, strokeWidth * 2);
        int dotsCount = Math.max(12, strokeWidth * 3);

        for (int i = 0; i < dotsCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            int x = center.x + (int) (Math.cos(angle) * distance);
            int y = center.y + (int) (Math.sin(angle) * distance);
            graphics.fillRect(x, y, 1, 1);
        }

        graphics.dispose();
    }

    private void addText(Point position) {
        String text = JOptionPane.showInputDialog(
                this,
                "Введите текст:",
                "Добавление текста",
                JOptionPane.PLAIN_MESSAGE
        );

        if (text == null || text.isBlank()) {
            return;
        }

        document.saveUndoState();

        Graphics2D graphics = document.getImage().createGraphics();
        graphics.setColor(drawingColor);
        graphics.setFont(new Font(
                Font.SANS_SERIF,
                Font.PLAIN,
                Math.max(12, strokeWidth * 5)
        ));
        graphics.drawString(text, position.x, position.y);
        graphics.dispose();
        repaint();
    }

    private void pickColor(Point point) {
        drawingColor = new Color(document.getImage().getRGB(point.x, point.y), true);
        firePropertyChange("drawingColor", null, drawingColor);
    }

    // Методы ниже вызываются панелью инструментов и главным окном.
    public void undo() {
        document.undo();
        repaint();
    }

    public void redo() {
        document.redo();
        repaint();
    }

    public void clear() {
        document.clear();
        repaint();
    }

    public void open(java.io.File file) throws java.io.IOException {
        document.open(file);
        setPreferredSize(new Dimension(
                document.getImage().getWidth(),
                document.getImage().getHeight()
        ));
        revalidate();
        repaint();
    }

    public void save(java.io.File file) throws java.io.IOException {
        document.save(file);
    }

    public void setTool(Tool tool) {
        selectedTool = tool;
    }

    public Tool getTool() {
        return selectedTool;
    }

    public void setColor(Color color) {
        if (color != null) {
            drawingColor = color;
        }
    }

    public Color getColor() {
        return drawingColor;
    }

    public void setStrokeWidth(int width) {
        strokeWidth = Math.max(1, width);
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    public void setFilledShapes(boolean filledShapes) {
        this.filledShapes = filledShapes;
    }
}
