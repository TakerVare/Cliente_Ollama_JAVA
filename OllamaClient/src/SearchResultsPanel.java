package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Panel para mostrar resultados de búsqueda en diferentes formatos
 * Soporta visualización en forma de lista o grilla
 */
public class SearchResultsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultsPanel.class);

    // Modos de visualización
    public enum DisplayMode {
        LIST("Lista", "Mostrar resultados en una lista vertical"),
        GRID("Cuadrícula", "Mostrar resultados en una cuadrícula"),
        COMPACT("Compacto", "Mostrar resultados en una lista compacta"),
        DETAILED("Detallado", "Mostrar resultados con todos los detalles");

        private final String displayName;
        private final String description;

        DisplayMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Tipos de contenidos
    public enum ContentType {
        TEXT,
        IMAGE,
        VIDEO,
        NEWS,
        ALL
    }

    // Componentes de la interfaz
    private JPanel resultsContainer;
    private JScrollPane scrollPane;
    private JPanel controlPanel;
    private JComboBox<DisplayMode> displayModeComboBox;
    private JComboBox<ContentType> contentTypeComboBox;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton addToPromptButton;
    private JSlider zoomSlider;

    // Estado del panel
    private DisplayMode currentDisplayMode = DisplayMode.LIST;
    private ContentType currentContentType = ContentType.ALL;
    private final List<SearchResultItem> allResults = new ArrayList<>();
    private final List<SearchResultItem> filteredResults = new ArrayList<>();
    private int zoomLevel = 100;

    // Servicio para cargar imágenes en segundo plano
    private final ExecutorService imageLoaderService = Executors.newFixedThreadPool(3);

    // Callback para añadir resultados al prompt
    private Runnable onAddToPromptCallback;

    /**
     * Constructor principal
     */
    public SearchResultsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    /**
     * Inicializa los componentes de la interfaz
     */
    private void initializeComponents() {
        // Control panel
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // Modo de visualización
        displayModeComboBox = new JComboBox<>(DisplayMode.values());
        displayModeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DisplayMode) {
                    setText(((DisplayMode) value).getDisplayName());
                    setToolTipText(((DisplayMode) value).getDescription());
                }
                return this;
            }
        });

        // Tipo de contenido
        contentTypeComboBox = new JComboBox<>(ContentType.values());
        contentTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContentType) {
                    switch ((ContentType) value) {
                        case TEXT:
                            setText("Texto");
                            break;
                        case IMAGE:
                            setText("Imágenes");
                            break;
                        case VIDEO:
                            setText("Videos");
                            break;
                        case NEWS:
                            setText("Noticias");
                            break;
                        case ALL:
                            setText("Todos");
                            break;
                    }
                }
                return this;
            }
        });

        // Status label
        statusLabel = new JLabel("No hay resultados");

        // Botones
        refreshButton = new JButton("Actualizar");
        refreshButton.setToolTipText("Actualizar resultados");

        addToPromptButton = new JButton("Añadir al Prompt");
        addToPromptButton.setToolTipText("Añadir resultados seleccionados al prompt");
        addToPromptButton.setEnabled(false);

        // Slider de zoom
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(false);
        zoomSlider.setToolTipText("Ajustar tamaño de visualización");

        // Panel de resultados
        resultsContainer = new JPanel();
        scrollPane = new JScrollPane(resultsContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    /**
     * Configura el layout de los componentes
     */
    private void layoutComponents() {
        // Panel de control
        controlPanel.add(new JLabel("Visualización:"));
        controlPanel.add(displayModeComboBox);
        controlPanel.add(new JLabel("Contenido:"));
        controlPanel.add(contentTypeComboBox);
        controlPanel.add(new JLabel("Zoom:"));
        controlPanel.add(zoomSlider);
        controlPanel.add(refreshButton);
        controlPanel.add(addToPromptButton);

        // Panel de estado
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Layout principal
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // Configuración inicial del panel de resultados
        updateResultsLayout();
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Cambio de modo de visualización
        displayModeComboBox.addActionListener(e -> {
            currentDisplayMode = (DisplayMode) displayModeComboBox.getSelectedItem();
            updateResultsLayout();
        });

        // Filtro por tipo de contenido
        contentTypeComboBox.addActionListener(e -> {
            currentContentType = (ContentType) contentTypeComboBox.getSelectedItem();
            filterResults();
            updateResultsLayout();
        });

        // Cambio de nivel de zoom
        zoomSlider.addChangeListener(e -> {
            if (!zoomSlider.getValueIsAdjusting()) {
                zoomLevel = zoomSlider.getValue();
                updateResultsLayout();
            }
        });

        // Botón de actualizar
        refreshButton.addActionListener(e -> refreshResults());

        // Botón de añadir al prompt
        addToPromptButton.addActionListener(e -> {
            if (onAddToPromptCallback != null) {
                onAddToPromptCallback.run();
            }
        });
    }

    /**
     * Actualiza el layout de los resultados según el modo de visualización
     */
    private void updateResultsLayout() {
        resultsContainer.removeAll();

        switch (currentDisplayMode) {
            case LIST:
                resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
                displayResultsAsList();
                break;

            case GRID:
                // Calcular columnas según el ancho disponible
                int columns = Math.max(1, getWidth() / (200 * zoomLevel / 100));
                resultsContainer.setLayout(new GridLayout(0, columns, 10, 10));
                displayResultsAsGrid();
                break;

            case COMPACT:
                resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
                displayResultsAsCompact();
                break;

            case DETAILED:
                resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
                displayResultsAsDetailed();
                break;
        }

        // Actualizar panel de desplazamiento
        scrollPane.getViewport().setView(resultsContainer);
        scrollPane.getVerticalScrollBar().setValue(0);
        resultsContainer.revalidate();
        resultsContainer.repaint();

        // Actualizar etiqueta de estado
        updateStatusLabel();
    }

    /**
     * Muestra los resultados en formato de lista
     */
    private void displayResultsAsList() {
        for (SearchResultItem result : filteredResults) {
            JPanel resultPanel = new JPanel(new BorderLayout(10, 5));
            resultPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            // Panel para título y URL
            JPanel headerPanel = new JPanel(new BorderLayout());

            // Título con enlace
            JLabel titleLabel = createLinkLabel(result.getTitle(), result.getUrl());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            headerPanel.add(titleLabel, BorderLayout.NORTH);

            // URL
            JLabel urlLabel = new JLabel(result.getUrl());
            urlLabel.setForeground(Color.GRAY);
            urlLabel.setFont(urlLabel.getFont().deriveFont((float) (urlLabel.getFont().getSize() - 2)));
            headerPanel.add(urlLabel, BorderLayout.CENTER);

            resultPanel.add(headerPanel, BorderLayout.NORTH);

            // Snippet de texto
            if (result.getSnippet() != null && !result.getSnippet().isEmpty()) {
                JLabel snippetLabel = new JLabel("<html><body width='" +
                        (500 * zoomLevel / 100) + "'>" + result.getSnippet() + "</body></html>");
                resultPanel.add(snippetLabel, BorderLayout.CENTER);
            }

            // Imagen de vista previa (si existe)
            if (result.getContentType() == ContentType.IMAGE || result.getImageUrl() != null) {
                JLabel imageLabel = new JLabel("Cargando imagen...");
                imageLabel.setHorizontalAlignment(JLabel.CENTER);
                imageLabel.setPreferredSize(new Dimension(150 * zoomLevel / 100, 100 * zoomLevel / 100));

                // Cargar imagen en segundo plano
                loadImageAsync(result.getImageUrl(), imageLabel, 150 * zoomLevel / 100);

                resultPanel.add(imageLabel, BorderLayout.EAST);
            }

            // Panel de acciones
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JCheckBox selectCheckBox = new JCheckBox("Seleccionar");
            selectCheckBox.addActionListener(e -> result.setSelected(selectCheckBox.isSelected()));

            actionPanel.add(selectCheckBox);

            resultPanel.add(actionPanel, BorderLayout.SOUTH);

            // Añadir al contenedor
            resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, resultPanel.getPreferredSize().height));
            resultsContainer.add(resultPanel);
        }
    }

    /**
     * Muestra los resultados en formato de cuadrícula
     */
    private void displayResultsAsGrid() {
        int cardWidth = 180 * zoomLevel / 100;
        int cardHeight = 220 * zoomLevel / 100;
        int imageSize = 160 * zoomLevel / 100;

        for (SearchResultItem result : filteredResults) {
            JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
            resultPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            resultPanel.setPreferredSize(new Dimension(cardWidth, cardHeight));

            // Imagen o placeholder
            JLabel imageLabel = new JLabel("", JLabel.CENTER);
            imageLabel.setPreferredSize(new Dimension(imageSize, imageSize));

            // Cargar imagen o usar placeholder
            if (result.getContentType() == ContentType.IMAGE || result.getImageUrl() != null) {
                loadImageAsync(result.getImageUrl(), imageLabel, imageSize);
            } else {
                // Usar icono placeholder según tipo de contenido
                ImageIcon icon = getPlaceholderIcon(result.getContentType(), 48);
                imageLabel.setIcon(icon);
            }

            // Título truncado
            String title = result.getTitle();
            if (title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }

            JLabel titleLabel = createLinkLabel(title, result.getUrl());
            titleLabel.setHorizontalAlignment(JLabel.CENTER);

            // Selector
            JCheckBox selectCheckBox = new JCheckBox("Seleccionar");
            selectCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
            selectCheckBox.addActionListener(e -> result.setSelected(selectCheckBox.isSelected()));

            // Añadir componentes
            resultPanel.add(imageLabel, BorderLayout.CENTER);
            resultPanel.add(titleLabel, BorderLayout.NORTH);
            resultPanel.add(selectCheckBox, BorderLayout.SOUTH);

            resultsContainer.add(resultPanel);
        }
    }

    /**
     * Muestra los resultados en formato compacto
     */
    private void displayResultsAsCompact() {
        for (SearchResultItem result : filteredResults) {
            JPanel resultPanel = new JPanel(new BorderLayout(5, 0));
            resultPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

            // Título con enlace
            JLabel titleLabel = createLinkLabel(result.getTitle(), result.getUrl());

            // Checkbox
            JCheckBox selectCheckBox = new JCheckBox();
            selectCheckBox.addActionListener(e -> result.setSelected(selectCheckBox.isSelected()));

            // Añadir componentes
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.add(selectCheckBox, BorderLayout.WEST);
            leftPanel.add(titleLabel, BorderLayout.CENTER);

            resultPanel.add(leftPanel, BorderLayout.CENTER);

            // Añadir al contenedor
            resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, resultPanel.getPreferredSize().height));
            resultsContainer.add(resultPanel);

            // Añadir separador
            if (filteredResults.indexOf(result) < filteredResults.size() - 1) {
                resultsContainer.add(new JSeparator(JSeparator.HORIZONTAL));
            }
        }
    }

    /**
     * Muestra los resultados en formato detallado
     */
    private void displayResultsAsDetailed() {
        for (SearchResultItem result : filteredResults) {
            JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
            resultPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                    BorderFactory.createEmptyBorder(15, 10, 15, 10)
            ));

            // Panel superior con título y URL
            JPanel headerPanel = new JPanel(new BorderLayout());

            // Título con enlace
            JLabel titleLabel = createLinkLabel(result.getTitle(), result.getUrl());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 2));
            headerPanel.add(titleLabel, BorderLayout.NORTH);

            // URL
            JLabel urlLabel = new JLabel(result.getUrl());
            urlLabel.setForeground(Color.GRAY);
            headerPanel.add(urlLabel, BorderLayout.CENTER);

            // Tipo de contenido y fecha
            JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            metaPanel.add(new JLabel(getContentTypeLabel(result.getContentType())));

            if (result.getDate() != null) {
                metaPanel.add(new JLabel(result.getDate()));
            }

            headerPanel.add(metaPanel, BorderLayout.SOUTH);

            // Panel central
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));

            // Imagen si existe
            if (result.getContentType() == ContentType.IMAGE || result.getImageUrl() != null) {
                JLabel imageLabel = new JLabel();
                imageLabel.setPreferredSize(new Dimension(200 * zoomLevel / 100, 150 * zoomLevel / 100));

                loadImageAsync(result.getImageUrl(), imageLabel, 200 * zoomLevel / 100);

                contentPanel.add(imageLabel, BorderLayout.WEST);
            }

            // Texto completo
            JTextPane textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setEditable(false);
            textPane.setText("<html><body style='width: " + (400 * zoomLevel / 100) +
                    "px; font-family: Arial; font-size: " + (10 * zoomLevel / 100) + "pt;'>" +
                    result.getSnippet() + "</body></html>");

            // Manejar clics en enlaces dentro del texto
            textPane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        logger.error("Error al abrir URL", ex);
                    }
                }
            });

            JScrollPane snippetScrollPane = new JScrollPane(textPane);
            snippetScrollPane.setPreferredSize(new Dimension(0, 100 * zoomLevel / 100));

            contentPanel.add(snippetScrollPane, BorderLayout.CENTER);

            // Panel inferior con acciones
            JPanel actionPanel = new JPanel(new BorderLayout());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JCheckBox selectCheckBox = new JCheckBox("Seleccionar este resultado");
            selectCheckBox.addActionListener(e -> result.setSelected(selectCheckBox.isSelected()));

            JButton viewButton = new JButton("Ver en navegador");
            viewButton.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(result.getUrl()));
                } catch (Exception ex) {
                    logger.error("Error al abrir URL en navegador", ex);
                }
            });

            buttonPanel.add(selectCheckBox);
            buttonPanel.add(viewButton);

            actionPanel.add(buttonPanel, BorderLayout.CENTER);

            // Ensamblar paneles
            resultPanel.add(headerPanel, BorderLayout.NORTH);
            resultPanel.add(contentPanel, BorderLayout.CENTER);
            resultPanel.add(actionPanel, BorderLayout.SOUTH);

            // Añadir al contenedor
            resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, resultPanel.getPreferredSize().height));
            resultsContainer.add(resultPanel);
        }
    }

    /**
     * Crea una etiqueta con formato de enlace
     */
    private JLabel createLinkLabel(String text, String url) {
        JLabel label = new JLabel("<html><a href='#'>" + text + "</a></html>");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    logger.error("Error al abrir URL en navegador", ex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setText("<html><a href='#'><u>" + text + "</u></a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setText("<html><a href='#'>" + text + "</a></html>");
            }
        });

        return label;
    }

    /**
     * Carga una imagen de forma asíncrona
     */
    private void loadImageAsync(String imageUrl, JLabel imageLabel, int maxSize) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageLabel.setIcon(getPlaceholderIcon(ContentType.IMAGE, 48));
            return;
        }

        imageLoaderService.submit(() -> {
            try {
                URL url = new URL(imageUrl);
                BufferedImage image = ImageIO.read(url);

                if (image != null) {
                    // Redimensionar la imagen manteniendo la proporción
                    double scale = Math.min(
                            (double) maxSize / image.getWidth(),
                            (double) maxSize / image.getHeight()
                    );

                    int width = (int) (image.getWidth() * scale);
                    int height = (int) (image.getHeight() * scale);

                    BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = resizedImage.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(image, 0, 0, width, height, null);
                    g.dispose();

                    ImageIcon icon = new ImageIcon(resizedImage);

                    // Actualizar la UI en el hilo de eventos
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(icon);
                        imageLabel.setText("");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(getPlaceholderIcon(ContentType.IMAGE, 48));
                        imageLabel.setText("No se pudo cargar");
                    });
                }
            } catch (IOException e) {
                logger.error("Error al cargar imagen desde URL: " + imageUrl, e);
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setIcon(getPlaceholderIcon(ContentType.IMAGE, 48));
                    imageLabel.setText("Error al cargar");
                });
            }
        });
    }

    /**
     * Obtiene un icono placeholder según el tipo de contenido
     */
    private ImageIcon getPlaceholderIcon(ContentType type, int size) {
        // Crear iconos de placeholder basados en el tipo de contenido
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Configurar renderizado
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Color de fondo según tipo
        Color backgroundColor;
        String symbol;

        switch (type) {
            case IMAGE:
                backgroundColor = new Color(41, 128, 185); // Azul
                symbol = "🖼";
                break;
            case VIDEO:
                backgroundColor = new Color(192, 57, 43); // Rojo
                symbol = "🎬";
                break;
            case NEWS:
                backgroundColor = new Color(39, 174, 96); // Verde
                symbol = "📰";
                break;
            default:
                backgroundColor = new Color(52, 73, 94); // Gris azulado
                symbol = "📄";
                break;
        }

        // Dibujar fondo redondeado
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(0, 0, size, size, 10, 10);

        // Dibujar símbolo
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));

        FontMetrics fm = g2d.getFontMetrics();
        int stringWidth = fm.stringWidth(symbol);
        int stringHeight = fm.getHeight();

        g2d.drawString(symbol, (size - stringWidth) / 2, size / 2 + stringHeight / 4);

        g2d.dispose();

        return new ImageIcon(image);
    }

    /**
     * Obtiene una etiqueta descriptiva para el tipo de contenido
     */
    private String getContentTypeLabel(ContentType type) {
        switch (type) {
            case TEXT:
                return "Texto";
            case IMAGE:
                return "Imagen";
            case VIDEO:
                return "Video";
            case NEWS:
                return "Noticia";
            default:
                return "General";
        }
    }

    /**
     * Actualiza la etiqueta de estado
     */
    private void updateStatusLabel() {
        if (filteredResults.isEmpty()) {
            statusLabel.setText("No hay resultados para mostrar");
        } else {
            statusLabel.setText(String.format("Mostrando %d de %d resultados",
                    filteredResults.size(), allResults.size()));
        }

        // Habilitar/deshabilitar botón de añadir al prompt
        boolean hasSelectedResults = filteredResults.stream()
                .anyMatch(SearchResultItem::isSelected);

        addToPromptButton.setEnabled(hasSelectedResults);
    }

    /**
     * Filtra los resultados según el tipo de contenido seleccionado
     */
    private void filterResults() {
        filteredResults.clear();

        if (currentContentType == ContentType.ALL) {
            filteredResults.addAll(allResults);
        } else {
            for (SearchResultItem result : allResults) {
                if (result.getContentType() == currentContentType) {
                    filteredResults.add(result);
                }
            }
        }

        updateStatusLabel();
    }

    /**
     * Establece los resultados de búsqueda
     */
    public void setResults(List<WebSearchService.SearchResult> results) {
        allResults.clear();

        // Convertir resultados de WebSearchService a SearchResultItem
        for (WebSearchService.SearchResult result : results) {
            ContentType contentType = determineContentType(result);

            SearchResultItem item = new SearchResultItem(
                    result.getTitle(),
                    result.getUrl(),
                    result.getSnippet(),
                    extractImageUrl(result),
                    contentType
            );

            allResults.add(item);
        }

        // Filtrar y mostrar resultados
        filterResults();
        updateResultsLayout();
    }

    /**
     * Determina el tipo de contenido de un resultado basado en su URL y otros atributos
     */
    private ContentType determineContentType(WebSearchService.SearchResult result) {
        String url = result.getUrl().toLowerCase();
        String title = result.getTitle().toLowerCase();

        // Detectar imágenes
        if (url.matches(".+\\.(jpg|jpeg|png|gif|bmp)$") ||
                url.contains("/images/") ||
                title.contains("image") ||
                title.contains("imagen") ||
                title.contains("photo") ||
                title.contains("foto")) {
            return ContentType.IMAGE;
        }

        // Detectar videos
        if (url.contains("youtube.com") ||
                url.contains("vimeo.com") ||
                url.contains("dailymotion.com") ||
                url.matches(".+\\.(mp4|avi|mov|wmv)$") ||
                title.contains("video") ||
                title.contains("película") ||
                title.contains("watch")) {
            return ContentType.VIDEO;
        }

        // Detectar noticias
        if (url.contains("news") ||
                url.contains("noticias") ||
                url.contains("article") ||
                url.contains("articulo") ||
                title.contains("news") ||
                title.contains("noticia") ||
                title.contains("times") ||
                title.contains("herald") ||
                title.contains("journal")) {
            return ContentType.NEWS;
        }

        // Por defecto, asumir texto
        return ContentType.TEXT;
    }

    /**
     * Extrae la URL de la imagen de un resultado (si existe)
     */
    private String extractImageUrl(WebSearchService.SearchResult result) {
        // Implementación simple: buscar URL de imagen en el snippet
        String snippet = result.getSnippet();

        // Buscar patrones de URL de imagen en el snippet
        if (snippet != null && !snippet.isEmpty()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "https?://[^\\s]+\\.(jpg|jpeg|png|gif|bmp)[^\\s]*",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

            java.util.regex.Matcher matcher = pattern.matcher(snippet);
            if (matcher.find()) {
                return matcher.group(0);
            }
        }

        return null;
    }

    /**
     * Refresca los resultados actuales
     */
    public void refreshResults() {
        // Simplemente vuelve a mostrar los resultados existentes
        updateResultsLayout();
    }

    /**
     * Establece la callback para añadir resultados al prompt
     */
    public void setOnAddToPromptCallback(Runnable callback) {
        this.onAddToPromptCallback = callback;
    }

    /**
     * Obtiene los resultados seleccionados
     */
    public List<SearchResultItem> getSelectedResults() {
        List<SearchResultItem> selected = new ArrayList<>();

        for (SearchResultItem result : allResults) {
            if (result.isSelected()) {
                selected.add(result);
            }
        }

        return selected;
    }

    /**
     * Obtiene el texto formateado de los resultados seleccionados para añadir al prompt
     */
    public String getSelectedResultsText() {
        List<SearchResultItem> selected = getSelectedResults();

        if (selected.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### RESULTADOS DE BÚSQUEDA SELECCIONADOS ###\n\n");

        for (int i = 0; i < selected.size(); i++) {
            SearchResultItem result = selected.get(i);
            sb.append("RESULTADO ").append(i + 1).append(":\n");
            sb.append("Título: ").append(result.getTitle()).append("\n");
            sb.append("URL: ").append(result.getUrl()).append("\n");
            sb.append("Tipo: ").append(getContentTypeLabel(result.getContentType())).append("\n");
            sb.append("Extracto: ").append(result.getSnippet()).append("\n\n");
        }

        sb.append("### FIN DE RESULTADOS SELECCIONADOS ###\n\n");
        return sb.toString();
    }

    public void setDisplayMode(DisplayMode mode) {
        this.currentDisplayMode = mode;
        updateResultsLayout();
    }

    /**
     * Clase para representar un elemento de resultado de búsqueda
     */
    public static class SearchResultItem {
        private final String title;
        private final String url;
        private final String snippet;
        private final String imageUrl;
        private final ContentType contentType;
        private final String date;
        private boolean selected;

        public SearchResultItem(String title, String url, String snippet, String imageUrl, ContentType contentType) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.imageUrl = imageUrl;
            this.contentType = contentType;
            this.date = null;
            this.selected = false;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getSnippet() {
            return snippet;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public String getDate() {
            return date;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}