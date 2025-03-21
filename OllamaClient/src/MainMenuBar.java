package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

/**
 * Barra de menú principal para la aplicación
 * Proporciona acceso a todas las funcionalidades a través de menús desplegables
 */
public class MainMenuBar extends JMenuBar {
    private static final Logger logger = LoggerFactory.getLogger(MainMenuBar.class);

    // Componentes del menú
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenu searchMenu;
    private JMenu toolsMenu;
    private JMenu helpMenu;

    // Referencias a menús dinámicos
    private JMenu recentFilesMenu;

    // Ventana principal
    private final Window ownerWindow;

    // Listeners para acciones
    private ActionListener onFileOpenListener;
    private ActionListener onFileSaveListener;
    private ActionListener onExitListener;
    private ActionListener onSearchConfigListener;
    private ActionListener onThemeChangeListener;
    private ActionListener onAboutListener;
    private ActionListener onErrorLogListener;

    /**
     * Constructor principal
     */
    public MainMenuBar(Window ownerWindow) {
        this.ownerWindow = ownerWindow;

        initMenus();
        populateMenus();
    }

    /**
     * Inicializa los menús principales
     */
    private void initMenus() {
        // Menú Archivo
        fileMenu = new JMenu("Archivo");
        fileMenu.setMnemonic(KeyEvent.VK_A);

        // Menú Edición
        editMenu = new JMenu("Edición");
        editMenu.setMnemonic(KeyEvent.VK_E);

        // Menú Ver
        viewMenu = new JMenu("Ver");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        // Menú Búsqueda
        searchMenu = new JMenu("Búsqueda");
        searchMenu.setMnemonic(KeyEvent.VK_B);

        // Menú Herramientas
        toolsMenu = new JMenu("Herramientas");
        toolsMenu.setMnemonic(KeyEvent.VK_H);

        // Menú Ayuda
        helpMenu = new JMenu("Ayuda");
        helpMenu.setMnemonic(KeyEvent.VK_Y);

        // Añadir menús a la barra
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(searchMenu);
        add(toolsMenu);
        add(helpMenu);
    }

    /**
     * Rellena los menús con sus ítems
     */
    private void populateMenus() {
        // Menú Archivo
        JMenuItem openItem = new JMenuItem("Abrir archivo...", KeyEvent.VK_A);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        openItem.addActionListener(e -> {
            if (onFileOpenListener != null) {
                onFileOpenListener.actionPerformed(e);
            }
        });

        JMenuItem saveItem = new JMenuItem("Guardar respuesta...", KeyEvent.VK_G);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveItem.addActionListener(e -> {
            if (onFileSaveListener != null) {
                onFileSaveListener.actionPerformed(e);
            }
        });

        // Menú de archivos recientes
        recentFilesMenu = new JMenu("Archivos recientes");
        refreshRecentFilesMenu();

        JMenuItem exportConfigItem = new JMenuItem("Exportar configuración...");
        exportConfigItem.addActionListener(e -> showConfigImportExportDialog(true));

        JMenuItem importConfigItem = new JMenuItem("Importar configuración...");
        importConfigItem.addActionListener(e -> showConfigImportExportDialog(false));

        JMenuItem exitItem = new JMenuItem("Salir", KeyEvent.VK_S);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        exitItem.addActionListener(e -> {
            if (onExitListener != null) {
                onExitListener.actionPerformed(e);
            } else {
                System.exit(0);
            }
        });

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(recentFilesMenu);
        fileMenu.addSeparator();
        fileMenu.add(exportConfigItem);
        fileMenu.add(importConfigItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Menú Edición
        JMenuItem cutItem = new JMenuItem("Cortar", KeyEvent.VK_T);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        cutItem.addActionListener(e -> {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner instanceof JTextComponent) {
                ((JTextComponent) focusOwner).cut();
            }
        });

        JMenuItem copyItem = new JMenuItem("Copiar", KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        copyItem.addActionListener(e -> {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner instanceof JTextComponent) {
                ((JTextComponent) focusOwner).copy();
            }
        });

        JMenuItem pasteItem = new JMenuItem("Pegar", KeyEvent.VK_P);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        pasteItem.addActionListener(e -> {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner instanceof JTextComponent) {
                ((JTextComponent) focusOwner).paste();
            }
        });

