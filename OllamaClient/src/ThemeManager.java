package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Administrador de temas para la aplicación
 * Permite cambiar la apariencia visual de toda la aplicación
 */
public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static final ThemeManager instance = new ThemeManager();

    // Predefined themes
    public enum Theme {
        LIGHT("Light", "Tema claro estándar"),
        DARK("Dark", "Tema oscuro para uso nocturno"),
        HIGH_CONTRAST("High Contrast", "Tema de alto contraste para accesibilidad"),
        BLUE("Blue", "Tema azul profesional"),
        SEPIA("Sepia", "Tema sepia para lectura"),
        CUSTOM("Custom", "Tema personalizado por el usuario");

        private final String displayName;
        private final String description;

        Theme(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Current theme
    private Theme currentTheme = Theme.LIGHT;

    // Theme configurations
    private final Map<Theme, Map<String, Object>> themeConfigurations = new HashMap<>();

    // Custom theme properties
    private Color customAccentColor = new Color(51, 102, 204); // #3366CC
    private Color customBackgroundColor = Color.WHITE;
    private Color customTextColor = Color.BLACK;
    private Color customSelectionColor = new Color(173, 214, 255);
    private Font customFont = new Font("SansSerif", Font.PLAIN, 12);

    /**
     * Constructor privado para Singleton
     */
    private ThemeManager() {
        initializeThemeConfigurations();
    }

    /**
     * Obtiene la instancia única del administrador de temas
     */
    public static ThemeManager getInstance() {
        return instance;
    }

    /**
     * Inicializa las configuraciones de los temas predefinidos
     */
    private void initializeThemeConfigurations() {
        // Tema Claro (Light)
        Map<String, Object> lightTheme = new HashMap<>();
        lightTheme.put("backgroundColor", Color.WHITE);
        lightTheme.put("foregroundColor", Color.BLACK);
        lightTheme.put("accentColor", new Color(51, 102, 204)); // #3366CC
        lightTheme.put("selectionColor", new Color(173, 214, 255));
        lightTheme.put("buttonColor", new Color(240, 240, 240));
        lightTheme.put("borderColor", new Color(200, 200, 200));
        lightTheme.put("font", new Font("SansSerif", Font.PLAIN, 12));
        lightTheme.put("codeFont", new Font("Monospaced", Font.PLAIN, 12));
        lightTheme.put("codeBackgroundColor", new Color(240, 240, 240));
        themeConfigurations.put(Theme.LIGHT, lightTheme);

        // Tema Oscuro (Dark)
        Map<String, Object> darkTheme = new HashMap<>();
        darkTheme.put("backgroundColor", new Color(43, 43, 43));
        darkTheme.put("foregroundColor", new Color(220, 220, 220));
        darkTheme.put("accentColor", new Color(97, 175, 239)); // #61AFEF
        darkTheme.put("selectionColor", new Color(62, 81, 105));
        darkTheme.put("buttonColor", new Color(60, 60, 60));
        darkTheme.put("borderColor", new Color(80, 80, 80));
        darkTheme.put("font", new Font("SansSerif", Font.PLAIN, 12));
        darkTheme.put("codeFont", new Font("Monospaced", Font.PLAIN, 12));
        darkTheme.put("codeBackgroundColor", new Color(30, 30, 30));
        themeConfigurations.put(Theme.DARK, darkTheme);

        // Tema de Alto Contraste (High Contrast)
        Map<String, Object> highContrastTheme = new HashMap<>();
        highContrastTheme.put("backgroundColor", Color.BLACK);
        highContrastTheme.put("foregroundColor", Color.WHITE);
        highContrastTheme.put("accentColor", Color.YELLOW);
        highContrastTheme.put("selectionColor", new Color(0, 128, 255));
        highContrastTheme.put("buttonColor", new Color(40, 40, 40));
        highContrastTheme.put("borderColor", Color.WHITE);
        highContrastTheme.put("font", new Font("SansSerif", Font.BOLD, 14));
        highContrastTheme.put("codeFont", new Font("Monospaced", Font.BOLD, 14));
        highContrastTheme.put("codeBackgroundColor", Color.BLACK);
        themeConfigurations.put(Theme.HIGH_CONTRAST, highContrastTheme);

        // Tema Azul (Blue)
        Map<String, Object> blueTheme = new HashMap<>();
        blueTheme.put("backgroundColor", new Color(235, 244, 255));
        blueTheme.put("foregroundColor", new Color(16, 46, 95));
        blueTheme.put("accentColor", new Color(0, 102, 204)); // #0066CC
        blueTheme.put("selectionColor", new Color(179, 215, 255));
        blueTheme.put("buttonColor", new Color(210, 230, 255));
        blueTheme.put("borderColor", new Color(153, 186, 221));
        blueTheme.put("font", new Font("SansSerif", Font.PLAIN, 12));
        blueTheme.put("codeFont", new Font("Monospaced", Font.PLAIN, 12));
        blueTheme.put("codeBackgroundColor", new Color(245, 250, 255));
        themeConfigurations.put(Theme.BLUE, blueTheme);

        // Tema Sepia (Sepia)
        Map<String, Object> sepiaTheme = new HashMap<>();
        sepiaTheme.put("backgroundColor", new Color(249, 241, 228)); // Fondo sepia claro
        sepiaTheme.put("foregroundColor", new Color(91, 70, 50));    // Texto marrón oscuro
        sepiaTheme.put("accentColor", new Color(161, 102, 47));      // Marrón (sepia)
        sepiaTheme.put("selectionColor", new Color(233, 215, 185));
        sepiaTheme.put("buttonColor", new Color(242, 232, 212));
        sepiaTheme.put("borderColor", new Color(200, 182, 155));
        sepiaTheme.put("font", new Font("Serif", Font.PLAIN, 12));
        sepiaTheme.put("codeFont", new Font("Monospaced", Font.PLAIN, 12));
        sepiaTheme.put("codeBackgroundColor", new Color(242, 235, 217));
        themeConfigurations.put(Theme.SEPIA, sepiaTheme);

        // Tema Personalizado (Custom) - inicialmente igual al tema Light
        Map<String, Object> customTheme = new HashMap<>(lightTheme);
        themeConfigurations.put(Theme.CUSTOM, customTheme);
    }

    /**
     * Aplica el tema especificado a toda la aplicación
     */
    public void applyTheme(Theme theme) {
        this.currentTheme = theme;
        logger.info("Aplicando tema: {}", theme.getDisplayName());

        Map<String, Object> themeConfig = themeConfigurations.get(theme);
        if (themeConfig == null) {
            logger.error("Configuración de tema no encontrada para: {}", theme);
            return;
        }

        // Actualizar valores del tema personalizado desde la configuración guardada
        if (theme == Theme.CUSTOM) {
            updateCustomThemeFromConfig();
        }

        // Obtener colores y fuentes del tema actual
        Color backgroundColor = (Color) themeConfig.get("backgroundColor");
        Color foregroundColor = (Color) themeConfig.get("foregroundColor");
        Color accentColor = (Color) themeConfig.get("accentColor");
        Color selectionColor = (Color) themeConfig.get("selectionColor");
        Color buttonColor = (Color) themeConfig.get("buttonColor");
        Color borderColor = (Color) themeConfig.get("borderColor");
        Font font = (Font) themeConfig.get("font");
        Font codeFont = (Font) themeConfig.get("codeFont");
        Color codeBackgroundColor = (Color) themeConfig.get("codeBackgroundColor");

        // Aplicar colores al UIManager
        UIManager.put("Panel.background", backgroundColor);
        UIManager.put("Panel.foreground", foregroundColor);
        UIManager.put("Label.background", backgroundColor);
        UIManager.put("Label.foreground", foregroundColor);
        UIManager.put("TextField.background", backgroundColor);
        UIManager.put("TextField.foreground", foregroundColor);
        UIManager.put("TextField.selectionBackground", selectionColor);
        UIManager.put("TextField.selectionForeground", foregroundColor);
        UIManager.put("TextArea.background", backgroundColor);
        UIManager.put("TextArea.foreground", foregroundColor);
        UIManager.put("TextArea.selectionBackground", selectionColor);
        UIManager.put("TextArea.selectionForeground", foregroundColor);
        UIManager.put("EditorPane.background", backgroundColor);
        UIManager.put("EditorPane.foreground", foregroundColor);
        UIManager.put("TextPane.background", backgroundColor);
        UIManager.put("TextPane.foreground", foregroundColor);
        UIManager.put("Button.background", buttonColor);
        UIManager.put("Button.foreground", foregroundColor);
        UIManager.put("Button.select", accentColor);
        UIManager.put("ComboBox.background", backgroundColor);
        UIManager.put("ComboBox.foreground", foregroundColor);
        UIManager.put("ComboBox.selectionBackground", selectionColor);
        UIManager.put("ComboBox.selectionForeground", foregroundColor);
        UIManager.put("List.background", backgroundColor);
        UIManager.put("List.foreground", foregroundColor);
        UIManager.put("List.selectionBackground", selectionColor);
        UIManager.put("List.selectionForeground", foregroundColor);
        UIManager.put("Table.background", backgroundColor);
        UIManager.put("Table.foreground", foregroundColor);
        UIManager.put("Table.selectionBackground", selectionColor);
        UIManager.put("Table.selectionForeground", foregroundColor);
        UIManager.put("TableHeader.background", buttonColor);
        UIManager.put("TableHeader.foreground", foregroundColor);
        UIManager.put("Tree.background", backgroundColor);
        UIManager.put("Tree.foreground", foregroundColor);
        UIManager.put("Tree.selectionBackground", selectionColor);
        UIManager.put("Tree.selectionForeground", foregroundColor);
        UIManager.put("ScrollPane.background", backgroundColor);
        UIManager.put("ScrollPane.foreground", foregroundColor);
        UIManager.put("Viewport.background", backgroundColor);
        UIManager.put("Viewport.foreground", foregroundColor);
        UIManager.put("Menu.background", backgroundColor);
        UIManager.put("Menu.foreground", foregroundColor);
        UIManager.put("Menu.selectionBackground", selectionColor);
        UIManager.put("Menu.selectionForeground", foregroundColor);
        UIManager.put("MenuItem.background", backgroundColor);
        UIManager.put("MenuItem.foreground", foregroundColor);
        UIManager.put("MenuItem.selectionBackground", selectionColor);
        UIManager.put("MenuItem.selectionForeground", foregroundColor);
        UIManager.put("MenuBar.background", backgroundColor);
        UIManager.put("MenuBar.foreground", foregroundColor);
        UIManager.put("OptionPane.background", backgroundColor);
        UIManager.put("OptionPane.foreground", foregroundColor);
        UIManager.put("ProgressBar.background", backgroundColor);
        UIManager.put("ProgressBar.foreground", accentColor);
        UIManager.put("ProgressBar.selectionBackground", selectionColor);
        UIManager.put("ProgressBar.selectionForeground", foregroundColor);
        UIManager.put("RadioButton.background", backgroundColor);
        UIManager.put("RadioButton.foreground", foregroundColor);
        UIManager.put("CheckBox.background", backgroundColor);
        UIManager.put("CheckBox.foreground", foregroundColor);
        UIManager.put("Slider.background", backgroundColor);
        UIManager.put("Slider.foreground", foregroundColor);
        UIManager.put("TabbedPane.background", backgroundColor);
        UIManager.put("TabbedPane.foreground", foregroundColor);
        UIManager.put("TabbedPane.selected", new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50));
        UIManager.put("TabbedPane.selectedForeground", foregroundColor);
        UIManager.put("TitledBorder.titleColor", foregroundColor);

        // Aplicar fuente principal a toda la UI
        setUIFont(new FontUIResource(font));

        // Guardar los valores del tema actual para acceso rápido
        updateCustomThemeProperties(theme);

        // Notificar cambio de LookAndFeel
        SwingUtilities.invokeLater(() -> {
            try {
                // Guardar la configuración del tema
                saveThemeToConfig();

                // Actualizar la UI de todas las ventanas
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }
            } catch (Exception e) {
                logger.error("Error al actualizar la UI después de cambiar el tema", e);
            }
        });
    }

    /**
     * Guarda la configuración del tema actual
     */
    private void saveThemeToConfig() {
        ConfigManager configManager = ConfigManager.getInstance();

        // Guardar el nombre del tema actual
        configManager.setThemeConfig("themeName", currentTheme.name());

        // Si es un tema personalizado, guardar sus propiedades
        if (currentTheme == Theme.CUSTOM) {
            configManager.setThemeConfig("customAccentColor", "#" +
                    String.format("%02X%02X%02X",
                            customAccentColor.getRed(),
                            customAccentColor.getGreen(),
                            customAccentColor.getBlue()));

            configManager.setThemeConfig("customBackgroundColor", "#" +
                    String.format("%02X%02X%02X",
                            customBackgroundColor.getRed(),
                            customBackgroundColor.getGreen(),
                            customBackgroundColor.getBlue()));

            configManager.setThemeConfig("customFontFamily", customFont.getFamily());
            configManager.setThemeConfig("customFontSize", customFont.getSize());
        }
    }

    /**
     * Actualiza las propiedades del tema personalizado desde la configuración
     */
    private void updateCustomThemeFromConfig() {
        ConfigManager configManager = ConfigManager.getInstance();

        // Color de acento personalizado
        String accentColorHex = configManager.getThemeConfig("customAccentColor", "#3366CC");
        try {
            customAccentColor = Color.decode(accentColorHex);
        } catch (NumberFormatException e) {
            logger.warn("Color de acento personalizado inválido: {}", accentColorHex);
            customAccentColor = new Color(51, 102, 204); // #3366CC (predeterminado)
        }

        // Color de fondo personalizado
        String backgroundColorHex = configManager.getThemeConfig("customBackgroundColor", "#FFFFFF");
        try {
            customBackgroundColor = Color.decode(backgroundColorHex);
        } catch (NumberFormatException e) {
            logger.warn("Color de fondo personalizado inválido: {}", backgroundColorHex);
            customBackgroundColor = Color.WHITE; // Predeterminado
        }

        // Calcular color de texto basado en el brillo del fondo
        double brightness = (customBackgroundColor.getRed() * 0.299 +
                customBackgroundColor.getGreen() * 0.587 +
                customBackgroundColor.getBlue() * 0.114) / 255;

        if (brightness > 0.5) {
            customTextColor = Color.BLACK; // Fondo claro, texto oscuro
        } else {
            customTextColor = Color.WHITE; // Fondo oscuro, texto claro
        }

        // Fuente personalizada
        String fontFamily = configManager.getThemeConfig("customFontFamily", "SansSerif");
        int fontSize = configManager.getThemeConfig("customFontSize", 12);
        customFont = new Font(fontFamily, Font.PLAIN, fontSize);

        // Actualizar el mapa de configuración del tema personalizado
        Map<String, Object> customTheme = themeConfigurations.get(Theme.CUSTOM);
        customTheme.put("backgroundColor", customBackgroundColor);
        customTheme.put("foregroundColor", customTextColor);
        customTheme.put("accentColor", customAccentColor);
        customTheme.put("selectionColor", new Color(
                Math.min(255, customAccentColor.getRed() + 50),
                Math.min(255, customAccentColor.getGreen() + 50),
                Math.min(255, customAccentColor.getBlue() + 50),
                128));
        customTheme.put("font", customFont);

        // Calcular colores secundarios basados en los colores principales
        Color buttonColor = new Color(
                Math.max(0, Math.min(255, customBackgroundColor.getRed() - 10)),
                Math.max(0, Math.min(255, customBackgroundColor.getGreen() - 10)),
                Math.max(0, Math.min(255, customBackgroundColor.getBlue() - 10)));

        Color borderColor = new Color(
                Math.max(0, Math.min(255, customBackgroundColor.getRed() - 40)),
                Math.max(0, Math.min(255, customBackgroundColor.getGreen() - 40)),
                Math.max(0, Math.min(255, customBackgroundColor.getBlue() - 40)));

        customTheme.put("buttonColor", buttonColor);
        customTheme.put("borderColor", borderColor);
    }

    /**
     * Actualiza las propiedades rápidas del tema actual
     */
    private void updateCustomThemeProperties(Theme theme) {
        Map<String, Object> themeConfig = themeConfigurations.get(theme);

        // Actualizar propiedades para acceso rápido
        customAccentColor = (Color) themeConfig.get("accentColor");
        customBackgroundColor = (Color) themeConfig.get("backgroundColor");
        customTextColor = (Color) themeConfig.get("foregroundColor");
        customSelectionColor = (Color) themeConfig.get("selectionColor");
        customFont = (Font) themeConfig.get("font");
    }

    /**
     * Aplica una fuente a todos los componentes de la UI
     */
    private void setUIFont(FontUIResource font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    /**
     * Crea una fuente derivada con el estilo especificado
     */
    public Font deriveFont(Font baseFont, int style, float size) {
        return baseFont.deriveFont(style, size);
    }

    /**
     * Crea una fuente desde una familia, estilo y tamaño
     */
    public Font createFont(String family, int style, int size) {
        return StyleContext.getDefaultStyleContext().getFont(family, style, size);
    }

    /**
     * Carga el tema desde la configuración guardada
     */
    public void loadThemeFromConfig() {
        ConfigManager configManager = ConfigManager.getInstance();
        String themeName = configManager.getThemeConfig("themeName", Theme.LIGHT.name());

        try {
            Theme theme = Theme.valueOf(themeName);
            applyTheme(theme);
        } catch (IllegalArgumentException e) {
            logger.warn("Nombre de tema inválido en la configuración: {}", themeName);
            applyTheme(Theme.LIGHT);
        }
    }

    /**
     * Obtiene el tema actual
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Obtiene el color de acento actual
     */
    public Color getAccentColor() {
        return customAccentColor;
    }

    /**
     * Obtiene el color de fondo actual
     */
    public Color getBackgroundColor() {
        return customBackgroundColor;
    }

    /**
     * Obtiene el color de texto actual
     */
    public Color getTextColor() {
        return customTextColor;
    }

    /**
     * Obtiene el color de selección actual
     */
    public Color getSelectionColor() {
        return customSelectionColor;
    }

    /**
     * Obtiene la fuente actual
     */
    public Font getFont() {
        return customFont;
    }

    /**
     * Obtiene la fuente de código actual
     */
    public Font getCodeFont() {
        Map<String, Object> themeConfig = themeConfigurations.get(currentTheme);
        return (Font) themeConfig.get("codeFont");
    }

    /**
     * Obtiene el color de fondo de código actual
     */
    public Color getCodeBackgroundColor() {
        Map<String, Object> themeConfig = themeConfigurations.get(currentTheme);
        return (Color) themeConfig.get("codeBackgroundColor");
    }

    /**
     * Establece el color de acento personalizado
     */
    public void setCustomAccentColor(Color color) {
        if (color != null) {
            this.customAccentColor = color;
            if (currentTheme == Theme.CUSTOM) {
                Map<String, Object> customTheme = themeConfigurations.get(Theme.CUSTOM);
                customTheme.put("accentColor", color);
                applyTheme(Theme.CUSTOM);
            }
        }
    }

    /**
     * Establece el color de fondo personalizado
     */
    public void setCustomBackgroundColor(Color color) {
        if (color != null) {
            this.customBackgroundColor = color;
            if (currentTheme == Theme.CUSTOM) {
                Map<String, Object> customTheme = themeConfigurations.get(Theme.CUSTOM);
                customTheme.put("backgroundColor", color);
                applyTheme(Theme.CUSTOM);
            }
        }
    }

    /**
     * Establece la fuente personalizada
     */
    public void setCustomFont(Font font) {
        if (font != null) {
            this.customFont = font;
            if (currentTheme == Theme.CUSTOM) {
                Map<String, Object> customTheme = themeConfigurations.get(Theme.CUSTOM);
                customTheme.put("font", font);
                applyTheme(Theme.CUSTOM);
            }
        }
    }

    /**
     * Crea un borde con los colores del tema actual
     */
    public Border createBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /**
     * Crea un borde titulado con los colores del tema actual
     */
    public Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder((Color) themeConfigurations.get(currentTheme).get("borderColor")),
                title
        );
    }

    /**
     * Muestra el diálogo de configuración de tema
     */
    public void showThemeDialog(Window owner) {
        // Crear diálogo modal
        JDialog dialog = new JDialog();
        dialog.setTitle("Seleccionar Fuente");
        dialog.setModal(true);
        if (owner != null) {
            dialog.setLocationRelativeTo(owner);
        }
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(owner);

        // Panel principal con borde
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de selección de tema
        JPanel themeSelectionPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        themeSelectionPanel.setBorder(BorderFactory.createTitledBorder("Temas predefinidos"));

        // Grupo de botones de radio para los temas
        ButtonGroup themeGroup = new ButtonGroup();
        Map<Theme, JRadioButton> themeButtons = new HashMap<>();

        for (Theme theme : Theme.values()) {
            JRadioButton radioButton = new JRadioButton(theme.getDisplayName());
            radioButton.setToolTipText(theme.getDescription());
            radioButton.setSelected(currentTheme == theme);

            radioButton.addActionListener(e -> {
                applyTheme(theme);
                JPanel customPanel = new JPanel(new BorderLayout());
                updateCustomPanel(customPanel, theme == Theme.CUSTOM);
            });

            themeGroup.add(radioButton);
            themeSelectionPanel.add(radioButton);
            themeButtons.put(theme, radioButton);
        }

        // Panel para configuración de tema personalizado
        JPanel customPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        customPanel.setBorder(BorderFactory.createTitledBorder("Personalización"));

        // Color de acento
        JButton accentColorButton = new JButton("Color de acento");
        JPanel accentColorPreview = new JPanel();
        accentColorPreview.setBackground(customAccentColor);
        accentColorPreview.setPreferredSize(new Dimension(50, 25));
        accentColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        accentColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    dialog,
                    "Seleccionar color de acento",
                    customAccentColor
            );

            if (newColor != null) {
                customAccentColor = newColor;
                accentColorPreview.setBackground(newColor);

                // Activar tema personalizado si no está seleccionado
                if (currentTheme != Theme.CUSTOM) {
                    themeButtons.get(Theme.CUSTOM).setSelected(true);
                    applyTheme(Theme.CUSTOM);
                    updateCustomPanel(customPanel, true);
                } else {
                    setCustomAccentColor(newColor);
                }
            }
        });

        // Color de fondo
        JButton backgroundColorButton = new JButton("Color de fondo");
        JPanel backgroundColorPreview = new JPanel();
        backgroundColorPreview.setBackground(customBackgroundColor);
        backgroundColorPreview.setPreferredSize(new Dimension(50, 25));
        backgroundColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        backgroundColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    dialog,
                    "Seleccionar color de fondo",
                    customBackgroundColor
            );

            if (newColor != null) {
                customBackgroundColor = newColor;
                backgroundColorPreview.setBackground(newColor);

                // Activar tema personalizado si no está seleccionado
                if (currentTheme != Theme.CUSTOM) {
                    themeButtons.get(Theme.CUSTOM).setSelected(true);
                    applyTheme(Theme.CUSTOM);
                    updateCustomPanel(customPanel, true);
                } else {
                    setCustomBackgroundColor(newColor);
                }
            }
        });

        // Fuente personalizada
        JButton fontButton = new JButton("Fuente");
        JLabel fontPreview = new JLabel(customFont.getFamily() + ", " + customFont.getSize() + "pt");
        fontPreview.setFont(customFont);

        fontButton.addActionListener(e -> {
            // Implementar selector de fuente personalizado
            Font newFont = showFontChooserDialog(dialog, customFont);
            if (newFont != null) {
                customFont = newFont;
                fontPreview.setText(newFont.getFamily() + ", " + newFont.getSize() + "pt");
                fontPreview.setFont(newFont);

                // Activar tema personalizado si no está seleccionado
                if (currentTheme != Theme.CUSTOM) {
                    themeButtons.get(Theme.CUSTOM).setSelected(true);
                    applyTheme(Theme.CUSTOM);
                    updateCustomPanel(customPanel, true);
                } else {
                    setCustomFont(newFont);
                }
            }
        });

        // Agregar componentes al panel personalizado
        customPanel.add(accentColorButton);
        customPanel.add(accentColorPreview);
        customPanel.add(backgroundColorButton);
        customPanel.add(backgroundColorPreview);
        customPanel.add(fontButton);
        customPanel.add(fontPreview);

        // Estado inicial del panel personalizado
        updateCustomPanel(customPanel, currentTheme == Theme.CUSTOM);

        // Panel de vista previa
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Vista previa"));

        // Componentes de ejemplo para la vista previa
        JPanel examplePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        JLabel exampleLabel = new JLabel("Texto de ejemplo");
        JTextField exampleTextField = new JTextField("Campo de texto");
        JComboBox<String> exampleComboBox = new JComboBox<>(new String[]{"Elemento 1", "Elemento 2"});
        JButton exampleButton = new JButton("Botón de ejemplo");
        JCheckBox exampleCheckBox = new JCheckBox("Casilla de verificación");

        examplePanel.add(exampleLabel);
        examplePanel.add(exampleTextField);
        examplePanel.add(exampleComboBox);
        examplePanel.add(exampleButton);
        examplePanel.add(exampleCheckBox);

        previewPanel.add(examplePanel, BorderLayout.CENTER);

        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Aplicar");
        JButton cancelButton = new JButton("Cancelar");

        applyButton.addActionListener(e -> {
            saveThemeToConfig();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            // Restaurar tema previo
            loadThemeFromConfig();
            dialog.dispose();
        });

        // Atajos de teclado
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        dialog.getRootPane().registerKeyboardAction(
                e -> applyButton.doClick(),
                enterKey,
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.getRootPane().registerKeyboardAction(
                e -> cancelButton.doClick(),
                escapeKey,
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        // Panel izquierdo para selección de tema y personalización
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(themeSelectionPanel, BorderLayout.NORTH);
        leftPanel.add(customPanel, BorderLayout.CENTER);

        // Agregar paneles al panel principal
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, previewPanel);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Agregar el panel principal al diálogo
        dialog.add(mainPanel);

        // Mostrar diálogo
        dialog.setVisible(true);
    }

    /**
     * Actualiza el estado del panel de personalización
     */
    private void updateCustomPanel(JPanel customPanel, boolean enabled) {
        for (Component component : customPanel.getComponents()) {
            component.setEnabled(enabled);
        }
    }

    /**
     * Muestra un diálogo para seleccionar una fuente
     */
    private Font showFontChooserDialog(Window owner, Font currentFont) {
        // Crear diálogo modal
        JDialog dialog = new JDialog();
        dialog.setTitle("Título del diálogo");
        dialog.setModal(true);
        if (owner != null) {
            dialog.setLocationRelativeTo(owner);
        }
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(owner);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Obtener fuentes disponibles
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();

        // Lista de familias de fuentes
        JList<String> fontList = new JList<>(fontFamilies);
        fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Estilos de fuente
        String[] fontStyles = {"Normal", "Negrita", "Cursiva", "Negrita y cursiva"};
        JComboBox<String> styleComboBox = new JComboBox<>(fontStyles);

        // Tamaños de fuente
        Integer[] fontSizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72};
        JComboBox<Integer> sizeComboBox = new JComboBox<>(fontSizes);

        // Panel de vista previa
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Vista previa"));

        JTextArea previewText = new JTextArea("AaBbCcDdEeFfGgHhIiJj123456!@#$%");
        previewText.setEditable(false);
        previewText.setLineWrap(true);
        previewText.setWrapStyleWord(true);
        previewText.setFont(currentFont);

        // Configurar valores iniciales
        fontList.setSelectedValue(currentFont.getFamily(), true);
        styleComboBox.setSelectedIndex(currentFont.getStyle());
        sizeComboBox.setSelectedItem(currentFont.getSize());

        // Manejar eventos de cambio
        fontList.addListSelectionListener(e -> updatePreviewFont(
                fontList, styleComboBox, sizeComboBox, previewText
        ));

        styleComboBox.addActionListener(e -> updatePreviewFont(
                fontList, styleComboBox, sizeComboBox, previewText
        ));

        sizeComboBox.addActionListener(e -> updatePreviewFont(
                fontList, styleComboBox, sizeComboBox, previewText
        ));

        // Panel de selección de fuente
        JPanel selectionPanel = new JPanel(new BorderLayout(10, 10));

        // Panel para la lista de fuentes
        JPanel fontListPanel = new JPanel(new BorderLayout());
        fontListPanel.setBorder(BorderFactory.createTitledBorder("Familia de fuente"));
        fontListPanel.add(new JScrollPane(fontList), BorderLayout.CENTER);

        // Panel para estilo y tamaño
        JPanel stylePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        stylePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        stylePanel.add(new JLabel("Estilo:"));
        stylePanel.add(styleComboBox);
        stylePanel.add(new JLabel("Tamaño:"));
        stylePanel.add(sizeComboBox);

        // Agregar componentes
        selectionPanel.add(fontListPanel, BorderLayout.CENTER);
        selectionPanel.add(stylePanel, BorderLayout.SOUTH);

        previewPanel.add(new JScrollPane(previewText), BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancelar");

        // Variable para almacenar el resultado
        final Font[] result = new Font[1];

        okButton.addActionListener(e -> {
            updatePreviewFont(fontList, styleComboBox, sizeComboBox, previewText);
            result[0] = previewText.getFont();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Agregar paneles al panel principal
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                selectionPanel,
                previewPanel
        );
        splitPane.setResizeWeight(0.7);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Agregar a la ventana
        dialog.add(mainPanel);

        // Mostrar diálogo
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Actualiza la fuente de vista previa
     */
    private void updatePreviewFont(JList<String> fontList, JComboBox<String> styleComboBox,
                                   JComboBox<Integer> sizeComboBox, JTextArea previewText) {
        // Obtener valores seleccionados
        String family = fontList.getSelectedValue();

        int style;
        switch (styleComboBox.getSelectedIndex()) {
            case 1:
                style = Font.BOLD;
                break;
            case 2:
                style = Font.ITALIC;
                break;
            case 3:
                style = Font.BOLD | Font.ITALIC;
                break;
            default:
                style = Font.PLAIN;
        }

        int size = (Integer) sizeComboBox.getSelectedItem();

        // Actualizar fuente
        Font newFont = new Font(family, style, size);
        previewText.setFont(newFont);
    }
}