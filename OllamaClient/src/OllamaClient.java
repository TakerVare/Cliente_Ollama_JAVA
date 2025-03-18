package OllamaClient.src;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente Java para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos.
 */
public class OllamaClient {
    // URLs de la API
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODELS_URL = "http://localhost:11434/api/tags";

    // Configuración por defecto
    private static final Map<String, Float> DEFAULT_PARAMETERS = new HashMap<String, Float>() {{
        put("temperature", 0.9f);
        put("top_p", 0.9f);
        put("max_tokens", 1000000f);
    }};

    // Cache de modelos
    private static final Map<String, String> modelCache = new ConcurrentHashMap<>();

    // Logger para registro de errores y eventos
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);

    // Historial de conversaciones
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

    public static void ejecutar() {
        logger.info("Iniciando Cliente Java para Ollama");

        System.out.println("Cliente Java para Ollama");
        System.out.println("========================");
        System.out.println("Nota: Para utilizar las funciones de lectura de PDF y DOCX,");
        System.out.println("necesitas añadir las siguientes dependencias a tu proyecto:");
        System.out.println("- org.apache.pdfbox:pdfbox:2.0.27");
        System.out.println("- org.apache.poi:poi-ooxml:5.2.3");
        System.out.println("- org.slf4j:slf4j-api:1.7.36");
        System.out.println("- ch.qos.logback:logback-classic:1.2.11");
        System.out.println("========================");

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                try {
                    showMainMenu(scanner);
                } catch (IOException e) {
                    logger.error("Error de conexión con Ollama", e);
                    System.out.println("Error al conectar con Ollama: " + e.getMessage());
                    System.out.println("Asegúrate de que Ollama está ejecutándose en http://localhost:11434");
                } catch (Exception e) {
                    logger.error("Error inesperado", e);
                    System.out.println("Error inesperado: " + e.getMessage());
                }

                System.out.println("\n¿Deseas realizar otra consulta? (s/n): ");
                String continueOption = scanner.nextLine().trim().toLowerCase();
                running = continueOption.equals("s") || continueOption.equals("si") || continueOption.equals("sí");
            }
        }

        logger.info("Finalizando Cliente Java para Ollama");
        System.out.println("¡Hasta luego!");
    }

    /**
     * Muestra el menú principal y maneja la entrada del usuario
     */
    private static void showMainMenu(Scanner scanner) throws IOException {
        // Obtener la lista de modelos disponibles
        List<String> availableModels = getAvailableModels();

        if (availableModels.isEmpty()) {
            System.out.println("\nNo se encontraron modelos en Ollama. Asegúrate de haber descargado al menos un modelo.");
            return;
        }

        // Mostrar modelos disponibles
        System.out.println("\nModelos disponibles en Ollama:");
        for (int i = 0; i < availableModels.size(); i++) {
            System.out.println((i + 1) + ". " + availableModels.get(i));
        }

        System.out.println("\nEscribe 'salir' para terminar el programa");
        System.out.print("Nombre del modelo (o número de la lista): ");
        String modelInput = scanner.nextLine();

        if (modelInput.equalsIgnoreCase("salir")) {
            return;
        }

        // Selección de modelo
        String model = selectModel(modelInput, availableModels);
        if (model == null) {
            System.out.println("Modelo no válido. Por favor, intenta de nuevo.");
            return;
        }

        // Configuración de parámetros
        Map<String, Float> parameters = configureParameters(scanner);

        // Cargar archivo (opcional)
        String fileContent = handleFileLoading(scanner);

        // Solicitar prompt de texto
        System.out.println("\nIntroduce tu prompt de texto:");
        String textPrompt = scanner.nextLine();

        if (textPrompt.equalsIgnoreCase("salir")) {
            return;
        }

        // Combinar contenido y enviar prompt
        String finalPrompt = combinePrompt(fileContent, textPrompt);

        // Enviar el prompt a Ollama
        String response = sendPromptToOllama(model, finalPrompt, parameters);

        // Guardar en el historial
        saveToHistory(model, finalPrompt, response);

        // Opción para guardar la respuesta
        offerToSaveResponse(scanner, response);
    }

    /**
     * Selecciona un modelo de la lista de disponibles
     */
    private static String selectModel(String modelInput, List<String> availableModels) {
        // Comprobar si el usuario ingresó un número
        if (modelInput.matches("\\d+")) {
            int modelIndex = Integer.parseInt(modelInput) - 1;
            if (modelIndex >= 0 && modelIndex < availableModels.size()) {
                return availableModels.get(modelIndex);
            } else {
                logger.warn("Selección de modelo inválida: {}", modelInput);
                return null;
            }
        } else {
            // Verificar si el modelo existe
            if (availableModels.contains(modelInput)) {
                return modelInput;
            } else {
                logger.warn("Modelo no encontrado: {}", modelInput);
                return null;
            }
        }
    }

    /**
     * Configura los parámetros para la generación
     */
    private static Map<String, Float> configureParameters(Scanner scanner) {
        Map<String, Float> parameters = new HashMap<>(DEFAULT_PARAMETERS);

        System.out.println("\n¿Deseas configurar parámetros avanzados? (s/n): ");
        String configOption = scanner.nextLine().trim().toLowerCase();

        if (configOption.equals("s") || configOption.equals("si") || configOption.equals("sí")) {
            try {
                System.out.print("Temperatura (0.0-1.0, default 0.9): ");
                String tempInput = scanner.nextLine().trim();
                if (!tempInput.isEmpty()) {
                    parameters.put("temperature", Float.parseFloat(tempInput));
                }

                System.out.print("Top P (0.0-1.0, default 0.9): ");
                String topPInput = scanner.nextLine().trim();
                if (!topPInput.isEmpty()) {
                    parameters.put("top_p", Float.parseFloat(topPInput));
                }

                System.out.print("Tokens máximos (default 1000000): ");
                String maxTokensInput = scanner.nextLine().trim();
                if (!maxTokensInput.isEmpty()) {
                    parameters.put("max_tokens", Float.parseFloat(maxTokensInput));
                }
            } catch (NumberFormatException e) {
                logger.warn("Error al parsear parámetros", e);
                System.out.println("Error en formato de número, usando valores por defecto.");
                return DEFAULT_PARAMETERS;
            }
        }

        return parameters;
    }

    /**
     * Maneja la carga de archivos
     */
    private static String handleFileLoading(Scanner scanner) {
        System.out.println("\n¿Deseas cargar un archivo? (s/n): ");
        String loadFileOption = scanner.nextLine().trim().toLowerCase();

        if (loadFileOption.equals("s") || loadFileOption.equals("si") || loadFileOption.equals("sí")) {
            System.out.println("Introduce la ruta del archivo:");
            System.out.println("Formatos soportados: txt, pdf, docx");
            String filePath = scanner.nextLine().trim();

            try {
                String fileContent = readFileContent(filePath);
                System.out.println("Archivo cargado: " + filePath);
                System.out.println("Tamaño: " + fileContent.length() + " caracteres");

                // Mostrar el contenido del archivo
                System.out.println("\n¿Deseas ver el contenido del archivo? (s/n): ");
                String showContentOption = scanner.nextLine().trim().toLowerCase();
                if (showContentOption.equals("s") || showContentOption.equals("si") || showContentOption.equals("sí")) {
                    displayCodeWithFormatting(fileContent);
                }

                return fileContent;
            } catch (IOException e) {
                logger.error("Error al leer archivo", e);
                System.out.println("Error al leer el archivo: " + e.getMessage());
            } catch (UnsupportedOperationException e) {
                logger.error("Formato de archivo no soportado", e);
                System.out.println("Error: " + e.getMessage());
            }
        }

        return "";
    }

    /**
     * Combina el contenido del archivo con el prompt
     */
    private static String combinePrompt(String fileContent, String textPrompt) {
        if (!fileContent.isEmpty()) {
            return "Archivo:\n\n" + fileContent + "\n\nPrompt:\n\n" + textPrompt;
        } else {
            return textPrompt;
        }
    }

    /**
     * Guarda la conversación en el historial
     */
    private static void saveToHistory(String model, String prompt, String response) {
        Map<String, String> conversation = new HashMap<>();
        conversation.put("timestamp", new Date().toString());
        conversation.put("model", model);
        conversation.put("prompt", prompt);
        conversation.put("response", response);

        conversationHistory.add(conversation);
        logger.info("Conversación guardada en historial. Total: {}", conversationHistory.size());
    }

    /**
     * Ofrece la opción de guardar la respuesta en un archivo
     */
    private static void offerToSaveResponse(Scanner scanner, String response) {
        System.out.println("\n¿Deseas guardar la respuesta en un archivo? (s/n): ");
        String saveOption = scanner.nextLine().trim().toLowerCase();

        if (saveOption.equals("s") || saveOption.equals("si") || saveOption.equals("sí")) {
            System.out.print("Nombre del archivo: ");
            String fileName = scanner.nextLine().trim();

            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(response);
                System.out.println("Respuesta guardada en: " + fileName);
                logger.info("Respuesta guardada en archivo: {}", fileName);
            } catch (IOException e) {
                logger.error("Error al guardar respuesta", e);
                System.out.println("Error al guardar la respuesta: " + e.getMessage());
            }
        }
    }

    /**
     * Muestra el código con formato
     */
    private static void displayCodeWithFormatting(String code) {
        System.out.println("\n```");
        System.out.println(code);
        System.out.println("```\n");
    }

    /**
     * Lee el contenido de un archivo según su extensión
     */
    private static String readFileContent(String filePath) throws IOException {
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
                    return readTextFile(file);
                case "pdf":
                    return readPdfFile(file);
                case "docx":
                    return readDocxFile(file);
                default:
                    logger.warn("Formato no soportado: {}", extension);
                    throw new UnsupportedOperationException("Formato de archivo no soportado: " + extension +
                            ". Los formatos soportados son: txt, pdf, docx");
            }
        } catch (Exception e) {
            logger.error("Error al leer archivo", e);
            throw new IOException("Error al leer el archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * Lee un archivo de texto
     */
    private static String readTextFile(File file) throws IOException {
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
    private static String readPdfFile(File file) throws IOException {
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
    private static String readDocxFile(File file) throws IOException {
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
    private static List<String> getAvailableModels() throws IOException {
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
    private static String sendPromptToOllama(String model, String prompt, Map<String, Float> parameters) throws IOException {
        logger.info("Enviando prompt al modelo: {}", model);

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        // Construir el JSON de forma segura sin escapar manualmente
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
        System.out.println("\nRespuesta de Ollama (modelo: " + model + "):");
        System.out.println("--------------------");

        Thread progressThread = startProgressIndicator();
        StringBuilder fullResponse = new StringBuilder();
        try (Scanner responseScanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (responseScanner.hasNextLine()) {
                String line = responseScanner.nextLine();
                if (line.contains("\"response\":")) {
                    try {
                        int start = line.indexOf("\"response\":\"") + 12;
                        int end = line.indexOf("\"", start);
                        if (end > start) {
                            String responsePart = line.substring(start, end);
                            // Se convierten los caracteres escapados a sus representaciones reales
                            responsePart = responsePart
                                    .replace("\\n", "\n")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\");
                            System.out.print(responsePart);
                            fullResponse.append(responsePart);
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        logger.warn("Error al parsear respuesta", e);
                    }
                }
            }
            System.out.println("\n");
            System.out.println("Respuesta completa recibida.");
        } finally {
            progressThread.interrupt();
            connection.disconnect();
        }

        logger.info("Respuesta recibida: {} caracteres", fullResponse.length());
        return fullResponse.toString();
    }


    /**
     * Muestra un indicador de progreso mientras se espera la respuesta
     */
    private static Thread startProgressIndicator() {
        Thread thread = new Thread(() -> {
            String[] animation = new String[]{"|", "/", "-", "\\"};
            int i = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.print("\r" + animation[i % animation.length] + " Procesando...");
                    i++;
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // Limpiar línea al finalizar
                System.out.print("\r                     \r");
            }
        });

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Clase personalizada para manejar errores específicos de la aplicación
     */
    public static class OllamaClientException extends Exception {
        public OllamaClientException(String message) {
            super(message);
        }

        public OllamaClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}