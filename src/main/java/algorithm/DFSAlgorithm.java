package algorithm;

import model.*;
import java.util.*;

public class DFSAlgorithm {
    public Route findRoute(Graph graph, Location source, Location destination, RouteOptimizer.Option ignored) {
        long start = System.nanoTime();
        Deque<Location> stack = new ArrayDeque<>();
        Set<Location> discovered = new HashSet<>();
        Map<Location, Location> previous = new HashMap<>();
        List<Location> visited = new ArrayList<>();
        stack.push(source);
        discovered.add(source);
        while (!stack.isEmpty()) {
            Location current = stack.pop();
            visited.add(current);
            if (current.equals(destination)) break;
            List<Road> roads = new ArrayList<>(graph.getRoads(current));
            Collections.reverse(roads);
            for (Road road : roads) {
                if (!road.isOpen()) continue;
                Location neighbor = road.other(current);
                if (discovered.add(neighbor)) {
                    previous.put(neighbor, current);
                    stack.push(neighbor);
                }
            }
        }
        return new Route(DijkstraAlgorithm.buildPath(previous, source, destination),
                visited, graph, System.nanoTime() - start);
    }
}