package algorithm;

import model.Road;

public final class RouteOptimizer {
    public enum Option {
        SHORTEST_DISTANCE("Shortest Distance"),
        FASTEST_ROUTE("Fastest Route"),
        FUEL_EFFICIENT("Fuel Efficient"),
        AVOID_TRAFFIC("Avoid Traffic"),
        AVOID_CLOSED_ROADS("Avoid Closed Roads");

        private final String label;
        Option(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private RouteOptimizer() {
    }

 public static double weight(Road road, Option option) {
    if (!road.isOpen()) return Double.POSITIVE_INFINITY;

    return switch (option) {

        case FASTEST_ROUTE ->
            road.travelTimeHours();

        case AVOID_TRAFFIC -> {
    double congestionPenalty = switch (road.getTrafficLevel()) {
        case LOW -> 1.0;
        case MEDIUM -> 1.5;
        case HIGH -> 2.5;
    };

    yield road.travelTimeHours() * congestionPenalty;
}

        case FUEL_EFFICIENT -> {
            double speedPenalty = 1 + Math.abs(60 - road.getSpeedLimit()) / 50.0;
            double trafficPenalty = road.getTrafficLevel().multiplier();
            yield road.getDistance() * speedPenalty * trafficPenalty;
        }

        case SHORTEST_DISTANCE, AVOID_CLOSED_ROADS ->
            road.getDistance();
    };
 }
}