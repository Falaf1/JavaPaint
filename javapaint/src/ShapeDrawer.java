import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;

/** Содержит отдельные методы отрисовки геометрических фигур. */
public final class ShapeDrawer {
    private ShapeDrawer() {
        // Утилитный класс: создавать его экземпляры не нужно.
    }

    public static void draw(
            Graphics2D graphics,
            Canvas.Tool shape,
            Point start,
            Point end,
            int lineWidth,
            boolean filled
    ) {
        graphics.setStroke(new BasicStroke(
                lineWidth,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        switch (shape) {
            case LINE -> graphics.drawLine(start.x, start.y, end.x, end.y);
            case RECT -> drawRectangle(graphics, x, y, width, height, filled);
            case ROUND_RECT -> drawRoundedRectangle(graphics, x, y, width, height, filled);
            case OVAL -> drawOval(graphics, x, y, width, height, filled);
            case TRIANGLE -> drawTriangle(graphics, x, y, width, height, filled);
            case DIAMOND -> drawDiamond(graphics, x, y, width, height, filled);
            default -> { }
        }
    }

    private static void drawRectangle(
            Graphics2D graphics, int x, int y, int width, int height, boolean filled
    ) {
        if (filled) {
            graphics.fillRect(x, y, width, height);
        } else {
            graphics.drawRect(x, y, width, height);
        }
    }

    private static void drawRoundedRectangle(
            Graphics2D graphics, int x, int y, int width, int height, boolean filled
    ) {
        int cornerSize = 24;
        if (filled) {
            graphics.fillRoundRect(x, y, width, height, cornerSize, cornerSize);
        } else {
            graphics.drawRoundRect(x, y, width, height, cornerSize, cornerSize);
        }
    }

    private static void drawOval(
            Graphics2D graphics, int x, int y, int width, int height, boolean filled
    ) {
        if (filled) {
            graphics.fillOval(x, y, width, height);
        } else {
            graphics.drawOval(x, y, width, height);
        }
    }

    private static void drawTriangle(
            Graphics2D graphics, int x, int y, int width, int height, boolean filled
    ) {
        Polygon triangle = new Polygon(
                new int[]{x + width / 2, x, x + width},
                new int[]{y, y + height, y + height},
                3
        );
        drawPolygon(graphics, triangle, filled);
    }

    private static void drawDiamond(
            Graphics2D graphics, int x, int y, int width, int height, boolean filled
    ) {
        Polygon diamond = new Polygon(
                new int[]{x + width / 2, x + width, x + width / 2, x},
                new int[]{y, y + height / 2, y + height, y + height / 2},
                4
        );
        drawPolygon(graphics, diamond, filled);
    }

    private static void drawPolygon(Graphics2D graphics, Polygon polygon, boolean filled) {
        if (filled) {
            graphics.fillPolygon(polygon);
        } else {
            graphics.drawPolygon(polygon);
        }
    }
}
