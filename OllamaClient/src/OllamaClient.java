import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class OllamaClient {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Cliente Java para Ollama");
        System.out.println("========================");

        while (true) {
            System.out.println("\nEscribe 'salir' para terminar el programa");
            System.out.print("Nombre del modelo (ej. llama2): ");
            String model = scanner.nextLine();

            if (model.equalsIgnoreCase("salir")) {
                break;
            }

            System.out.print("Prompt: ");
            String prompt = scanner.nextLine();

            if (prompt.equalsIgnoreCase("salir")) {
                break;
            }

            try {
                sendPromptToOllama(model, prompt);
            } catch (IOException e) {
                System.out.println("Error al conectar con Ollama: " + e.getMessage());
                System.out.println("Asegúrate de que Ollama está ejecutándose en http://localhost:11434");
            }
        }

        scanner.close();
        System.out.println("¡Hasta luego!");
    }

    private static void sendPromptToOllama(String model, String prompt) throws IOException {
        // Crear conexión HTTP
        URL url = new URL(OLLAMA_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Crear JSON para el request
        String jsonRequest = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":true}",
                model,
                prompt.replace("\"", "\\\"")  // Escapar comillas dobles
        );

        // Enviar petición
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Procesar respuesta
        System.out.println("\nRespuesta de Ollama:");
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