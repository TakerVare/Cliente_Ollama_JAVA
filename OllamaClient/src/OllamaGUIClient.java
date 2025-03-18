package OllamaClient.src;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente Java con GUI para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos y una interfaz gráfica de usuario.
 */
public class OllamaGUIClient extends JFrame {
    // URLs de la API
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODELS_URL = "http://localhost:11434/api/tags";

    // Configuración por defecto
    private static final Map<String, Float> DEFAULT_PARAMETERS = new HashMap<String, Float>() {{
        put("temperature", 0.7f);
        put("top_p", 0.9f);
        put("max_tokens", 2000f);
    }};

    // Cache de modelos
    private static final Map<String, String> modelCache = new ConcurrentHashMap<>();

    // Logger para registro de errores y eventos
    private static final Logger logger = LoggerFactory.getLogger(OllamaGUIClient.class);

    // Historial de conversaciones
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

    // Componentes de la interfaz gráfica
    private JComboBox<String> modelComboBox;
    private JTextField temperatureField;
    private JTextField topPField;
    private JTextField maxTokensField;
    private JButton loadFileButton;
    private JButton clearFileButton;
    private JLabel fileNameLabel;
    private JTextArea promptTextArea;
    private JTextPane responseTextPane;
    private JButton sendButton;
    private JButton saveResponseButton;
    private JButton viewFileButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // Estado de la aplicación
    private String fileContent = "";
    private String loadedFilePath = "";
    private String currentResponse = "";
    private boolean isRequestInProgress = false;

