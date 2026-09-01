package service;

import model.Road;

public class TrafficService {
    public double adjustedTravelTime(Road road) {
        return road.travelTimeHours();
    }

    public String description(Road road) {
        return switch (road.getTrafficLevel()) {
            case LOW -> "Traffic is flowing normally";
            case MEDIUM -> "Moderate congestion";
            case HIGH -> "Heavy congestion—expect delays";
        };
    }
}