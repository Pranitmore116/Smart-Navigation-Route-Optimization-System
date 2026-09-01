package algorithm;

import model.*;
import java.util.*;

public class BFSAlgorithm {
    public Route findRoute(Graph graph, Location source, Location destination, RouteOptimizer.Option ignored) {
        long start = System.nanoTime();
        Queue<Location> queue = new ArrayDeque<>();
        Set<Location> discovered = new HashSet<>();
        Map<Location, Location> previous = new HashMap<>();
        List<Location> visited = new ArrayList<>();
        queue.offer(source);
        discovered.add(source);
        while (!queue.isEmpty()) {
            Location current = queue.poll();
            visited.add(current);
            if (current.equals(destination)) break;
            for (Road road : graph.getRoads(current)) {
                if (!road.isOpen()) continue;
                Location neighbor = road.other(current);
                if (discovered.add(neighbor)) {
                    previous.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }
        return new Route(DijkstraAlgorithm.buildPath(previous, source, destination),
                visited, graph, System.nanoTime() - start);
    }
}