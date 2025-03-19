package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente Java para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos.
 */
public class Main {
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
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Historial de conversaciones
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

    public static void main(String[] args) {
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

        }

        logger.info("Finalizando Cliente Java para Ollama");
        System.out.println("¡Hasta luego!");
    }

    private static void showMainMenu(Scanner scanner) throws IOException {

        // Mostrar modelos disponibles
        System.out.println("\n1. Ejecutar en terminal:");
        System.out.println("\n2. Ejecutar en entorno gráfico:");
        System.out.println("\nEscribe 'salir' para terminar el programa");

        System.out.print("Introduce el número de la lista o 'salir': ");
        String modoTrabajo = scanner.nextLine();

        if (modoTrabajo.equalsIgnoreCase("salir")) {
            return;
        }
        if (modoTrabajo.equalsIgnoreCase("1")){
            OllamaClient.ejecutar();
        }
        if (modoTrabajo.equalsIgnoreCase("2")){
            OllamaGUIClient gui = new OllamaGUIClient();
        }
        //OllamaGUIClient()

    }




}