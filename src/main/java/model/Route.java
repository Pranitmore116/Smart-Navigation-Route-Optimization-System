package model;

import java.util.*;

public class Route {
    private final List<Location> path;
    private final List<Location> visited;
    private final double distance;
    private final double travelTimeHours;
    private final long executionNanos;

    public Route(List<Location> path, List<Location> visited, Graph graph, long executionNanos) {
        this.path = List.copyOf(path);
        this.visited = List.copyOf(visited);
        this.executionNanos = executionNanos;
        double distanceSum = 0;
        double timeSum = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            Road road = graph.findRoad(path.get(i), path.get(i + 1)).orElseThrow();
            distanceSum += road.getDistance();
            timeSum += road.travelTimeHours();
        }
        this.distance = distanceSum;
        this.travelTimeHours = timeSum;
    }

    public List<Location> getPath() { return path; }
    public List<Location> getVisited() { return visited; }
    public double getDistance() { return distance; }
    public double getTravelTimeHours() { return travelTimeHours; }
    public long getExecutionNanos() { return executionNanos; }
    public boolean isFound() { return !path.isEmpty(); }
    public String pathText() {
        return path.isEmpty() ? "No route found" :
                String.join("  →  ", path.stream().map(Location::getName).toList());
    }
}