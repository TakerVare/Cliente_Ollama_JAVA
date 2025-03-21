package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador centralizado de errores para la aplicación
 * Proporciona registro, visualización y exportación de errores
 */
public class ErrorManager {
    private static final Logger logger = LoggerFactory.getLogger(ErrorManager.class);
    private static final ErrorManager instance = new ErrorManager();

    // Categorías de errores
    public enum ErrorCategory {
        NETWORK("Error de Red"),
        FILE_SYSTEM("Error de Sistema de Archivos"),
        OLLAMA_API("Error de API de Ollama"),
        CONFIGURATION("Error de Configuración"),
        SEARCH_API("Error de API de Búsqueda"),
        UI("Error de Interfaz de Usuario"),
        UNKNOWN("Error Desconocido");

        private final String description;

        ErrorCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Niveles de severidad
    public enum ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    // Registro de errores recientes
    private final Map<Long, ErrorInfo> recentErrors = new HashMap<>();
    private Window ownerWindow;
    private String errorLogPath;

    // Constructor privado para Singleton
    private ErrorManager() {
        // Configurar la ruta del archivo de registro de errores
        String userHome = System.getProperty("user.home");
        File logDir = new File(userHome, ".ollamaclient/logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        errorLogPath = new File(logDir, "error_log.txt").getAbsolutePath();
        logger.info("Ruta de registro de errores: {}", errorLogPath);
    }

    /**
     * Obtiene la instancia única del manejador de errores
     */
    public static ErrorManager getInstance() {
        return instance;
    }

    /**
     * Establece la ventana propietaria para los diálogos de error
     */
    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    /**
     * Maneja un error con todos los detalles
     */
    public void handleError(ErrorCategory category, ErrorSeverity severity, String title,
                            String message, Throwable exception, boolean showDialog) {
        // Crear información del error
        ErrorInfo errorInfo = new ErrorInfo(
                System.currentTimeMillis(),
                category,
                severity,
                title,
                message,
                exception
        );

        // Registrar en el sistema de logs
        logError(errorInfo);

        // Guardar en el registro de errores recientes
        recentErrors.put(errorInfo.getTimestamp(), errorInfo);

        // Mostrar diálogo según la configuración y nivel de severidad
        if (showDialog) {
            showErrorDialog(errorInfo);
        }
    }

    /**
     * Versión simplificada para errores sin excepción
     */
    public void handleError(ErrorCategory category, ErrorSeverity severity,
                            String title, String message, boolean showDialog) {
        handleError(category, severity, title, message, null, showDialog);
    }

    /**
     * Versión simplificada para excepciones
     */
    public void handleException(ErrorCategory category, String title,
                                Throwable exception, boolean showDialog) {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            message = exception.getClass().getSimpleName();
        }
        handleError(category, ErrorSeverity.ERROR, title, message, exception, showDialog);
    }

    /**
     * Versión simplificada para errores de red
     */
    public void handleNetworkError(String title, String message,
                                   Throwable exception, boolean showDialog) {
        handleError(ErrorCategory.NETWORK, ErrorSeverity.ERROR, title, message, exception, showDialog);
    }

    /**
     * Versión simplificada para errores de Ollama API
     */
    public void handleOllamaApiError(String title, String message,
                                     Throwable exception, boolean showDialog) {
        handleError(ErrorCategory.OLLAMA_API, ErrorSeverity.ERROR, title, message, exception, showDialog);
    }

    /**
     * Registra el error en el archivo de log
     */
    private void logError(ErrorInfo errorInfo) {
        // Registrar en SLF4J según la severidad
        switch (errorInfo.getSeverity()) {
            case INFO:
                logger.info("{}: {}", errorInfo.getTitle(), errorInfo.getMessage());
                break;
            case WARNING:
                logger.warn("{}: {}", errorInfo.getTitle(), errorInfo.getMessage());
                break;
            case ERROR:
                if (errorInfo.getException() != null) {
                    logger.error("{}: {}", errorInfo.getTitle(), errorInfo.getMessage(), errorInfo.getException());
                } else {
                    logger.error("{}: {}", errorInfo.getTitle(), errorInfo.getMessage());
                }
                break;
            case CRITICAL:
                if (errorInfo.getException() != null) {
                    logger.error("CRÍTICO - {}: {}", errorInfo.getTitle(), errorInfo.getMessage(), errorInfo.getException());
                } else {
                    logger.error("CRÍTICO - {}: {}", errorInfo.getTitle(), errorInfo.getMessage());
                }
                break;
        }

        // Escribir en el archivo de registro de errores
        try (FileWriter fw = new FileWriter(errorLogPath, true);
             PrintWriter pw = new PrintWriter(fw)) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date(errorInfo.getTimestamp()));

