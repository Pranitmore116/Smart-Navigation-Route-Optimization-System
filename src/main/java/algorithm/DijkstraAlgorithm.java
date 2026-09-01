package algorithm;

import model.*;
import java.util.*;

public class DijkstraAlgorithm {
    private record Entry(Location location, double cost) {}

    public Route findRoute(Graph graph, Location source, Location destination, RouteOptimizer.Option option) {
        long start = System.nanoTime();
        Map<Location, Double> distances = new HashMap<>();
        Map<Location, Location> previous = new HashMap<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingDouble(Entry::cost));
        List<Location> visitedOrder = new ArrayList<>();
        Set<Location> settled = new HashSet<>();
        for (Location location : graph.getLocations()) distances.put(location, Double.POSITIVE_INFINITY);
        distances.put(source, 0.0);
        queue.add(new Entry(source, 0));

        while (!queue.isEmpty()) {
            Entry entry = queue.poll();
            Location current = entry.location();
            if (!settled.add(current)) continue;
            visitedOrder.add(current);
            if (current.equals(destination)) break;
            for (Road road : graph.getRoads(current)) {
                double weight = RouteOptimizer.weight(road, option);
                if (!Double.isFinite(weight)) continue;
                Location neighbor = road.other(current);
                double candidate = distances.get(current) + weight;
                if (candidate < distances.get(neighbor)) {
                    distances.put(neighbor, candidate);
                    previous.put(neighbor, current);
                    queue.add(new Entry(neighbor, candidate));
                }
            }
        }
        return new Route(buildPath(previous, source, destination), visitedOrder, graph, System.nanoTime() - start);
    }

    static List<Location> buildPath(Map<Location, Location> previous, Location source, Location destination) {
        if (!source.equals(destination) && !previous.containsKey(destination)) return List.of();
        LinkedList<Location> path = new LinkedList<>();
        for (Location at = destination; at != null; at = previous.get(at)) path.addFirst(at);
        return path.get(0).equals(source) ? path : List.of();
    }
}