package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel para configurar y gestionar los ajustes de temas de la aplicación
 */
public class ThemeSettingsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ThemeSettingsPanel.class);

    // Componentes principales
    private final JTabbedPane tabbedPane;
    private final ThemeManager themeManager;

    // Selector de tema
    private JComboBox<ThemeManager.Theme> themeSelector;
    private JPanel customizationPanel;
    private JButton applyButton;
    private JButton resetButton;

    // Controles de tema personalizado
    private JPanel accentColorPreview;
    private JPanel backgroundColorPreview;
    private JPanel textColorPreview;
    private JLabel fontPreviewLabel;
    private JCheckBox darkModeCheckBox;
    private JCheckBox highContrastCheckBox;
    private JSpinner fontSizeSpinner;

    // Colores y fuente personalizados
    private Color accentColor;
    private Color backgroundColor;
    private Color textColor;
    private Font customFont;

    // Callback cuando se aplican cambios
    private Runnable onThemeChangeCallback;

    /**
     * Constructor principal
     */
    public ThemeSettingsPanel() {
        themeManager = ThemeManager.getInstance();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Inicializar con colores actuales
        accentColor = themeManager.getAccentColor();
        backgroundColor = themeManager.getBackgroundColor();
        textColor = themeManager.getTextColor();
        customFont = themeManager.getFont();

        // Crear panel con pestañas
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        // Inicializar componentes
        initThemePanel();
        initCustomizationPanel();
        initPreviewPanel();

        // Añadir pestaña de configuración avanzada
        JPanel advancedPanel = createAdvancedPanel();
        tabbedPane.addTab("Avanzado", null, advancedPanel, "Configuración avanzada del tema");

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        applyButton = new JButton("Aplicar Cambios");
        applyButton.addActionListener(e -> applyThemeChanges());

        resetButton = new JButton("Restablecer");
        resetButton.addActionListener(e -> resetThemeChanges());

        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);

        // Añadir componentes al panel principal
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Inicializa el panel de selección de tema
     */
    private void initThemePanel() {
        JPanel themePanel = new JPanel(new BorderLayout(10, 10));
        themePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel superior con selector de tema
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        selectorPanel.setBorder(new TitledBorder("Tema Actual"));

        selectorPanel.add(new JLabel("Seleccionar tema:"));

        themeSelector = new JComboBox<>(ThemeManager.Theme.values());
        themeSelector.setSelectedItem(themeManager.getCurrentTheme());

        themeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof ThemeManager.Theme) {
                    ThemeManager.Theme theme = (ThemeManager.Theme) value;
                    setText(theme.getDisplayName());
                    setToolTipText(theme.getDescription());
                }

                return this;
            }
        });

        themeSelector.addActionListener(e -> {
            ThemeManager.Theme selectedTheme = (ThemeManager.Theme) themeSelector.getSelectedItem();
            updateCustomizationPanelForTheme(selectedTheme);
        });

        selectorPanel.add(themeSelector);

        // Añadir al panel de tema
        themePanel.add(selectorPanel, BorderLayout.NORTH);

        // Panel de descripciones de tema
        JPanel descriptionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        descriptionsPanel.setBorder(new TitledBorder("Temas Disponibles"));

        for (ThemeManager.Theme theme : ThemeManager.Theme.values()) {
            JPanel themeDescPanel = new JPanel(new BorderLayout());
            JLabel nameLabel = new JLabel(theme.getDisplayName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

            JLabel descLabel = new JLabel(theme.getDescription());

            themeDescPanel.add(nameLabel, BorderLayout.WEST);
            themeDescPanel.add(descLabel, BorderLayout.CENTER);

            descriptionsPanel.add(themeDescPanel);
        }

        JScrollPane descScrollPane = new JScrollPane(descriptionsPanel);
        descScrollPane.setBorder(null);

        themePanel.add(descScrollPane, BorderLayout.CENTER);

        // Añadir pestaña de tema
        tabbedPane.addTab("Temas", null, themePanel, "Selección de temas predefinidos");
    }

    /**
     * Inicializa el panel de personalización
     */
    private void initCustomizationPanel() {
        customizationPanel = new JPanel(new BorderLayout(10, 10));
        customizationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Grupo de colores
        JPanel colorsPanel = new JPanel(new GridBagLayout());
        colorsPanel.setBorder(new TitledBorder("Colores"));

        // Color de acento
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        colorsPanel.add(new JLabel("Color de acento:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        accentColorPreview = new JPanel();
        accentColorPreview.setBackground(accentColor);
        accentColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        accentColorPreview.setPreferredSize(new Dimension(40, 20));
        colorsPanel.add(accentColorPreview, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton accentColorButton = new JButton("Cambiar...");
        accentColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this, "Seleccionar color de acento", accentColor);
            if (newColor != null) {
                accentColor = newColor;
                accentColorPreview.setBackground(newColor);
                updatePreviewPanel();
            }
        });
        colorsPanel.add(accentColorButton, gbc);

        // Color de fondo
        gbc.gridx = 0;
        gbc.gridy = 1;
        colorsPanel.add(new JLabel("Color de fondo:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        backgroundColorPreview = new JPanel();
        backgroundColorPreview.setBackground(backgroundColor);
        backgroundColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        backgroundColorPreview.setPreferredSize(new Dimension(40, 20));
        colorsPanel.add(backgroundColorPreview, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton backgroundColorButton = new JButton("Cambiar...");
        backgroundColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this, "Seleccionar color de fondo", backgroundColor);
            if (newColor != null) {
                backgroundColor = newColor;
                backgroundColorPreview.setBackground(newColor);

                // Calcular automáticamente un color de texto adecuado
                double brightness = (newColor.getRed() * 0.299 +
                        newColor.getGreen() * 0.587 +
                        newColor.getBlue() * 0.114) / 255;

                if (brightness > 0.5) {
                    textColor = Color.BLACK;
                } else {
                    textColor = Color.WHITE;
                }

                textColorPreview.setBackground(textColor);
                updatePreviewPanel();
            }
        });
        colorsPanel.add(backgroundColorButton, gbc);

        // Color de texto
        gbc.gridx = 0;
        gbc.gridy = 2;
        colorsPanel.add(new JLabel("Color de texto:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        textColorPreview = new JPanel();
        textColorPreview.setBackground(textColor);
        textColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textColorPreview.setPreferredSize(new Dimension(40, 20));
        colorsPanel.add(textColorPreview, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton textColorButton = new JButton("Cambiar...");
        textColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this, "Seleccionar color de texto", textColor);
            if (newColor != null) {
                textColor = newColor;
                textColorPreview.setBackground(newColor);
                updatePreviewPanel();
            }
        });
        colorsPanel.add(textColorButton, gbc);

        // Grupo de fuentes
        JPanel fontsPanel = new JPanel(new GridBagLayout());
        fontsPanel.setBorder(new TitledBorder("Fuentes"));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Fuente principal
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        fontsPanel.add(new JLabel("Fuente principal:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        fontPreviewLabel = new JLabel(customFont.getFamily() + ", " + customFont.getSize() + "pt");
        fontPreviewLabel.setFont(customFont);
        fontsPanel.add(fontPreviewLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton fontButton = new JButton("Cambiar...");
        fontButton.addActionListener(e -> {
            Font selectedFont = showFontChooserDialog(customFont);
            if (selectedFont != null) {
                customFont = selectedFont;
                fontPreviewLabel.setText(customFont.getFamily() + ", " + customFont.getSize() + "pt");
                fontPreviewLabel.setFont(customFont);
                updatePreviewPanel();
            }
        });
        fontsPanel.add(fontButton, gbc);

        // Tamaño de fuente
        gbc.gridx = 0;
        gbc.gridy = 1;
        fontsPanel.add(new JLabel("Tamaño de fuente:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        SpinnerNumberModel fontSizeModel = new SpinnerNumberModel(customFont.getSize(), 8, 24, 1);
        fontSizeSpinner = new JSpinner(fontSizeModel);
        fontSizeSpinner.addChangeListener(e -> {
            int size = (Integer) fontSizeSpinner.getValue();
            customFont = new Font(customFont.getFamily(), customFont.getStyle(), size);
            fontPreviewLabel.setText(customFont.getFamily() + ", " + size + "pt");
            fontPreviewLabel.setFont(customFont);
            updatePreviewPanel();
        });
        fontsPanel.add(fontSizeSpinner, gbc);

        // Panel de opciones
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(new TitledBorder("Opciones Adicionales"));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;

        // Modo oscuro
        gbc.gridx = 0;
        gbc.gridy = 0;
        darkModeCheckBox = new JCheckBox("Modo oscuro");
        darkModeCheckBox.addActionListener(e -> {
            if (darkModeCheckBox.isSelected()) {
                // Cambiar a colores oscuros
                backgroundColor = new Color(43, 43, 43);
                textColor = new Color(220, 220, 220);
                backgroundColorPreview.setBackground(backgroundColor);
                textColorPreview.setBackground(textColor);
                updatePreviewPanel();
            } else {
                // Volver a colores claros
                backgroundColor = Color.WHITE;
                textColor = Color.BLACK;
                backgroundColorPreview.setBackground(backgroundColor);
                textColorPreview.setBackground(textColor);
                updatePreviewPanel();
            }
        });
        optionsPanel.add(darkModeCheckBox, gbc);

        // Alto contraste
        gbc.gridy = 1;
        highContrastCheckBox = new JCheckBox("Alto contraste");
        highContrastCheckBox.addActionListener(e -> {
            if (highContrastCheckBox.isSelected()) {
                // Aplicar colores de alto contraste
                backgroundColor = Color.BLACK;
                textColor = Color.WHITE;
                accentColor = Color.YELLOW;
                backgroundColorPreview.setBackground(backgroundColor);
                textColorPreview.setBackground(textColor);
                accentColorPreview.setBackground(accentColor);
                updatePreviewPanel();
            } else {
                // Restaurar colores normales
                backgroundColor = Color.WHITE;
                textColor = Color.BLACK;
                accentColor = new Color(51, 102, 204);
                backgroundColorPreview.setBackground(backgroundColor);
                textColorPreview.setBackground(textColor);
                accentColorPreview.setBackground(accentColor);
                updatePreviewPanel();
            }
        });
        optionsPanel.add(highContrastCheckBox, gbc);

        // Organizar paneles en el panel de controles
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        controlsPanel.add(colorsPanel, gbc);

        gbc.gridy = 1;
        controlsPanel.add(fontsPanel, gbc);

        gbc.gridy = 2;
        controlsPanel.add(optionsPanel, gbc);

        // Agregar al panel principal
        customizationPanel.add(controlsPanel, BorderLayout.NORTH);

        // Añadir pestaña de personalización
        tabbedPane.addTab("Personalización", null, customizationPanel, "Personalizar apariencia");
    }

    /**
     * Inicializa el panel de vista previa
     */
    private void initPreviewPanel() {
        JPanel previewPanel = new JPanel(new BorderLayout(10, 10));
        previewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Título del panel
        JLabel titleLabel = new JLabel("Vista Previa del Tema");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        previewPanel.add(titleLabel, BorderLayout.NORTH);

        // Crear panel de vista previa con diferentes componentes
        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.setBorder(new TitledBorder("Componentes"));

        // Añadir varios componentes para demostrar el tema
        componentsPanel.add(createPreviewSection("Botones", createButtonsPanel()));
        componentsPanel.add(Box.createVerticalStrut(10));
        componentsPanel.add(createPreviewSection("Entrada de Texto", createTextInputPanel()));
        componentsPanel.add(Box.createVerticalStrut(10));
        componentsPanel.add(createPreviewSection("Listas y Tablas", createListsPanel()));

        JScrollPane scrollPane = new JScrollPane(componentsPanel);
        previewPanel.add(scrollPane, BorderLayout.CENTER);

        // Añadir a las pestañas
        tabbedPane.addTab("Vista Previa", null, previewPanel, "Vista previa del tema");

        // Actualizar el panel con los colores actuales
        updatePreviewPanel();
    }

    /**
     * Actualiza el panel de personalización según el tema seleccionado
     */
    private void updateCustomizationPanelForTheme(ThemeManager.Theme theme) {
        // Habilitar o deshabilitar controles según el tema
        boolean isCustomTheme = (theme == ThemeManager.Theme.CUSTOM);

        // Obtener colores del tema seleccionado
        switch (theme) {
            case DARK:
                accentColor = new Color(97, 175, 239);
                backgroundColor = new Color(43, 43, 43);
                textColor = new Color(220, 220, 220);
                darkModeCheckBox.setSelected(true);
                highContrastCheckBox.setSelected(false);
                break;

            case HIGH_CONTRAST:
                accentColor = Color.YELLOW;
                backgroundColor = Color.BLACK;
                textColor = Color.WHITE;
                darkModeCheckBox.setSelected(true);
                highContrastCheckBox.setSelected(true);
                break;

            case BLUE:
                accentColor = new Color(0, 102, 204);
                backgroundColor = new Color(235, 244, 255);
                textColor = new Color(16, 46, 95);
                darkModeCheckBox.setSelected(false);
                highContrastCheckBox.setSelected(false);
                break;

            case SEPIA:
                accentColor = new Color(161, 102, 47);
                backgroundColor = new Color(249, 241, 228);
                textColor = new Color(91, 70, 50);
                darkModeCheckBox.setSelected(false);
                highContrastCheckBox.setSelected(false);
                break;

            case LIGHT:
            default:
                accentColor = new Color(51, 102, 204);
                backgroundColor = Color.WHITE;
                textColor = Color.BLACK;
                darkModeCheckBox.setSelected(false);
                highContrastCheckBox.setSelected(false);
                break;
        }

        // Actualizar previsualizaciones
        accentColorPreview.setBackground(accentColor);
        backgroundColorPreview.setBackground(backgroundColor);
        textColorPreview.setBackground(textColor);

        // Actualizar panel de vista previa
        updatePreviewPanel();
    }

    /**
     * Crea un panel avanzado para configuraciones adicionales
     */
    private JPanel createAdvancedPanel() {
        JPanel advancedPanel = new JPanel(new BorderLayout(10, 10));
        advancedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel con opciones avanzadas
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Resaltado de sintaxis de código
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;

        JCheckBox syntaxHighlightingCheckBox = new JCheckBox("Resaltado de sintaxis en bloques de código");
        syntaxHighlightingCheckBox.setSelected(
                ConfigManager.getInstance().getThemeConfig("syntaxHighlighting", true));
        optionsPanel.add(syntaxHighlightingCheckBox, gbc);

        // Color de bloques de código
        gbc.gridy = 1;
        optionsPanel.add(new JLabel("Color de fondo para bloques de código:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel codeBackgroundColorPreview = new JPanel();
        codeBackgroundColorPreview.setBackground(themeManager.getCodeBackgroundColor());
        codeBackgroundColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        codeBackgroundColorPreview.setPreferredSize(new Dimension(40, 20));
        optionsPanel.add(codeBackgroundColorPreview, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton codeBackgroundColorButton = new JButton("Cambiar...");
        codeBackgroundColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this, "Seleccionar color de fondo para código", themeManager.getCodeBackgroundColor());
            if (newColor != null) {
                codeBackgroundColorPreview.setBackground(newColor);
                ConfigManager.getInstance().setThemeConfig("codeBlockColor",
                        String.format("#%02X%02X%02X", newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
            }
        });
        optionsPanel.add(codeBackgroundColorButton, gbc);

        // Fuente de código
        gbc.gridx = 0;
        gbc.gridy = 2;
        optionsPanel.add(new JLabel("Fuente para bloques de código:"), gbc);

        gbc.gridx = 1;
        Font codeFont = themeManager.getCodeFont();
        JLabel codeFontPreviewLabel = new JLabel(codeFont.getFamily() + ", " + codeFont.getSize() + "pt");
        codeFontPreviewLabel.setFont(codeFont);
        optionsPanel.add(codeFontPreviewLabel, gbc);

        gbc.gridx = 2;
        JButton codeFontButton = new JButton("Cambiar...");
        codeFontButton.addActionListener(e -> {
            Font selectedFont = showFontChooserDialog(codeFont);
            if (selectedFont != null) {
                codeFontPreviewLabel.setText(selectedFont.getFamily() + ", " + selectedFont.getSize() + "pt");
                codeFontPreviewLabel.setFont(selectedFont);

                ConfigManager configManager = ConfigManager.getInstance();
                configManager.setThemeConfig("codeFontFamily", selectedFont.getFamily());
                configManager.setThemeConfig("codeFontSize", selectedFont.getSize());
            }
        });
        optionsPanel.add(codeFontButton, gbc);

        // Botón para guardar configuración avanzada
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton saveAdvancedButton = new JButton("Guardar Configuración Avanzada");
        saveAdvancedButton.addActionListener(e -> {
            // Guardar configuración avanzada
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.setThemeConfig("syntaxHighlighting", syntaxHighlightingCheckBox.isSelected());

            // Mostrar mensaje de confirmación
            JOptionPane.showMessageDialog(
                    this,
                    "Configuración avanzada guardada correctamente.\nAlgunos cambios pueden requerir reiniciar la aplicación.",
                    "Configuración Guardada",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        optionsPanel.add(saveAdvancedButton, gbc);

        // Añadir al panel principal
        advancedPanel.add(optionsPanel, BorderLayout.NORTH);

        return advancedPanel;
    }

    /**
     * Crea una sección de vista previa
     */
    private JPanel createPreviewSection(String title, JPanel content) {
        JPanel sectionPanel = new JPanel(new BorderLayout(5, 5));
        sectionPanel.setBorder(new TitledBorder(title));
        sectionPanel.add(content, BorderLayout.CENTER);
        return sectionPanel;
    }

    /**
     * Crea un panel de vista previa de botones
     */
    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        panel.add(new JButton("Botón Normal"));

        JButton accentButton = new JButton("Botón de Acento");
        accentButton.setForeground(accentColor);
        panel.add(accentButton);

        panel.add(new JToggleButton("Botón de Alternancia", true));

        return panel;
    }

    /**
     * Crea un panel de vista previa de entrada de texto
     */
    private JPanel createTextInputPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        panel.add(new JTextField("Campo de texto"));

        JTextArea textArea = new JTextArea("Área de texto\nCon múltiples líneas");
        textArea.setRows(3);
        panel.add(new JScrollPane(textArea));

        JComboBox<String> comboBox = new JComboBox<>(
                new String[]{"Opción 1", "Opción 2", "Opción 3"});
        panel.add(comboBox);

        return panel;
    }

    /**
     * Crea un panel de vista previa de listas y tablas
     */
    private JPanel createListsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 5));

        // Lista
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Elemento 1");
        listModel.addElement("Elemento 2");
        listModel.addElement("Elemento 3");
        JList<String> list = new JList<>(listModel);
        panel.add(new JScrollPane(list));

        // Tabla
        String[] columnNames = {"Columna 1", "Columna 2"};
        Object[][] data = {
                {"Valor 1,1", "Valor 1,2"},
                {"Valor 2,1", "Valor 2,2"}
        };
        JTable table = new JTable(data, columnNames);
        panel.add(new JScrollPane(table));

        return panel;
    }

    /**
     * Actualiza el panel de vista previa con los colores y fuentes actuales
     */
    private void updatePreviewPanel() {
        // Esta función se llamaría cuando cambien los colores o fuentes
        // y actualizaría las vistas previas para mostrar los cambios
        SwingUtilities.invokeLater(() -> {
            // Actualizar colores de los componentes de vista previa
            updatePreviewPanelComponents(tabbedPane.getComponentAt(2));
        });
    }

    /**
     * Actualiza recursivamente los componentes con los colores actuales
     */
    private void updatePreviewPanelComponents(Component component) {
        if (component instanceof JPanel) {
            component.setBackground(backgroundColor);
            component.setForeground(textColor);
        } else if (component instanceof JButton) {
            component.setForeground(textColor);
        } else if (component instanceof JLabel) {
            component.setForeground(textColor);
            component.setFont(customFont);
        } else if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(backgroundColor);
            component.setForeground(textColor);
            component.setFont(customFont);
        } else if (component instanceof JList || component instanceof JTable) {
            component.setBackground(backgroundColor);
            component.setForeground(textColor);
            component.setFont(customFont);
        }

        // Procesar componentes hijos
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                updatePreviewPanelComponents(child);
            }
        }
    }

    /**
     * Aplica los cambios de tema
     */
    private void applyThemeChanges() {
        ThemeManager.Theme selectedTheme = (ThemeManager.Theme) themeSelector.getSelectedItem();

        if (selectedTheme == ThemeManager.Theme.CUSTOM) {
            // Para tema personalizado, guardar los valores personalizados
            ConfigManager configManager = ConfigManager.getInstance();

            configManager.setThemeConfig("customAccentColor", String.format("#%02X%02X%02X",
                    accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue()));

            configManager.setThemeConfig("customBackgroundColor", String.format("#%02X%02X%02X",
                    backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue()));

            configManager.setThemeConfig("customFontFamily", customFont.getFamily());
            configManager.setThemeConfig("customFontSize", customFont.getSize());

            configManager.setThemeConfig("enableDarkMode", darkModeCheckBox.isSelected());
            configManager.setThemeConfig("enableHighContrast", highContrastCheckBox.isSelected());

            // Aplicar tema personalizado
            themeManager.setCustomAccentColor(accentColor);
            themeManager.setCustomBackgroundColor(backgroundColor);
            themeManager.setCustomFont(customFont);
        }

        // Aplicar el tema seleccionado
        themeManager.applyTheme(selectedTheme);

        // Notificar cambio si hay callback
        if (onThemeChangeCallback != null) {
            onThemeChangeCallback.run();
        }

        JOptionPane.showMessageDialog(
                this,
                "Tema aplicado correctamente.",
                "Tema Actualizado",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Restablece los cambios de tema
     */
    private void resetThemeChanges() {
        // Restablecer a la configuración guardada
        themeManager.loadThemeFromConfig();

        // Actualizar controles
        themeSelector.setSelectedItem(themeManager.getCurrentTheme());
        updateCustomizationPanelForTheme(themeManager.getCurrentTheme());

        JOptionPane.showMessageDialog(
                this,
                "Configuración de tema restablecida.",
                "Tema Restablecido",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Muestra un diálogo para seleccionar una fuente
     */
    private Font showFontChooserDialog(Font currentFont) {
        // Crear un selector de fuente sencillo
        JFontChooser fontChooser = new JFontChooser(currentFont);
        int result = fontChooser.showDialog(this);

        if (result == JFontChooser.OK_OPTION) {
            return fontChooser.getSelectedFont();
        }

        return null;
    }

    /**
     * Establece la callback para notificar cambios de tema
     */
    public void setOnThemeChangeCallback(Runnable callback) {
        this.onThemeChangeCallback = callback;
    }

    /**
     * Clase interna para seleccionar fuentes
     */
    private static class JFontChooser extends JDialog {
        public static final int OK_OPTION = 0;
        public static final int CANCEL_OPTION = 1;

        private Font selectedFont;
        private int result = CANCEL_OPTION;

        private JList<String> fontList;
        private JComboBox<String> styleComboBox;
        private JSpinner sizeSpinner;
        private JTextArea previewArea;

        public JFontChooser(Font initialFont) {
            super((Frame) null, "Seleccionar Fuente", true);
            setSize(400, 400);
            setLocationRelativeTo(null);

            this.selectedFont = initialFont;

            initComponents();
        }

        private void initComponents() {
            // Panel principal
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Lista de fuentes
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fontNames = ge.getAvailableFontFamilyNames();

            fontList = new JList<>(fontNames);
            fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fontList.setSelectedValue(selectedFont.getFamily(), true);

            // Estilos de fuente
            String[] styles = {"Normal", "Negrita", "Cursiva", "Negrita y Cursiva"};
            styleComboBox = new JComboBox<>(styles);
            styleComboBox.setSelectedIndex(selectedFont.getStyle());

            // Tamaño de fuente
            SpinnerNumberModel sizeModel = new SpinnerNumberModel(
                    selectedFont.getSize(), 6, 72, 1);
            sizeSpinner = new JSpinner(sizeModel);

            // Panel de configuración
            JPanel configPanel = new JPanel(new BorderLayout(10, 10));

            JPanel fontPanel = new JPanel(new BorderLayout());
            fontPanel.setBorder(new TitledBorder("Familia"));
            fontPanel.add(new JScrollPane(fontList), BorderLayout.CENTER);

            JPanel stylePanel = new JPanel(new GridLayout(2, 2, 5, 5));
            stylePanel.setBorder(new TitledBorder("Estilo y Tamaño"));

            stylePanel.add(new JLabel("Estilo:"));
            stylePanel.add(styleComboBox);
            stylePanel.add(new JLabel("Tamaño:"));
            stylePanel.add(sizeSpinner);

            configPanel.add(fontPanel, BorderLayout.CENTER);
            configPanel.add(stylePanel, BorderLayout.SOUTH);

            // Panel de vista previa
            JPanel previewPanel = new JPanel(new BorderLayout());
            previewPanel.setBorder(new TitledBorder("Vista Previa"));

            previewArea = new JTextArea(
                    "AaBbCcDdEeFfGgHhIiJj\n1234567890\n!@#$%^&*()");
            previewArea.setEditable(false);
            previewArea.setFont(selectedFont);

            previewPanel.add(new JScrollPane(previewArea), BorderLayout.CENTER);

            // Panel de botones
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton okButton = new JButton("Aceptar");
            JButton cancelButton = new JButton("Cancelar");

            okButton.addActionListener(e -> {
                updateSelectedFont();
                result = OK_OPTION;
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            // Añadir paneles al panel principal
            mainPanel.add(configPanel, BorderLayout.CENTER);
            mainPanel.add(previewPanel, BorderLayout.SOUTH);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Añadir event listeners
            fontList.addListSelectionListener(e -> updatePreview());
            styleComboBox.addActionListener(e -> updatePreview());
            sizeSpinner.addChangeListener(e -> updatePreview());

            setContentPane(mainPanel);
        }

        private void updatePreview() {
            String fontFamily = fontList.getSelectedValue();
            int fontStyle;

            switch (styleComboBox.getSelectedIndex()) {
                case 1:
                    fontStyle = Font.BOLD;
                    break;
                case 2:
                    fontStyle = Font.ITALIC;
                    break;
                case 3:
                    fontStyle = Font.BOLD | Font.ITALIC;
                    break;
                default:
                    fontStyle = Font.PLAIN;
            }

            int fontSize = (Integer) sizeSpinner.getValue();

            Font font = new Font(fontFamily, fontStyle, fontSize);
            previewArea.setFont(font);
        }

        private void updateSelectedFont() {
            String fontFamily = fontList.getSelectedValue();
            int fontStyle;

            switch (styleComboBox.getSelectedIndex()) {
                case 1:
                    fontStyle = Font.BOLD;
                    break;
                case 2:
                    fontStyle = Font.ITALIC;
                    break;
                case 3:
                    fontStyle = Font.BOLD | Font.ITALIC;
                    break;
                default:
                    fontStyle = Font.PLAIN;
            }

            int fontSize = (Integer) sizeSpinner.getValue();

            selectedFont = new Font(fontFamily, fontStyle, fontSize);
        }

        public int showDialog(Component parent) {
            setLocationRelativeTo(parent);
            setVisible(true);
            return result;
        }

        public Font getSelectedFont() {
            return selectedFont;
        }
    }
}