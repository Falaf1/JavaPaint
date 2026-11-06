import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Хранит изображение и историю изменений.
 * Здесь нет кода интерфейса: класс отвечает только за данные документа.
 */
public class DrawingDocument {
    public static final int DEFAULT_WIDTH = 1200;
    public static final int DEFAULT_HEIGHT = 800;
    private static final int HISTORY_LIMIT = 50;

    private BufferedImage image;
    private final Deque<BufferedImage> undoHistory = new ArrayDeque<>();
    private final Deque<BufferedImage> redoHistory = new ArrayDeque<>();

    public DrawingDocument() {
        createBlankImage();
    }

    public BufferedImage getImage() {
        return image;
    }

    /** Запоминает текущее состояние перед изменением изображения. */
    public void saveUndoState() {
        undoHistory.push(copyImage(image));

        while (undoHistory.size() > HISTORY_LIMIT) {
            undoHistory.removeLast();
        }

        // После нового действия повтор ранее отменённых действий невозможен.
        redoHistory.clear();
    }

    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }

        redoHistory.push(copyImage(image));
        image = undoHistory.pop();
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }

        undoHistory.push(copyImage(image));
        image = redoHistory.pop();
    }

    public void clear() {
        saveUndoState();
        createBlankImage();
    }

    /** Очищает документ и создаёт новый прозрачный холст стандартного размера. */
    private void createBlankImage() {
        image = new BufferedImage(
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );
    }

    public void open(File file) throws IOException {
        BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Формат изображения не поддерживается.");
        }

        saveUndoState();
        image = copyToArgb(loadedImage);
    }

    public void save(File file) throws IOException {
        String format = getFormatFromFileName(file);
        BufferedImage imageToSave = image;

        // JPEG и BMP не поддерживают прозрачность. Для них используем белый фон.
        if (format.equals("jpg") || format.equals("bmp")) {
            imageToSave = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D graphics = imageToSave.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, imageToSave.getWidth(), imageToSave.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        if (!ImageIO.write(imageToSave, format, file)) {
            throw new IOException("Не удалось сохранить изображение в формате " + format);
        }
    }

    private String getFormatFromFileName(File file) {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".bmp")) return "bmp";
        if (name.endsWith(".gif")) return "gif";
        return "png";
    }

    /** Заливает соседние пиксели, имеющие исходный цвет. */
    public void floodFill(int startX, int startY, Color replacementColor) {
        int targetColor = image.getRGB(startX, startY);
        int newColor = replacementColor.getRGB();

        if (targetColor == newColor) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] positions = new int[width * height];
        int stackSize = 0;

        int startPosition = startY * width + startX;
        positions[stackSize++] = startPosition;
        pixels[startPosition] = newColor;

        // Каждый пиксель помечается новым цветом при добавлении в стек,
        // поэтому один и тот же пиксель не попадёт в стек дважды.
        while (stackSize > 0) {
            int position = positions[--stackSize];
            int x = position % width;
            int y = position / width;

            if (x > 0 && pixels[position - 1] == targetColor) {
                pixels[position - 1] = newColor;
                positions[stackSize++] = position - 1;
            }
            if (x < width - 1 && pixels[position + 1] == targetColor) {
                pixels[position + 1] = newColor;
                positions[stackSize++] = position + 1;
            }
            if (y > 0 && pixels[position - width] == targetColor) {
                pixels[position - width] = newColor;
                positions[stackSize++] = position - width;
            }
            if (y < height - 1 && pixels[position + width] == targetColor) {
                pixels[position + width] = newColor;
                positions[stackSize++] = position + width;
            }
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
    }

    private BufferedImage copyToArgb(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private BufferedImage copyImage(BufferedImage source) {
        return copyToArgb(source);
    }
}
