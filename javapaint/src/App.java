import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/** Точка входа в приложение JavaPaint. */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowWindow);
    }

    private static void createAndShowWindow() {
        setSystemLookAndFeel();

        JFrame window = new JFrame("JavaPaint");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1250, 900);
        window.setLayout(new BorderLayout());

        Canvas canvas = new Canvas();
        Toolbar toolbar = new Toolbar(canvas);

        window.add(toolbar, BorderLayout.NORTH);
        window.add(new JScrollPane(canvas), BorderLayout.CENTER);
        window.setIconImage(createApplicationIcon());

        installKeyboardShortcuts(window, canvas);

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            // Если системная тема недоступна, Swing использует стандартную.
        }
    }

    /** Назначает сочетания клавиш, действующие независимо от фокуса компонентов. */
    private static void installKeyboardShortcuts(JFrame window, Canvas canvas) {
        JComponent root = window.getRootPane();

        addShortcut(root, "control Z", "undo", canvas::undo);
        addShortcut(root, "control Y", "redo", canvas::redo);
    }

    private static void addShortcut(
            JComponent component,
            String keyStroke,
            String actionName,
            Runnable action
    ) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyStroke), actionName);

        component.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    /** Иконка холста с кистью в сине-оранжевой цветовой гамме JavaPaint. */
    private static Image createApplicationIcon() {
        int iconSize = 64;
        BufferedImage icon = new BufferedImage(
                iconSize,
                iconSize,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = icon.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color javaBlue = new Color(30, 95, 165);
        Color javaOrange = new Color(238, 120, 35);

        graphics.setColor(javaBlue);
        graphics.fillRoundRect(3, 5, 58, 54, 12, 12);

        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(10, 11, 44, 40, 6, 6);

        graphics.setStroke(new BasicStroke(
                5,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));
        graphics.setColor(javaOrange);
        graphics.drawLine(18, 39, 39, 18);

        graphics.setColor(javaBlue);
        graphics.drawLine(25, 45, 47, 23);

        graphics.setColor(javaOrange);
        graphics.fillOval(14, 35, 9, 9);
        graphics.setColor(javaBlue);
        graphics.fillOval(41, 15, 9, 9);

        graphics.dispose();
        return icon;
    }
}
