# Smart Navigation & Route Optimization System

A JavaFX-based desktop application that demonstrates graph algorithms and route optimization through an interactive city-road network.

The system allows users to create and edit a graph of locations and roads, select a source and destination, choose different pathfinding algorithms and optimization strategies, and visualize the resulting route.

---

## Overview

The **Smart Navigation & Route Optimization System** is designed as a practical implementation of concepts from **Design and Analysis of Algorithms (DAA)**.

The application represents a city as a weighted graph:

- **Vertices (V)** → Locations such as College, Hospital, Airport, etc.
- **Edges (E)** → Roads connecting locations
- **Edge weights** → Distance, travel time, traffic, and other route-related factors

The user can experiment with different algorithms and optimization criteria and observe how the selected route changes.

---

## Features

### 🗺️ Interactive Graph Visualization

- Visual representation of locations and roads
- Select source and destination
- Drag locations on the graph
- Add and remove locations
- Add and remove roads
- Edit road properties
- Visual indication of visited nodes and selected routes
- Closed roads are visually distinguished

### 🧠 Pathfinding Algorithms

The system includes:

- **Dijkstra's Algorithm**
- **A* Search**
- **Breadth-First Search (BFS)**
- **Depth-First Search (DFS)**

These algorithms demonstrate different approaches to graph traversal and pathfinding.

### ⚙️ Route Optimization

Routes can be optimized using:

- Shortest Distance
- Fastest Route
- Fuel Efficient
- Avoid Traffic
- Avoid Closed Roads

Different optimization strategies assign different costs to roads before the pathfinding algorithm determines the route.

### 🚦 Traffic Management

Each road can have a traffic condition:

- Low
- Medium
- High

Traffic affects estimated travel time and can influence route selection.

### 🛣️ Road Properties

Each road can store:

- Distance
- Speed Limit
- Traffic Level
- Road Type
- Open / Closed status

Supported road types include:

- City Road
- Highway
- Expressway
- Service Road

### ⛽ Fuel Calculator

The application can estimate:

- Fuel required
- Fuel cost
- Mileage
- Fuel price per litre

### 📊 Graph Analytics

The application provides graph-level statistics such as:

- Total Nodes
- Total Roads
- Average Degree
- Connected Components
- Graph Density
- Disconnected Nodes
- Cycle Detection

### 💾 Local Database

The application uses **SQLite** for local persistence.

It stores:

- Locations
- Roads
- Road properties
- Saved routes

### 📁 Graph Management

Users can:

- Create a new graph
- Open saved data
- Save graph data
- Import graph data
- Export graph data
- Reset the graph

---

## Algorithms

### Dijkstra's Algorithm

Dijkstra's algorithm finds the minimum-cost path from a source vertex to a destination in a weighted graph with non-negative edge weights.

In this application, the edge weight depends on the selected optimization mode.

**Typical complexity:**

```text
Time:  O((V + E) log V)
Space: O(V)
