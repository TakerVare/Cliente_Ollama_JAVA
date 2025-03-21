package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Diálogo para importar y exportar configuraciones de la aplicación
 */
public class ConfigImportExportDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ConfigImportExportDialog.class);

    // Componentes de la interfaz
    private JPanel contentPanel;
    private JRadioButton exportRadioButton;
    private JRadioButton importRadioButton;
    private JPanel exportPanel;
    private JPanel importPanel;
    private JCheckBox exportMainConfigCheckBox;
    private JCheckBox exportSearchConfigCheckBox;
    private JCheckBox exportThemeConfigCheckBox;
    private JCheckBox exportApiKeysCheckBox;
    private JTextField exportFilePathField;
    private JButton exportBrowseButton;
    private JTextField importFilePathField;
    private JButton importBrowseButton;
    private JLabel statusLabel;
    private JButton actionButton;
    private JButton closeButton;

    // Resultado del diálogo
    private boolean operationSuccess = false;

    /**
     * Constructor principal
     */
    public ConfigImportExportDialog(Window owner) {
        super(owner, "Importar/Exportar Configuración", ModalityType.APPLICATION_MODAL);
        initComponents();
        setupLayout();
        setupEventHandlers();

        setSize(550, 450);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel de selección de operación
        JPanel operationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        operationPanel.setBorder(new TitledBorder("Operación"));

        exportRadioButton = new JRadioButton("Exportar Configuración");
        importRadioButton = new JRadioButton("Importar Configuración");

        ButtonGroup operationGroup = new ButtonGroup();
        operationGroup.add(exportRadioButton);
        operationGroup.add(importRadioButton);

        exportRadioButton.setSelected(true);

        operationPanel.add(exportRadioButton);
        operationPanel.add(importRadioButton);

        // Panel de exportación
        exportPanel = new JPanel(new GridBagLayout());
        exportPanel.setBorder(new TitledBorder("Opciones de Exportación"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel configsToExportLabel = new JLabel("Configuraciones a exportar:");
        exportPanel.add(configsToExportLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(5, 20, 5, 5);
        exportMainConfigCheckBox = new JCheckBox("Configuración Principal");
        exportMainConfigCheckBox.setSelected(true);
        exportPanel.add(exportMainConfigCheckBox, gbc);

        gbc.gridy = 2;
        exportSearchConfigCheckBox = new JCheckBox("Configuración de Búsqueda");
        exportSearchConfigCheckBox.setSelected(true);
        exportPanel.add(exportSearchConfigCheckBox, gbc);

        gbc.gridy = 3;
        exportThemeConfigCheckBox = new JCheckBox("Configuración de Tema");
        exportThemeConfigCheckBox.setSelected(true);
        exportPanel.add(exportThemeConfigCheckBox, gbc);

        gbc.gridy = 4;
        exportApiKeysCheckBox = new JCheckBox("Claves API (Puede contener información sensible)");
        exportApiKeysCheckBox.setSelected(false);
        exportPanel.add(exportApiKeysCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 5, 5, 5);
        JLabel exportPathLabel = new JLabel("Ruta de exportación:");
        exportPanel.add(exportPathLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 20, 5, 5);

        exportFilePathField = new JTextField();
        exportFilePathField.setText(getDefaultExportPath());
        exportPanel.add(exportFilePathField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        exportBrowseButton = new JButton("Examinar...");
        exportPanel.add(exportBrowseButton, gbc);

        // Panel de importación
        importPanel = new JPanel(new GridBagLayout());
        importPanel.setBorder(new TitledBorder("Opciones de Importación"));
        importPanel.setVisible(false);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel importWarningLabel = new JLabel("<html><b>Advertencia:</b> Importar una configuración sobrescribirá la configuración actual.</html>");
        importWarningLabel.setForeground(Color.RED);
        importPanel.add(importWarningLabel, gbc);

        gbc.gridy = 1;
        JLabel importPathLabel = new JLabel("Archivo de configuración a importar:");
        importPanel.add(importPathLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 20, 5, 5);

        importFilePathField = new JTextField();
        importPanel.add(importFilePathField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        importBrowseButton = new JButton("Examinar...");
        importPanel.add(importBrowseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(15, 5, 5, 5);

        JLabel importInfoLabel = new JLabel("<html>Se importarán todas las configuraciones presentes en el archivo.<br>La aplicación puede requerir un reinicio para aplicar todos los cambios.</html>");
        importInfoLabel.setForeground(Color.DARK_GRAY);
        importPanel.add(importInfoLabel, gbc);

        // Panel de estado
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        statusLabel = new JLabel(" ");
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        actionButton = new JButton("Exportar");
        closeButton = new JButton("Cerrar");

        buttonPanel.add(actionButton);
        buttonPanel.add(closeButton);

        // Organizar paneles en el panel principal
        JPanel centerPanel = new JPanel(new CardLayout());
        centerPanel.add(exportPanel, "export");
        centerPanel.add(importPanel, "import");

        contentPanel.add(operationPanel, BorderLayout.NORTH);
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(statusPanel, BorderLayout.SOUTH);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    /**
     * Configura el layout de los componentes
     */
    private void setupLayout() {
        getRootPane().setDefaultButton(actionButton);
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Cambio de operación
        exportRadioButton.addActionListener(e -> {
            exportPanel.setVisible(true);
            importPanel.setVisible(false);
            actionButton.setText("Exportar");
            statusLabel.setText(" ");
        });

        importRadioButton.addActionListener(e -> {
            exportPanel.setVisible(false);
            importPanel.setVisible(true);
            actionButton.setText("Importar");
            statusLabel.setText(" ");
        });

        // Botones de examinar
        exportBrowseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Seleccionar archivo para exportar configuración");
            fileChooser.setSelectedFile(new File(exportFilePathField.getText()));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                // Añadir extensión .json si no la tiene
                if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
                }

                exportFilePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        importBrowseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Seleccionar archivo de configuración para importar");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Archivos de configuración (*.json)", "json"));

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                importFilePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Botón de acción (exportar/importar)
        actionButton.addActionListener(e -> {
            if (exportRadioButton.isSelected()) {
                exportConfiguration();
            } else {
                importConfiguration();
            }
        });

        // Botón de cerrar
        closeButton.addActionListener(e -> dispose());
    }

    /**
     * Obtiene la ruta de exportación predeterminada
     */
    private String getDefaultExportPath() {
        String userHome = System.getProperty("user.home");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());

        return userHome + File.separator + "ollamaclient_config_" + timestamp + ".json";
    }

    /**
     * Exporta la configuración al archivo especificado
     */
    private void exportConfiguration() {
        if (!validateExportOptions()) {
            return;
        }

        String filePath = exportFilePathField.getText().trim();
        if (filePath.isEmpty()) {
            showError("Debe especificar una ruta de archivo para la exportación.");
            return;
        }

        File outputFile = new File(filePath);

        // Verificar si el archivo ya existe
        if (outputFile.exists()) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "El archivo ya existe. ¿Desea sobrescribirlo?",
                    "Confirmar sobrescritura",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            // Aquí deberíamos llamar a ConfigManager para exportar las configuraciones seleccionadas
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.exportConfigurations(outputFile);

            // Mostrar mensaje de éxito
            statusLabel.setText("Configuración exportada correctamente a: " + filePath);
            statusLabel.setForeground(new Color(0, 128, 0));

            operationSuccess = true;

            logger.info("Configuración exportada a: {}", filePath);

        } catch (Exception ex) {
            showError("Error al exportar la configuración: " + ex.getMessage());
            logger.error("Error al exportar configuración", ex);
        }
    }

    /**
     * Valida las opciones de exportación
     */
    private boolean validateExportOptions() {
        // Verificar que se ha seleccionado al menos una configuración para exportar
        if (!exportMainConfigCheckBox.isSelected() &&
                !exportSearchConfigCheckBox.isSelected() &&
                !exportThemeConfigCheckBox.isSelected() &&
                !exportApiKeysCheckBox.isSelected()) {

            showError("Debe seleccionar al menos una configuración para exportar.");
            return false;
        }

        return true;
    }

    /**
     * Importa la configuración desde el archivo especificado
     */
    private void importConfiguration() {
        String filePath = importFilePathField.getText().trim();
        if (filePath.isEmpty()) {
            showError("Debe especificar un archivo de configuración para importar.");
            return;
        }

        File inputFile = new File(filePath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            showError("El archivo especificado no existe o no es un archivo válido.");
            return;
        }

        // Confirmación adicional
        int option = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea importar la configuración?\n" +
                        "La configuración actual será sobrescrita y no se puede deshacer.",
                "Confirmar importación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Llamar a ConfigManager para importar la configuración
            ConfigManager configManager = ConfigManager.getInstance();
            boolean success = configManager.importConfigurations(inputFile);

            if (success) {
                // Mostrar mensaje de éxito
                statusLabel.setText("Configuración importada correctamente desde: " + filePath);
                statusLabel.setForeground(new Color(0, 128, 0));

                operationSuccess = true;

                logger.info("Configuración importada desde: {}", filePath);

                // Informar al usuario que puede ser necesario reiniciar
                JOptionPane.showMessageDialog(
                        this,
                        "La configuración se ha importado correctamente.\n" +
                                "Algunos cambios pueden requerir reiniciar la aplicación para aplicarse completamente.",
                        "Importación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                showError("No se pudo importar la configuración. Verifique que el archivo tenga el formato correcto.");
            }

        } catch (Exception ex) {
            showError("Error al importar la configuración: " + ex.getMessage());
            logger.error("Error al importar configuración", ex);
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);

        // También mostrar un diálogo de error
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Verifica si la operación fue exitosa
     */
    public boolean isOperationSuccess() {
        return operationSuccess;
    }

    /**
     * Método estático para mostrar el diálogo
     */
    public static boolean showDialog(Window owner) {
        ConfigImportExportDialog dialog = new ConfigImportExportDialog(owner);
        dialog.setVisible(true);
        return dialog.isOperationSuccess();
    }
}