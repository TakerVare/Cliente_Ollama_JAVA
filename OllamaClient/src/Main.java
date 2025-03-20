package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente Java para interactuar con la API de Ollama
 *
 * Esta aplicación permite enviar prompts a modelos de lenguaje
 * alojados localmente en Ollama, con soporte para lectura de
 * archivos en varios formatos e imágenes.
 */
public class Main {
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

    // Cache de modelos
    private static final Map<String, String> modelCache = new ConcurrentHashMap<>();

    // Logger para registro de errores y eventos
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Historial de conversaciones
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

    public static void main(String[] args) {
        logger.info("Iniciando Cliente Java para Ollama");

        OllamaGUIClient gui = new OllamaGUIClient();

        logger.info("Finalizando Cliente Java para Ollama");
        System.out.println("¡Hasta luego!");
    }
}