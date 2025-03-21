package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Diálogo para configurar las preferencias de búsqueda
 * Permite al usuario configurar varios aspectos de la búsqueda web
 */
public class SearchPreferencesDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(SearchPreferencesDialog.class);

    // Configuraciones por defecto
    private static final String[] SUPPORTED_LANGUAGES = {"es", "en", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko"};
    private static final String[] SUPPORTED_TIME_LIMITS = {"all", "day", "week", "month", "year"};

    // UI Components
    private JComboBox<WebSearchService.SearchAPI> searchAPIComboBox;
    private JSpinner maxResultsSpinner;
    private JCheckBox includeImagesCheckBox;
    private JCheckBox includeNewsCheckBox;
    private JCheckBox includeVideosCheckBox;
    private JComboBox<String> languageComboBox;
    private JComboBox<String> timeLimitComboBox;
    private JCheckBox safeSearchCheckBox;
    private JTextField serpApiKeyField;
    private JTextField googleApiKeyField;
    private JTextField googleCseIdField;

    // Configuración inicial
    private final Map<String, Object> initialConfig;

    // Resultado tras aceptar
    private boolean accepted = false;

    /**
     * Constructor principal
     */
    public SearchPreferencesDialog(Window owner) {
        super(owner, "Preferencias de Búsqueda", ModalityType.APPLICATION_MODAL);

        // Obtener configuración actual
        ConfigManager configManager = ConfigManager.getInstance();
        initialConfig = new HashMap<>();

        // Cargar valores de configuración
        initialConfig.put("searchAPIProvider", configManager.getSearchConfig("searchAPIProvider", "DUCKDUCKGO"));
        initialConfig.put("maxResults", configManager.getSearchConfig("maxResults", 5));
        initialConfig.put("includeWebImagesInSearch", configManager.getSearchConfig("includeWebImagesInSearch", false));
        initialConfig.put("includeNewsInSearch", configManager.getSearchConfig("includeNewsInSearch", true));
        initialConfig.put("includeVideosInSearch", configManager.getSearchConfig("includeVideosInSearch", false));
        initialConfig.put("preferredLanguage", configManager.getSearchConfig("preferredLanguage", "es"));
        initialConfig.put("timeLimit", configManager.getSearchConfig("timeLimit", "all"));
        initialConfig.put("safeSearch", configManager.getSearchConfig("safeSearch", true));

        // Cargar claves API
        initialConfig.put("serpApiKey", configManager.getApiKey("serpapi"));
        initialConfig.put("googleApiKey", configManager.getApiKey("google"));
        initialConfig.put("googleCseId", configManager.getApiKey("google_cse"));

        // Inicializar y configurar diálogo
        setSize(550, 500);
        setLocationRelativeTo(owner);
        setResizable(true);

        initComponents();
        setupLayout();
        setupEventHandlers();
    }

    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        // API de búsqueda
        searchAPIComboBox = new JComboBox<>(WebSearchService.SearchAPI.values());

        // Seleccionar API actual
        try {
            String apiName = (String) initialConfig.get("searchAPIProvider");
            WebSearchService.SearchAPI currentApi = WebSearchService.SearchAPI.valueOf(apiName);
            searchAPIComboBox.setSelectedItem(currentApi);
        } catch (IllegalArgumentException | NullPointerException e) {
            // En caso de error, usar valor por defecto
            searchAPIComboBox.setSelectedItem(WebSearchService.SearchAPI.DUCKDUCKGO);
        }

        // Número máximo de resultados
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                (int) initialConfig.get("maxResults"),
                1, 20, 1);
        maxResultsSpinner = new JSpinner(spinnerModel);

        // Tipos de contenido
        includeImagesCheckBox = new JCheckBox("Incluir imágenes en la búsqueda");
        includeImagesCheckBox.setSelected((Boolean) initialConfig.get("includeWebImagesInSearch"));

        includeNewsCheckBox = new JCheckBox("Incluir noticias en la búsqueda");
        includeNewsCheckBox.setSelected((Boolean) initialConfig.get("includeNewsInSearch"));

        includeVideosCheckBox = new JCheckBox("Incluir videos en la búsqueda");
        includeVideosCheckBox.setSelected((Boolean) initialConfig.get("includeVideosInSearch"));

        // Idioma preferido
        languageComboBox = new JComboBox<>(SUPPORTED_LANGUAGES);
        languageComboBox.setSelectedItem(initialConfig.get("preferredLanguage"));

        // Límite de tiempo
        timeLimitComboBox = new JComboBox<>(SUPPORTED_TIME_LIMITS);
        timeLimitComboBox.setSelectedItem(initialConfig.get("timeLimit"));

        // Configuración del renderizador para mostrar etiquetas descriptivas
        timeLimitComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value != null) {
                    switch (value.toString()) {
                        case "all":
                            setText("Todo el tiempo");
                            break;
                        case "day":
                            setText("Último día");
                            break;
                        case "week":
                            setText("Última semana");
                            break;
                        case "month":
                            setText("Último mes");
                            break;
                        case "year":
                            setText("Último año");
                            break;
                    }
                }

                return this;
            }
        });

        // Búsqueda segura
        safeSearchCheckBox = new JCheckBox("Activar búsqueda segura (filtrar contenido para adultos)");
        safeSearchCheckBox.setSelected((Boolean) initialConfig.get("safeSearch"));

        // Claves API
        serpApiKeyField = new JTextField((String) initialConfig.get("serpApiKey"));
        googleApiKeyField = new JTextField((String) initialConfig.get("googleApiKey"));
        googleCseIdField = new JTextField((String) initialConfig.get("googleCseId"));
    }

    /**
     * Configura el layout de los componentes
     */
    private void setupLayout() {
        // Panel principal con borde
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Panel de opciones generales
        JPanel generalPanel = new JPanel(new GridBagLayout());
        generalPanel.setBorder(new TitledBorder("Opciones Generales"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // API de búsqueda
        generalPanel.add(new JLabel("API de búsqueda:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        generalPanel.add(searchAPIComboBox, gbc);

        // Máximo de resultados
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        generalPanel.add(new JLabel("Máximo de resultados:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        generalPanel.add(maxResultsSpinner, gbc);

        // Panel de tipos de contenido
        JPanel contentTypePanel = new JPanel(new GridLayout(0, 1));
        contentTypePanel.setBorder(new TitledBorder("Tipos de Contenido"));

        contentTypePanel.add(includeImagesCheckBox);
        contentTypePanel.add(includeNewsCheckBox);
        contentTypePanel.add(includeVideosCheckBox);

        // Panel de filtros
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(new TitledBorder("Filtros"));

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Idioma
        filterPanel.add(new JLabel("Idioma preferido:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        filterPanel.add(languageComboBox, gbc);

        // Límite de tiempo
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        filterPanel.add(new JLabel("Límite de tiempo:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        filterPanel.add(timeLimitComboBox, gbc);

        // Búsqueda segura
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        filterPanel.add(safeSearchCheckBox, gbc);

        // Panel de claves API
        JPanel apiKeysPanel = new JPanel(new GridBagLayout());
        apiKeysPanel.setBorder(new TitledBorder("Claves API"));

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // SerpAPI Key
        apiKeysPanel.add(new JLabel("Clave SerpAPI:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeysPanel.add(serpApiKeyField, gbc);

        // Google API Key
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        apiKeysPanel.add(new JLabel("Clave Google API:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeysPanel.add(googleApiKeyField, gbc);

        // Google CSE ID
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        apiKeysPanel.add(new JLabel("Google CSE ID:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeysPanel.add(googleCseIdField, gbc);

        // Añadir información de ayuda
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel helpLabel = new JLabel("<html><small>Nota: algunas APIs requieren claves. " +
                "Visita los sitios web correspondientes para obtenerlas.</small></html>");
        helpLabel.setForeground(Color.GRAY);
        apiKeysPanel.add(helpLabel, gbc);

        // Panel de opciones
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(generalPanel);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(contentTypePanel);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(filterPanel);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(apiKeysPanel);

        // Panel de scroll para las opciones
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Aplicar");
        JButton cancelButton = new JButton("Cancelar");

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Establecer botón por defecto
        getRootPane().setDefaultButton(applyButton);

        // Atajos de teclado
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        getRootPane().registerKeyboardAction(
                e -> applyButton.doClick(),
                enterKey,
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> cancelButton.doClick(),
                escapeKey,
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Manejo de eventos para los botones
        applyButton.addActionListener(e -> {
            saveSettings();
            accepted = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Actualizar campos de API según la selección
        searchAPIComboBox.addActionListener(e -> updateApiKeyFieldsVisibility());

        // Actualizar inicialmente
        updateApiKeyFieldsVisibility();
    }

    /**
     * Actualiza la visibilidad de los campos de clave API según la API seleccionada
     */
    private void updateApiKeyFieldsVisibility() {
        WebSearchService.SearchAPI selectedApi = (WebSearchService.SearchAPI) searchAPIComboBox.getSelectedItem();

        boolean showSerpApi = (selectedApi == WebSearchService.SearchAPI.SERPAPI);
        boolean showGoogleApi = (selectedApi == WebSearchService.SearchAPI.CUSTOM_GOOGLE);

        // Actualizar visibilidad de campos
        serpApiKeyField.setEnabled(showSerpApi);
        googleApiKeyField.setEnabled(showGoogleApi);
        googleCseIdField.setEnabled(showGoogleApi);
    }

    /**
     * Guarda la configuración
     */
    private void saveSettings() {
        ConfigManager configManager = ConfigManager.getInstance();

        // Opciones generales
        WebSearchService.SearchAPI selectedApi = (WebSearchService.SearchAPI) searchAPIComboBox.getSelectedItem();
        configManager.setSearchConfig("searchAPIProvider", selectedApi.name());

        int maxResults = (Integer) maxResultsSpinner.getValue();
        configManager.setSearchConfig("maxResults", maxResults);

        // Tipos de contenido
        configManager.setSearchConfig("includeWebImagesInSearch", includeImagesCheckBox.isSelected());
        configManager.setSearchConfig("includeNewsInSearch", includeNewsCheckBox.isSelected());
        configManager.setSearchConfig("includeVideosInSearch", includeVideosCheckBox.isSelected());

        // Filtros
        String language = (String) languageComboBox.getSelectedItem();
        configManager.setSearchConfig("preferredLanguage", language);

        String timeLimit = (String) timeLimitComboBox.getSelectedItem();
        configManager.setSearchConfig("timeLimit", timeLimit);

        configManager.setSearchConfig("safeSearch", safeSearchCheckBox.isSelected());

        // Claves API
        configManager.setApiKey("serpapi", serpApiKeyField.getText().trim());
        configManager.setApiKey("google", googleApiKeyField.getText().trim());
        configManager.setApiKey("google_cse", googleCseIdField.getText().trim());

        // Guardar configuración
        configManager.saveSearchConfig();
        configManager.saveApiKeysConfig();

        logger.info("Configuración de búsqueda guardada");
    }

    /**
     * Verifica si el usuario aceptó los cambios
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Método estático para mostrar el diálogo
     * @return true si el usuario aplicó cambios, false si canceló
     */
    public static boolean showDialog(Window owner) {
        SearchPreferencesDialog dialog = new SearchPreferencesDialog(owner);
        dialog.setVisible(true);
        return dialog.isAccepted();
    }
}