package algorithm;

import model.*;
import java.util.*;

public class AStarAlgorithm {
    private record Entry(Location location, double score) {}

    public Route findRoute(Graph graph, Location source, Location destination, RouteOptimizer.Option option) {
        long start = System.nanoTime();
        Map<Location, Double> gScore = new HashMap<>();
        Map<Location, Location> previous = new HashMap<>();
        PriorityQueue<Entry> open = new PriorityQueue<>(Comparator.comparingDouble(Entry::score));
        Set<Location> closed = new HashSet<>();
        List<Location> visited = new ArrayList<>();
        double heuristicScale = heuristicScale(graph, option);
        for (Location location : graph.getLocations()) gScore.put(location, Double.POSITIVE_INFINITY);
        gScore.put(source, 0.0);
        open.add(new Entry(source, heuristic(source, destination, heuristicScale)));

        while (!open.isEmpty()) {
            Location current = open.poll().location();
            if (!closed.add(current)) continue;
            visited.add(current);
            if (current.equals(destination)) break;
            for (Road road : graph.getRoads(current)) {
                double weight = RouteOptimizer.weight(road, option);
                if (!Double.isFinite(weight)) continue;
                Location neighbor = road.other(current);
                double tentative = gScore.get(current) + weight;
                if (tentative < gScore.get(neighbor)) {
                    previous.put(neighbor, current);
                    gScore.put(neighbor, tentative);
                    open.add(new Entry(neighbor, tentative + heuristic(neighbor, destination, heuristicScale)));
                }
            }
        }
        return new Route(DijkstraAlgorithm.buildPath(previous, source, destination),
                visited, graph, System.nanoTime() - start);
    }

    private double heuristic(Location a, Location b, double scale) {
        return a.distanceTo(b) * scale;
    }

    private double heuristicScale(Graph graph, RouteOptimizer.Option option) {
        double minimum = Double.POSITIVE_INFINITY;
        for (Road road : graph.getRoads()) {
            double pixels = road.getFrom().distanceTo(road.getTo());
            double weight = RouteOptimizer.weight(road, option);
            if (pixels > 0 && Double.isFinite(weight)) minimum = Math.min(minimum, weight / pixels);
        }
        return Double.isFinite(minimum) ? minimum : 0;
    }
}