            pw.println("====================================");
            pw.println("Fecha: " + timestamp);
            pw.println("Categoría: " + errorInfo.getCategory().getDescription());
            pw.println("Severidad: " + errorInfo.getSeverity());
            pw.println("Título: " + errorInfo.getTitle());
            pw.println("Mensaje: " + errorInfo.getMessage());

            if (errorInfo.getException() != null) {
                pw.println("Excepción: " + errorInfo.getException().getClass().getName());
                pw.println("Stack Trace:");
                StringWriter sw = new StringWriter();
                PrintWriter stackTracePw = new PrintWriter(sw);
                errorInfo.getException().printStackTrace(stackTracePw);
                pw.println(sw.toString());
            }

            pw.println("====================================");
            pw.println();

        } catch (IOException e) {
            // Si no podemos escribir en el archivo de log, al menos registramos en SLF4J
            logger.error("No se pudo escribir en el archivo de registro de errores", e);
        }
    }

    /**
     * Muestra un diálogo de error personalizado
     */
    private void showErrorDialog(ErrorInfo errorInfo) {
        SwingUtilities.invokeLater(() -> {
            // Determinar el tipo de icono según la severidad
            int messageType = JOptionPane.ERROR_MESSAGE;
            switch (errorInfo.getSeverity()) {
                case INFO:
                    messageType = JOptionPane.INFORMATION_MESSAGE;
                    break;
                case WARNING:
                    messageType = JOptionPane.WARNING_MESSAGE;
                    break;
                case ERROR:
                case CRITICAL:
                    messageType = JOptionPane.ERROR_MESSAGE;
                    break;
            }

            // Crear panel personalizado para el mensaje
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Agregar mensaje principal
            JLabel messageLabel = new JLabel("<html><body width='400px'>" +
                    errorInfo.getMessage() + "</body></html>");
            panel.add(messageLabel, BorderLayout.NORTH);

            // Si hay una excepción, agregar opción para ver detalles
            if (errorInfo.getException() != null) {
                JButton detailsButton = new JButton("Ver detalles");
                detailsButton.addActionListener(e -> showExceptionDetails(errorInfo));

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(detailsButton);
                panel.add(buttonPanel, BorderLayout.SOUTH);
            }

            // Mostrar diálogo
            JOptionPane.showMessageDialog(
                    ownerWindow,
                    panel,
                    errorInfo.getCategory().getDescription() + ": " + errorInfo.getTitle(),
                    messageType
            );
        });
    }

    /**
     * Muestra los detalles de la excepción en un diálogo separado
     */
    private void showExceptionDetails(ErrorInfo errorInfo) {
        if (errorInfo.getException() == null) {
            return;
        }

        // Crear diálogo modal
        JDialog dialog;
        if (ownerWindow instanceof Frame) {
            dialog = new JDialog((Frame) ownerWindow, "Detalles del error", true);
        } else if (ownerWindow instanceof Dialog) {
            dialog = new JDialog((Dialog) ownerWindow, "Detalles del error", true);
        } else {
            dialog = new JDialog();
            dialog.setTitle("Detalles del error");
            dialog.setModal(true);
        }

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(ownerWindow);

        // Panel de información
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.add(new JLabel("Tipo: " + errorInfo.getException().getClass().getName()));
        infoPanel.add(new JLabel("Mensaje: " + errorInfo.getMessage()));
        infoPanel.add(new JLabel("Categoría: " + errorInfo.getCategory().getDescription()));

        // Stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        errorInfo.getException().printStackTrace(pw);
        JTextArea stackTraceArea = new JTextArea(sw.toString());
        stackTraceArea.setEditable(false);
        stackTraceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copiar");
        copyButton.addActionListener(e -> {
            stackTraceArea.selectAll();
            stackTraceArea.copy();
            stackTraceArea.select(0, 0);
        });

        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);

        // Agregar componentes al diálogo
        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(stackTraceArea), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Mostrar diálogo
        dialog.setVisible(true);
    }

    /**
     * Muestra un diálogo con todos los errores recientes
     */
    public void showErrorLog() {
        if (recentErrors.isEmpty()) {
            JOptionPane.showMessageDialog(
                    ownerWindow,
                    "No hay errores recientes registrados.",
                    "Registro de Errores",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Crear diálogo modal
        JDialog dialog;
        if (ownerWindow instanceof Frame) {
            dialog = new JDialog((Frame) ownerWindow, "Registro de Errores Recientes", true);
        } else if (ownerWindow instanceof Dialog) {
            dialog = new JDialog((Dialog) ownerWindow, "Registro de Errores Recientes", true);
        } else {
            dialog = new JDialog();
            dialog.setTitle("Registro de Errores Recientes");
            dialog.setModal(true);
        }

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(ownerWindow);

        // Crear tabla de errores
        String[] columnNames = {"Fecha", "Categoría", "Severidad", "Título", "Mensaje"};
        Object[][] data = new Object[recentErrors.size()][5];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int i = 0;
        for (ErrorInfo error : recentErrors.values()) {
            data[i][0] = sdf.format(new Date(error.getTimestamp()));
            data[i][1] = error.getCategory().getDescription();
            data[i][2] = error.getSeverity();
            data[i][3] = error.getTitle();
            data[i][4] = error.getMessage();
            i++;
        }

        JTable table = new JTable(data, columnNames);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Establecer anchos de columna
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(270);

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton viewButton = new JButton("Ver Detalles");
        viewButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .parse((String)data[row][0], new java.text.ParsePosition(0)).getTime();
                ErrorInfo selectedError = recentErrors.get(timestamp);
                if (selectedError != null) {
                    showExceptionDetails(selectedError);
                }
            }
        });

        JButton exportButton = new JButton("Exportar");
        exportButton.addActionListener(e -> exportErrorLog());

        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(viewButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);

        // Agregar componentes al diálogo
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Mostrar diálogo
        dialog.setVisible(true);
    }

    /**
     * Exporta el registro de errores a un archivo
     */
    private void exportErrorLog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar registro de errores");
        fileChooser.setSelectedFile(new File("errores_ollamaclient.log"));

        int result = fileChooser.showSaveDialog(ownerWindow);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try (FileWriter fw = new FileWriter(selectedFile);
                 PrintWriter pw = new PrintWriter(fw)) {

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                pw.println("REGISTRO DE ERRORES DE OLLAMA CLIENT");
                pw.println("====================================");
                pw.println("Exportado: " + sdf.format(new Date()));
                pw.println("====================================");
                pw.println();

                for (ErrorInfo error : recentErrors.values()) {
                    String timestamp = sdf.format(new Date(error.getTimestamp()));

                    pw.println("------------------------------------");
                    pw.println("Fecha: " + timestamp);
                    pw.println("Categoría: " + error.getCategory().getDescription());
                    pw.println("Severidad: " + error.getSeverity());
                    pw.println("Título: " + error.getTitle());
                    pw.println("Mensaje: " + error.getMessage());

                    if (error.getException() != null) {
                        pw.println("Excepción: " + error.getException().getClass().getName());
                        pw.println("Stack Trace:");
                        StringWriter sw = new StringWriter();
                        PrintWriter stackTracePw = new PrintWriter(sw);
                        error.getException().printStackTrace(stackTracePw);
                        pw.println(sw.toString());
                    }

                    pw.println("------------------------------------");
                    pw.println();
                }

                JOptionPane.showMessageDialog(
                        ownerWindow,
                        "Registro de errores exportado correctamente a:\n" + selectedFile.getPath(),
                        "Exportación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (IOException e) {
                logger.error("Error al exportar el registro de errores", e);
                JOptionPane.showMessageDialog(
                        ownerWindow,
                        "Error al exportar el registro de errores: " + e.getMessage(),
                        "Error de Exportación",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Clase interna para almacenar información detallada de un error
     */
    public static class ErrorInfo {
        private final long timestamp;
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String title;
        private final String message;
        private final Throwable exception;

        public ErrorInfo(long timestamp, ErrorCategory category, ErrorSeverity severity,
                         String title, String message, Throwable exception) {
            this.timestamp = timestamp;
            this.category = category;
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.exception = exception;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public ErrorCategory getCategory() {
            return category;
        }

        public ErrorSeverity getSeverity() {
            return severity;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getException() {
            return exception;
        }
    }
}
