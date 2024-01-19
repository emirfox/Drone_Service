package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static uk.ac.ed.inf.ilp.constant.OrderStatus.DELIVERED;

/**
 * Optimizes drone routes for delivering orders.
 */
public class RouteOptimizer {
    private final NamedRegion[] noFlyZones;
    private final NamedRegion centralArea;
    private final Restaurant[] restaurants;
    private final List<Order> orders;

    /**
     * Constructor to initialize the RouteOptimizer with necessary data.
     * @param noFlyZones Array of no-fly zones to avoid.
     * @param centralArea Central area for the drone operations.
     * @param restaurants Array of available restaurants.
     * @param orders List of orders to be delivered.
     */
    public RouteOptimizer(NamedRegion[] noFlyZones, NamedRegion centralArea, Restaurant[] restaurants, List<Order> orders) {
        this.noFlyZones = noFlyZones;
        this.centralArea = centralArea;
        this.restaurants = restaurants;
        this.orders = orders;
    }

    /**
     * Finds the location of the restaurant for a given order.
     * @param order The order for which to find the restaurant location.
     * @return The location (LngLat) of the restaurant.
     */
    private LngLat findRestaurantLocation(Order order) {
        for (Restaurant restaurant : restaurants) {
            List<Pizza> orderPizzas = Arrays.asList(order.getPizzasInOrder());
            List<Pizza> restaurantMenu = Arrays.asList(restaurant.menu());
            if (new HashSet<>(restaurantMenu).containsAll(orderPizzas)) {
                return restaurant.location();
            }
        }
        throw new IllegalArgumentException("Restaurant location for the order not found.");
    }

    /**
     * Optimizes and calculates routes for all orders.
     * @return A list of DroneMovement objects representing the optimized routes.
     */
    public List<DroneMovement> optimizeRoutes() {
        DronePathPlanner planner = new DronePathPlanner(noFlyZones, centralArea);
        List<DroneMovement> allRoutes = new ArrayList<>();

        LngLat deliveryPoint = new LngLat(-3.186874, 55.944494); // Appleton Tower coordinates
        for (Order order : orders) {
            LngLat restaurantLocation = findRestaurantLocation(order);

            // Calculate the round trip path for each order
            List<DroneMovement> roundTripRoute = planner.findTotalPath(deliveryPoint, restaurantLocation, order.getOrderNo());
            allRoutes.addAll(roundTripRoute);

            order.setOrderStatus(DELIVERED); // Mark the order as delivered
        }

        return allRoutes;
    }
}
