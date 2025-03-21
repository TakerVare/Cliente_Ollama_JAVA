package OllamaClient.src;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente Java mejorado con GUI para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos, imágenes, búsqueda web integrada
 * y una interfaz gráfica mejorada.
 */
public class ImprovedOllamaGUIClient extends JFrame {
    // URLs de la API
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_CHAT_API_URL = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODELS_URL = "http://localhost:11434/api/tags";

    // Tipos de archivos de imagen soportados
    private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp"};

    // Logger para registro de errores y eventos
    private static final Logger logger = LoggerFactory.getLogger(ImprovedOllamaGUIClient.class);

    // Servicio para tareas en segundo plano
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    // Componentes de la interfaz gráfica
    private JComboBox<String> modelComboBox;
    private JTextField temperatureField;
    private JTextField topPField;
    private JTextField maxTokensField;
    private JButton loadFileButton;
    private JButton loadImageButton;
    private JButton clearFileButton;
    private JLabel fileNameLabel;
    private JTextArea promptTextArea;
    private JPanel responsePanel;
    private JTextPane responseTextPane;
    private JLabel responseImageLabel;
    private JButton sendButton;
    private JButton saveResponseButton;
    private JButton viewFileButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JCheckBox multimodalCheckBox;
    private JCheckBox webSearchCheckBox;
    private JTextField webSearchQueryField;
    private JPanel imagePreviewPanel;
    private JLabel imagePreviewLabel;
    private FileExplorerPanel fileExplorerPanel;
    private JSplitPane mainSplitPane;
    private JSplitPane contentSplitPane;
    private SearchResultsPanel searchResultsPanel;
    private MainMenuBar mainMenuBar;
    private boolean darkModeEnabled = false;

    // Contenedores de estados
    private CardLayout viewCardLayout;
    private JPanel viewContainer;
    private JPanel searchViewPanel;
    private JPanel responseViewPanel;

    // Estado de la aplicación
    private String fileContent = "";
    private String loadedFilePath = "";
    private String currentResponse = "";
    private boolean isRequestInProgress = false;
    private BufferedImage loadedImage = null;
    private String imageBase64 = "";
    private BufferedImage responseImage = null;
    private List<FileExplorerPanel.FileInfo> selectedFiles = new ArrayList<>();

    // Gestores de subsistemas
    private final WebSearchService webSearchService;
    private final ConfigManager configManager;
    private final ErrorManager errorManager;
    private final ThemeManager themeManager;