        JMenuItem selectAllItem = new JMenuItem("Seleccionar todo", KeyEvent.VK_S);
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        selectAllItem.addActionListener(e -> {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner instanceof JTextComponent) {
                ((JTextComponent) focusOwner).selectAll();
            }
        });

        JMenuItem clearPromptItem = new JMenuItem("Limpiar prompt", KeyEvent.VK_L);
        clearPromptItem.addActionListener(e -> {
            // Esta acción se manejará externamente
            ActionEvent newEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "clearPrompt");
            fireActionPerformed(newEvent);
        });

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);
        editMenu.addSeparator();
        editMenu.add(clearPromptItem);

        // Menú Ver
        JMenu displayModeMenu = new JMenu("Modo de visualización");

        ButtonGroup displayModeGroup = new ButtonGroup();
        JRadioButtonMenuItem listViewItem = new JRadioButtonMenuItem("Vista de lista");
        JRadioButtonMenuItem gridViewItem = new JRadioButtonMenuItem("Vista de cuadrícula");
        JRadioButtonMenuItem compactViewItem = new JRadioButtonMenuItem("Vista compacta");
        JRadioButtonMenuItem detailedViewItem = new JRadioButtonMenuItem("Vista detallada");

        displayModeGroup.add(listViewItem);
        displayModeGroup.add(gridViewItem);
        displayModeGroup.add(compactViewItem);
        displayModeGroup.add(detailedViewItem);

        listViewItem.setSelected(true);

        listViewItem.addActionListener(e -> setDisplayMode(SearchResultsPanel.DisplayMode.LIST));
        gridViewItem.addActionListener(e -> setDisplayMode(SearchResultsPanel.DisplayMode.GRID));
        compactViewItem.addActionListener(e -> setDisplayMode(SearchResultsPanel.DisplayMode.COMPACT));
        detailedViewItem.addActionListener(e -> setDisplayMode(SearchResultsPanel.DisplayMode.DETAILED));

        displayModeMenu.add(listViewItem);
        displayModeMenu.add(gridViewItem);
        displayModeMenu.add(compactViewItem);
        displayModeMenu.add(detailedViewItem);

        JMenuItem themeSettingsItem = new JMenuItem("Configuración de tema...");
        themeSettingsItem.addActionListener(e -> {
            if (onThemeChangeListener != null) {
                onThemeChangeListener.actionPerformed(e);
            } else {
                ThemeManager.getInstance().showThemeDialog(ownerWindow);
            }
        });

        viewMenu.add(displayModeMenu);
        viewMenu.addSeparator();
        viewMenu.add(themeSettingsItem);

        // Menú Búsqueda
        JMenuItem searchPreferencesItem = new JMenuItem("Preferencias de búsqueda...");
        searchPreferencesItem.addActionListener(e -> {
            if (onSearchConfigListener != null) {
                onSearchConfigListener.actionPerformed(e);
            } else {
                SearchPreferencesDialog.showDialog(ownerWindow);
            }
        });

        JMenu searchEngineMenu = new JMenu("Motor de búsqueda");

        ButtonGroup searchEngineGroup = new ButtonGroup();
        JRadioButtonMenuItem duckDuckGoItem = new JRadioButtonMenuItem("DuckDuckGo");
        JRadioButtonMenuItem serpApiItem = new JRadioButtonMenuItem("SerpAPI");
        JRadioButtonMenuItem googleApiItem = new JRadioButtonMenuItem("Google Custom Search");

        searchEngineGroup.add(duckDuckGoItem);
        searchEngineGroup.add(serpApiItem);
        searchEngineGroup.add(googleApiItem);

        // Seleccionar el motor actual
        String currentEngine = ConfigManager.getInstance().getSearchConfig("searchAPIProvider", "DUCKDUCKGO");
        if ("SERPAPI".equals(currentEngine)) {
            serpApiItem.setSelected(true);
        } else if ("CUSTOM_GOOGLE".equals(currentEngine)) {
            googleApiItem.setSelected(true);
        } else {
            duckDuckGoItem.setSelected(true);
        }

        duckDuckGoItem.addActionListener(e -> setSearchEngine(WebSearchService.SearchAPI.DUCKDUCKGO));
        serpApiItem.addActionListener(e -> setSearchEngine(WebSearchService.SearchAPI.SERPAPI));
        googleApiItem.addActionListener(e -> setSearchEngine(WebSearchService.SearchAPI.CUSTOM_GOOGLE));

        searchEngineMenu.add(duckDuckGoItem);
        searchEngineMenu.add(serpApiItem);
        searchEngineMenu.add(googleApiItem);

        JCheckBoxMenuItem webSearchEnabledItem = new JCheckBoxMenuItem("Habilitar búsqueda web");
        webSearchEnabledItem.setSelected(
                ConfigManager.getInstance().getSearchConfig("webSearchEnabled", false));
        webSearchEnabledItem.addActionListener(e -> {
            ConfigManager.getInstance().setSearchConfig(
                    "webSearchEnabled", webSearchEnabledItem.isSelected());

            // Notificar cambio
            ActionEvent newEvent = new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED, "webSearchEnabledChanged");
            fireActionPerformed(newEvent);
        });

        searchMenu.add(searchPreferencesItem);
        searchMenu.add(searchEngineMenu);
        searchMenu.addSeparator();
        searchMenu.add(webSearchEnabledItem);

        // Menú Herramientas
        JMenuItem errorLogItem = new JMenuItem("Ver registro de errores");
        errorLogItem.addActionListener(e -> {
            if (onErrorLogListener != null) {
                onErrorLogListener.actionPerformed(e);
            } else {
                ErrorManager.getInstance().showErrorLog();
            }
        });

        JMenuItem clearCacheItem = new JMenuItem("Limpiar caché");
        clearCacheItem.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                    ownerWindow,
                    "¿Está seguro de que desea limpiar la caché?\nEsto eliminará datos temporales, pero no afectará a la configuración.",
                    "Confirmar limpieza de caché",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                // Acción para limpiar la caché
                ActionEvent newEvent = new ActionEvent(
                        this, ActionEvent.ACTION_PERFORMED, "clearCache");
                fireActionPerformed(newEvent);
            }
        });

        JMenuItem resetSettingsItem = new JMenuItem("Restablecer configuración");
        resetSettingsItem.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                    ownerWindow,
                    "¿Está seguro de que desea restablecer toda la configuración a los valores predeterminados?\n" +
                            "Esta acción no se puede deshacer.",
                    "Confirmar restablecimiento",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                ConfigManager.getInstance().resetToDefaults();

                JOptionPane.showMessageDialog(
                        ownerWindow,
                        "La configuración ha sido restablecida a los valores predeterminados.\n" +
                                "Algunos cambios pueden requerir reiniciar la aplicación.",
                        "Configuración Restablecida",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Notificar cambio
                ActionEvent newEvent = new ActionEvent(
                        this, ActionEvent.ACTION_PERFORMED, "settingsReset");
                fireActionPerformed(newEvent);
            }
        });

        toolsMenu.add(errorLogItem);
        toolsMenu.addSeparator();
        toolsMenu.add(clearCacheItem);
        toolsMenu.add(resetSettingsItem);

        // Menú Ayuda
        JMenuItem helpContentsItem = new JMenuItem("Contenido de ayuda", KeyEvent.VK_C);
        helpContentsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpContentsItem.addActionListener(e -> showHelp());

        JMenuItem aboutItem = new JMenuItem("Acerca de", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> {
            if (onAboutListener != null) {
                onAboutListener.actionPerformed(e);
            } else {
                showAboutDialog();
            }
        });

        helpMenu.add(helpContentsItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
    }

    /**
     * Actualiza el menú de archivos recientes
     */
    public void refreshRecentFilesMenu() {
        recentFilesMenu.removeAll();

        List<String> recentFiles = ConfigManager.getInstance().getRecentFiles();

        if (recentFiles.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("(No hay archivos recientes)");
            emptyItem.setEnabled(false);
            recentFilesMenu.add(emptyItem);
        } else {
            for (String filePath : recentFiles) {
                File file = new File(filePath);
                JMenuItem fileItem = new JMenuItem(file.getName());
                fileItem.setToolTipText(filePath);

                fileItem.addActionListener(e -> {
                    // Acción para abrir archivo reciente
                    ActionEvent newEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "openRecentFile");
                    newEvent.setSource(filePath);
                    fireActionPerformed(newEvent);
                });

                recentFilesMenu.add(fileItem);
            }

            recentFilesMenu.addSeparator();

            JMenuItem clearRecentItem = new JMenuItem("Limpiar lista");
            clearRecentItem.addActionListener(e -> {
                ConfigManager configManager = ConfigManager.getInstance();
                configManager.setMainConfig("recentFiles", new java.util.ArrayList<String>());
                configManager.saveMainConfig();
                refreshRecentFilesMenu();
            });

            recentFilesMenu.add(clearRecentItem);
        }
    }

    /**
     * Establece el modo de visualización de resultados
     */
    private void setDisplayMode(SearchResultsPanel.DisplayMode mode) {
        ActionEvent newEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "setDisplayMode");
        newEvent.setSource(mode);
        fireActionPerformed(newEvent);
    }

    /**
     * Establece el motor de búsqueda
     */
    private void setSearchEngine(WebSearchService.SearchAPI engine) {
        ConfigManager.getInstance().setSearchConfig("searchAPIProvider", engine.name());

        // Notificar cambio
        ActionEvent newEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "searchEngineChanged");
        newEvent.setSource(engine);
        fireActionPerformed(newEvent);
    }

    /**
     * Muestra el diálogo de importación/exportación de configuración
     */
    private void showConfigImportExportDialog(boolean exportMode) {
        ConfigImportExportDialog dialog = new ConfigImportExportDialog(ownerWindow);

        // Seleccionar pestaña adecuada
        if (exportMode) {
            // Ya está en modo exportación por defecto
        } else {
            // Seleccionar modo importación
            ActionEvent importAction = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selectImport");
            dialog.dispatchEvent(importAction);
        }

        dialog.setVisible(true);

        // Si la operación fue exitosa, refrescar la interfaz
        if (dialog.isOperationSuccess()) {
            ActionEvent refreshEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "configChanged");
            fireActionPerformed(refreshEvent);
        }
    }

    /**
     * Muestra el diálogo de ayuda
     */
    private void showHelp() {
        // Por ahora, mostrar un diálogo simple
        JOptionPane.showMessageDialog(
                ownerWindow,
                "La ayuda completa está en desarrollo.\n\n" +
                        "Para una guía rápida:\n" +
                        "1. Seleccione un modelo de Ollama en el desplegable.\n" +
                        "2. Cargue archivos o imágenes según el tipo de modelo.\n" +
                        "3. Escriba su prompt y haga clic en 'Enviar consulta'.\n" +
                        "4. Use los menús para acceder a más opciones y preferencias.",
                "Ayuda",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Muestra el diálogo "Acerca de"
     */
    private void showAboutDialog() {
        JDialog aboutDialog = new JDialog(
                (ownerWindow instanceof Frame) ? (Frame) ownerWindow : null,
                "Acerca de Cliente GUI para Ollama",
                true
        );

        aboutDialog.setLayout(new BorderLayout(10, 10));
        aboutDialog.setSize(450, 300);
        aboutDialog.setLocationRelativeTo(ownerWindow);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Título
        JLabel titleLabel = new JLabel("Cliente GUI para Ollama");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        // Versión
        JLabel versionLabel = new JLabel("Versión 1.0.0");
        versionLabel.setHorizontalAlignment(JLabel.CENTER);

        // Panel superior con título y versión
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(versionLabel, BorderLayout.CENTER);

        // Información
        JTextArea infoArea = new JTextArea(
                "Una interfaz gráfica para interactuar con modelos de Ollama.\n\n" +
                        "Características:\n" +
                        "- Soporte para múltiples modelos de lenguaje\n" +
                        "- Carga de archivos en diversos formatos\n" +
                        "- Búsqueda web integrada\n" +
                        "- Temas personalizables\n" +
                        "- Opciones de configuración avanzadas\n\n" +
                        "© 2024 - Desarrollado para OllamaClient"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(contentPanel.getBackground());

        // Botón Cerrar
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> aboutDialog.dispose());

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);

        // Añadir componentes al panel principal
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        aboutDialog.add(contentPanel);
        aboutDialog.setVisible(true);
    }

    /**
     * Establece el listener para abrir archivos
     */
    public void setOnFileOpenListener(ActionListener listener) {
        this.onFileOpenListener = listener;
    }

    /**
     * Establece el listener para guardar respuestas
     */
    public void setOnFileSaveListener(ActionListener listener) {
        this.onFileSaveListener = listener;
    }

    /**
     * Establece el listener para salir de la aplicación
     */
    public void setOnExitListener(ActionListener listener) {
        this.onExitListener = listener;
    }

    /**
     * Establece el listener para configuración de búsqueda
     */
    public void setOnSearchConfigListener(ActionListener listener) {
        this.onSearchConfigListener = listener;
    }

    /**
     * Establece el listener para cambio de tema
     */
    public void setOnThemeChangeListener(ActionListener listener) {
        this.onThemeChangeListener = listener;
    }

    /**
     * Establece el listener para el diálogo Acerca de
     */
    public void setOnAboutListener(ActionListener listener) {
        this.onAboutListener = listener;
    }

    /**
     * Establece el listener para ver el registro de errores
     */
    public void setOnErrorLogListener(ActionListener listener) {
        this.onErrorLogListener = listener;
    }

    /**
     * Notifica a todos los listeners sobre un evento
     */
    protected void fireActionPerformed(ActionEvent e) {
        for (ActionListener listener : getListeners(ActionListener.class)) {
            listener.actionPerformed(e);
        }
    }

    /**
     * Agrega un listener para todos los eventos del menú
     */
    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    /**
     * Elimina un listener
     */
    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }
}