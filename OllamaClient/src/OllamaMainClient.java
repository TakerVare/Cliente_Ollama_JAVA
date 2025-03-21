package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal para iniciar el cliente de Ollama con todas las mejoras implementadas
 * Gestiona la inicialización de recursos y la coordinación de componentes
 */
public class OllamaMainClient {
    private static final Logger logger = LoggerFactory.getLogger(OllamaMainClient.class);

    /**
     * Método principal para iniciar la aplicación
     */
    public static void main(String[] args) {
        // Configurar el sistema de logs
        setupLogging();

        // Verificar si Ollama está en ejecución
        if (!checkOllamaRunning()) {
            showOllamaNotRunningDialog();
            return;
        }

        // Configurar aspecto nativo del sistema operativo
        setupLookAndFeel();

        // Verificar dependencias necesarias
        if (!checkDependencies()) {
            showDependenciesDialog();
            return;
        }

        // Iniciar la aplicación
        SwingUtilities.invokeLater(() -> {
            try {
                ImprovedOllamaGUIClient client = new ImprovedOllamaGUIClient();

                // Mostrar splash screen
                showSplashScreen(client);

                // Mostrar la ventana principal
                client.setVisible(true);

                // Registrar en el log
                logger.info("Aplicación iniciada correctamente");

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

    /**
     * Configura el sistema de registros
     */
    private static void setupLogging() {
        try {
            // Configurar directorio de logs
            String userHome = System.getProperty("user.home");
            File logDir = new File(userHome, ".ollamaclient/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // El resto de la configuración de logging se hace a través de logback.xml
            logger.info("Sistema de logs inicializado en: {}", logDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error al configurar el sistema de logs: " + e.getMessage());
        }
    }

    /**
     * Verifica si Ollama está en ejecución
     */
    private static boolean checkOllamaRunning() {
        try {
            // Intentar conectar con la API de Ollama
            java.net.URL url = new java.net.URL("http://localhost:11434/api/tags");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode == 200;

        } catch (Exception e) {
            logger.error("Error al verificar si Ollama está en ejecución", e);
            return false;
        }
    }

    /**
     * Muestra un diálogo indicando que Ollama no está en ejecución
     */
    private static void showOllamaNotRunningDialog() {
        String message = "No se pudo conectar con Ollama en http://localhost:11434.\n\n" +
                "Asegúrate de que Ollama esté instalado y en ejecución antes de iniciar esta aplicación.\n\n" +
                "1. Descarga Ollama desde: https://ollama.ai/\n" +
                "2. Instala y ejecuta Ollama\n" +
                "3. Inicia esta aplicación nuevamente";

        JOptionPane.showMessageDialog(
                null,
                message,
                "Ollama no está en ejecución",
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Configura el aspecto visual de la aplicación
     */
    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            logger.info("Look and Feel establecido: {}", UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("Error al establecer Look and Feel", e);
        }
    }

    /**
     * Verifica si las dependencias necesarias están disponibles
     */
    private static boolean checkDependencies() {
        List<String> missingDependencies = new ArrayList<>();

        // Verificar dependencias para PDFBox (para leer archivos PDF)
        try {
            Class.forName("org.apache.pdfbox.Loader");
        } catch (ClassNotFoundException e) {
            missingDependencies.add("PDFBox (org.apache.pdfbox:pdfbox:2.0.27)");
        }

        // Verificar dependencias para Apache POI (para leer archivos DOCX)
        try {
            Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument");
        } catch (ClassNotFoundException e) {
            missingDependencies.add("Apache POI (org.apache.poi:poi-ooxml:5.2.3)");
        }

        // Verificar dependencias para JSON
        try {
            Class.forName("org.json.JSONObject");
        } catch (ClassNotFoundException e) {
            missingDependencies.add("JSON (org.json:json:20230227)");
        }

        // Verificar dependencias para logging
        try {
            Class.forName("org.slf4j.Logger");
        } catch (ClassNotFoundException e) {
            missingDependencies.add("SLF4J (org.slf4j:slf4j-api:1.7.36)");
        }

        try {
            Class.forName("ch.qos.logback.classic.Logger");
        } catch (ClassNotFoundException e) {
            missingDependencies.add("Logback (ch.qos.logback:logback-classic:1.2.11)");
        }

        return missingDependencies.isEmpty();
    }

    /**
     * Muestra un diálogo indicando las dependencias faltantes
     */
    private static void showDependenciesDialog() {
        String message = "Faltan algunas dependencias necesarias para el funcionamiento completo de la aplicación.\n\n" +
                "Para utilizar todas las funciones, añade las siguientes dependencias a tu proyecto:\n" +
                "- org.apache.pdfbox:pdfbox:2.0.27 (para leer archivos PDF)\n" +
                "- org.apache.poi:poi-ooxml:5.2.3 (para leer archivos DOCX)\n" +
                "- org.json:json:20230227 (para procesamiento JSON)\n" +
                "- org.slf4j:slf4j-api:1.7.36 (para logging)\n" +
                "- ch.qos.logback:logback-classic:1.2.11 (para logging)\n\n" +
                "¿Deseas continuar con funcionalidad limitada?";

        int option = JOptionPane.showConfirmDialog(
                null,
                message,
                "Dependencias Faltantes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.NO_OPTION) {
            System.exit(0);
        }
    }

    /**
     * Muestra una pantalla de bienvenida mientras se carga la aplicación
     */
    private static void showSplashScreen(Window ownerWindow) {
        JWindow splashScreen = new JWindow();
        splashScreen.setSize(450, 300);
        splashScreen.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Título
        JLabel titleLabel = new JLabel("Cliente Mejorado para Ollama");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 10, 10, 10));

        // Mensaje de carga
        JLabel loadingLabel = new JLabel("Cargando modelos y configuración...");
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        loadingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        // Barra de progreso
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(0, 50, 30, 50));

        // Versión
        JLabel versionLabel = new JLabel("Versión 1.0.0");
        versionLabel.setHorizontalAlignment(JLabel.CENTER);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        // Añadir componentes
        content.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(loadingLabel, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);
        content.add(centerPanel, BorderLayout.CENTER);

        content.add(versionLabel, BorderLayout.SOUTH);

        splashScreen.setContentPane(content);
        splashScreen.setVisible(true);

        // Cerrar splash después de un tiempo
        Timer timer = new Timer(2500, e -> {
            splashScreen.dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }
}