    /**
     * Constructor principal
     */
    public ImprovedOllamaGUIClient() {
        super("Cliente Mejorado para Ollama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Inicializar gestor de errores y establecer ventana propietaria
        errorManager = ErrorManager.getInstance();
        errorManager.setOwnerWindow(this);

        // Inicializar gestor de configuración
        configManager = ConfigManager.getInstance();

        // Inicializar gestor de temas
        themeManager = ThemeManager.getInstance();

        // Inicializar servicio de búsqueda web
        webSearchService = new WebSearchService();

        // Obtener tamaño de ventana guardado o usar predeterminado
        int windowWidth = configManager.getMainConfig("windowWidth", 1100);
        int windowHeight = configManager.getMainConfig("windowHeight", 700);
        setSize(windowWidth, windowHeight);
        setMinimumSize(new Dimension(900, 500));

        // Cargar tema antes de inicializar componentes
        themeManager.loadThemeFromConfig();

        initComponents();
        layoutComponents();
        initEventHandlers();

        // Configurar barra de menú
        setupMainMenu();

        // Centrar la ventana
        setLocationRelativeTo(null);

        // Cargar modelos al iniciar
        loadModels();

        // Cargar configuraciones adicionales
        loadSettings();

        // Registrar handler para cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowSettings();
            }
        });
    }

    /**
     * Inicializa los componentes de la interfaz gráfica
     */
    private void initComponents() {
        // Modelos
        modelComboBox = new JComboBox<>();
        modelComboBox.setToolTipText("Selecciona un modelo de Ollama");

        // Parámetros
        temperatureField = new JTextField("0.9", 5);
        temperatureField.setToolTipText("Temperatura (0.0-1.0)");
        topPField = new JTextField("0.9", 5);
        topPField.setToolTipText("Top P (0.0-1.0)");
        maxTokensField = new JTextField("10000", 5);
        maxTokensField.setToolTipText("Número máximo de tokens");

        // Opciones de modelo
        multimodalCheckBox = new JCheckBox("Modo multimodal");
        multimodalCheckBox.setToolTipText("Habilitar para usar modelos que soporten imágenes como gemma:27b");

        // Búsqueda web
        webSearchCheckBox = new JCheckBox("Búsqueda web");
        webSearchCheckBox.setToolTipText("Habilitar para enriquecer las consultas con resultados de búsqueda web");

        webSearchQueryField = new JTextField("", 15);
        webSearchQueryField.setToolTipText("Consulta personalizada para búsqueda web (opcional)");
        webSearchQueryField.setEnabled(false);

        // Carga de archivos
        loadFileButton = new JButton("Cargar archivo");
        loadFileButton.setToolTipText("Cargar un archivo de texto, PDF o DOCX");
        loadImageButton = new JButton("Cargar imagen");
        loadImageButton.setToolTipText("Cargar una imagen (JPG, PNG, GIF)");
        clearFileButton = new JButton("Eliminar");
        clearFileButton.setToolTipText("Eliminar el archivo o imagen cargado");
        clearFileButton.setEnabled(false);
        fileNameLabel = new JLabel("Sin archivo cargado");
        viewFileButton = new JButton("Ver");
        viewFileButton.setToolTipText("Ver el contenido del archivo o imagen cargado");
        viewFileButton.setEnabled(false);

        // Panel de vista previa de imagen
        imagePreviewPanel = new JPanel(new BorderLayout());
        imagePreviewPanel.setBorder(BorderFactory.createTitledBorder("Vista previa de imagen"));
        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setHorizontalAlignment(JLabel.CENTER);
        imagePreviewPanel.add(new JScrollPane(imagePreviewLabel), BorderLayout.CENTER);
        imagePreviewPanel.setVisible(false);

        // Área de prompt
        promptTextArea = new JTextArea(5, 20);
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        promptTextArea.setToolTipText("Escribe tu prompt aquí");

        // Panel de respuesta con soporte para texto e imágenes
        responsePanel = new JPanel(new BorderLayout());

        // Área de respuesta para texto
        responseTextPane = new JTextPane();
        responseTextPane.setEditable(false);
        responseTextPane.setContentType("text/plain");

        // Área para mostrar imágenes en la respuesta
        responseImageLabel = new JLabel();
        responseImageLabel.setHorizontalAlignment(JLabel.CENTER);
        responseImageLabel.setVisible(false);

        // Botones de acción
        sendButton = new JButton("Enviar consulta");
        sendButton.setToolTipText("Enviar la consulta a Ollama");
        saveResponseButton = new JButton("Guardar respuesta");
        saveResponseButton.setToolTipText("Guardar la respuesta en un archivo");
        saveResponseButton.setEnabled(false);

        // Barra de progreso y estado
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        statusLabel = new JLabel("Listo");

        // Inicializar explorador de archivos
        fileExplorerPanel = new FileExplorerPanel();
        fileExplorerPanel.setPreferredSize(new Dimension(300, 500));
        fileExplorerPanel.setOnFilesSelectedForAnalysis(this::handleSelectedFiles);

        // Inicializar panel de resultados de búsqueda
        searchResultsPanel = new SearchResultsPanel();
        searchResultsPanel.setOnAddToPromptCallback(this::addSearchResultsToPrompt);

        // Vista contenedora con CardLayout para alternar entre paneles
        viewCardLayout = new CardLayout();
        viewContainer = new JPanel(viewCardLayout);

        // Paneles de vista
        searchViewPanel = new JPanel(new BorderLayout());
        searchViewPanel.add(searchResultsPanel, BorderLayout.CENTER);

        responseViewPanel = new JPanel(new BorderLayout());
    }

    /**
     * Establece la disposición de los componentes en la interfaz
     */
    private void layoutComponents() {
        // Panel principal con bordes
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Panel superior: selección de modelo y parámetros
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Selección de modelo
        JPanel modelPanel = new JPanel(new BorderLayout(5, 0));
        modelPanel.add(new JLabel("Modelo:"), BorderLayout.WEST);
        modelPanel.add(modelComboBox, BorderLayout.CENTER);

        JPanel checkboxesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        checkboxesPanel.add(multimodalCheckBox);
        checkboxesPanel.add(webSearchCheckBox);
        modelPanel.add(checkboxesPanel, BorderLayout.EAST);

        topPanel.add(modelPanel, BorderLayout.NORTH);

        // Parámetros
        JPanel paramsPanel = new JPanel(new GridLayout(2, 1, 0, 5));

        // Primera fila: parámetros de modelo
        JPanel modelParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        modelParamsPanel.add(new JLabel("Temperatura:"));
        modelParamsPanel.add(temperatureField);
        modelParamsPanel.add(new JLabel("Top P:"));
        modelParamsPanel.add(topPField);
        modelParamsPanel.add(new JLabel("Max tokens:"));
        modelParamsPanel.add(maxTokensField);
        paramsPanel.add(modelParamsPanel);

        // Segunda fila: parámetros de búsqueda web
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.add(new JLabel("Búsqueda:"));
        searchPanel.add(webSearchQueryField);
        paramsPanel.add(searchPanel);

        topPanel.add(paramsPanel, BorderLayout.CENTER);

        // Panel de carga de archivos
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        JPanel fileButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fileButtonsPanel.add(loadFileButton);
        fileButtonsPanel.add(loadImageButton);
        fileButtonsPanel.add(clearFileButton);
        fileButtonsPanel.add(viewFileButton);
        filePanel.add(fileButtonsPanel, BorderLayout.WEST);
        filePanel.add(fileNameLabel, BorderLayout.CENTER);
        topPanel.add(filePanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Panel central: área de prompt, vista previa de imagen y respuesta
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Panel superior (prompt y vista previa)
        JPanel upperPanel = new JPanel(new BorderLayout(10, 10));

        // Panel de prompt
        JPanel promptPanel = new JPanel(new BorderLayout(5, 5));
        promptPanel.add(new JLabel("Prompt:"), BorderLayout.NORTH);
        promptPanel.add(new JScrollPane(promptTextArea), BorderLayout.CENTER);
        upperPanel.add(promptPanel, BorderLayout.CENTER);

        // Panel de vista previa de imagen
        upperPanel.add(imagePreviewPanel, BorderLayout.EAST);

        // Configurar panel de respuesta para la vista de respuesta
        JPanel responseContentPanel = new JPanel(new BorderLayout());
        responseContentPanel.add(new JScrollPane(responseTextPane), BorderLayout.CENTER);
        responseContentPanel.add(responseImageLabel, BorderLayout.SOUTH);

        responsePanel.add(new JLabel("Respuesta:"), BorderLayout.NORTH);
        responsePanel.add(responseContentPanel, BorderLayout.CENTER);

        // Añadir respuesta a la vista de respuesta
        responseViewPanel.add(responsePanel, BorderLayout.CENTER);

        // Añadir las vistas al contenedor con CardLayout
        viewContainer.add(responseViewPanel, "response");
        viewContainer.add(searchViewPanel, "search");

        // Por defecto, mostrar la vista de respuesta
        viewCardLayout.show(viewContainer, "response");

        // Dividir la pantalla entre prompt y vista (respuesta/búsqueda)
        contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        contentSplitPane.setResizeWeight(0.3);
        contentSplitPane.setOneTouchExpandable(true);
        contentSplitPane.setTopComponent(upperPanel);
        contentSplitPane.setBottomComponent(viewContainer);

        centerPanel.add(contentSplitPane, BorderLayout.CENTER);

        // Split pane principal: explorer + contenido
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.2);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setLeftComponent(fileExplorerPanel);
        mainSplitPane.setRightComponent(centerPanel);

        // Establecer la posición inicial del divisor desde la configuración
        int splitterPosition = configManager.getMainConfig("splitterPosition", 250);
        mainSplitPane.setDividerLocation(splitterPosition);

        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        // Panel inferior: botones y barra de progreso
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(saveResponseButton);
        bottomPanel.add(buttonPanel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new BorderLayout(5, 0));
        statusPanel.add(progressBar, BorderLayout.NORTH);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Inicializa los manejadores de eventos
     */
    private void initEventHandlers() {
        // Botón de cargar archivo
        loadFileButton.addActionListener(e -> loadFile());

        // Botón de cargar imagen
        loadImageButton.addActionListener(e -> loadImage());

        // Botón de eliminar archivo
        clearFileButton.addActionListener(e -> clearFile());

        // Botón de ver archivo
        viewFileButton.addActionListener(e -> viewFile());

        // Botón de enviar consulta
        sendButton.addActionListener(e -> sendQuery());

        // Botón de guardar respuesta
        saveResponseButton.addActionListener(e -> saveResponse());

        // Checkbox multimodal - deshabilitar/habilitar opciones
        multimodalCheckBox.addActionListener(e -> {
            boolean isMultimodal = multimodalCheckBox.isSelected();
            loadImageButton.setEnabled(isMultimodal);
            configManager.setMainConfig("multimodalEnabled", isMultimodal);
        });

        // Checkbox de búsqueda web
        webSearchCheckBox.addActionListener(e -> {
            boolean isWebSearchEnabled = webSearchCheckBox.isSelected();
            webSearchQueryField.setEnabled(isWebSearchEnabled);
            configManager.setSearchConfig("webSearchEnabled", isWebSearchEnabled);

            // Cambiar la vista según si está habilitada la búsqueda web
            if (isWebSearchEnabled) {
                viewCardLayout.show(viewContainer, "search");
            } else {
                viewCardLayout.show(viewContainer, "response");
            }
        });

        // Detectar Ctrl+Enter para enviar consulta
        promptTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendQuery();
                    e.consume();
                }
            }
        });

        // Guardar cambios en los parámetros cuando se modifiquen
        FocusAdapter paramChangeFocusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveModelParameters();
            }
        };

        temperatureField.addFocusListener(paramChangeFocusAdapter);
        topPField.addFocusListener(paramChangeFocusAdapter);
        maxTokensField.addFocusListener(paramChangeFocusAdapter);

        // Guardar el modelo seleccionado cuando cambie
        modelComboBox.addActionListener(e -> {
            String selectedModel = (String) modelComboBox.getSelectedItem();
            if (selectedModel != null) {
                configManager.setMainConfig("lastUsedModel", selectedModel);
            }
        });

        // Manejar cambios de la posición del divisor
        mainSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> configManager.setMainConfig("splitterPosition", mainSplitPane.getDividerLocation()));
    }

    /**
     * Configura la barra de menú principal
     */
    private void setupMainMenu() {
        mainMenuBar = new MainMenuBar(this);

        // Establecer handlers para acciones del menú
        mainMenuBar.setOnFileOpenListener(e -> loadFile());
        mainMenuBar.setOnFileSaveListener(e -> saveResponse());
        mainMenuBar.setOnExitListener(e -> {
            saveWindowSettings();
            System.exit(0);
        });

        mainMenuBar.setOnSearchConfigListener(e -> {
            if (SearchPreferencesDialog.showDialog(this)) {
                // Actualizar la configuración de búsqueda
                updateSearchSettings();
            }
        });

        mainMenuBar.setOnThemeChangeListener(e -> {
            themeManager.showThemeDialog(this);
        });

        mainMenuBar.setOnErrorLogListener(e -> errorManager.showErrorLog());

        // Añadir listener para otros eventos del menú
        mainMenuBar.addActionListener(e -> {
            String command = e.getActionCommand();

            switch (command) {
                case "clearPrompt":
                    promptTextArea.setText("");
                    break;

                case "clearCache":
                    clearCache();
                    break;

                case "settingsReset":
                    reloadSettings();
                    break;

                case "webSearchEnabledChanged":
                    boolean enabled = webSearchCheckBox.isSelected();
                    webSearchQueryField.setEnabled(enabled);
                    if (enabled) {
                        viewCardLayout.show(viewContainer, "search");
                    } else {
                        viewCardLayout.show(viewContainer, "response");
                    }
                    break;

                case "searchEngineChanged":
                    WebSearchService.SearchAPI engine = (WebSearchService.SearchAPI) e.getSource();
                    webSearchService.setSearchAPI(engine);
                    break;

                case "setDisplayMode":
                    SearchResultsPanel.DisplayMode mode = (SearchResultsPanel.DisplayMode) e.getSource();
                    searchResultsPanel.setDisplayMode(mode);
                    break;

                case "openRecentFile":
                    String filePath = (String) e.getSource();
                    if (filePath != null) {
                        openRecentFile(filePath);
                    }
                    break;

                case "configChanged":
                    reloadSettings();
                    break;
            }
        });

        setJMenuBar(mainMenuBar);
    }

    /**
     * Abre un archivo reciente
     */
    private void openRecentFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            errorManager.handleError(
                    ErrorManager.ErrorCategory.FILE_SYSTEM,
                    ErrorManager.ErrorSeverity.WARNING,
                    "Archivo no encontrado",
                    "El archivo reciente no existe: " + filePath,
                    true
            );
            return;
        }

        loadedFilePath = filePath;

        setStatus("Cargando archivo reciente: " + file.getName(), true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return readFileContent(loadedFilePath);
            }

            @Override
            protected void done() {
                try {
                    fileContent = get();
                    fileNameLabel.setText(file.getName() + " (" + fileContent.length() + " caracteres)");
                    clearFileButton.setEnabled(true);
                    viewFileButton.setEnabled(true);
                    setStatus("Archivo cargado: " + file.getName(), false);

                    // Añadir a archivos recientes
                    configManager.addRecentFile(filePath);
                    mainMenuBar.refreshRecentFilesMenu();

                } catch (Exception e) {
                    errorManager.handleException(
                            ErrorManager.ErrorCategory.FILE_SYSTEM,
                            "Error al cargar archivo reciente",
                            e,
                            true
                    );

                    fileContent = "";
                    loadedFilePath = "";
                    fileNameLabel.setText("Sin archivo cargado");
                    clearFileButton.setEnabled(false);
                    viewFileButton.setEnabled(false);
                    setStatus("Error al cargar archivo", false);
                }
            }
        };

        worker.execute();
    }

    /**
     * Guarda la configuración del tamaño de la ventana al cerrar
     */
    private void saveWindowSettings() {
        configManager.setMainConfig("windowWidth", getWidth());
        configManager.setMainConfig("windowHeight", getHeight());
        configManager.saveMainConfig();
        logger.info("Configuración de ventana guardada: {}x{}", getWidth(), getHeight());
    }

    /**
     * Guarda los parámetros del modelo
     */
    private void saveModelParameters() {
        try {
            float temperature = Float.parseFloat(temperatureField.getText());
            float topP = Float.parseFloat(topPField.getText());
            int maxTokens = Integer.parseInt(maxTokensField.getText());

            // Validar rangos
            temperature = Math.max(0.0f, Math.min(1.0f, temperature));
            topP = Math.max(0.0f, Math.min(1.0f, topP));
            maxTokens = Math.max(1, maxTokens);

            // Actualizar campos por si se modificaron los valores
            temperatureField.setText(String.valueOf(temperature));
            topPField.setText(String.valueOf(topP));
            maxTokensField.setText(String.valueOf(maxTokens));

            // Guardar en configuración
            configManager.setMainConfig("temperature", temperature);
            configManager.setMainConfig("topP", topP);
            configManager.setMainConfig("maxTokens", maxTokens);

        } catch (NumberFormatException e) {
            // Restaurar valores anteriores
            temperatureField.setText(String.valueOf(configManager.getMainConfig("temperature", 0.9f)));
            topPField.setText(String.valueOf(configManager.getMainConfig("topP", 0.9f)));
            maxTokensField.setText(String.valueOf(configManager.getMainConfig("maxTokens", 10000)));

            errorManager.handleError(
                    ErrorManager.ErrorCategory.CONFIGURATION,
                    ErrorManager.ErrorSeverity.WARNING,
                    "Formato inválido",
                    "Los parámetros deben ser valores numéricos válidos",
                    false
            );
        }
    }

    /**
     * Limpia la caché de datos temporales
     */
    private void clearCache() {
        // Limpiar caché de imágenes, archivos temporales, etc.
        try {
            // Limpiar caché de imágenes en memoria
            imageBase64 = "";
            loadedImage = null;
            responseImage = null;

            // Actualizar interfaz
            imagePreviewPanel.setVisible(false);
            imagePreviewLabel.setIcon(null);
            responseImageLabel.setVisible(false);
            responseImageLabel.setIcon(null);

            setStatus("Caché limpiada correctamente", false);

        } catch (Exception e) {
            errorManager.handleException(
                    ErrorManager.ErrorCategory.UNKNOWN,
                    "Error al limpiar caché",
                    e,
                    true
            );
        }
    }

    /**
     * Recarga todos los ajustes desde la configuración
     */
    private void reloadSettings() {
        // Recargar configuración
        configManager.loadAllConfigurations();

        // Actualizar controles con los valores cargados
        updateFromSettings();

        // Recargar tema
        themeManager.loadThemeFromConfig();

        // Actualizar lista de archivos recientes
        mainMenuBar.refreshRecentFilesMenu();

        setStatus("Configuración recargada", false);
    }

    /**
     * Carga los ajustes iniciales desde la configuración
     */
    private void loadSettings() {
        // Cargar y aplicar ajustes
        updateFromSettings();

        // Actualizar lista de archivos recientes en el menú
        mainMenuBar.refreshRecentFilesMenu();
    }

    /**
     * Actualiza la interfaz con los ajustes desde la configuración
     */
    private void updateFromSettings() {
        // Actualizar parámetros del modelo
        temperatureField.setText(String.valueOf(configManager.getMainConfig("temperature", 0.9f)));
        topPField.setText(String.valueOf(configManager.getMainConfig("topP", 0.9f)));
        maxTokensField.setText(String.valueOf(configManager.getMainConfig("maxTokens", 10000)));

        // Actualizar opciones
        multimodalCheckBox.setSelected(configManager.getMainConfig("multimodalEnabled", false));
        loadImageButton.setEnabled(multimodalCheckBox.isSelected());

        // Actualizar configuración de búsqueda
        updateSearchSettings();
    }

    /**
     * Actualiza la configuración del servicio de búsqueda
     */
    private void updateSearchSettings() {
        // Obtener configuración de búsqueda
        boolean webSearchEnabled = configManager.getSearchConfig("webSearchEnabled", false);
        webSearchCheckBox.setSelected(webSearchEnabled);
        webSearchQueryField.setEnabled(webSearchEnabled);

        // Establecer API de búsqueda
        try {
            String apiName = configManager.getSearchConfig("searchAPIProvider", "DUCKDUCKGO");
            WebSearchService.SearchAPI searchAPI = WebSearchService.SearchAPI.valueOf(apiName);
            webSearchService.setSearchAPI(searchAPI);
        } catch (IllegalArgumentException e) {
            logger.warn("API de búsqueda inválida en configuración: {}",
                    configManager.getSearchConfig("searchAPIProvider", ""));
            webSearchService.setSearchAPI(WebSearchService.SearchAPI.DUCKDUCKGO);
        }

        // Establecer claves API
        webSearchService.setApiKey("serpapi", configManager.getApiKey("serpapi"));
        webSearchService.setApiKey("google", configManager.getApiKey("google"));
        webSearchService.setApiKey("google_cse", configManager.getApiKey("google_cse"));

        // Actualizar vista
        if (webSearchEnabled) {
            viewCardLayout.show(viewContainer, "search");
        } else {
            viewCardLayout.show(viewContainer, "response");
        }
    }

    /**
     * Método para manejar los archivos seleccionados desde el explorador
     */
    private void handleSelectedFiles(List<FileExplorerPanel.FileInfo> files) {
        if (files.isEmpty()) {
            return;
        }

        this.selectedFiles = files;

        // Actualizar el área de prompt con información sobre los archivos seleccionados
        StringBuilder filesInfo = new StringBuilder();
        filesInfo.append("Archivos seleccionados para análisis:\n");

        for (FileExplorerPanel.FileInfo file : files) {
            filesInfo.append("- ").append(file.getName())
                    .append(" (").append(file.getPath()).append(")\n");
        }

        promptTextArea.setText(filesInfo.toString() + "\n" + promptTextArea.getText());

        // Si hay imágenes entre los archivos seleccionados, mostrar la primera en el panel de vista previa
        files.stream()
                .filter(FileExplorerPanel.FileInfo::isImage)
                .findFirst()
                .ifPresent(this::previewImageFile);

        setStatus("Archivos seleccionados: " + files.size(), false);
    }

    /**
     * Añade los resultados de búsqueda seleccionados al prompt
     */
    private void addSearchResultsToPrompt() {
        String resultsText = searchResultsPanel.getSelectedResultsText();

        if (!resultsText.isEmpty()) {
            // Añadir al prompt
            if (promptTextArea.getText().isEmpty()) {
                promptTextArea.setText(resultsText);
            } else {
                promptTextArea.setText(promptTextArea.getText() + "\n\n" + resultsText);
            }

            // Cambiar a la vista de respuesta
            viewCardLayout.show(viewContainer, "response");

            setStatus("Resultados añadidos al prompt", false);
        }
    }

    /**
     * Método para mostrar una vista previa de un archivo de imagen
     */
    private void previewImageFile(FileExplorerPanel.FileInfo imageFile) {
        try {
            File file = new File(imageFile.getPath());
            loadedImage = ImageIO.read(file);
            loadedFilePath = imageFile.getPath();

            // Convertir a base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String extension = imageFile.getExtension();
            ImageIO.write(loadedImage, extension, baos);
            imageBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Mostrar vista previa
            displayImagePreview(loadedImage);

            // Añadir a archivos recientes
            configManager.addRecentFile(imageFile.getPath());
            mainMenuBar.refreshRecentFilesMenu();

            // Activar multimodal si hay una imagen
            if (!multimodalCheckBox.isSelected()) {
                multimodalCheckBox.setSelected(true);
                // Guardar en configuración
                configManager.setMainConfig("multimodalEnabled", true);
            }

        } catch (Exception e) {
            errorManager.handleException(
                    ErrorManager.ErrorCategory.FILE_SYSTEM,
                    "Error al previsualizar imagen",
                    e,
                    true
            );
        }
    }

    /**
     * Prepara el contenido de múltiples archivos para enviar al modelo
     */
    private String prepareMultiFileContent() {
        if (selectedFiles.isEmpty()) {
            return fileContent; // Usar el contenido de archivo único si no hay selección múltiple
        }

        StringBuilder content = new StringBuilder();
        content.append("# ARCHIVOS SELECCIONADOS\n\n");

        for (FileExplorerPanel.FileInfo file : selectedFiles) {
            if (!file.isImage()) {
                try {
                    file.loadContent();
                    content.append("## ARCHIVO: ").append(file.getName())
                            .append(" (").append(file.getPath()).append(")\n\n");
                    content.append("```").append(file.getExtension()).append("\n");
                    content.append(file.getContent()).append("\n");
                    content.append("```\n\n");
                } catch (IOException e) {
                    logger.error("Error al cargar contenido del archivo: " + file.getPath(), e);
                    content.append("## ERROR al cargar ").append(file.getName())
                            .append(": ").append(e.getMessage()).append("\n\n");
                }
            } else {
                content.append("## IMAGEN: ").append(file.getName())
                        .append(" (no se muestra contenido binario)\n\n");
            }
        }

        return content.toString();
    }

    /**
     * Carga la lista de modelos disponibles
     */
    private void loadModels() {
        setStatus("Cargando modelos...", true);

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return getAvailableModels();
            }

            @Override
            protected void done() {
                try {
                    List<String> models = get();
                    modelComboBox.removeAllItems();

                    if (models.isEmpty()) {
                        setStatus("No se encontraron modelos en Ollama", false);
                        errorManager.handleError(
                                ErrorManager.ErrorCategory.OLLAMA_API,
                                ErrorManager.ErrorSeverity.WARNING,
                                "No se encontraron modelos",
                                "Asegúrate de haber descargado al menos un modelo y que Ollama esté ejecutándose.",
                                true
                        );
                    } else {
                        // Agregar todos los modelos
                        for (String model : models) {
                            modelComboBox.addItem(model);
                        }

                        // Obtener modelo usado anteriormente
                        String lastUsedModel = configManager.getMainConfig("lastUsedModel", "");

                        // Si existe en la lista, seleccionarlo
                        if (!lastUsedModel.isEmpty()) {
                            for (int i = 0; i < modelComboBox.getItemCount(); i++) {
                                if (modelComboBox.getItemAt(i).equals(lastUsedModel)) {
                                    modelComboBox.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }

                        // Si no hay modelo previo, buscar un modelo compatible con nuestras características
                        if (lastUsedModel.isEmpty()) {
                            // Preseleccionar gemma3:27b si está disponible (compatible con multimodal)
                            for (int i = 0; i < modelComboBox.getItemCount(); i++) {
                                String model = modelComboBox.getItemAt(i);
                                if (model.contains("gemma3") && model.contains("27b")) {
                                    modelComboBox.setSelectedIndex(i);
                                    multimodalCheckBox.setSelected(true);
                                    configManager.setMainConfig("multimodalEnabled", true);
                                    break;
                                }
                            }
                        }

                        setStatus("Modelos cargados: " + models.size(), false);
                    }
                } catch (Exception e) {
                    modelComboBox.removeAllItems();
                    setStatus("Error al cargar modelos", false);

                    errorManager.handleException(
                            ErrorManager.ErrorCategory.OLLAMA_API,
                            "Error de conexión con Ollama",
                            e,
                            true
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Carga un archivo utilizando un selector de archivos
     */
    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo");

        // Filtros de archivo
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Documentos PDF (*.pdf)", "pdf"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Documentos Word (*.docx)", "docx"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Archivos de código (*.java, *.py, *.js, *.html, *.css, *.xml)",
                "java", "py", "js", "html", "css", "xml"));
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadedFilePath = selectedFile.getAbsolutePath();

            // Reiniciar el estado de la imagen
            loadedImage = null;
            imageBase64 = "";
            imagePreviewPanel.setVisible(false);
            imagePreviewLabel.setIcon(null);

            setStatus("Cargando archivo: " + selectedFile.getName(), true);

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    return readFileContent(loadedFilePath);
                }

                @Override
                protected void done() {
                    try {
                        fileContent = get();
                        fileNameLabel.setText(selectedFile.getName() + " (" + fileContent.length() + " caracteres)");
                        clearFileButton.setEnabled(true);
                        viewFileButton.setEnabled(true);
                        setStatus("Archivo cargado: " + selectedFile.getName(), false);

                        // Añadir a archivos recientes
                        configManager.addRecentFile(loadedFilePath);
                        mainMenuBar.refreshRecentFilesMenu();

                    } catch (Exception e) {
                        fileContent = "";
                        loadedFilePath = "";
                        fileNameLabel.setText("Sin archivo cargado");
                        clearFileButton.setEnabled(false);
                        viewFileButton.setEnabled(false);
                        setStatus("Error al cargar archivo", false);

                        errorManager.handleException(
                                ErrorManager.ErrorCategory.FILE_SYSTEM,
                                "Error al cargar archivo",
                                e.getCause(),
                                true
                        );
                    }
                }
            };

            worker.execute();
        }
    }

    /**
     * Carga una imagen utilizando un selector de archivos
     */
    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar imagen");

        // Filtros de imagen
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Imágenes (*.jpg, *.jpeg, *.png, *.gif, *.bmp)",
                IMAGE_EXTENSIONS));
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadedFilePath = selectedFile.getAbsolutePath();

            // Reiniciar el estado del archivo de texto
            fileContent = "";

            setStatus("Cargando imagen: " + selectedFile.getName(), true);

            SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() throws Exception {
                    // Leer la imagen
                    BufferedImage img = ImageIO.read(selectedFile);

                    // Convertir a base64
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String extension = getFileExtension(loadedFilePath);
                    ImageIO.write(img, extension, baos);
                    byte[] imageBytes = baos.toByteArray();
                    imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

                    return img;
                }

                @Override
                protected void done() {
                    try {
                        loadedImage = get();

                        // Mostrar vista previa
                        displayImagePreview(loadedImage);

                        fileNameLabel.setText(selectedFile.getName() + " (" +
                                loadedImage.getWidth() + "x" + loadedImage.getHeight() + ")");
                        clearFileButton.setEnabled(true);
                        viewFileButton.setEnabled(true);
                        setStatus("Imagen cargada: " + selectedFile.getName(), false);

                        // Añadir a archivos recientes
                        configManager.addRecentFile(loadedFilePath);
                        mainMenuBar.refreshRecentFilesMenu();

                    } catch (Exception e) {
                        loadedImage = null;
                        imageBase64 = "";
                        loadedFilePath = "";
                        fileNameLabel.setText("Sin archivo cargado");
                        clearFileButton.setEnabled(false);
                        viewFileButton.setEnabled(false);
                        imagePreviewPanel.setVisible(false);
                        setStatus("Error al cargar imagen", false);

                        errorManager.handleException(
                                ErrorManager.ErrorCategory.FILE_SYSTEM,
                                "Error al cargar imagen",
                                e.getCause(),
                                true
                        );
                    }
                }
            };

            worker.execute();
        }
    }

    /**
     * Muestra una vista previa de la imagen cargada
     */
    private void displayImagePreview(BufferedImage image) {
        if (image == null) {
            imagePreviewPanel.setVisible(false);
            return;
        }

        // Redimensionar la imagen para la vista previa
        int maxWidth = 200;
        int maxHeight = 200;

        double widthRatio = (double) maxWidth / image.getWidth();
        double heightRatio = (double) maxHeight / image.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int width = (int) (image.getWidth() * ratio);
        int height = (int) (image.getHeight() * ratio);

        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(resizedImage);

        imagePreviewLabel.setIcon(icon);
        imagePreviewPanel.setVisible(true);
    }

    /**
     * Elimina el archivo o imagen cargado
     */
    private void clearFile() {
        fileContent = "";
        loadedImage = null;
        imageBase64 = "";
        loadedFilePath = "";
        fileNameLabel.setText("Sin archivo cargado");
        clearFileButton.setEnabled(false);
        viewFileButton.setEnabled(false);
        imagePreviewPanel.setVisible(false);
        imagePreviewLabel.setIcon(null);
        setStatus("Archivo eliminado", false);
    }

    /**
     * Muestra el contenido del archivo o imagen cargado
     */
    private void viewFile() {
        if (fileContent.isEmpty() && loadedImage == null) {
            return;
        }

        JDialog dialog = new JDialog(this, "Contenido: " + new File(loadedFilePath).getName(), true);
        dialog.setSize(700, 500);
        dialog.setLayout(new BorderLayout(10, 10));

        if (loadedImage != null) {
            // Mostrar imagen
            JLabel imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);

            // Ajustar imagen al tamaño del diálogo, manteniendo la proporción
            int maxWidth = 650;
            int maxHeight = 400;

            double widthRatio = (double) maxWidth / loadedImage.getWidth();
            double heightRatio = (double) maxHeight / loadedImage.getHeight();
            double ratio = Math.min(widthRatio, heightRatio);

            int width = (int) (loadedImage.getWidth() * ratio);
            int height = (int) (loadedImage.getHeight() * ratio);

            Image resizedImage = loadedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(resizedImage));

            JScrollPane scrollPane = new JScrollPane(imageLabel);
            dialog.add(scrollPane, BorderLayout.CENTER);

            // Mostrar información de la imagen
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            infoPanel.add(new JLabel("Dimensiones: " + loadedImage.getWidth() + " x " + loadedImage.getHeight()));
            infoPanel.add(new JLabel(" | Formato: " + getFileExtension(loadedFilePath).toUpperCase()));
            dialog.add(infoPanel, BorderLayout.NORTH);
        } else {
            // Área de texto con sintaxis coloreada
            JTextPane textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setText(fileContent);

            // Detectar el tipo de archivo y aplicar coloreado de sintaxis
            String extension = getFileExtension(loadedFilePath).toLowerCase();
            if (extension.equals("java") || extension.equals("py") || extension.equals("js") ||
                    extension.equals("html") || extension.equals("css") || extension.equals("xml")) {
                applySyntaxHighlighting(textPane, fileContent, extension);
            }

            JScrollPane scrollPane = new JScrollPane(textPane);
            dialog.add(scrollPane, BorderLayout.CENTER);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Envía la consulta a Ollama
     */
    private void sendQuery() {
        if (isRequestInProgress) {
            return;
        }

        String prompt = promptTextArea.getText().trim();
        if (prompt.isEmpty() && !webSearchCheckBox.isSelected()) {
            errorManager.handleError(
                    ErrorManager.ErrorCategory.UNKNOWN,
                    ErrorManager.ErrorSeverity.INFO,
                    "Prompt vacío",
                    "El prompt no puede estar vacío",
                    true
            );
            return;
        }

        // Si la búsqueda web está habilitada, permitir un prompt vacío
        if (prompt.isEmpty() && webSearchCheckBox.isSelected()) {
            prompt = "Por favor analiza y responde basándote en esta información.";
        }

        String model = (String) modelComboBox.getSelectedItem();
        if (model == null || model.isEmpty()) {
            errorManager.handleError(
                    ErrorManager.ErrorCategory.OLLAMA_API,
                    ErrorManager.ErrorSeverity.WARNING,
                    "Modelo no seleccionado",
                    "Debes seleccionar un modelo",
                    true
            );
            return;
        }

        Map<String, Float> parameters = new HashMap<>();
        try {
            float temperature = Float.parseFloat(temperatureField.getText());
            float topP = Float.parseFloat(topPField.getText());
            float maxTokens = Float.parseFloat(maxTokensField.getText());

            parameters.put("temperature", temperature);
            parameters.put("top_p", topP);
            parameters.put("max_tokens", maxTokens);

            // Guardar parámetros para uso futuro
            saveModelParameters();

        } catch (NumberFormatException e) {
            errorManager.handleError(
                    ErrorManager.ErrorCategory.CONFIGURATION,
                    ErrorManager.ErrorSeverity.WARNING,
                    "Error en parámetros",
                    "Los parámetros deben ser números válidos",
                    true
            );
            return;
        }

        // Determinar el tipo de consulta (texto, multimodal, con búsqueda web)
        final boolean isMultimodalQuery = multimodalCheckBox.isSelected() && loadedImage != null;
        final boolean isWebSearchEnabled = webSearchCheckBox.isSelected();

        // Verificar búsqueda web
        if (isWebSearchEnabled) {
            String searchQuery = webSearchQueryField.getText().trim();
            if (searchQuery.isEmpty()) {
                // Usar el prompt como consulta de búsqueda si no hay consulta específica
                webSearchQueryField.setText(prompt);
                searchQuery = prompt;
            }
        }

        // Configurar prompt final
        final String multiFileContent = prepareMultiFileContent();
        final String basePrompt = (fileContent.isEmpty() && selectedFiles.isEmpty()) ?
                prompt :
                "Archivos:\n\n" + multiFileContent + "\n\nPrompt:\n\n" + prompt;

        // Variable para el prompt final (se modificará si la búsqueda web está habilitada)
        final String[] finalTextPrompt = {basePrompt};

        // Capturar el valor final de prompt en una variable final para uso en el SwingWorker
        final String finalPrompt = prompt;

        // Cambiar a la vista de respuesta
        viewCardLayout.show(viewContainer, "response");

        // Enviar consulta
        isRequestInProgress = true;
        setStatus("Enviando consulta a Ollama (" + model + ")...", true);
        sendButton.setEnabled(false);
        saveResponseButton.setEnabled(false);

        // Ocultar imagen de respuesta anterior si la hay
        responseImageLabel.setVisible(false);
        responseImageLabel.setIcon(null);
        responseImage = null;

        // Limpiar área de respuesta
        responseTextPane.setText("");

        SwingWorker<Map<String, Object>, String> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                // Si la búsqueda web está habilitada, realizar búsqueda
                if (isWebSearchEnabled && !isMultimodalQuery) {
                    publish("Realizando búsqueda web...");

                    // Determinar la consulta de búsqueda
                    String searchQuery = webSearchQueryField.getText().trim();

                    try {
                        // Realizar búsqueda
                        List<WebSearchService.SearchResult> searchResults = webSearchService.search(searchQuery);

                        // Actualizar panel de resultados de búsqueda
                        SwingUtilities.invokeLater(() -> searchResultsPanel.setResults(searchResults));

                        if (!searchResults.isEmpty()) {
                            // Formatear resultados y añadirlos al prompt
                            String formattedResults = webSearchService.formatSearchResultsForPrompt(searchResults);
                            publish("Búsqueda completada. Realizando consulta con información adicional...");

                            // Estructura mejorada para el prompt
                            finalTextPrompt[0] = "A continuación hay información obtenida de una búsqueda web reciente sobre: \"" +
                                    searchQuery + "\"\n\n" + formattedResults +
                                    "\n\nInstrucciones: Utiliza ÚNICAMENTE la información anterior para responder lo siguiente. Si la información " +
                                    "proporcionada no es suficiente, indícalo claramente. No uses conocimientos previos que no estén en los resultados de búsqueda.\n\n" +
                                    "Consulta: " + (basePrompt.isEmpty() ? "Proporciona un resumen detallado de esta información." : basePrompt);
                        } else {
                            publish("No se encontraron resultados en la búsqueda. Realizando consulta normal...");
                            finalTextPrompt[0] = "Realicé una búsqueda web sobre \"" + searchQuery + "\" pero no se encontraron resultados. " +
                                    "Por favor responde a lo siguiente con tu mejor conocimiento: " + basePrompt;
                        }
                    } catch (Exception e) {
                        logger.error("Error en búsqueda web", e);
                        publish("Error en búsqueda web: " + e.getMessage() + ". Realizando consulta normal...");
                        finalTextPrompt[0] = "Intenté realizar una búsqueda web sobre \"" + searchQuery + "\" pero ocurrió un error: " +
                                e.getMessage() + ". Por favor responde a lo siguiente con tu mejor conocimiento: " + basePrompt;

                        errorManager.handleException(
                                ErrorManager.ErrorCategory.SEARCH_API,
                                "Error de búsqueda web",
                                e,
                                false // No mostrar diálogo para no interrumpir el flujo
                        );
                    }
                }

                // Enviar el prompt a Ollama
                if (isMultimodalQuery) {
                    return sendMultimodalPromptToOllama(model, finalPrompt, imageBase64, parameters);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("text", sendPromptToOllama(model, finalTextPrompt[0], parameters));
                    return result;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                // Actualizar el área de respuesta con la respuesta parcial
                String lastChunk = chunks.get(chunks.size() - 1);
                responseTextPane.setText(lastChunk);
                responseTextPane.setCaretPosition(lastChunk.length());
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> result = get();

                    // Procesar respuesta de texto
                    if (result.containsKey("text")) {
                        currentResponse = (String) result.get("text");

                        // Verificar si la respuesta está vacía
                        if (currentResponse == null || currentResponse.trim().isEmpty()) {
                            currentResponse = "[El modelo no generó una respuesta textual. Esto puede ocurrir con algunos modelos de menor tamaño en modo multimodal.]";
                            logger.warn("Se recibió una respuesta vacía del modelo {}", model);
                        }

                        responseTextPane.setText(currentResponse);

                        // Aplicar coloreado de sintaxis
                        highlightCodeBlocks(responseTextPane, currentResponse);
                    } else {
                        // Si no hay texto en la respuesta, mostrar mensaje informativo
                        String noTextMessage = "[No se recibió texto en la respuesta del modelo.]";
                        responseTextPane.setText(noTextMessage);
                        currentResponse = noTextMessage;
                        logger.warn("La respuesta no contiene texto para el modelo {}", model);
                    }

                    // Procesar respuesta de imagen
                    if (result.containsKey("image")) {
                        responseImage = (BufferedImage) result.get("image");
                        displayResponseImage(responseImage);
                    }

                    setStatus("Respuesta recibida", false);
                    saveResponseButton.setEnabled(true);
                } catch (Exception e) {
                    logger.error("Error al procesar la consulta", e);
                    setStatus("Error al procesar la consulta", false);

                    errorManager.handleException(
                            ErrorManager.ErrorCategory.OLLAMA_API,
                            "Error de comunicación",
                            e.getCause(),
                            true
                    );
                } finally {
                    isRequestInProgress = false;
                    sendButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Muestra una imagen en el área de respuesta
     */
    private void displayResponseImage(BufferedImage image) {
        if (image == null) {
            responseImageLabel.setVisible(false);
            return;
        }

        // Redimensionar la imagen para mostrarla (ajustada al ancho del panel)
        int maxWidth = responsePanel.getWidth() - 20;  // Margen
        int maxHeight = 400;  // Altura máxima razonable

        double widthRatio = (double) maxWidth / image.getWidth();
        double heightRatio = (double) maxHeight / image.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int width = (int) (image.getWidth() * ratio);
        int height = (int) (image.getHeight() * ratio);

        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(resizedImage);

        responseImageLabel.setIcon(icon);
        responseImageLabel.setVisible(true);

        // Añadir información sobre la imagen
        responseTextPane.setText(responseTextPane.getText() +
                "\n\n[Imagen generada: " + image.getWidth() + "x" + image.getHeight() + " píxeles]");
    }

    /**
     * Guarda la respuesta en un archivo (texto o imagen)
     */
    private void saveResponse() {
        if (currentResponse.isEmpty() && responseImage == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar respuesta");

        if (responseImage != null) {
            // Guardar imagen
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Imagen JPEG (*.jpg)", "jpg"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Imagen GIF (*.gif)", "gif"));

            // Sugerir un nombre de archivo para la imagen
            String suggestedImageName = "imagen_respuesta_" + new Date().getTime() + ".png";
            fileChooser.setSelectedFile(new File(suggestedImageName));
        } else {
            // Guardar texto
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Archivo de texto (*.txt)", "txt"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Archivo Markdown (*.md)", "md"));

            // Sugerir un nombre de archivo para el texto
            String suggestedTextName = "respuesta_" + new Date().getTime() + ".txt";
            fileChooser.setSelectedFile(new File(suggestedTextName));
        }

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                if (responseImage != null) {
                    // Guardar imagen
                    String extension = getFileExtension(selectedFile.getPath());
                    if (extension.isEmpty()) {
                        extension = "png";  // Por defecto
                        selectedFile = new File(selectedFile.getPath() + ".png");
                    }

                    ImageIO.write(responseImage, extension, selectedFile);

                    // Si también hay texto, guardarlo en un archivo adicional
                    if (!currentResponse.isEmpty()) {
                        File textFile = new File(selectedFile.getParent(),
                                "texto_" + selectedFile.getName().replace("." + extension, ".txt"));
                        try (FileWriter writer = new FileWriter(textFile)) {
                            writer.write(currentResponse);
                        }
                    }
                } else {
                    // Guardar solo texto
                    try (FileWriter writer = new FileWriter(selectedFile)) {
                        writer.write(currentResponse);
                    }
                }
                setStatus("Respuesta guardada en: " + selectedFile.getName(), false);
            } catch (IOException e) {
                logger.error("Error al guardar respuesta", e);
                setStatus("Error al guardar respuesta", false);

                errorManager.handleException(
                        ErrorManager.ErrorCategory.FILE_SYSTEM,
                        "Error al guardar",
                        e,
                        true
                );
            }
        }
    }

    /**
     * Actualiza el estado de la aplicación
     */
    private void setStatus(String message, boolean inProgress) {
        statusLabel.setText(message);
        progressBar.setVisible(inProgress);
    }

    /**
     * Lee el contenido de un archivo según su extensión
     */
    private String readFileContent(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            logger.error("Archivo no encontrado: {}", filePath);
            throw new IOException("El archivo no existe: " + filePath);
        }

        String extension = getFileExtension(filePath);
        logger.info("Leyendo archivo: {} ({})", filePath, extension);

        try {
            switch (extension.toLowerCase()) {
                case "txt":
                case "java":
                case "py":
                case "js":
                case "html":
                case "css":
                case "xml":
                    return readTextFile(file);
                case "pdf":
                    return readPdfFile(file);
                case "docx":
                    return readDocxFile(file);
                default:
                    logger.warn("Formato no soportado: {}", extension);
                    throw new UnsupportedOperationException("Formato de archivo no soportado: " + extension +
                            ". Los formatos soportados son: txt, pdf, docx, java, py, js, html, css, xml");
            }
        } catch (Exception e) {
            logger.error("Error al leer archivo", e);
            throw new IOException("Error al leer el archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * Lee un archivo de texto
     */
    private String readTextFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    /**
     * Lee un archivo PDF
     */
    private String readPdfFile(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (NoClassDefFoundError e) {
            logger.error("Dependencia faltante: PDFBox", e);
            throw new IOException("Error al leer PDF: Falta la dependencia de PDFBox. " +
                    "Añade 'org.apache.pdfbox:pdfbox:2.0.27' a tu proyecto.");
        }
    }

    /**
     * Lee un archivo DOCX
     */
    private String readDocxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String text = extractor.getText();
            extractor.close();
            return text;

        } catch (NoClassDefFoundError e) {
            logger.error("Dependencia faltante: Apache POI", e);
            throw new IOException("Error al leer DOCX: Falta la dependencia de Apache POI. " +
                    "Añade 'org.apache.poi:poi-ooxml:5.2.3' a tu proyecto.");
        }
    }

    /**
     * Obtiene la lista de modelos disponibles en Ollama
     */
    private List<String> getAvailableModels() throws IOException {
        List<String> models = new ArrayList<>();
        logger.info("Obteniendo lista de modelos disponibles");

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_MODELS_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Leer respuesta
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }

            // Extraer nombres de modelos del JSON
            Pattern pattern = Pattern.compile("\"name\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response.toString());

            while (matcher.find()) {
                String modelName = matcher.group(1);
                models.add(modelName);
            }

            logger.info("Modelos encontrados: {}", models.size());
        } catch (Exception e) {
            logger.error("Error al obtener modelos", e);
            throw new IOException("Error al obtener la lista de modelos: " + e.getMessage(), e);
        } finally {
            connection.disconnect();
        }

        return models;
    }

    /**
     * Envía un prompt a Ollama y procesa la respuesta (solo texto)
     */
    private String sendPromptToOllama(String model, String prompt, Map<String, Float> parameters) throws IOException {
        logger.info("Enviando prompt de texto al modelo: {}", model);

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        // Construir el JSON de forma segura usando JSONObject
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        jsonRequest.put("prompt", prompt);
        for (Map.Entry<String, Float> param : parameters.entrySet()) {
            jsonRequest.put(param.getKey(), param.getValue());
        }
        jsonRequest.put("stream", true);

        // Enviar petición
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Procesar respuesta
        StringBuilder fullResponse = new StringBuilder();
        try (Scanner responseScanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (responseScanner.hasNextLine()) {
                String line = responseScanner.nextLine();

                // Utilizar JSONObject para procesar correctamente la respuesta JSON
                try {
                    JSONObject jsonResponse = new JSONObject(line);
                    if (jsonResponse.has("response")) {
                        // Esto automáticamente descodifica los caracteres escapados en JSON
                        String responsePart = jsonResponse.getString("response");
                        fullResponse.append(responsePart);
                    }
                } catch (Exception e) {
                    // Algunas líneas pueden no ser JSON válido, ignorarlas
                    logger.warn("Error al parsear respuesta JSON: {}", line);
                }
            }
        } finally {
            connection.disconnect();
        }

        logger.info("Respuesta recibida: {} caracteres", fullResponse.length());
        return fullResponse.toString();
    }

    /**
     * Envía un prompt multimodal (texto + imagen) a Ollama y procesa la respuesta
     */
    private Map<String, Object> sendMultimodalPromptToOllama(String model, String prompt, String imageBase64,
                                                             Map<String, Float> parameters) throws IOException {
        logger.info("Enviando prompt multimodal al modelo: {}", model);

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_CHAT_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(60000); // Aumentado a 60 segundos
        connection.setReadTimeout(60000);    // Aumentado a 60 segundos
        connection.setDoOutput(true);

        // Determinar el tipo MIME según la extensión
        String extension = getFileExtension(loadedFilePath).toLowerCase();
        String mimeType;
        switch (extension) {
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "png":
                mimeType = "image/png";
                break;
            case "gif":
                mimeType = "image/gif";
                break;
            case "bmp":
                mimeType = "image/bmp";
                break;
            default:
                mimeType = "image/jpeg";  // Por defecto
        }

        // Base64 debe incluir el prefijo de datos URI
        String base64WithPrefix = "data:" + mimeType + ";base64," + imageBase64;

        // Construir el mensaje multimodal según el formato actualizado de Ollama
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        // Formato de contenido para modelos multimodal
        // Primero el texto, luego la imagen separados con newlines y formato específico
        String multimodalContent = prompt + "\n\n![image](" + base64WithPrefix + ")";
        userMessage.put("content", multimodalContent);

        messages.put(userMessage);

        // Construir el JSON principal
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        jsonRequest.put("messages", messages);
        for (Map.Entry<String, Float> param : parameters.entrySet()) {
            jsonRequest.put(param.getKey(), param.getValue());
        }
        jsonRequest.put("stream", true);

        try {
            // Primero registramos el JSON enviado para depuración (omitimos la imagen para no sobrecargar los logs)
            JSONObject logJsonRequest = new JSONObject(jsonRequest.toString());
            JSONArray logMessages = logJsonRequest.getJSONArray("messages");
            JSONObject logUserMessage = logMessages.getJSONObject(0);
            logUserMessage.put("content", prompt + "\n\n[IMAGEN BASE64 OMITIDA EN LOGS]");
            logger.info("Enviando solicitud JSON: {}", logJsonRequest.toString());

            // Enviar petición
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Comprobar si hay error en la respuesta
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                // Leer mensaje de error
                StringBuilder errorResponse = new StringBuilder();
                try (Scanner scanner = new Scanner(connection.getErrorStream(), StandardCharsets.UTF_8.name())) {
                    while (scanner.hasNextLine()) {
                        errorResponse.append(scanner.nextLine());
                    }
                }

                logger.error("Error del servidor: {} - {}", responseCode, errorResponse.toString());
                throw new IOException("Error del servidor: " + responseCode + " - " + errorResponse.toString());
            }

            // Procesar respuesta exitosa
            StringBuilder textResponse = new StringBuilder();
            String imageDataResponse = null;

            try (Scanner responseScanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
                while (responseScanner.hasNextLine()) {
                    String line = responseScanner.nextLine();
                    logger.debug("Línea de respuesta: {}", line);

                    try {
                        JSONObject jsonResponse = new JSONObject(line);

                        // Procesar mensaje completo
                        if (jsonResponse.has("message")) {
                            JSONObject message = jsonResponse.getJSONObject("message");

                            if (message.has("content")) {
                                String content = message.getString("content");
                                textResponse.append(content);

                                // Buscar imágenes en formato de datos URI
                                extractImagesFromMarkdown(content, textResponse);

                                // Registrar que recibimos contenido
                                if (!content.trim().isEmpty()) {
                                    logger.info("Contenido recibido del modelo: {} caracteres", content.length());
                                }
                            } else {
                                logger.warn("El mensaje no tiene campo 'content'");
                            }
                        }
                        // Procesar delta en streaming
                        else if (jsonResponse.has("delta")) {
                            if (jsonResponse.has("delta") && !jsonResponse.isNull("delta")) {
                                Object deltaObj = jsonResponse.get("delta");
                                if (deltaObj instanceof JSONObject) {
                                    JSONObject delta = (JSONObject) deltaObj;
                                    if (delta.has("content") && !delta.isNull("content")) {
                                        String content = delta.getString("content");
                                        textResponse.append(content);

                                        // Buscar imágenes en el contenido delta
                                        extractImagesFromMarkdown(content, textResponse);

                                        // Registrar que recibimos delta
                                        if (!content.trim().isEmpty()) {
                                            logger.debug("Delta recibido: {} caracteres", content.length());
                                        }
                                    } else {
                                        logger.debug("Delta sin contenido o con contenido nulo");
                                    }
                                } else {
                                    logger.debug("Delta no es un objeto JSON: {}", deltaObj);
                                }
                            } else {
                                logger.debug("Delta nulo o no presente");
                            }
                        } else {
                            logger.debug("Respuesta sin mensaje ni delta reconocibles: {}", line);
                        }
                    } catch (Exception e) {
                        // Algunas líneas pueden no ser JSON válido
                        logger.warn("Error al parsear respuesta JSON: {}", e.getMessage());
                    }
                }
            }

            logger.info("Respuesta de texto recibida: {} caracteres", textResponse.length());
            logger.info("¿Se recibió una imagen?: {}", imageDataResponse != null);

            // Preparar resultado
            Map<String, Object> result = new HashMap<>();
            result.put("text", textResponse.toString());

            // Procesar imagen si se recibió
            if (imageDataResponse != null) {
                try {
                    // Primero eliminar el prefijo de data URI si existe
                    String base64Data = imageDataResponse;
                    if (base64Data.contains(";base64,")) {
                        base64Data = base64Data.substring(base64Data.indexOf(";base64,") + 8);
                    }

                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                    BufferedImage image = ImageIO.read(bis);
                    result.put("image", image);
                } catch (Exception e) {
                    logger.error("Error al decodificar imagen de respuesta", e);
                }
            }

            return result;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Extrae imágenes de respuesta en formato markdown
     * Busca patrones como ![image](data:image/png;base64,...)
     */
    private void extractImagesFromMarkdown(String content, StringBuilder textResponse) {
        // Buscar patrones de imágenes en markdown con data URI
        Pattern pattern = Pattern.compile("!\\[.*?\\]\\((data:image\\/[^;]+;base64,[^\\)]+)\\)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String dataUri = matcher.group(1);
            processImageFromMarkdown(dataUri);
            logger.info("Imagen encontrada en respuesta markdown");
        }
    }

    /**
     * Procesa una imagen encontrada en la respuesta markdown
     */
    private void processImageFromMarkdown(String dataUri) {
        try {
            // Primero eliminar el prefijo de data URI si existe
            String base64Data = dataUri;
            if (base64Data.contains(";base64,")) {
                base64Data = base64Data.substring(base64Data.indexOf(";base64,") + 8);
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);

            if (image != null) {
                // Actualizar en el hilo de la UI
                SwingUtilities.invokeLater(() -> {
                    responseImage = image;
                    displayResponseImage(image);
                });
            }
        } catch (Exception e) {
            logger.error("Error al procesar imagen de respuesta markdown", e);
        }
    }

    /**
     * Aplica coloreado de sintaxis a los bloques de código en la respuesta.
     * Busca bloques delimitados por "```" y les aplica un estilo monoespaciado con fondo claro.
     */
    private void highlightCodeBlocks(JTextPane textPane, String text) {
        // Obtener el documento actual del JTextPane
        StyledDocument doc = textPane.getStyledDocument();
        try {
            // Limpiar el documento e insertar el texto completo
            doc.remove(0, doc.getLength());
            doc.insertString(0, text, null);
        } catch (BadLocationException e) {
            logger.error("Error al insertar texto en el documento", e);
        }

        // Definir estilos
        Style defaultStyle = textPane.getStyle("default");
        if (defaultStyle == null) {
            defaultStyle = textPane.addStyle("default", null);
            StyleConstants.setForeground(defaultStyle, Color.BLACK);
            StyleConstants.setFontFamily(defaultStyle, "SansSerif");
        }

        Style codeStyle = textPane.getStyle("code");
        if (codeStyle == null) {
            codeStyle = textPane.addStyle("code", null);
            StyleConstants.setForeground(codeStyle, Color.DARK_GRAY);
            StyleConstants.setBackground(codeStyle, themeManager.getCodeBackgroundColor());
            StyleConstants.setFontFamily(codeStyle, themeManager.getCodeFont().getFamily());
            StyleConstants.setFontSize(codeStyle, themeManager.getCodeFont().getSize());
        }

        // Buscar bloques de código delimitados por triple backticks
        Pattern pattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start() + 3; // omitir los backticks iniciales
            int end = matcher.end() - 3;     // omitir los backticks finales
            int length = end - start;
            if (length > 0) {
                doc.setCharacterAttributes(start, length, codeStyle, true);
            }
        }

        // Verificar si está habilitado el tema oscuro
        darkModeEnabled = configManager.getThemeConfig("enableDarkMode", false);
        if (darkModeEnabled) {
            textPane.setBackground(themeManager.getBackgroundColor());
            textPane.setForeground(themeManager.getTextColor());
        }
    }

    /**
     * Aplica coloreado de sintaxis básico para archivos de código.
     * Para este ejemplo, se establece la fuente a monoespaciada.
     * Se puede ampliar para resaltar palabras clave según el lenguaje.
     */
    private void applySyntaxHighlighting(JTextPane textPane, String code, String extension) {
        textPane.setFont(themeManager.getCodeFont());
    }

    /**
     * Clase para manejar actualizaciones periódicas de la interfaz
     */
    private class UIUpdater {
        //private Timer timer;
        javax.swing.Timer timer;

        public UIUpdater() {
            timer = new javax.swing.Timer(5000, e -> updateUI());
            timer.setRepeats(true);
        }

        public void start() {
            timer.start();
        }

        public void stop() {
            timer.stop();
        }

        private void updateUI() {
            // Actualizar elementos de la interfaz que necesiten refrescarse periódicamente
            if (webSearchCheckBox.isSelected()) {
                // Actualizar panel de resultados de búsqueda
                searchResultsPanel.refreshResults();
            }
        }
    }

    /**
     * Método de punto de entrada para la aplicación
     */
    public static void main(String[] args) {
        // Configurar aspecto nativo del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("Error al establecer Look and Feel", e);
        }

        // Iniciar la aplicación
        SwingUtilities.invokeLater(() -> {
            try {
                ImprovedOllamaGUIClient client = new ImprovedOllamaGUIClient();
                client.setVisible(true);
            } catch (Exception e) {
                logger.error("Error al iniciar la aplicación", e);
                JOptionPane.showMessageDialog(
                        null,
                        "Error al iniciar la aplicación: " + e.getMessage(),
                        "Error de Inicialización",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}