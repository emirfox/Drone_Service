package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The main class of the drone delivery service application.
 * It initializes the service by validating input arguments, checking service health,
 * fetching necessary data, and processing orders for drone delivery routes.
 */
public class App {
    /**
     * The entry point of the application.
     * @param args Command line arguments containing the date and URL for data retrieval.
     * @throws IOException if there's an issue with writing to or reading from files.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Extract and validate command line arguments.
        String date = args[0];
        String url = args[1];
        validateInputArguments(args, date, url);

        // Check if the API service is up and running.
        boolean serviceAlive = ApiDataRetriever.getInstance().serviceAlive(url);
        if (!serviceAlive) {
            throw new IllegalStateException("Service error: Service is not responding");
        }

        // Fetch data from the API using the singleton instance of ApiDataRetriever.
        Restaurant[] restaurants = ApiDataRetriever.getInstance().fetchRestaurants(url);
        Order[] orders = ApiDataRetriever.getInstance().fetchOrders(url, date);
        NamedRegion centralArea = ApiDataRetriever.getInstance().fetchCentralArea(url);
        NamedRegion[] noFlyZones = ApiDataRetriever.getInstance().fetchNoFlyZones(url);

        // Validate and process orders to generate optimal drone delivery paths.
        List<Order> validOrders = validateOrders(orders, restaurants);
        RouteOptimizer optimizer = new RouteOptimizer(noFlyZones, centralArea, restaurants, validOrders);
        List<DroneMovement> paths = optimizer.optimizeRoutes();

        // Prepare file names based on the provided date.
        String year = date.substring(0, 4);
        String month = date.substring(5, 7);
        String day = date.substring(8, 10);

        // Ensure the output directory exists.
        new File("resultfiles").mkdirs();

        // Write the delivery, flightpath, and geojson data to files.
        writeDeliveryJson(orders, year, month, day);
        writeFlightpathJson(paths, year, month, day);
        writeGeoJson(paths, year, month, day);
    }

    // Validates input arguments for proper format and completeness.
    private static void validateInputArguments(String[] args, String date, String url) {
        // Argument count and format checks.
        if (args.length != 2) {
            throw new IllegalArgumentException("Argument error: Two arguments required - date and API URL");
        } else if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date error: Date must be in YYYY-MM-DD format");
        } else if (!url.matches("https://.*")) {
            throw new IllegalArgumentException("URL error: URL must begin with 'https://'");
        }
    }

    // Filters valid orders from all fetched orders based on restaurant menus.
    private static List<Order> validateOrders(Order[] orders, Restaurant[] restaurants) {
        OrderValidationImpl validator = new OrderValidationImpl();
        List<Order> validOrders = new ArrayList<>();
        // Iterate through orders to validate each one.
        for (Order order : orders) {
            Order checkedOrder = validator.validateOrder(order, restaurants);
            // Add order to list if it's valid.
            if (checkedOrder.getOrderStatus() != OrderStatus.INVALID) {
                validOrders.add(order);
            }
        }
        return validOrders;
    }

    // Writes delivery information to a JSON file.
    private static void writeDeliveryJson(Order[] orders, String year, String month, String day) throws IOException {
        try (FileWriter fileWriter = new FileWriter("resultfiles/deliveries-" + year + "-" + month + "-" + day + ".json")) {
            fileWriter.write(OrderDeliveryJsonFormatter.formatDeliveriesToJson(orders));
        }
    }

    // Writes flight paths to a JSON file.
    private static void writeFlightpathJson(List<DroneMovement> paths, String year, String month, String day) throws IOException {
        try (FileWriter fileWriter = new FileWriter("resultfiles/flightpath-" + year + "-" + month + "-" + day + ".json")) {
            fileWriter.write(DroneFlightpathJsonFormatter.formatFlightpathsToJson(paths));
        }
    }

    // Writes drone paths to a GeoJSON file for visualization.
    private static void writeGeoJson(List<DroneMovement> paths, String year, String month, String day) throws IOException {
        try (FileWriter fileWriter = new FileWriter("resultfiles/drone-" + year + "-" + month + "-" + day + ".geojson")) {
            fileWriter.write(DronePathGeoJsonFormatter.formatPathToGeoJson(paths));
        }
    }
}
