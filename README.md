# 🚗 Smart Navigation & Route Optimization System

A desktop-based **Smart Navigation and Route Optimization System** built using **Java, JavaFX, Maven, and SQLite**. The application demonstrates how classical **graph algorithms and data structures** can be applied to real-world navigation and route-planning problems.

The system allows users to create and manage a road network, select source and destination locations, choose different graph algorithms, and optimize routes according to distance, travel time, fuel efficiency, traffic, and road availability.

---

## 📌 Project Overview

The Smart Navigation & Route Optimization System models a city as a **weighted graph**:

- **Locations** → Graph vertices (V)
- **Roads** → Graph edges (E)
- **Distance** → Edge weight
- **Speed limit** → Used for travel-time calculation
- **Traffic level** → Affects travel time and route optimization
- **Road status** → Open or Closed

The application then uses graph traversal and shortest-path algorithms to find and visualize routes between locations.

---

## ✨ Key Features

### 🗺️ Interactive Graph Visualization

- Visual representation of the city road network
- Locations represented as graph nodes
- Roads represented as graph edges
- Distance labels displayed on roads
- Interactive node selection
- Drag-and-drop node positioning
- Closed roads visually distinguished
- Selected routes highlighted

### 🧭 Route Planning

Users can select:

- Source location
- Destination location
- Graph algorithm
- Route optimization strategy

The system calculates and displays the resulting route.

### ⚙️ Supported Algorithms

| Algorithm | Purpose | Complexity |
|---|---|---|
| **BFS** | Graph traversal / unweighted traversal | O(V + E) |
| **DFS** | Depth-first graph traversal | O(V + E) |
| **Dijkstra** | Weighted shortest-path routing | O((V + E) log V) |
| **A\*** | Heuristic-based shortest-path search | Depends on implementation |

### 🎯 Route Optimization Modes

The application supports multiple optimization strategies:

#### Shortest Distance

Finds the route with the minimum total road distance.

#### Fastest Route

Uses estimated travel time based on:

```text
Travel Time = Distance / Speed × Traffic Multiplier
