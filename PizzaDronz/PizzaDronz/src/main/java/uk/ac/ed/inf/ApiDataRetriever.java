package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.Order;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Singleton class that handles the retrieval of data from a RESTful API.
 * It provides methods to check the service status and fetch various types
 * of data related to drone delivery services.
 */
public class ApiDataRetriever {

    // Singleton instance to ensure only one instance of the HttpClient and ObjectMapper.
    private static ApiDataRetriever instance;
    // HttpClient to send requests to the API.
    private final HttpClient client;
    // ObjectMapper to map JSON response to Java objects.
    private final ObjectMapper objectMapper;

    // Private constructor to initialize the HttpClient and ObjectMapper.
    private ApiDataRetriever() {
        client = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    // Method to get the singleton instance of ApiDataRetriever.
    public static synchronized ApiDataRetriever getInstance() {
        if (instance == null) {
            instance = new ApiDataRetriever();
        }
        return instance;
    }

    // Helper method to send a GET request to the specified URI and return the response body as a String.
    private String sendRequest(String uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch data: HTTP " + response.statusCode() + " for URI " + uri);
        }
        return response.body();
    }

    // Checks if the API service is available and responsive.
    public boolean serviceAlive(String url) throws IOException, InterruptedException {
        String urlString = url + "/isAlive";
        String response = sendRequest(urlString);
        return Boolean.parseBoolean(response);
    }

    // Retrieves the list of restaurants from the API.
    public Restaurant[] fetchRestaurants(String url) throws IOException, InterruptedException {
        String urlString = url + "/restaurants";
        String response = sendRequest(urlString);
        return objectMapper.readValue(response, Restaurant[].class);
    }

    // Fetches the orders for a specific date from the API.
    public Order[] fetchOrders(String url, String date) throws IOException, InterruptedException {
        String urlString = url + "/orders/" + date;
        String response = sendRequest(urlString);
        return objectMapper.readValue(response, Order[].class);
    }

    // Obtains the central area coordinates from the API.
    public NamedRegion fetchCentralArea(String url) throws IOException, InterruptedException {
        String urlString = url + "/centralArea";
        String response = sendRequest(urlString);
        return objectMapper.readValue(response, NamedRegion.class);
    }

    // Gets the no-fly zones from the API.
    public NamedRegion[] fetchNoFlyZones(String url) throws IOException, InterruptedException {
        String urlString = url + "/noFlyZones";
        String response = sendRequest(urlString);
        return objectMapper.readValue(response, NamedRegion[].class);
    }
}
