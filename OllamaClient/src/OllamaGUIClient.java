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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente Java con GUI para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos, imágenes y una interfaz gráfica de usuario.
 */
public class OllamaGUIClient extends JFrame {
    // URLs de la API
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_CHAT_API_URL = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODELS_URL = "http://localhost:11434/api/tags";

    // Configuración por defecto
    private static final Map<String, Float> DEFAULT_PARAMETERS = new HashMap<String, Float>() {{
        put("temperature", 0.9f);
        put("top_p", 0.9f);
        put("max_tokens", 1000000f);
    }};

    // Tipos de archivos de imagen soportados
    private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp"};

    // Cache de modelos
    private static final Map<String, String> modelCache = new ConcurrentHashMap<>();

    // Logger para registro de errores y eventos
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);

    // Historial de conversaciones
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

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
    private JCheckBox multimodalCheckbox;
    private JCheckBox webSearchCheckbox;
    private JTextField webSearchQueryField;
    private JComboBox<WebSearchService.SearchAPI> searchAPIComboBox;
    private JPanel imagePreviewPanel;
    private JLabel imagePreviewLabel;
    private JButton webSearchConfigButton;

    // Servicio de búsqueda web
    private WebSearchService webSearchService;

    // Estado de la aplicación
    private String fileContent = "";
    private String loadedFilePath = "";
    private String currentResponse = "";
    private boolean isRequestInProgress = false;
    private BufferedImage loadedImage = null;
    private String imageBase64 = "";
    private BufferedImage responseImage = null;
    private String imageDataResponse = null;

    /**
     * Constructor principal
     */
    public OllamaGUIClient() {
        super("Cliente GUI para Ollama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setMinimumSize(new Dimension(700, 500));

        // Inicializar el servicio de búsqueda web
        webSearchService = new WebSearchService();

        initComponents();
        layoutComponents();
        initEventHandlers();

        // Centrar la ventana
        setLocationRelativeTo(null);
        setVisible(true);

        // Cargar modelos al iniciar
        loadModels();
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

        // Multimodal checkbox
        multimodalCheckbox = new JCheckBox("Modo multimodal");
        multimodalCheckbox.setToolTipText("Habilitar para usar modelos que soporten imágenes como gemma3:27b");

        // Búsqueda web
        webSearchCheckbox = new JCheckBox("Búsqueda web");
        webSearchCheckbox.setToolTipText("Habilitar para enriquecer las consultas con resultados de búsqueda web");

        webSearchQueryField = new JTextField("", 15);
        webSearchQueryField.setToolTipText("Consulta personalizada para búsqueda web (opcional)");
        webSearchQueryField.setEnabled(false);

        searchAPIComboBox = new JComboBox<>(WebSearchService.SearchAPI.values());
        searchAPIComboBox.setToolTipText("Seleccionar API para realizar búsquedas web");
        searchAPIComboBox.setEnabled(false);

        webSearchConfigButton = new JButton("Configurar");
        webSearchConfigButton.setToolTipText("Configurar las claves API para búsqueda web");
        webSearchConfigButton.setEnabled(false);

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
        checkboxesPanel.add(multimodalCheckbox);
        checkboxesPanel.add(webSearchCheckbox);
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
        searchPanel.add(searchAPIComboBox);
        searchPanel.add(webSearchConfigButton);
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

        // Panel de respuesta
        responsePanel.add(new JLabel("Respuesta:"), BorderLayout.NORTH);
        JPanel responseContentPanel = new JPanel(new BorderLayout());
        responseContentPanel.add(new JScrollPane(responseTextPane), BorderLayout.CENTER);
        responseContentPanel.add(responseImageLabel, BorderLayout.SOUTH);
        responsePanel.add(responseContentPanel, BorderLayout.CENTER);

        // Dividir la pantalla entre prompt y respuesta
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(upperPanel);
        splitPane.setBottomComponent(responsePanel);

        centerPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

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
        multimodalCheckbox.addActionListener(e -> {
            boolean isMultimodal = multimodalCheckbox.isSelected();
            loadImageButton.setEnabled(isMultimodal);
        });

        // Checkbox de búsqueda web
        webSearchCheckbox.addActionListener(e -> {
            boolean isWebSearchEnabled = webSearchCheckbox.isSelected();
            webSearchQueryField.setEnabled(isWebSearchEnabled);
            searchAPIComboBox.setEnabled(isWebSearchEnabled);
            webSearchConfigButton.setEnabled(isWebSearchEnabled);
        });

        // Selector de API de búsqueda
        searchAPIComboBox.addActionListener(e -> {
            WebSearchService.SearchAPI selectedAPI = (WebSearchService.SearchAPI) searchAPIComboBox.getSelectedItem();
            webSearchService.setSearchAPI(selectedAPI);
        });

        // Botón de configuración de API
        webSearchConfigButton.addActionListener(e -> configureSearchAPI());

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
    }

    /**
     * Abre un diálogo para configurar las claves API para búsqueda web
     */
    private void configureSearchAPI() {
        JDialog configDialog = new JDialog(this, "Configurar API de búsqueda", true);
        configDialog.setSize(400, 200);
        configDialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Configuración de SerpAPI
        panel.add(new JLabel("Clave SerpAPI:"));
        JTextField serpApiField = new JTextField(20);
        panel.add(serpApiField);

        // Configuración de Google API
        panel.add(new JLabel("Clave Google API:"));
        JTextField googleApiField = new JTextField(20);
        panel.add(googleApiField);

        // Configuración de Google CSE ID
        panel.add(new JLabel("Google CSE ID:"));
        JTextField googleCseField = new JTextField(20);
        panel.add(googleCseField);

        configDialog.add(panel, BorderLayout.CENTER);

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Guardar");
        JButton cancelButton = new JButton("Cancelar");

        saveButton.addActionListener(e -> {
            if (!serpApiField.getText().trim().isEmpty()) {
                webSearchService.setApiKey("serpapi", serpApiField.getText().trim());
            }
            if (!googleApiField.getText().trim().isEmpty()) {
                webSearchService.setApiKey("google", googleApiField.getText().trim());
            }
            if (!googleCseField.getText().trim().isEmpty()) {
                webSearchService.setApiKey("google_cse", googleCseField.getText().trim());
            }
            configDialog.dispose();
        });

        cancelButton.addActionListener(e -> configDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        configDialog.add(buttonPanel, BorderLayout.SOUTH);

        configDialog.setLocationRelativeTo(this);
        configDialog.setVisible(true);
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
                    for (String model : models) {
                        modelComboBox.addItem(model);

                        // Preseleccionar gemma3:27b si está disponible
                        if (model.contains("gemma3") && model.contains("27b")) {
                            modelComboBox.setSelectedItem(model);
                            multimodalCheckbox.setSelected(true);
                        }
                    }
                    if (models.isEmpty()) {
                        setStatus("No se encontraron modelos en Ollama", false);
                        showError("No se encontraron modelos en Ollama",
                                "Asegúrate de haber descargado al menos un modelo y que Ollama esté ejecutándose.");
                    } else {
                        setStatus("Modelos cargados: " + models.size(), false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error al cargar modelos", e);
                    setStatus("Error al cargar modelos", false);
                    showError("Error de conexión",
                            "No se pudo conectar con Ollama. Asegúrate de que está ejecutándose en http://localhost:11434");
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
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error al cargar archivo", e);
                        fileContent = "";
                        loadedFilePath = "";
                        fileNameLabel.setText("Error al cargar archivo");
                        clearFileButton.setEnabled(false);
                        viewFileButton.setEnabled(false);
                        setStatus("Error al cargar archivo", false);
                        showError("Error al cargar archivo", e.getCause().getMessage());
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
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error al cargar imagen", e);
                        loadedImage = null;
                        imageBase64 = "";
                        loadedFilePath = "";
                        fileNameLabel.setText("Error al cargar imagen");
                        clearFileButton.setEnabled(false);
                        viewFileButton.setEnabled(false);
                        imagePreviewPanel.setVisible(false);
                        setStatus("Error al cargar imagen", false);
                        showError("Error al cargar imagen", e.getCause().getMessage());
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
        if (prompt.isEmpty() && !webSearchCheckbox.isSelected()) {
            showError("Error", "El prompt no puede estar vacío");
            return;
        }

        // Si la búsqueda web está habilitada, permita un prompt vacío
        if (prompt.isEmpty() && webSearchCheckbox.isSelected()) {
            prompt = "Por favor analiza y responde basándote en esta información.";
        }

        String model = (String) modelComboBox.getSelectedItem();
        if (model == null || model.isEmpty()) {
            showError("Error", "Debes seleccionar un modelo");
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
        } catch (NumberFormatException e) {
            showError("Error en parámetros", "Los parámetros deben ser números válidos");
            return;
        }

        // Determinar el tipo de consulta (texto o multimodal)
        final boolean isMultimodalQuery = multimodalCheckbox.isSelected() && loadedImage != null;
        final boolean isWebSearchEnabled = webSearchCheckbox.isSelected();

        // Verificar que la búsqueda web tenga una consulta
        if (isWebSearchEnabled) {
            String searchQuery = webSearchQueryField.getText().trim();
            if (searchQuery.isEmpty()) {
                showError("Error", "Debe especificar una consulta de búsqueda cuando la búsqueda web está habilitada");
                return;
            }
        }

        // Configurar prompt final
        final String basePrompt = fileContent.isEmpty() ? prompt : "Archivo:\n\n" + fileContent + "\n\nPrompt:\n\n" + prompt;

        // Variable para el prompt final (se modificará si la búsqueda web está habilitada)
        final String[] finalTextPrompt = {basePrompt};

        // Capturar el valor final de prompt en una variable final para uso en el SwingWorker
        final String finalPrompt = prompt;

        // Enviar consulta
        isRequestInProgress = true;
        setStatus("Enviando consulta a Ollama (" + model + ")...", true);
        sendButton.setEnabled(false);
        saveResponseButton.setEnabled(false);

        // Ocultar imagen de respuesta anterior si la hay
        responseImageLabel.setVisible(false);
        responseImageLabel.setIcon(null);
        responseImage = null;

        SwingWorker<Map<String, Object>, String> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                // Limpiar el área de respuesta
                publish("");

                // Si la búsqueda web está habilitada, realizar búsqueda
                if (isWebSearchEnabled && !isMultimodalQuery) {
                    publish("Realizando búsqueda web...");

                    // Determinar la consulta de búsqueda
                    String searchQuery = webSearchQueryField.getText().trim();

                    try {
                        // Realizar búsqueda
                        List<WebSearchService.SearchResult> searchResults = webSearchService.search(searchQuery);

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

                    // Guardar en el historial
                    Map<String, String> historyItem = new HashMap<>();
                    historyItem.put("model", model);
                    historyItem.put("prompt", isMultimodalQuery ? finalPrompt + " [IMAGEN]" :
                            (isWebSearchEnabled ? finalPrompt + " [CON BÚSQUEDA WEB]" : finalTextPrompt[0]));
                    historyItem.put("response", currentResponse);
                    historyItem.put("hasImage", Boolean.toString(responseImage != null));
                    historyItem.put("hasWebSearch", Boolean.toString(isWebSearchEnabled));
                    conversationHistory.add(historyItem);

                    setStatus("Respuesta recibida", false);
                    saveResponseButton.setEnabled(true);
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error al procesar la consulta", e);
                    setStatus("Error al procesar la consulta", false);
                    showError("Error de comunicación",
                            "No se pudo procesar la consulta: " + e.getCause().getMessage());
                } finally {
                    isRequestInProgress = false;
                    sendButton.setEnabled(true);
                }
            }
        };

        worker.execute();
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
            imageDataResponse = dataUri;
            logger.info("Imagen encontrada en respuesta markdown");
        }
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
                showError("Error al guardar", "No se pudo guardar la respuesta: " + e.getMessage());
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
     * Muestra un mensaje de error
     */
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
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
        // Verificar si los modelos están en caché
        if (!modelCache.isEmpty()) {
            return new ArrayList<>(modelCache.keySet());
        }

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
                modelCache.put(modelName, modelName);  // Añadir a caché
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

        // Formato de contenido para gemma3
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

        // Enviar petición
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Procesar respuesta
        StringBuilder textResponse = new StringBuilder();
        String imageDataResponse = null;

        try {
            // Primero registramos el JSON enviado para depuración
            String jsonRequestString = jsonRequest.toString();
            logger.info("Enviando solicitud JSON: {}", jsonRequestString);

            // Enviar petición
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequestString.getBytes(StandardCharsets.UTF_8);
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
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        // Algunas líneas pueden no ser JSON válido
                        logger.warn("Error al parsear respuesta JSON: {}", e.getMessage());
                    }
                }
            }
        } finally {
            connection.disconnect();
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
    }

    /**
     * Guarda la conversación en el historial
     */
    private void saveToHistory(String model, String prompt, String response) {
        Map<String, String> conversation = new HashMap<>();
        conversation.put("timestamp", new Date().toString());
        conversation.put("model", model);
        conversation.put("prompt", prompt);
        conversation.put("response", response);

        conversationHistory.add(conversation);
        logger.info("Conversación guardada en historial. Total: {}", conversationHistory.size());
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
            StyleConstants.setBackground(codeStyle, new Color(230, 230, 230));
            StyleConstants.setFontFamily(codeStyle, "Monospaced");
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
    }

    /**
     * Aplica coloreado de sintaxis básico para archivos de código.
     * Para este ejemplo, se establece la fuente a monoespaciada.
     * Se puede ampliar para resaltar palabras clave según el lenguaje.
     */
    private void applySyntaxHighlighting(JTextPane textPane, String code, String extension) {
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        // Aquí se podría implementar un resaltado sintáctico más avanzado según el tipo de archivo.
    }
}