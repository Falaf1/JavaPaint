import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;

/** Панель команд и настроек инструментов. */
public class Toolbar extends JPanel {
    private final Canvas canvas;
    private final JFileChooser fileChooser = new JFileChooser();
    private JButton colorButton;

    public Toolbar(Canvas canvas) {
        this.canvas = canvas;
        setLayout(new BorderLayout(0, 4));

        configureFileChooser();
        add(createDrawingToolsPanel(), BorderLayout.NORTH);
        add(createSettingsPanel(), BorderLayout.CENTER);
    }

    private void configureFileChooser() {
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Изображения (PNG, JPG, BMP, GIF)",
                "png", "jpg", "jpeg", "bmp", "gif"
        ));
    }

    /** Первый ряд: основные инструменты и выпадающий список фигур. */
    private JPanel createDrawingToolsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        panel.add(createToolButton("Карандаш", Canvas.Tool.PENCIL));
        panel.add(createToolButton("Кисть", Canvas.Tool.BRUSH));
        panel.add(createToolButton("Ластик", Canvas.Tool.ERASER));
        panel.add(createShapesSelector());
        panel.add(createToolButton("Распылитель", Canvas.Tool.SPRAY));
        panel.add(createToolButton("Заливка", Canvas.Tool.FILL));
        panel.add(createToolButton("Пипетка", Canvas.Tool.EYEDROPPER));
        panel.add(createToolButton("Текст", Canvas.Tool.TEXT));

        return panel;
    }

    /** Фигуры собраны в одном списке, чтобы не занимать место на панели. */
    private JComboBox<ShapeOption> createShapesSelector() {
        ShapeOption[] shapes = {
                new ShapeOption("Фигуры: линия", Canvas.Tool.LINE),
                new ShapeOption("Прямоугольник", Canvas.Tool.RECT),
                new ShapeOption("Скруглённый прямоугольник", Canvas.Tool.ROUND_RECT),
                new ShapeOption("Эллипс", Canvas.Tool.OVAL),
                new ShapeOption("Треугольник", Canvas.Tool.TRIANGLE),
                new ShapeOption("Ромб", Canvas.Tool.DIAMOND)
        };

        JComboBox<ShapeOption> selector = new JComboBox<>(shapes);
        selector.setSelectedIndex(-1);
        selector.setToolTipText("Выберите фигуру для рисования");
        selector.addActionListener(event -> {
            ShapeOption selectedShape = (ShapeOption) selector.getSelectedItem();
            if (selectedShape != null) {
                canvas.setTool(selectedShape.tool());
            }
        });

        return selector;
    }

    /** Второй ряд: цвет, толщина, заливка, сетка и команды документа. */
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        colorButton = new JButton("Цвет");
        updateColorButton();
        colorButton.addActionListener(event -> chooseColor());
        panel.add(colorButton);

        panel.add(new JLabel("Толщина:"));
        JSpinner sizeSelector = new JSpinner(new SpinnerNumberModel(
                canvas.getStrokeWidth(), 1, 80, 1
        ));
        sizeSelector.addChangeListener(event ->
                canvas.setStrokeWidth((int) sizeSelector.getValue())
        );
        panel.add(sizeSelector);

        JCheckBox filledShapes = new JCheckBox("Заливка фигур");
        filledShapes.addActionListener(event ->
                canvas.setFilledShapes(filledShapes.isSelected())
        );
        panel.add(filledShapes);

        JCheckBox grid = new JCheckBox("Сетка");
        grid.addActionListener(event -> canvas.setShowGrid(grid.isSelected()));
        panel.add(grid);

        panel.add(new javax.swing.JSeparator(SwingConstants.VERTICAL));
        panel.add(createButton("Отменить", canvas::undo));
        panel.add(createButton("Повторить", canvas::redo));
        panel.add(createButton("Очистить", canvas::clear));
        panel.add(createButton("Открыть", this::openFile));
        panel.add(createButton("Сохранить", this::saveFile));

        canvas.addPropertyChangeListener("drawingColor", event -> updateColorButton());
        return panel;
    }

    private JButton createToolButton(String title, Canvas.Tool tool) {
        return createButton(title, () -> canvas.setTool(tool));
    }

    private JButton createButton(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void chooseColor() {
        Color selectedColor = JColorChooser.showDialog(
                this,
                "Основной цвет",
                canvas.getColor()
        );

        if (selectedColor != null) {
            canvas.setColor(selectedColor);
            updateColorButton();
        }
    }

    private void updateColorButton() {
        if (colorButton == null) {
            return;
        }

        Color color = canvas.getColor();
        colorButton.setBackground(color);
        colorButton.setForeground(getContrastingColor(color));
    }

    /** Выбирает читаемый цвет текста для кнопки с образцом цвета. */
    private Color getContrastingColor(Color color) {
        int brightness = (color.getRed() * 299
                + color.getGreen() * 587
                + color.getBlue() * 114) / 1000;

        return brightness > 140 ? Color.BLACK : Color.WHITE;
    }

    private void saveFile() {
        fileChooser.setDialogTitle("Сохранить изображение");
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = addDefaultExtension(fileChooser.getSelectedFile());
        try {
            canvas.save(file);
        } catch (IOException exception) {
            showFileError("Не удалось сохранить файл", exception);
        }
    }

    private File addDefaultExtension(File file) {
        String fileName = file.getName();
        boolean hasImageExtension = fileName.matches("(?i).*\\.(png|jpg|jpeg|bmp|gif)$");

        if (hasImageExtension) {
            return file;
        }

        return new File(file.getAbsolutePath() + ".png");
    }

    private void openFile() {
        fileChooser.setDialogTitle("Открыть изображение");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            canvas.open(fileChooser.getSelectedFile());
        } catch (IOException exception) {
            showFileError("Не удалось открыть файл", exception);
        }
    }

    private void showFileError(String message, IOException exception) {
        JOptionPane.showMessageDialog(
                this,
                message + ":\n" + exception.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /** Пункт списка: подпись фигуры и соответствующий инструмент. */
    private record ShapeOption(String title, Canvas.Tool tool) {
        @Override
        public String toString() {
            return title;
        }
    }
}
