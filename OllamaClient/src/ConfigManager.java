package OllamaClient.src;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Administrador de configuración para la aplicación
 * Permite guardar, cargar y gestionar preferencias del usuario
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final ConfigManager instance = new ConfigManager();

    // Rutas de configuración
    private final String configDir;
    private final String mainConfigPath;
    private final String searchConfigPath;
    private final String themeConfigPath;
    private final String apiKeysConfigPath;

    // Parámetros de configuración
    private JSONObject mainConfig;
    private JSONObject searchConfig;
    private JSONObject themeConfig;
    private JSONObject apiKeysConfig;

    // Configuraciones predeterminadas como JSON
    private static JSONObject DEFAULT_MAIN_CONFIG;
    private static JSONObject DEFAULT_SEARCH_CONFIG;
    private static JSONObject DEFAULT_THEME_CONFIG;

    static {
        System.out.println("Starting ConfigManager static initialization");
        try {
            // Inicializar configuraciones predeterminadas
            System.out.println("Creating DEFAULT_MAIN_CONFIG");
            DEFAULT_MAIN_CONFIG = new JSONObject();
            System.out.println("Adding lastUsedModel");
            DEFAULT_MAIN_CONFIG.put("lastUsedModel", "");
            System.out.println("Adding temperature");
            DEFAULT_MAIN_CONFIG.put("temperature", 0.9f);
            System.out.println("Adding topP");
            DEFAULT_MAIN_CONFIG.put("topP", 0.9f);
            System.out.println("Adding maxTokens");
            DEFAULT_MAIN_CONFIG.put("maxTokens", 1000000);
            System.out.println("Adding multimodalEnabled");
            DEFAULT_MAIN_CONFIG.put("multimodalEnabled", false);
            System.out.println("Adding recentFiles");
            DEFAULT_MAIN_CONFIG.put("recentFiles", new JSONArray());
            System.out.println("Adding windowWidth");
            DEFAULT_MAIN_CONFIG.put("windowWidth", 1100);
            System.out.println("Adding windowHeight");
            DEFAULT_MAIN_CONFIG.put("windowHeight", 700);
            System.out.println("Adding splitterPosition");
            DEFAULT_MAIN_CONFIG.put("splitterPosition", 250);
            System.out.println("Adding autoSaveEnabled");
            DEFAULT_MAIN_CONFIG.put("autoSaveEnabled", true);

            System.out.println("Creating DEFAULT_SEARCH_CONFIG");
            DEFAULT_SEARCH_CONFIG = new JSONObject();
            DEFAULT_SEARCH_CONFIG.put("searchAPIProvider", "DUCKDUCKGO");
            DEFAULT_SEARCH_CONFIG.put("maxResults", 5);
            DEFAULT_SEARCH_CONFIG.put("webSearchEnabled", false);
            DEFAULT_SEARCH_CONFIG.put("includeWebImagesInSearch", false);
            DEFAULT_SEARCH_CONFIG.put("includeNewsInSearch", true);
            DEFAULT_SEARCH_CONFIG.put("includeVideosInSearch", false);
            DEFAULT_SEARCH_CONFIG.put("preferredLanguage", "es");
            DEFAULT_SEARCH_CONFIG.put("safeSearch", true);
            DEFAULT_SEARCH_CONFIG.put("timeLimit", "all");

            System.out.println("Creating DEFAULT_THEME_CONFIG");
            DEFAULT_THEME_CONFIG = new JSONObject();
            DEFAULT_THEME_CONFIG.put("themeName", "Light");
            DEFAULT_THEME_CONFIG.put("customFontSize", 12);
            DEFAULT_THEME_CONFIG.put("customFontFamily", "SansSerif");
            DEFAULT_THEME_CONFIG.put("customAccentColor", "#3366CC");
            DEFAULT_THEME_CONFIG.put("customBackgroundColor", "#FFFFFF");
            DEFAULT_THEME_CONFIG.put("enableDarkMode", false);
            DEFAULT_THEME_CONFIG.put("enableHighContrast", false);
            DEFAULT_THEME_CONFIG.put("codeBlockColor", "#F0F0F0");
            DEFAULT_THEME_CONFIG.put("responseTextColor", "#000000");
            DEFAULT_THEME_CONFIG.put("syntaxHighlighting", true);

            System.out.println("ConfigManager static initialization completed successfully");
        } catch (Exception e) {
            System.err.println("Error in ConfigManager static block: " + e);
            e.printStackTrace();

            // Crear objetos vacíos para prevenir NullPointerException
            DEFAULT_MAIN_CONFIG = new JSONObject("{}");
            DEFAULT_SEARCH_CONFIG = new JSONObject("{}");
            DEFAULT_THEME_CONFIG = new JSONObject("{}");
        }
    }

    /**
     * Constructor privado para Singleton
     */
    private ConfigManager() {
        // Configurar directorio de configuración
        String userHome = System.getProperty("user.home");
        this.configDir = userHome + File.separator + ".ollamaclient";

        // Crear directorio si no existe
        File dir = new File(configDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("Directorio de configuración creado: {}", configDir);
            } else {
                logger.error("No se pudo crear el directorio de configuración: {}", configDir);
            }
        }

        // Definir rutas de archivos de configuración
        this.mainConfigPath = configDir + File.separator + "config.json";
        this.searchConfigPath = configDir + File.separator + "search_config.json";
        this.themeConfigPath = configDir + File.separator + "theme_config.json";
        this.apiKeysConfigPath = configDir + File.separator + "api_keys.json";

        // Inicializar objetos JSON
        this.mainConfig = new JSONObject();
        this.searchConfig = new JSONObject();
        this.themeConfig = new JSONObject();
        this.apiKeysConfig = new JSONObject();

        // Cargar configuraciones
        loadAllConfigurations();
    }

    /**
     * Obtiene la instancia única del administrador de configuración
     */
    public static ConfigManager getInstance() {
        return instance;
    }

    /**
     * Carga todas las configuraciones
     */
    public void loadAllConfigurations() {
        // Cargar cada configuración por separado
        loadMainConfig();
        loadSearchConfig();
        loadThemeConfig();
        loadApiKeysConfig();

        logger.info("Todas las configuraciones cargadas");
    }

    /**
     * Carga la configuración principal
     */
    private void loadMainConfig() {
        // Inicializar con valores predeterminados
        if (DEFAULT_MAIN_CONFIG != null) {
            mainConfig = new JSONObject(DEFAULT_MAIN_CONFIG.toString());
        } else {
            // Fallback if DEFAULT_MAIN_CONFIG is null
            mainConfig = new JSONObject();
            mainConfig.put("lastUsedModel", "");
            mainConfig.put("temperature", 0.9f);
            mainConfig.put("topP", 0.9f);
            mainConfig.put("maxTokens", 1000000);
            mainConfig.put("multimodalEnabled", false);
            mainConfig.put("recentFiles", new JSONArray());
            mainConfig.put("windowWidth", 1100);
            mainConfig.put("windowHeight", 700);
            mainConfig.put("splitterPosition", 250);
            mainConfig.put("autoSaveEnabled", true);
            logger.warn("Using fallback configuration because DEFAULT_MAIN_CONFIG is null");
        }

        // Intentar cargar desde archivo
        loadConfigFromFile(mainConfigPath, mainConfig);
    }

    /**
     * Carga la configuración de búsqueda
     */
    private void loadSearchConfig() {
        // Inicializar con valores predeterminados
        if (DEFAULT_SEARCH_CONFIG != null) {
            searchConfig = new JSONObject(DEFAULT_SEARCH_CONFIG.toString());
        } else {
            // Fallback if DEFAULT_SEARCH_CONFIG is null
            searchConfig = new JSONObject();
            searchConfig.put("searchAPIProvider", "DUCKDUCKGO");
            searchConfig.put("maxResults", 5);
            searchConfig.put("webSearchEnabled", false);
            searchConfig.put("includeWebImagesInSearch", false);
            searchConfig.put("includeNewsInSearch", true);
            searchConfig.put("includeVideosInSearch", false);
            searchConfig.put("preferredLanguage", "es");
            searchConfig.put("safeSearch", true);
            searchConfig.put("timeLimit", "all");
            logger.warn("Using fallback configuration because DEFAULT_SEARCH_CONFIG is null");
        }

        // Intentar cargar desde archivo
        loadConfigFromFile(searchConfigPath, searchConfig);
    }

    /**
     * Carga la configuración de tema
     */
    private void loadThemeConfig() {
        // Inicializar con valores predeterminados
        if (DEFAULT_THEME_CONFIG != null) {
            themeConfig = new JSONObject(DEFAULT_THEME_CONFIG.toString());
        } else {
            // Fallback if DEFAULT_THEME_CONFIG is null
            themeConfig = new JSONObject();
            themeConfig.put("themeName", "Light");
            themeConfig.put("customFontSize", 12);
            themeConfig.put("customFontFamily", "SansSerif");
            themeConfig.put("customAccentColor", "#3366CC");
            themeConfig.put("customBackgroundColor", "#FFFFFF");
            themeConfig.put("enableDarkMode", false);
            themeConfig.put("enableHighContrast", false);
            themeConfig.put("codeBlockColor", "#F0F0F0");
            themeConfig.put("responseTextColor", "#000000");
            themeConfig.put("syntaxHighlighting", true);
            logger.warn("Using fallback configuration because DEFAULT_THEME_CONFIG is null");
        }

        // Intentar cargar desde archivo
        loadConfigFromFile(themeConfigPath, themeConfig);
    }

    /**
     * Carga las claves API
     */
    private void loadApiKeysConfig() {
        // Inicializar objeto vacío
        apiKeysConfig = new JSONObject();

        // Intentar cargar desde archivo
        loadConfigFromFile(apiKeysConfigPath, apiKeysConfig);
    }

    /**
     * Método utilitario para cargar configuración desde un archivo JSON
     */
    private boolean loadConfigFromFile(String filePath, JSONObject configObj) {
        File configFile = new File(filePath);
        if (!configFile.exists()) {
            logger.info("Archivo de configuración no encontrado: {}", filePath);
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            JSONObject loadedJSON = new JSONObject(content);

            // Copiar todas las claves al objeto de configuración
            for (Object key : loadedJSON.keySet()) {
                configObj.put((String)key, loadedJSON.get((String)key));
            }

            logger.info("Configuración cargada desde: {}", filePath);
            return true;

        } catch (Exception e) {
            logger.error("Error al cargar la configuración desde: {}", filePath, e);
            return false;
        }
    }

    /**
     * Guarda todas las configuraciones
     */
    public void saveAllConfigurations() {
        saveMainConfig();
        saveSearchConfig();
        saveThemeConfig();
        saveApiKeysConfig();

        logger.info("Todas las configuraciones guardadas");
    }

    /**
     * Guarda la configuración principal
     */
    public void saveMainConfig() {
        saveConfigToFile(mainConfigPath, mainConfig);
    }

    /**
     * Guarda la configuración de búsqueda
     */
    public void saveSearchConfig() {
        saveConfigToFile(searchConfigPath, searchConfig);
    }

    /**
     * Guarda la configuración de tema
     */
    public void saveThemeConfig() {
        saveConfigToFile(themeConfigPath, themeConfig);
    }

    /**
     * Guarda las claves API
     */
    public void saveApiKeysConfig() {
        saveConfigToFile(apiKeysConfigPath, apiKeysConfig);
    }

    /**
     * Método utilitario para guardar configuración en un archivo JSON
     */
    private void saveConfigToFile(String filePath, JSONObject configObj) {
        try {
            // Escribir JSON al archivo
            try (FileWriter file = new FileWriter(filePath)) {
                file.write(configObj.toString(2)); // Indentación de 2 espacios
                file.flush();
            }

            logger.info("Configuración guardada en: {}", filePath);

        } catch (Exception e) {
            logger.error("Error al guardar configuración en: {}", filePath, e);

            // Notificar el error usando ErrorManager si está disponible
            try {
                ErrorManager.getInstance().handleError(
                        ErrorManager.ErrorCategory.CONFIGURATION,
                        ErrorManager.ErrorSeverity.ERROR,
                        "Error al guardar configuración",
                        "No se pudo guardar la configuración en: " + filePath,
                        e,
                        true
                );
            } catch (Exception ex) {
                // Si ErrorManager no está disponible, registrar en el log
                logger.error("Error al notificar error a través de ErrorManager", ex);
            }
        }
    }

    /**
     * Exporta todas las configuraciones a un archivo
     */
    public void exportConfigurations(File outputFile) {
        try {
            // Crear un objeto JSON principal para todas las configuraciones
            JSONObject allConfigs = new JSONObject();

            // Añadir cada configuración como una sección separada
            allConfigs.put("main", mainConfig);
            allConfigs.put("search", searchConfig);
            allConfigs.put("theme", themeConfig);
            allConfigs.put("apiKeys", apiKeysConfig);

            // Añadir metadatos
            JSONObject metadata = new JSONObject();
            metadata.put("exportDate", System.currentTimeMillis());
            metadata.put("exportVersion", "1.0");
            allConfigs.put("metadata", metadata);

            // Escribir al archivo
            try (FileWriter file = new FileWriter(outputFile)) {
                file.write(allConfigs.toString(2));
                file.flush();
            }

            logger.info("Configuraciones exportadas a: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Error al exportar configuraciones", e);

            // Notificar el error
            try {
                ErrorManager.getInstance().handleError(
                        ErrorManager.ErrorCategory.CONFIGURATION,
                        ErrorManager.ErrorSeverity.ERROR,
                        "Error al exportar configuraciones",
                        "No se pudieron exportar las configuraciones a: " + outputFile.getAbsolutePath(),
                        e,
                        true
                );
            } catch (Exception ex) {
                logger.error("Error al notificar error a través de ErrorManager", ex);
            }
        }
    }

    /**
     * Importa configuraciones desde un archivo
     */
    public boolean importConfigurations(File inputFile) {
        try {
            // Leer el archivo JSON
            String content = new String(Files.readAllBytes(inputFile.toPath()), StandardCharsets.UTF_8);
            JSONObject allConfigs = new JSONObject(content);

            // Verificar versión y metadatos
            if (allConfigs.has("metadata")) {
                JSONObject metadata = allConfigs.getJSONObject("metadata");
                String version = metadata.optString("exportVersion", "unknown");
                logger.info("Importando configuración versión: {}", version);
            }

            // Importar cada sección
            if (allConfigs.has("main")) {
                // Crear un nuevo objeto con el contenido del importado
                JSONObject importedMain = allConfigs.getJSONObject("main");
                mainConfig = new JSONObject(importedMain.toString());
            }

            if (allConfigs.has("search")) {
                JSONObject importedSearch = allConfigs.getJSONObject("search");
                searchConfig = new JSONObject(importedSearch.toString());
            }

            if (allConfigs.has("theme")) {
                JSONObject importedTheme = allConfigs.getJSONObject("theme");
                themeConfig = new JSONObject(importedTheme.toString());
            }

            if (allConfigs.has("apiKeys")) {
                JSONObject importedApiKeys = allConfigs.getJSONObject("apiKeys");
                apiKeysConfig = new JSONObject(importedApiKeys.toString());
            }

            // Guardar las configuraciones importadas
            saveAllConfigurations();
            logger.info("Configuraciones importadas correctamente desde: {}", inputFile.getAbsolutePath());

            return true;

        } catch (Exception e) {
            logger.error("Error al importar configuraciones", e);

            // Notificar el error
            try {
                ErrorManager.getInstance().handleError(
                        ErrorManager.ErrorCategory.CONFIGURATION,
                        ErrorManager.ErrorSeverity.ERROR,
                        "Error al importar configuraciones",
                        "No se pudieron importar las configuraciones desde: " + inputFile.getAbsolutePath(),
                        e,
                        true
                );
            } catch (Exception ex) {
                logger.error("Error al notificar error a través de ErrorManager", ex);
            }

            return false;
        }
    }

    /**
     * Restablece todas las configuraciones a los valores predeterminados
     */
    public void resetToDefaults() {
        // Restablecer cada configuración creando nuevos objetos desde los valores predeterminados
        mainConfig = new JSONObject(DEFAULT_MAIN_CONFIG.toString());
        searchConfig = new JSONObject(DEFAULT_SEARCH_CONFIG.toString());
        themeConfig = new JSONObject(DEFAULT_THEME_CONFIG.toString());
        apiKeysConfig = new JSONObject();

        // Guardar los valores predeterminados
        saveAllConfigurations();

        logger.info("Configuraciones restablecidas a valores predeterminados");
    }

    /**
     * Getter para obtener un valor como String de la configuración principal
     */
    public String getMainConfigString(String key, String defaultValue) {
        return mainConfig.optString(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como int de la configuración principal
     */
    public int getMainConfigInt(String key, int defaultValue) {
        return mainConfig.optInt(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como float de la configuración principal
     */
    public float getMainConfigFloat(String key, float defaultValue) {
        return (float) mainConfig.optDouble(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como boolean de la configuración principal
     */
    public boolean getMainConfigBoolean(String key, boolean defaultValue) {
        return mainConfig.optBoolean(key, defaultValue);
    }

    /**
     * Setter para la configuración principal
     */
    public void setMainConfig(String key, Object value) {
        mainConfig.put(key, value);
        if (mainConfig.optBoolean("autoSaveEnabled", true)) {
            saveMainConfig();
        }
    }

    /**
     * Getter para obtener un valor como String de la configuración de búsqueda
     */
    public String getSearchConfigString(String key, String defaultValue) {
        return searchConfig.optString(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como int de la configuración de búsqueda
     */
    public int getSearchConfigInt(String key, int defaultValue) {
        return searchConfig.optInt(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como boolean de la configuración de búsqueda
     */
    public boolean getSearchConfigBoolean(String key, boolean defaultValue) {
        return searchConfig.optBoolean(key, defaultValue);
    }

    /**
     * Setter para la configuración de búsqueda
     */
    public void setSearchConfig(String key, Object value) {
        searchConfig.put(key, value);
        if (mainConfig.optBoolean("autoSaveEnabled", true)) {
            saveSearchConfig();
        }
    }

    /**
     * Getter para obtener un valor como String de la configuración de tema
     */
    public String getThemeConfigString(String key, String defaultValue) {
        return themeConfig.optString(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como int de la configuración de tema
     */
    public int getThemeConfigInt(String key, int defaultValue) {
        return themeConfig.optInt(key, defaultValue);
    }

    /**
     * Getter para obtener un valor como boolean de la configuración de tema
     */
    public boolean getThemeConfigBoolean(String key, boolean defaultValue) {
        return themeConfig.optBoolean(key, defaultValue);
    }

    /**
     * Setter para la configuración de tema
     */
    public void setThemeConfig(String key, Object value) {
        themeConfig.put(key, value);
        if (mainConfig.optBoolean("autoSaveEnabled", true)) {
            saveThemeConfig();
        }
    }

    /**
     * Getter para obtener una clave API
     */
    public String getApiKey(String apiName) {
        return apiKeysConfig.optString(apiName, "");
    }

    /**
     * Setter para una clave API
     */
    public void setApiKey(String apiName, String apiKey) {
        apiKeysConfig.put(apiName, apiKey);
        if (mainConfig.optBoolean("autoSaveEnabled", true)) {
            saveApiKeysConfig();
        }
    }

    /**
     * Registra un archivo reciente en la configuración
     */
    public void addRecentFile(String filePath) {
        // Obtener lista actual
        JSONArray recentFiles;
        if (mainConfig.has("recentFiles")) {
            recentFiles = mainConfig.getJSONArray("recentFiles");
        } else {
            recentFiles = new JSONArray();
        }

        // Crear una nueva lista para manipular
        List<String> filesList = new ArrayList<>();
        for (int i = 0; i < recentFiles.length(); i++) {
            filesList.add(recentFiles.getString(i));
        }

        // Eliminar si ya existe para evitar duplicados
        filesList.remove(filePath);

        // Añadir al principio de la lista
        filesList.add(0, filePath);

        // Limitar a 10 archivos recientes
        if (filesList.size() > 10) {
            filesList = filesList.subList(0, 10);
        }

        // Crear un nuevo JSONArray con los resultados
        JSONArray newRecentFiles = new JSONArray();
        for (String file : filesList) {
            newRecentFiles.put(file);
        }

        // Actualizar la configuración
        mainConfig.put("recentFiles", newRecentFiles);

        // Guardar si está habilitado el guardado automático
        if (mainConfig.optBoolean("autoSaveEnabled", true)) {
            saveMainConfig();
        }
    }

    /**
     * Obtiene la lista de archivos recientes
     */
    public List<String> getRecentFiles() {
        List<String> result = new ArrayList<>();

        // Verificar si existe la configuración
        if (mainConfig.has("recentFiles")) {
            JSONArray recentFiles = mainConfig.getJSONArray("recentFiles");

            // Convertir JSONArray a List<String>
            for (int i = 0; i < recentFiles.length(); i++) {
                result.add(recentFiles.getString(i));
            }
        }

        return result;
    }

    /**
     * Métodos genéricos para compatibilidad con código existente
     */
    @SuppressWarnings("unchecked")
    public <T> T getMainConfig(String key, T defaultValue) {
        if (defaultValue instanceof String) {
            return (T) getMainConfigString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            Integer value = getMainConfigInt(key, (Integer) defaultValue);
            return (T) value;
        } else if (defaultValue instanceof Float) {
            Float value = getMainConfigFloat(key, (Float) defaultValue);
            return (T) value;
        } else if (defaultValue instanceof Boolean) {
            Boolean value = getMainConfigBoolean(key, (Boolean) defaultValue);
            return (T) value;
        } else if (defaultValue instanceof List) {
            return (T) getRecentFiles(); // Solo para recentFiles
        }

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSearchConfig(String key, T defaultValue) {
        if (defaultValue instanceof String) {
            return (T) getSearchConfigString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            Integer value = getSearchConfigInt(key, (Integer) defaultValue);
            return (T) value;
        } else if (defaultValue instanceof Boolean) {
            Boolean value = getSearchConfigBoolean(key, (Boolean) defaultValue);
            return (T) value;
        }

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T getThemeConfig(String key, T defaultValue) {
        if (defaultValue instanceof String) {
            return (T) getThemeConfigString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            Integer value = getThemeConfigInt(key, (Integer) defaultValue);
            return (T) value;
        } else if (defaultValue instanceof Boolean) {
            Boolean value = getThemeConfigBoolean(key, (Boolean) defaultValue);
            return (T) value;
        }

        return defaultValue;
    }
}