package service;

import algorithm.*;
import model.*;

public class NavigationService {
    public enum AlgorithmType {
        DIJKSTRA("Dijkstra"), A_STAR("A*"), BFS("BFS"), DFS("DFS");
        private final String label;
        AlgorithmType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final Graph graph;

    public NavigationService(Graph graph) {
        this.graph = graph;
    }

    public Route findRoute(Location source, Location destination, AlgorithmType algorithm,
                           RouteOptimizer.Option option) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Choose a source and destination");
        }
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Source and destination must be different");
        }
        return switch (algorithm) {
            case DIJKSTRA -> new DijkstraAlgorithm().findRoute(graph, source, destination, option);
            case A_STAR -> new AStarAlgorithm().findRoute(graph, source, destination, option);
            case BFS -> new BFSAlgorithm().findRoute(graph, source, destination, option);
            case DFS -> new DFSAlgorithm().findRoute(graph, source, destination, option);
        };
    }
}