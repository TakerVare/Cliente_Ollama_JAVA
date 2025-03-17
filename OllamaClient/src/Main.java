package OllamaClient.src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Importaciones para PDF
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

// Importaciones para DOCX
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class Main {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODELS_URL = "http://localhost:11434/api/tags";

    public static void main(String[] args) {
        System.out.println("Cliente Java para Ollama");
        System.out.println("========================");
        System.out.println("Nota: Para utilizar las funciones de lectura de PDF y DOCX,");
        System.out.println("necesitas añadir las siguientes dependencias a tu proyecto:");
        System.out.println("- org.apache.pdfbox:pdfbox:2.0.27");
        System.out.println("- org.apache.poi:poi-ooxml:5.2.3");
        System.out.println("========================");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                // Obtener la lista de modelos disponibles
                List<String> availableModels = getAvailableModels();

                if (availableModels.isEmpty()) {
                    System.out.println("\nNo se encontraron modelos en Ollama. Asegúrate de haber descargado al menos un modelo.");
                } else {
                    System.out.println("\nModelos disponibles en Ollama:");
                    for (int i = 0; i < availableModels.size(); i++) {
                        System.out.println((i + 1) + ". " + availableModels.get(i));
                    }
                }

                System.out.println("\nEscribe 'salir' para terminar el programa");
                System.out.print("Nombre del modelo (o número de la lista): ");
                String modelInput = scanner.nextLine();

                if (modelInput.equalsIgnoreCase("salir")) {
                    break;
                }

                String model;
                // Comprobar si el usuario ingresó un número
                if (modelInput.matches("\\d+")) {
                    int modelIndex = Integer.parseInt(modelInput) - 1;
                    if (modelIndex >= 0 && modelIndex < availableModels.size()) {
                        model = availableModels.get(modelIndex);
                    } else {
                        System.out.println("Número inválido. Por favor, selecciona un número de la lista.");
                        continue;
                    }
                } else {
                    model = modelInput;
                }

                System.out.println("\nEscribe tu prompt o indica un archivo usando 'file:/ruta/al/archivo'");
                System.out.println("Formatos soportados: txt, pdf, docx");
                System.out.print("Prompt: ");
                String promptInput = scanner.nextLine();

                if (promptInput.equalsIgnoreCase("salir")) {
                    break;
                }

                String prompt;
                // Verificar si es una ruta de archivo
                if (promptInput.toLowerCase().startsWith("file:")) {
                    String filePath = promptInput.substring(5).trim();
                    try {
                        prompt = readFileContent(filePath);
                        System.out.println("Archivo cargado: " + filePath);
                        System.out.println("Tamaño: " + prompt.length() + " caracteres");
                    } catch (IOException e) {
                        System.out.println("Error al leer el archivo: " + e.getMessage());
                        continue;
                    } catch (UnsupportedOperationException e) {
                        System.out.println("Error: " + e.getMessage());
                        continue;
                    }
                } else {
                    prompt = promptInput;
                }

                sendPromptToOllama(model, prompt);

            } catch (IOException e) {
                System.out.println("Error al conectar con Ollama: " + e.getMessage());
                System.out.println("Asegúrate de que Ollama está ejecutándose en http://localhost:11434");
            }
        }

        scanner.close();
        System.out.println("¡Hasta luego!");
    }

    private static String readFileContent(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("El archivo no existe: " + filePath);
        }

        String extension = getFileExtension(filePath);

        switch (extension.toLowerCase()) {
            case "txt":
                return readTextFile(file);
            case "pdf":
                return readPdfFile(file);
            case "docx":
                return readDocxFile(file);
            default:
                throw new UnsupportedOperationException("Formato de archivo no soportado: " + extension +
                        ". Los formatos soportados son: txt, pdf, docx");
        }
    }

    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

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

    private static String readPdfFile(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file);) {
        //try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (NoClassDefFoundError e) {
            throw new IOException("Error al leer PDF: Falta la dependencia de PDFBox. " +
                    "Añade 'org.apache.pdfbox:pdfbox:2.0.27' a tu proyecto.");
        }
    }

    private static String readDocxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String text = extractor.getText();
            extractor.close();
            return text;

        } catch (NoClassDefFoundError e) {
            throw new IOException("Error al leer DOCX: Falta la dependencia de Apache POI. " +
                    "Añade 'org.apache.poi:poi-ooxml:5.2.3' a tu proyecto.");
        }
    }


    private static List<String> getAvailableModels() throws IOException {
        List<String> models = new ArrayList<>();

        // Crear conexión HTTP
        URL url = new URL(OLLAMA_MODELS_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

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
                models.add(matcher.group(1));
            }
        }

        connection.disconnect();
        return models;
    }

    private static void sendPromptToOllama(String model, String prompt) throws IOException {
        // Crear conexión HTTP
        URL url = new URL(OLLAMA_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Crear JSON para el request - Escapar caracteres especiales para JSON
        String escapedPrompt = prompt
                .replace("\\", "\\\\")  // Escapar barras invertidas primero
                .replace("\"", "\\\"")  // Escapar comillas dobles
                .replace("\n", "\\n")   // Escapar saltos de línea
                .replace("\r", "\\r")   // Escapar retornos de carro
                .replace("\t", "\\t");  // Escapar tabulaciones

        String jsonRequest = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":true}",
                model,
                escapedPrompt
        );

        // Enviar petición
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Procesar respuesta
        System.out.println("\nRespuesta de Ollama (modelo: " + model + "):");
        System.out.println("--------------------");

        // Leer respuesta línea por línea (formato streaming)
        try (Scanner responseScanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder fullResponse = new StringBuilder();
            while (responseScanner.hasNextLine()) {
                String line = responseScanner.nextLine();

                // Extraer el texto de la respuesta JSON (simplificado)
                if (line.contains("\"response\":")) {
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
                        System.out.print(responsePart);
                        fullResponse.append(responsePart);
                    }
                }
            }
            System.out.println("\n");
            System.out.println("Respuesta completa recibida.");
        }

        connection.disconnect();
    }
}