    /**
     * Constructor principal
     */
    public OllamaGUIClient() {
        super("Cliente GUI para Ollama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));

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
        temperatureField = new JTextField("0.7", 5);
        temperatureField.setToolTipText("Temperatura (0.0-1.0)");
        topPField = new JTextField("0.9", 5);
        topPField.setToolTipText("Top P (0.0-1.0)");
        maxTokensField = new JTextField("2000", 5);
        maxTokensField.setToolTipText("Número máximo de tokens");

        // Carga de archivos
        loadFileButton = new JButton("Cargar archivo");
        loadFileButton.setToolTipText("Cargar un archivo de texto, PDF o DOCX");
        clearFileButton = new JButton("Eliminar");
        clearFileButton.setToolTipText("Eliminar el archivo cargado");
        clearFileButton.setEnabled(false);
        fileNameLabel = new JLabel("Sin archivo cargado");
        viewFileButton = new JButton("Ver");
        viewFileButton.setToolTipText("Ver el contenido del archivo cargado");
        viewFileButton.setEnabled(false);

        // Área de prompt
        promptTextArea = new JTextArea(5, 20);
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        promptTextArea.setToolTipText("Escribe tu prompt aquí");

        // Área de respuesta con soporte para coloreado de sintaxis
        responseTextPane = new JTextPane();
        responseTextPane.setEditable(false);
        responseTextPane.setContentType("text/plain");

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
        topPanel.add(modelPanel, BorderLayout.NORTH);

        // Parámetros
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        paramsPanel.add(new JLabel("Temperatura:"));
        paramsPanel.add(temperatureField);
        paramsPanel.add(new JLabel("Top P:"));
        paramsPanel.add(topPField);
        paramsPanel.add(new JLabel("Max tokens:"));
        paramsPanel.add(maxTokensField);
        topPanel.add(paramsPanel, BorderLayout.CENTER);

        // Panel de carga de archivos
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        JPanel fileButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fileButtonsPanel.add(loadFileButton);
        fileButtonsPanel.add(clearFileButton);
        fileButtonsPanel.add(viewFileButton);
        filePanel.add(fileButtonsPanel, BorderLayout.WEST);
        filePanel.add(fileNameLabel, BorderLayout.CENTER);
        topPanel.add(filePanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Panel central: área de prompt y respuesta
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3);
        splitPane.setOneTouchExpandable(true);

        // Panel de prompt
        JPanel promptPanel = new JPanel(new BorderLayout(5, 5));
        promptPanel.add(new JLabel("Prompt:"), BorderLayout.NORTH);
        promptPanel.add(new JScrollPane(promptTextArea), BorderLayout.CENTER);
        splitPane.setTopComponent(promptPanel);

        // Panel de respuesta
        JPanel responsePanel = new JPanel(new BorderLayout(5, 5));
        responsePanel.add(new JLabel("Respuesta:"), BorderLayout.NORTH);
        responsePanel.add(new JScrollPane(responseTextPane), BorderLayout.CENTER);
        splitPane.setBottomComponent(responsePanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

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

        // Botón de eliminar archivo
        clearFileButton.addActionListener(e -> clearFile());

        // Botón de ver archivo
        viewFileButton.addActionListener(e -> viewFile());

        // Botón de enviar consulta
        sendButton.addActionListener(e -> sendQuery());

        // Botón de guardar respuesta
        saveResponseButton.addActionListener(e -> saveResponse());

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
     * Elimina el archivo cargado
     */
    private void clearFile() {
        fileContent = "";
        loadedFilePath = "";
        fileNameLabel.setText("Sin archivo cargado");
        clearFileButton.setEnabled(false);
        viewFileButton.setEnabled(false);
        setStatus("Archivo eliminado", false);
    }

    /**
     * Muestra el contenido del archivo cargado
     */
    private void viewFile() {
        if (fileContent.isEmpty()) {
            return;
        }

        JDialog dialog = new JDialog(this, "Contenido del archivo: " + new File(loadedFilePath).getName(), true);
        dialog.setSize(700, 500);
        dialog.setLayout(new BorderLayout(10, 10));

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
        if (prompt.isEmpty()) {
            showError("Error", "El prompt no puede estar vacío");
            return;
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

        // MODIFICACIÓN AQUÍ: Hacer finalPrompt efectivamente final
        final String finalPrompt = fileContent.isEmpty()
                ? prompt
                : "Archivo:\n\n" + fileContent + "\n\nPrompt:\n\n" + prompt;

        // Enviar consulta
        isRequestInProgress = true;
        setStatus("Enviando consulta a Ollama (" + model + ")...", true);
        sendButton.setEnabled(false);
        saveResponseButton.setEnabled(false);

        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // Limpiar el área de respuesta
                publish("");

                // Enviar el prompt a Ollama
                return sendPromptToOllama(model, finalPrompt, parameters);
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
                    currentResponse = get();

                    // Aplicar coloreado de sintaxis
                    highlightCodeBlocks(responseTextPane, currentResponse);

                    // Guardar en el historial
                    saveToHistory(model, finalPrompt, currentResponse);

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
     * Guarda la respuesta en un archivo
     */
    private void saveResponse() {
        if (currentResponse.isEmpty()) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar respuesta");

        // Sugerir un nombre de archivo basado en la fecha
        String suggestedName = "respuesta_" + new Date().getTime() + ".txt";
        fileChooser.setSelectedFile(new File(suggestedName));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                try (FileWriter writer = new FileWriter(selectedFile)) {
                    writer.write(currentResponse);
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
     * Envía un prompt a Ollama y procesa la respuesta
     */
    private String sendPromptToOllama(String model, String prompt, Map<String, Float> parameters) throws IOException {
        logger.info("Enviando prompt al modelo: {}", model);

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        // Crear JSON para el request - Escapar caracteres especiales para JSON
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        // Construir JSON con parámetros
        StringBuilder jsonRequest = new StringBuilder();
        jsonRequest.append("{");
        jsonRequest.append("\"model\":\"").append(model).append("\",");
        jsonRequest.append("\"prompt\":\"").append(escapedPrompt).append("\",");

        // Añadir parámetros configurables
        for (Map.Entry<String, Float> param : parameters.entrySet()) {
            jsonRequest.append("\"").append(param.getKey()).append("\":");
            jsonRequest.append(param.getValue()).append(",");
        }

        jsonRequest.append("\"stream\":true");
        jsonRequest.append("}");

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

                // Extraer el texto de la respuesta JSON
                if (line.contains("\"response\":")) {
                    try {
                        int start = line.indexOf("\"response\":\"") + 12;
                        int end = line.indexOf("\"", start);
                        if (end > start) {
                            String responsePart = line.substring(start, end);
                            // Manejar caracteres escapados en la respuesta
                            responsePart = responsePart
                                    .replace("\\n", "\n")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\");

                            fullResponse.append(responsePart);
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        logger.warn("Error al parsear respuesta", e);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        logger.info("Respuesta recibida: {} caracteres", fullResponse.length());
        return fullResponse.toString();
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
