package OllamaClient.src;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio para realizar búsquedas web y proporcionar resultados
 * para enriquecer las consultas a modelos de lenguaje.
 */
public class WebSearchService {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchService.class);

    // APIs de búsqueda soportadas
    public enum SearchAPI {
        SERPAPI,
        DUCKDUCKGO,
        CUSTOM_GOOGLE
    }

    // API seleccionada por defecto
    private SearchAPI currentAPI = SearchAPI.DUCKDUCKGO;

    // Claves API (necesarias para algunas APIs)
    private String serpApiKey = "";
    private String googleApiKey = "";
    private String googleCseId = "";

    // Executor para procesamiento asíncrono
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Constructor por defecto
     */
    public WebSearchService() {
    }

    /**
     * Establece la API de búsqueda a utilizar
     * @param api API de búsqueda
     */
    public void setSearchAPI(SearchAPI api) {
        this.currentAPI = api;
    }

    /**
     * Establece las claves API para servicios que lo requieren
     * @param apiName Nombre de la API
     * @param apiKey Clave de la API
     */
    public void setApiKey(String apiName, String apiKey) {
        switch (apiName.toLowerCase()) {
            case "serpapi":
                this.serpApiKey = apiKey;
                break;
            case "google":
                this.googleApiKey = apiKey;
                break;
            case "google_cse":
                this.googleCseId = apiKey;
                break;
        }
    }

    /**
     * Realiza una búsqueda web de forma asíncrona
     * @param query Consulta de búsqueda
     * @return CompletableFuture con los resultados de la búsqueda
     */
    public CompletableFuture<List<SearchResult>> searchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return search(query);
            } catch (Exception e) {
                logger.error("Error realizando búsqueda: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }, executor);
    }

    /**
     * Realiza una búsqueda web sincrónica
     * @param query Consulta de búsqueda
     * @return Lista de resultados de búsqueda
     * @throws IOException Si ocurre un error durante la búsqueda
     */
    public List<SearchResult> search(String query) throws IOException {
        switch (currentAPI) {
            case SERPAPI:
                return searchUsingSerpApi(query);
            case CUSTOM_GOOGLE:
                return searchUsingGoogleApi(query);
            case DUCKDUCKGO:
            default:
                return searchUsingDuckDuckGo(query);
        }
    }

    /**
     * Realiza una búsqueda usando la API de SerpApi
     * Requiere clave API: https://serpapi.com/
     */
    private List<SearchResult> searchUsingSerpApi(String query) throws IOException {
        if (serpApiKey.isEmpty()) {
            throw new IOException("SerpAPI requiere una clave API. Por favor, configúrela con setApiKey()");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String urlString = "https://serpapi.com/search.json?q=" + encodedQuery + "&api_key=" + serpApiKey;

        URL serpApiUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) serpApiUrl.openConnection();
        connection.setRequestMethod("GET");

        List<SearchResult> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray organicResults = jsonResponse.getJSONArray("organic_results");

            for (int i = 0; i < organicResults.length() && i < 5; i++) {
                JSONObject result = organicResults.getJSONObject(i);
                String title = result.getString("title");
                String link = result.getString("link");
                String snippet = result.has("snippet") ? result.getString("snippet") : "";

                results.add(new SearchResult(title, link, snippet));
            }
        } finally {
            connection.disconnect();
        }

        return results;
    }

    /**
     * Realiza una búsqueda usando la API de Google Custom Search
     * Requiere clave API y ID CSE: https://developers.google.com/custom-search/v1/overview
     */
    private List<SearchResult> searchUsingGoogleApi(String query) throws IOException {
        if (googleApiKey.isEmpty() || googleCseId.isEmpty()) {
            throw new IOException("Google Custom Search requiere una clave API y un ID CSE. Por favor, configúrelos con setApiKey()");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String urlString = "https://www.googleapis.com/customsearch/v1?key=" + googleApiKey +
                "&cx=" + googleCseId + "&q=" + encodedQuery;

        URL googleApiUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) googleApiUrl.openConnection();
        connection.setRequestMethod("GET");

        List<SearchResult> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            for (int i = 0; i < items.length() && i < 5; i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.getString("title");
                String link = item.getString("link");
                String snippet = item.has("snippet") ? item.getString("snippet") : "";

                results.add(new SearchResult(title, link, snippet));
            }
        } finally {
            connection.disconnect();
        }

        return results;
    }

    /**
     * Realiza una búsqueda usando DuckDuckGo
     * Esta implementación utiliza la API no oficial que no requiere clave API
     */
    private List<SearchResult> searchUsingDuckDuckGo(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        // Cambiado a lite.duckduckgo.com que suele funcionar mejor para solicitudes simples
        String urlString = "https://lite.duckduckgo.com/lite/?q=" + encodedQuery;

        URL duckDuckGoUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) duckDuckGoUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        List<SearchResult> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            String currentTitle = "";
            String currentUrl = "";
            String currentSnippet = "";
            boolean collectingTitle = false;
            boolean collectingUrl = false;
            boolean collectingSnippet = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains("<a rel=\"nofollow\" href=\"")) {
                    // Extraer URL
                    int startIdx = line.indexOf("<a rel=\"nofollow\" href=\"") + 24;
                    int endIdx = line.indexOf("\"", startIdx);
                    if (startIdx > 24 && endIdx > startIdx) {
                        currentUrl = line.substring(startIdx, endIdx);
                        collectingUrl = false;
                        collectingTitle = true;
                    }
                } else if (collectingTitle && line.contains("</a>")) {
                    currentTitle = line.trim();
                    if (currentTitle.endsWith("</a>")) {
                        currentTitle = currentTitle.substring(0, currentTitle.length() - 4);
                    }
                    collectingTitle = false;
                    collectingSnippet = true;
                } else if (collectingSnippet && !line.trim().isEmpty() && !line.contains("<")) {
                    currentSnippet = line.trim();
                    collectingSnippet = false;

                    // Si tenemos un resultado completo, agregarlo a la lista
                    if (!currentTitle.isEmpty() && !currentUrl.isEmpty()) {
                        results.add(new SearchResult(currentTitle, currentUrl, currentSnippet));

                        // Reiniciar variables para el próximo resultado
                        currentTitle = "";
                        currentUrl = "";
                        currentSnippet = "";

                        // Limitar a 5 resultados
                        if (results.size() >= 5) {
                            break;
                        }
                    }
                }
            }

            // Si no se encontraron resultados con el método anterior, intentar con el respaldo
            if (results.isEmpty()) {
                return searchUsingDuckDuckGoAlternative(query);
            }
        } catch (Exception e) {
            logger.error("Error al buscar con DuckDuckGo Lite: " + e.getMessage(), e);
            // En caso de error, intentar con el método de respaldo
            return searchUsingDuckDuckGoAlternative(query);
        } finally {
            connection.disconnect();
        }

        return results;
    }

    /**
     * Método alternativo para buscar con DuckDuckGo en caso de que el principal falle
     */
    private List<SearchResult> searchUsingDuckDuckGoAlternative(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String urlString = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";

        URL duckDuckGoAltUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) duckDuckGoAltUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        List<SearchResult> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());

            // Extraer el abstract (información resumida principal)
            if (jsonResponse.has("AbstractText") && !jsonResponse.getString("AbstractText").isEmpty()) {
                String abstractText = jsonResponse.getString("AbstractText");
                String abstractURL = jsonResponse.getString("AbstractURL");
                String abstractSource = jsonResponse.getString("AbstractSource");

                results.add(new SearchResult(
                        "Abstract from " + abstractSource,
                        abstractURL,
                        abstractText
                ));
            }

            // Extraer resultados relacionados
            if (jsonResponse.has("RelatedTopics")) {
                JSONArray relatedTopics = jsonResponse.getJSONArray("RelatedTopics");

                for (int i = 0; i < relatedTopics.length() && i < 5; i++) {
                    JSONObject topic = relatedTopics.getJSONObject(i);

                    // Algunos temas están anidados
                    if (topic.has("Topics")) {
                        continue;
                    }

                    if (topic.has("Text") && topic.has("FirstURL")) {
                        String text = topic.getString("Text");
                        String url = topic.getString("FirstURL");

                        // Extraer título del texto (hasta la primera coma o los primeros 50 caracteres)
                        String title = text;
                        if (text.contains(" - ")) {
                            title = text.substring(0, text.indexOf(" - "));
                        } else if (text.length() > 50) {
                            title = text.substring(0, 50) + "...";
                        }

                        results.add(new SearchResult(title, url, text));
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        return results;
    }

    /**
     * Formatea los resultados de búsqueda para incluirlos en un prompt
     * @param results Lista de resultados de búsqueda
     * @return Texto formateado para incluir en un prompt
     */
    public String formatSearchResultsForPrompt(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "No se encontraron resultados de búsqueda.";
        }

        StringBuilder formattedResults = new StringBuilder();
        formattedResults.append("### RESULTADOS DE BÚSQUEDA ###\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            formattedResults.append("RESULTADO ").append(i + 1).append(":\n");
            formattedResults.append("Título: ").append(result.getTitle()).append("\n");
            formattedResults.append("URL: ").append(result.getUrl()).append("\n");
            formattedResults.append("Extracto: ").append(result.getSnippet()).append("\n\n");
        }

        formattedResults.append("### FIN DE RESULTADOS DE BÚSQUEDA ###\n\n");
        return formattedResults.toString();
    }

    /**
     * Método auxiliar para realizar una prueba rápida del servicio de búsqueda
     */
    public static void main(String[] args) {
        WebSearchService service = new WebSearchService();
        try {
            List<SearchResult> results = service.search("Java API for searching the web");
            System.out.println(service.formatSearchResultsForPrompt(results));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clase para representar un resultado de búsqueda
     */
    public static class SearchResult {
        private final String title;
        private final String url;
        private final String snippet;

        public SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
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

        @Override
        public String toString() {
            return "Title: " + title + "\nURL: " + url + "\nSnippet: " + snippet;
        }
    }
}