package OllamaClient.src;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel para explorar y seleccionar archivos de un directorio
 */
public class FileExplorerPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(FileExplorerPanel.class);

    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private JButton selectFolderButton;
    private JButton clearSelectionButton;
    private JButton analyzeSelectedButton;
    private JTextField currentFolderField;
    private JScrollPane treeScrollPane;

    private File rootFolder;
    private final Map<String, Boolean> selectedFiles = new HashMap<>();
    private final Set<String> supportedExtensions = new HashSet<>();

    // Consumidor para notificar cuando se seleccionan archivos para análisis
    private Consumer<List<FileInfo>> onFilesSelectedForAnalysis;

    /**
     * Construye un panel explorador de archivos
     */
    public FileExplorerPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Explorador de Archivos"));

        // Configurar extensiones de archivos soportadas
        configureSupportedExtensions();

        // Inicializar componentes
        initComponents();

        // Diseñar el panel
        layoutComponents();

        // Inicializar con un árbol vacío
        initEmptyTree();
    }

    /**
     * Configura las extensiones de archivos soportadas
     */
    private void configureSupportedExtensions() {
        // Texto y documentos
        supportedExtensions.addAll(Arrays.asList("txt", "md", "csv", "json", "xml", "html", "pdf", "docx"));

        // Código fuente
        supportedExtensions.addAll(Arrays.asList("java", "py", "js", "c", "cpp", "h", "cs", "php", "rb", "go", "rs", "ts"));

        // Imágenes (para modelos multimodales)
        supportedExtensions.addAll(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp"));
    }

    /**
     * Inicializa los componentes del panel
     */
    private void initComponents() {
        // Carpeta actual
        JPanel folderPanel = new JPanel(new BorderLayout(5, 0));
        folderPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        JLabel folderLabel = new JLabel("Carpeta: ");
        currentFolderField = new JTextField();
        currentFolderField.setEditable(false);

        folderPanel.add(folderLabel, BorderLayout.WEST);
        folderPanel.add(currentFolderField, BorderLayout.CENTER);

        // Botones de acción
        selectFolderButton = new JButton("Seleccionar Carpeta");
        clearSelectionButton = new JButton("Limpiar Selección");
        analyzeSelectedButton = new JButton("Analizar Seleccionados");

        // Árbol de archivos
        fileTree = new JTree();
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        fileTree.setCellRenderer(new CheckboxTreeCellRenderer());
        fileTree.setShowsRootHandles(true);
        fileTree.setRootVisible(false);

        // Panel de desplazamiento para el árbol
        treeScrollPane = new JScrollPane(fileTree);

        // Configurar manejadores de eventos
        setupEventHandlers();
    }

    /**
     * Configura la disposición de los componentes
     */
    private void layoutComponents() {
        // Panel superior con la ruta de la carpeta y botón de selección
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        topPanel.add(currentFolderField, BorderLayout.CENTER);
        topPanel.add(selectFolderButton, BorderLayout.EAST);

        // Panel inferior con botones de acción
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(clearSelectionButton);
        bottomPanel.add(analyzeSelectedButton);

        // Añadir componentes al panel principal
        add(topPanel, BorderLayout.NORTH);
        add(treeScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Manejador para seleccionar carpeta
        selectFolderButton.addActionListener(e -> selectFolder());

        // Manejador para limpiar selección
        clearSelectionButton.addActionListener(e -> clearSelection());

        // Manejador para analizar archivos seleccionados
        analyzeSelectedButton.addActionListener(e -> analyzeSelectedFiles());

        // Manejador para clicks en el árbol (para checkboxes)
        fileTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = fileTree.getClosestRowForLocation(evt.getX(), evt.getY());
                if (row != -1) {
                    Rectangle bounds = fileTree.getRowBounds(row);
                    if (bounds != null && evt.getX() < bounds.x + 20) { // Aproximadamente donde está el checkbox
                        TreePath path = fileTree.getPathForRow(row);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof FileNode) {
                            FileNode fileNode = (FileNode) node.getUserObject();
                            if (!fileNode.isDirectory()) {
                                fileNode.setSelected(!fileNode.isSelected());
                                selectedFiles.put(fileNode.getFilePath(), fileNode.isSelected());
                                fileTree.repaint();
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Inicializa el árbol con un nodo vacío
     */
    private void initEmptyTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode("Sin carpeta seleccionada", true, ""));
        treeModel = new DefaultTreeModel(root);
        fileTree.setModel(treeModel);
    }

    /**
     * Abre un diálogo para seleccionar una carpeta
     */
    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar Carpeta");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rootFolder = chooser.getSelectedFile();
            currentFolderField.setText(rootFolder.getAbsolutePath());
            refreshTree();
        }
    }

    /**
     * Refresca el árbol de archivos con el contenido de la carpeta seleccionada
     */
    private void refreshTree() {
        if (rootFolder == null || !rootFolder.exists() || !rootFolder.isDirectory()) {
            initEmptyTree();
            return;
        }

        FileNode rootNode = new FileNode(rootFolder.getName(), true, rootFolder.getAbsolutePath());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootNode);

        populateTree(root, rootFolder);

        treeModel = new DefaultTreeModel(root);
        fileTree.setModel(treeModel);

        // Expandir el árbol
        for (int i = 0; i < fileTree.getRowCount(); i++) {
            fileTree.expandRow(i);
        }
    }

    /**
     * Rellena el árbol con los archivos y subcarpetas
     */
    private void populateTree(DefaultMutableTreeNode parentNode, File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        // Primero agregar directorios
        Arrays.stream(files)
                .filter(File::isDirectory)
                .sorted()
                .forEach(file -> {
                    FileNode fileNode = new FileNode(file.getName(), true, file.getAbsolutePath());
                    DefaultMutableTreeNode directoryNode = new DefaultMutableTreeNode(fileNode);
                    parentNode.add(directoryNode);
                    populateTree(directoryNode, file);
                });

        // Luego agregar archivos
        Arrays.stream(files)
                .filter(file -> !file.isDirectory())
                .filter(this::isSupportedFile)
                .sorted()
                .forEach(file -> {
                    String filePath = file.getAbsolutePath();
                    boolean isSelected = selectedFiles.getOrDefault(filePath, false);

                    FileNode fileNode = new FileNode(file.getName(), false, filePath);
                    fileNode.setSelected(isSelected);
                    selectedFiles.put(filePath, isSelected);

                    DefaultMutableTreeNode fileTreeNode = new DefaultMutableTreeNode(fileNode);
                    parentNode.add(fileTreeNode);
                });
    }

    /**
     * Verifica si un archivo es de un tipo soportado
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            return supportedExtensions.contains(extension);
        }

        return false;
    }

    /**
     * Limpia todas las selecciones de archivos
     */
    private void clearSelection() {
        selectedFiles.clear();
        refreshTree();
    }

    /**
     * Analiza los archivos seleccionados
     */
    private void analyzeSelectedFiles() {
        List<FileInfo> selectedFilesList = new ArrayList<>();

        selectedFiles.forEach((path, selected) -> {
            if (selected) {
                try {
                    File file = new File(path);
                    if (file.exists() && file.isFile()) {
                        String extension = getFileExtension(path);
                        boolean isImage = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp").contains(extension);

                        FileInfo fileInfo = new FileInfo(
                                file.getName(),
                                path,
                                extension,
                                isImage
                        );
                        selectedFilesList.add(fileInfo);
                    }
                } catch (Exception e) {
                    logger.error("Error al procesar archivo seleccionado: " + path, e);
                }
            }
        });

        if (selectedFilesList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay archivos seleccionados para analizar.",
                    "Sin selección",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (onFilesSelectedForAnalysis != null) {
            onFilesSelectedForAnalysis.accept(selectedFilesList);
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Establece el consumidor para notificar cuando se seleccionan archivos para análisis
     */
    public void setOnFilesSelectedForAnalysis(Consumer<List<FileInfo>> consumer) {
        this.onFilesSelectedForAnalysis = consumer;
    }

    /**
     * Clase para representar un nodo en el árbol de archivos
     */
    private static class FileNode {
        private final String name;
        private final boolean isDirectory;
        private final String filePath;
        private boolean selected;

        public FileNode(String name, boolean isDirectory, String filePath) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.filePath = filePath;
            this.selected = false;
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public String getFilePath() {
            return filePath;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Renderizador personalizado para mostrar checkboxes en el árbol
     */
    private class CheckboxTreeCellRenderer extends DefaultTreeCellRenderer {
        private final JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component renderer = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof FileNode) {
                    FileNode fileNode = (FileNode) userObject;

                    if (fileNode.isDirectory()) {
                        setIcon(openIcon);
                        checkbox.setVisible(false);
                        return renderer;
                    } else {
                        checkbox.setSelected(fileNode.isSelected());
                        checkbox.setText(fileNode.getName());
                        checkbox.setOpaque(false);

                        // Establecer icono según extensión
                        String extension = getFileExtension(fileNode.getFilePath());
                        if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp").contains(extension)) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/image.png")));
                        } else if (Arrays.asList("pdf").contains(extension)) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/pdf.png")));
                        } else if (Arrays.asList("docx", "doc").contains(extension)) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/word.png")));
                        } else {
                            setIcon(leafIcon);
                        }

                        return checkbox;
                    }
                }
            }

            return renderer;
        }
    }

    /**
     * Clase para almacenar información sobre un archivo seleccionado
     */
    public static class FileInfo {
        private final String name;
        private final String path;
        private final String extension;
        private final boolean isImage;
        private String content;

        public FileInfo(String name, String path, String extension, boolean isImage) {
            this.name = name;
            this.path = path;
            this.extension = extension;
            this.isImage = isImage;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getExtension() {
            return extension;
        }

        public boolean isImage() {
            return isImage;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        /**
         * Carga el contenido del archivo
         */
        public void loadContent() throws IOException {
            if (isImage) {
                // Para imágenes no cargamos el contenido como texto
                return;
            }

            content = new String(Files.readAllBytes(Paths.get(path)));
        }

        @Override
        public String toString() {
            return name;
        }
    }
}