package ui;

import algorithm.DSAlgorithm;
import algorithm.RouteOptimizer;
import database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import model.*;
import service.FuelCalculator;
import service.NavigationService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class MainWindow extends BorderPane {
    private final Graph graph = new Graph();
    private final DatabaseManager database = new DatabaseManager();
    private final NavigationService navigation = new NavigationService(graph);
    private final FuelCalculator fuelCalculator = new FuelCalculator();
    private final DSAlgorithm animator = new DSAlgorithm();
    private final GraphCanvas canvas = new GraphCanvas(graph);

    private final ComboBox<Location> sourceBox = new ComboBox<>();
    private final ComboBox<Location> destinationBox = new ComboBox<>();
    private final ComboBox<NavigationService.AlgorithmType> algorithmBox = new ComboBox<>();
    private final ComboBox<RouteOptimizer.Option> optionBox = new ComboBox<>();

    private final Label pathValue = valueLabel("Select two locations to begin");
    private final Label distanceValue = metricValue("-");
    private final Label timeValue = metricValue("-");
    private final Label fuelRequiredValue = metricValue("-");
    private final Label fuelCostValue = metricValue("-");
    private final Label visitedValue = metricValue("-");
    private final Label executionValue = metricValue("-");
    private final Label algorithmValue = metricValue("-");
    private final Label fuelValue = metricValue("-");

    private final Label analyticsValue = valueLabel("-");
    private final Label comparisonValue = valueLabel("Run a route to compare Dijkstra and A*.");
    private final Label statusLabel = new Label("Ready");
    private final Label nodesStatus = new Label();
    private final Label roadsStatus = new Label();
    private final Label algorithmStatus = new Label();
    private final Label zoomStatus = new Label();
    private final Label mouseStatus = new Label("Mouse: -");

    private final TextField mileageField = new TextField("15");
    private final TextField fuelPriceField = new TextField("100");
    private Route currentRoute;

    public MainWindow() {
        getStyleClass().add("app-root");
        database.loadGraph(graph);
        if (graph.getLocations().isEmpty()) createSampleMap();
        setTop(new VBox(createHeader(), createToolbar()));
        setLeft(createSidebar());
        setCenter(createWorkspace());
        setBottom(createStatusBar());
        configureControls();
        refreshAll();
        installShortcutsWhenReady();
    }

    private Node createHeader() {
        Label mark = new Label("N");
        mark.getStyleClass().add("brand-mark");
        Label title = new Label("Smart Navigation");
        title.getStyleClass().add("brand-title");
        Label subtitle = new Label("ROUTE OPTIMIZATION SYSTEM");
        subtitle.getStyleClass().add("brand-subtitle");
        VBox names = new VBox(1, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label state = new Label("LOCAL DATABASE");
        state.getStyleClass().add("database-state");
        HBox header = new HBox(12, mark, names, spacer, state);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");
        return header;
    }

    private Node createToolbar() {
        HBox bar = new HBox(8,
                toolbarButton("New Graph", this::newGraph),
                toolbarButton("Open", this::openGraph),
                toolbarButton("Save", this::saveGraph),
                toolbarButton("Export Graph", this::exportGraph),
                toolbarButton("Import Graph", this::importGraph),
                toolbarButton("Reset Graph", this::resetGraph),
                toolbarButton("About", this::showAbout));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    private Node createSidebar() {
        VBox content = new VBox(16,
                section("ROUTE PLANNER", routeControls()),
                section("MAP EDITOR", editorControls()),
                section("FUEL ESTIMATOR", fuelControls()),
                section("GRAPH ANALYTICS", analyticsValue),
                section("ALGORITHM INFO", algorithmInfo()));
        content.setPadding(new Insets(18));
        content.setPrefWidth(320);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("sidebar");
        return scroll;
    }

    private VBox routeControls() {
        sourceBox.setPromptText("Choose source");
        destinationBox.setPromptText("Choose destination");
        algorithmBox.getItems().setAll(NavigationService.AlgorithmType.values());
        algorithmBox.setValue(NavigationService.AlgorithmType.DIJKSTRA);
        optionBox.getItems().setAll(RouteOptimizer.Option.values());
        optionBox.setValue(RouteOptimizer.Option.SHORTEST_DISTANCE);
        Button swap = secondaryButton("Swap locations", () -> {
            Location source = sourceBox.getValue();
            sourceBox.setValue(destinationBox.getValue());
            destinationBox.setValue(source);
        });
        Button run = new Button("Run algorithm");
        run.getStyleClass().add("primary-button");
        run.setMaxWidth(Double.MAX_VALUE);
        run.setOnAction(event -> runAlgorithm());
        return form(
                field("SOURCE", sourceBox),
                field("DESTINATION", destinationBox),
                swap,
                field("ALGORITHM", algorithmBox),
                field("OPTIMIZE FOR", optionBox),
                run);
    }

    private VBox editorControls() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        addEditorButton(grid, "+ Location", 0, 0, this::addLocation);
        addEditorButton(grid, "Edit node", 1, 0, this::editLocation);
        addEditorButton(grid, "- Location", 0, 1, this::deleteLocation);
        addEditorButton(grid, "+ Road", 1, 1, () -> roadDialog(null));
        addEditorButton(grid, "Edit road", 0, 2, this::editRoad);
        addEditorButton(grid, "- Road", 1, 2, this::deleteRoad);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);
        Label help = new Label("Tip: drag nodes, click a node to select it, and press Delete to remove selected nodes.");
        help.getStyleClass().add("help-text");
        help.setWrapText(true);
        return new VBox(11, grid, help);
    }

    private VBox fuelControls() {
        mileageField.setPromptText("km/l");
        fuelPriceField.setPromptText("Rs/litre");
        Button calculate = secondaryButton("Calculate fuel cost", this::calculateFuel);
        HBox inputs = new HBox(8, field("MILEAGE", mileageField), field("PRICE/L", fuelPriceField));
        HBox.setHgrow(inputs.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(inputs.getChildren().get(1), Priority.ALWAYS);
        return new VBox(10, inputs, calculate, metricCard("FUEL SUMMARY", fuelValue));
    }

    private Node createWorkspace() {
        Label mapTitle = new Label("City Network");
        mapTitle.getStyleClass().add("panel-title");
        Label legend = new Label("Blue normal  |  Green start/route  |  Red destination/closed  |  Orange selected  |  Gray visited");
        legend.getStyleClass().add("legend");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox mapHeader = new HBox(10, mapTitle, spacer, legend);
        mapHeader.setAlignment(Pos.CENTER_LEFT);
        mapHeader.getStyleClass().add("panel-header");
        VBox mapCard = new VBox(mapHeader, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        mapCard.getStyleClass().add("map-card");

        VBox workspace = new VBox(14, mapCard, createResults(), createComparisonPanel());
        VBox.setVgrow(mapCard, Priority.ALWAYS);
        workspace.setPadding(new Insets(18));
        return workspace;
    }

    private Node createResults() {
        Label heading = new Label("Route Result");
        heading.getStyleClass().add("panel-title");
        pathValue.setWrapText(true);
        VBox pathBox = new VBox(4, smallLabel("SHORTEST PATH"), pathValue);
        HBox row1 = new HBox(10,
                metricCard("DISTANCE", distanceValue),
                metricCard("TRAVEL TIME", timeValue),
                metricCard("FUEL REQUIRED", fuelRequiredValue),
                metricCard("FUEL COST", fuelCostValue));
        HBox row2 = new HBox(10,
                metricCard("VISITED NODES", visitedValue),
                metricCard("EXECUTION", executionValue),
                metricCard("ALGORITHM", algorithmValue));
        for (Node node : row1.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);
        for (Node node : row2.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);
        VBox result = new VBox(12, heading, pathBox, row1, row2);
        result.getStyleClass().add("result-card");
        return result;
    }

    private Node createComparisonPanel() {
        Label heading = new Label("Route Comparison: Dijkstra vs A*");
        heading.getStyleClass().add("panel-title");
        comparisonValue.setWrapText(true);
        VBox box = new VBox(10, heading, comparisonValue);
        box.getStyleClass().add("result-card");
        return box;
    }

    private Node createStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(14, statusLabel, spacer, nodesStatus, roadsStatus, algorithmStatus, zoomStatus, mouseStatus);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void configureControls() {
        canvas.setSelectionListener(location -> {
            statusLabel.setText(location == null ? "No node selected" : "Selected: " + location.getName());
            updateStatusBar();
        });
        canvas.setMoveListener(location -> {
            database.saveLocation(location);
            statusLabel.setText("Moved " + location.getName());
            updateStatusBar();
        });
        canvas.setMouseListener((x, y) -> {
            mouseStatus.setText(String.format("Mouse: %.0f, %.0f", x, y));
            zoomStatus.setText(String.format("Zoom: %.0f%%", canvas.getZoomPercent()));
        });
        algorithmBox.setOnAction(event -> updateStatusBar());
        sourceBox.setOnAction(event -> updateEndpoints());
        destinationBox.setOnAction(event -> updateEndpoints());
    }

    private void installShortcutsWhenReady() {
        sceneProperty().addListener((ignored, oldScene, scene) -> {
            if (scene == null) return;
            scene.getAccelerators().put(KeyCombination.valueOf("Ctrl+N"), this::newGraph);
            scene.getAccelerators().put(KeyCombination.valueOf("Ctrl+S"), this::saveGraph);
            scene.getAccelerators().put(KeyCombination.valueOf("Ctrl+O"), this::openGraph);
            scene.getAccelerators().put(KeyCombination.valueOf("Delete"), this::deleteLocation);
        });
    }

    private void runAlgorithm() {
        try {
            animator.stop();
            canvas.clearVisualization();
            updateEndpoints();
            Route route = navigation.findRoute(sourceBox.getValue(), destinationBox.getValue(),
                    algorithmBox.getValue(), optionBox.getValue());
            currentRoute = route;
            showResult(route);
            compareRoutes();
            if (route.isFound()) {
                statusLabel.setText("Animating " + algorithmBox.getValue() + " exploration...");
                animator.animate(route, canvas::showVisit, canvas::showEdge, () -> {
                    canvas.showRoute(route);
                    canvas.setBadges(buildRouteBadges(route));
                    database.saveRoute(route);
                    statusLabel.setText("Route found successfully");
                });
            } else {
                statusLabel.setText("No open route connects these locations");
            }
            updateStatusBar();
        } catch (RuntimeException exception) {
            showError("Cannot calculate route", exception.getMessage());
        }
    }

    private void showResult(Route route) {
        pathValue.setText(route.pathText());
        distanceValue.setText(route.isFound() ? String.format("%.2f km", route.getDistance()) : "-");
        timeValue.setText(route.isFound() ? formatTime(route.getTravelTimeHours()) : "-");
        visitedValue.setText(route.getVisited().size() + " nodes");
        executionValue.setText(String.format("%.3f ms", route.getExecutionNanos() / 1_000_000.0));
        algorithmValue.setText(String.valueOf(algorithmBox.getValue()));
        calculateFuel();
    }

    private void calculateFuel() {
        if (currentRoute == null || !currentRoute.isFound()) {
            fuelValue.setText("Run a route first");
            fuelRequiredValue.setText("-");
            fuelCostValue.setText("-");
            return;
        }
        try {
            double mileage = Double.parseDouble(mileageField.getText());
            double price = Double.parseDouble(fuelPriceField.getText());
            FuelCalculator.FuelResult result = fuelCalculator.calculate(currentRoute.getDistance(), mileage, price);
            fuelRequiredValue.setText(String.format("%.2f L", result.requiredLitres()));
            fuelCostValue.setText(String.format("Rs %.2f", result.cost()));
            fuelValue.setText(String.format("Mileage %.1f km/L | Price Rs %.2f/L | Fuel %.2f L | Cost Rs %.2f",
                    mileage, price, result.requiredLitres(), result.cost()));
        } catch (NumberFormatException exception) {
            showError("Invalid fuel values", "Enter valid numbers for mileage and fuel price.");
        } catch (IllegalArgumentException exception) {
            showError("Invalid fuel values", exception.getMessage());
        }
    }

    private void compareRoutes() {
        Location source = sourceBox.getValue();
        Location destination = destinationBox.getValue();
        if (source == null || destination == null || source.equals(destination)) return;
        try {
            Route dijkstra = navigation.findRoute(source, destination, NavigationService.AlgorithmType.DIJKSTRA,
                    optionBox.getValue());
            Route astar = navigation.findRoute(source, destination, NavigationService.AlgorithmType.A_STAR,
                    optionBox.getValue());
            comparisonValue.setText("Dijkstra  |  " + comparisonText(dijkstra) + "\nA*        |  " +
                    comparisonText(astar));
        } catch (RuntimeException exception) {
            comparisonValue.setText("Comparison unavailable: " + exception.getMessage());
        }
    }

    private String comparisonText(Route route) {
        if (!route.isFound()) return "No route";
        double price = parseOrDefault(fuelPriceField.getText(), 100);
        double mileage = parseOrDefault(mileageField.getText(), 15);
        FuelCalculator.FuelResult fuel = fuelCalculator.calculate(route.getDistance(), mileage, price);
        return String.format("Distance %.2f km | Visited %d | Time %.3f ms | Travel %s | Fuel Rs %.2f",
                route.getDistance(), route.getVisited().size(), route.getExecutionNanos() / 1_000_000.0,
                formatTime(route.getTravelTimeHours()), fuel.cost());
    }

    private void addLocation() {
        TextInputDialog dialog = new TextInputDialog("New Location");
        dialog.setTitle("Add Location");
        dialog.setHeaderText("Create a node on the city map");
        dialog.setContentText("Location name:");
        dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty()).ifPresent(name -> {
            double offset = graph.getLocations().size() * 37 % 280;
            Location location = graph.addLocation(name, 180 + offset, 160 + offset / 2);
            database.saveLocation(location);
            refreshAll();
            canvas.setSelected(location);
            statusLabel.setText("Added " + name);
        });
    }

    private void editLocation() {
        Location selected = requireSelectedLocation();
        if (selected == null) return;
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Edit Location");
        dialog.setHeaderText("Rename " + selected.getName());
        dialog.setContentText("Location name:");
        dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty()).ifPresent(name -> {
            selected.setName(name);
            database.saveLocation(selected);
            refreshAll();
            statusLabel.setText("Location updated");
        });
    }

    private void deleteLocation() {
        Location selected = requireSelectedLocation();
        if (selected == null || !confirm("Delete location?",
                "Delete " + selected.getName() + " and every connected road?")) return;
        database.deleteLocation(selected);
        graph.removeLocation(selected);
        canvas.setSelected(null);
        currentRoute = null;
        clearResults();
        refreshAll();
        statusLabel.setText("Location deleted");
    }

    private void roadDialog(Road existing) {
        if (graph.getLocations().size() < 2) {
            showError("Not enough locations", "Add at least two locations before creating a road.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Road" : "Edit Road");
        dialog.setHeaderText(existing == null ? "Connect two locations" : "Update road properties");
        ButtonType save = new ButtonType("Save road", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Location> from = new ComboBox<>();
        ComboBox<Location> to = new ComboBox<>();
        from.getItems().setAll(graph.getLocations());
        to.getItems().setAll(graph.getLocations());
        TextField distance = new TextField(existing == null ? "10" : String.valueOf(existing.getDistance()));
        TextField speed = new TextField(existing == null ? "50" : String.valueOf(existing.getSpeedLimit()));
        ComboBox<Road.TrafficLevel> traffic = new ComboBox<>();
        traffic.getItems().setAll(Road.TrafficLevel.values());
        ComboBox<Road.RoadType> roadType = new ComboBox<>();
        roadType.getItems().setAll(Road.RoadType.values());
        ComboBox<Road.Status> status = new ComboBox<>();
        status.getItems().setAll(Road.Status.values());
        if (existing == null) {
            from.getSelectionModel().selectFirst();
            to.getSelectionModel().select(1);
            traffic.setValue(Road.TrafficLevel.LOW);
            roadType.setValue(Road.RoadType.CITY);
            status.setValue(Road.Status.OPEN);
        } else {
            from.setValue(existing.getFrom());
            to.setValue(existing.getTo());
            from.setDisable(true);
            to.setDisable(true);
            traffic.setValue(existing.getTrafficLevel());
            roadType.setValue(existing.getRoadType());
            status.setValue(existing.getStatus());
        }
        GridPane form = dialogForm();
        addFormRow(form, 0, "From", from);
        addFormRow(form, 1, "To", to);
        addFormRow(form, 2, "Distance (km)", distance);
        addFormRow(form, 3, "Speed (km/h)", speed);
        addFormRow(form, 4, "Traffic", traffic);
        addFormRow(form, 5, "Road type", roadType);
        addFormRow(form, 6, "Status", status);
        dialog.getDialogPane().setContent(form);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                double roadDistance = Double.parseDouble(distance.getText());
                double roadSpeed = Double.parseDouble(speed.getText());
                if (from.getValue() == null || to.getValue() == null || from.getValue().equals(to.getValue())) {
                    throw new IllegalArgumentException("Choose two different locations.");
                }
                if (existing == null) {
                    Road road = graph.addRoad(from.getValue(), to.getValue(), roadDistance, roadSpeed,
                            traffic.getValue(), roadType.getValue(), status.getValue());
                    database.saveRoad(road);
                } else {
                    existing.setDistance(roadDistance);
                    existing.setSpeedLimit(roadSpeed);
                    existing.setTrafficLevel(traffic.getValue());
                    existing.setRoadType(roadType.getValue());
                    existing.setStatus(status.getValue());
                    database.saveRoad(existing);
                }
            } catch (NumberFormatException exception) {
                event.consume();
                showError("Invalid road values", "Distance and speed must be valid numbers.");
            } catch (IllegalArgumentException exception) {
                event.consume();
                showError("Cannot save road", exception.getMessage());
            }
        });
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) == save) {
            canvas.clearVisualization();
            refreshAll();
            statusLabel.setText(existing == null ? "Road added" : "Road updated");
            if (currentRoute != null && optionBox.getValue() == RouteOptimizer.Option.FASTEST_ROUTE) runAlgorithm();
        }
    }

    private void editRoad() {
        chooseRoad("Edit Road", "Choose a road to update").ifPresent(this::roadDialog);
    }

    private void deleteRoad() {
        chooseRoad("Delete Road", "Choose a road to remove").ifPresent(road -> {
            if (!confirm("Delete road?", "Remove the road between " + road.getFrom() + " and " + road.getTo() + "?")) return;
            database.deleteRoad(road);
            graph.removeRoad(road);
            currentRoute = null;
            clearResults();
            canvas.clearVisualization();
            refreshAll();
            statusLabel.setText("Road deleted");
        });
    }

    private Optional<Road> chooseRoad(String title, String header) {
        if (graph.getRoads().isEmpty()) {
            showError("No roads", "There are no roads on the map.");
            return Optional.empty();
        }
        ChoiceDialog<Road> dialog = new ChoiceDialog<>(graph.getRoads().get(0), graph.getRoads());
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Road:");
        return dialog.showAndWait();
    }

    private void newGraph() {
        if (!confirm("New graph?", "Clear the current graph from the local database?")) return;
        database.clearAll();
        graph.clear();
        currentRoute = null;
        clearResults();
        refreshAll();
        statusLabel.setText("New empty graph created");
    }

    private void openGraph() {
        database.loadGraph(graph);
        currentRoute = null;
        clearResults();
        refreshAll();
        statusLabel.setText("Graph loaded from SQLite database");
    }

    private void saveGraph() {
        graph.getLocations().forEach(database::saveLocation);
        graph.getRoads().forEach(database::saveRoad);
        statusLabel.setText("Graph saved to SQLite database");
        updateStatusBar();
    }

    private void resetGraph() {
        if (!confirm("Reset graph?", "Replace the current graph with the sample city map?")) return;
        database.clearAll();
        graph.clear();
        createSampleMap();
        currentRoute = null;
        clearResults();
        refreshAll();
        statusLabel.setText("Sample graph restored");
    }

    private void exportGraph() {
        FileChooser chooser = graphChooser("Export Graph");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), serializeGraph(), StandardCharsets.UTF_8);
            statusLabel.setText("Graph exported: " + file.getName());
        } catch (IOException exception) {
            showError("Export failed", exception.getMessage());
        }
    }

    private void importGraph() {
        FileChooser chooser = graphChooser("Import Graph");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;
        try {
            loadSerializedGraph(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            database.clearAll();
            saveGraph();
            refreshAll();
            statusLabel.setText("Graph imported: " + file.getName());
        } catch (RuntimeException | IOException exception) {
            showError("Import failed", exception.getMessage());
        }
    }

    private FileChooser graphChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Smart Navigation Graph", "*.sng"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        return chooser;
    }

    private String serializeGraph() {
        StringBuilder text = new StringBuilder("SMART_NAVIGATION_GRAPH_V1\n");
        for (Location location : graph.getLocations()) {
            text.append("LOCATION|").append(location.getId()).append('|').append(clean(location.getName()))
                    .append('|').append(location.getX()).append('|').append(location.getY()).append('\n');
        }
        for (Road road : graph.getRoads()) {
            text.append("ROAD|").append(road.getId()).append('|').append(road.getFrom().getId()).append('|')
                    .append(road.getTo().getId()).append('|').append(road.getDistance()).append('|')
                    .append(road.getSpeedLimit()).append('|').append(road.getTrafficLevel().name()).append('|')
                    .append(road.getRoadType().name()).append('|').append(road.getStatus().name()).append('\n');
        }
        return text.toString();
    }

    private void loadSerializedGraph(List<String> lines) {
        Graph imported = new Graph();
        Map<Integer, Location> byId = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length == 0 || !"LOCATION".equals(parts[0])) continue;
            Location location = new Location(Integer.parseInt(parts[1]), parts[2],
                    Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
            imported.addLocation(location);
            byId.put(location.getId(), location);
        }
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length == 0 || !"ROAD".equals(parts[0])) continue;
            imported.addRoad(new Road(Integer.parseInt(parts[1]), byId.get(Integer.parseInt(parts[2])),
                    byId.get(Integer.parseInt(parts[3])), Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]), Road.TrafficLevel.valueOf(parts[6]),
                    Road.RoadType.valueOf(parts[7]), Road.Status.valueOf(parts[8])));
        }
        graph.clear();
        imported.getLocations().forEach(graph::addLocation);
        imported.getRoads().forEach(graph::addRoad);
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Smart Navigation & Route Optimization System");
        alert.setContentText("""
                Developer: College DSA Project
                Algorithms: BFS, DFS, Dijkstra, A*
                Data Structures: Graph, HashMap, HashSet, Queue, Stack, PriorityQueue
                Java: JavaFX desktop app with Maven and SQLite

                Complexity:
                BFS/DFS: Time O(V+E), Space O(V)
                Dijkstra: Time O((V+E) log V)
                A*: Uses straight-line heuristic to guide shortest route search.
                """);
        alert.showAndWait();
    }

    private void createSampleMap() {
        Location central = graph.addLocation("Central", 160, 170);
        Location museum = graph.addLocation("Museum", 410, 115);
        Location university = graph.addLocation("University", 700, 175);
        Location market = graph.addLocation("Market", 285, 380);
        Location station = graph.addLocation("Station", 575, 335);
        Location airport = graph.addLocation("Airport", 850, 320);
        graph.addRoad(central, museum, 6.5, 50, Road.TrafficLevel.LOW, Road.RoadType.CITY, Road.Status.OPEN);
        graph.addRoad(museum, university, 7.2, 60, Road.TrafficLevel.MEDIUM, Road.RoadType.HIGHWAY, Road.Status.OPEN);
        graph.addRoad(central, market, 8.0, 40, Road.TrafficLevel.HIGH, Road.RoadType.CITY, Road.Status.OPEN);
        graph.addRoad(museum, market, 5.4, 45, Road.TrafficLevel.LOW, Road.RoadType.CITY, Road.Status.OPEN);
        graph.addRoad(market, station, 9.1, 55, Road.TrafficLevel.MEDIUM, Road.RoadType.SERVICE, Road.Status.OPEN);
        graph.addRoad(university, station, 6.8, 50, Road.TrafficLevel.LOW, Road.RoadType.CITY, Road.Status.OPEN);
        graph.addRoad(university, airport, 12.0, 80, Road.TrafficLevel.LOW, Road.RoadType.EXPRESSWAY, Road.Status.OPEN);
        graph.addRoad(station, airport, 10.2, 65, Road.TrafficLevel.HIGH, Road.RoadType.HIGHWAY, Road.Status.OPEN);
        graph.addRoad(central, station, 15.5, 70, Road.TrafficLevel.LOW, Road.RoadType.HIGHWAY, Road.Status.CLOSED);
        saveGraph();
    }

    private void refreshAll() {
        Location source = sourceBox.getValue();
        Location destination = destinationBox.getValue();
        sourceBox.getItems().setAll(graph.getLocations());
        destinationBox.getItems().setAll(graph.getLocations());
        if (source != null && graph.getLocations().contains(source)) sourceBox.setValue(source);
        else if (!sourceBox.getItems().isEmpty()) sourceBox.getSelectionModel().selectFirst();
        if (destination != null && graph.getLocations().contains(destination)) destinationBox.setValue(destination);
        else if (destinationBox.getItems().size() > 1) destinationBox.getSelectionModel().selectLast();
        updateEndpoints();
        updateAnalytics();
        updateStatusBar();
        canvas.refresh();
    }

    private void updateEndpoints() {
        canvas.setRouteEndpoints(sourceBox.getValue(), destinationBox.getValue());
    }

    private void updateAnalytics() {
        int nodes = graph.getLocations().size();
        int roads = graph.getRoads().size();
        int components = countComponents();
        long disconnected = graph.getLocations().stream().filter(location -> graph.getRoads(location).isEmpty()).count();
        double averageDegree = nodes == 0 ? 0 : (2.0 * roads) / nodes;
        double density = nodes <= 1 ? 0 : (2.0 * roads) / (nodes * (nodes - 1));
        boolean cycles = roads >= nodes - components && nodes > 0;
        analyticsValue.setText(String.format("""
                Total Nodes: %d
                Total Roads: %d
                Average Degree: %.2f
                Connected Components: %d
                Graph Density: %.2f
                Disconnected Nodes: %d
                Cycles Detected: %s
                """, nodes, roads, averageDegree, components, density, disconnected, cycles ? "Yes" : "No"));
    }

    private int countComponents() {
        Set<Location> seen = new HashSet<>();
        int components = 0;
        for (Location start : graph.getLocations()) {
            if (!seen.add(start)) continue;
            components++;
            Deque<Location> stack = new ArrayDeque<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                Location current = stack.pop();
                for (Road road : graph.getRoads(current)) {
                    Location next = road.other(current);
                    if (seen.add(next)) stack.push(next);
                }
            }
        }
        return components;
    }

    private Map<Location, String> buildRouteBadges(Route route) {
        Map<Location, String> labels = new HashMap<>();
        double total = 0;
        List<Location> path = route.getPath();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                total += graph.findRoad(path.get(i - 1), path.get(i)).map(Road::getDistance).orElse(0.0);
            }
            labels.put(path.get(i), String.format("%.1f km", total));
        }
        if (algorithmBox.getValue() == NavigationService.AlgorithmType.A_STAR && destinationBox.getValue() != null) {
            Location destination = destinationBox.getValue();
            for (Location location : route.getVisited()) {
                labels.putIfAbsent(location, String.format("h %.0f", location.distanceTo(destination)));
            }
        }
        return labels;
    }

    private void updateStatusBar() {
        nodesStatus.setText("Nodes: " + graph.getLocations().size());
        roadsStatus.setText("Roads: " + graph.getRoads().size());
        algorithmStatus.setText("Algorithm: " + algorithmBox.getValue());
        zoomStatus.setText(String.format("Zoom: %.0f%%", canvas.getZoomPercent()));
    }

    private void clearResults() {
        pathValue.setText("Select two locations to begin");
        distanceValue.setText("-");
        timeValue.setText("-");
        fuelRequiredValue.setText("-");
        fuelCostValue.setText("-");
        visitedValue.setText("-");
        executionValue.setText("-");
        algorithmValue.setText("-");
        fuelValue.setText("-");
        comparisonValue.setText("Run a route to compare Dijkstra and A*.");
        canvas.clearVisualization();
    }

    private VBox algorithmInfo() {
        Label info = valueLabel("""
                BFS: O(V+E), Space O(V)
                DFS: O(V+E), Space O(V)
                Dijkstra: O((V+E) log V)
                A*: heuristic guides search toward destination
                """);
        info.setWrapText(true);
        return new VBox(info);
    }

    private Location requireSelectedLocation() {
        Location location = canvas.getSelected();
        if (location == null) showError("No location selected", "Click a location on the map first.");
        return location;
    }

    private VBox section(String title, Node content) {
        Label label = smallLabel(title);
        VBox box = new VBox(11, label, content);
        box.getStyleClass().add("sidebar-section");
        return box;
    }

    private VBox form(Node... nodes) { return new VBox(10, nodes); }

    private VBox field(String label, Node node) {
        if (node instanceof Control control) control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, smallLabel(label), node);
    }

    private Label smallLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("path-value");
        return label;
    }

    private Label metricValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metric-value");
        return label;
    }

    private VBox metricCard(String title, Label value) {
        VBox box = new VBox(4, smallLabel(title), value);
        box.getStyleClass().add("metric-card");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Button secondaryButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button toolbarButton(String text, Runnable action) {
        Button button = secondaryButton(text, action);
        button.getStyleClass().add("toolbar-button");
        return button;
    }

    private void addEditorButton(GridPane grid, String text, int column, int row, Runnable action) {
        Button button = secondaryButton(text, action);
        grid.add(button, column, row);
        GridPane.setHgrow(button, Priority.ALWAYS);
    }

    private GridPane dialogForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));
        return grid;
    }

    private void addFormRow(GridPane grid, int row, String label, Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }

    private String formatTime(double hours) {
        long minutes = Math.round(hours * 60);
        return minutes >= 60 ? String.format("%dh %02dm", minutes / 60, minutes % 60) : minutes + " min";
    }

    private double parseOrDefault(String text, double fallback) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String clean(String text) {
        return text.replace("|", " ").trim();
    }
}