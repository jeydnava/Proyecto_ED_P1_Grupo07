package proyectoaeropuerto;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewController {

    private SistemaDeVuelos sistema;
    private final Map<Aeropuerto, Node> nodosVisuales = new HashMap<>();
    private final Map<Aeropuerto, Pair<Double, Double>> posiciones = new HashMap<>();

    private final Group graphContentGroup = new Group();
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    @FXML
    private ComboBox<Aeropuerto> origenComboBox;
    @FXML
    private ComboBox<Aeropuerto> destinoComboBox;
    @FXML
    private ToggleGroup criterioGroup;
    @FXML
    private Pane graphPane;
    @FXML
    private Label resultadoLabel;

    @FXML
    public void initialize() {
        sistema = new SistemaDeVuelos();
        sistema.cargarDatosDesdeArchivos();

        graphPane.getChildren().add(graphContentGroup);
        setupPanningAndZooming();

        actualizarVistas();
        resultadoLabel.setText("Bienvenido al Sistema de Gestión de Vuelos.");
    }

    private void setupPanningAndZooming() {
        graphPane.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                event.consume();
            }
        });

        graphPane.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;
                graphContentGroup.setTranslateX(graphContentGroup.getTranslateX() + deltaX);
                graphContentGroup.setTranslateY(graphContentGroup.getTranslateY() + deltaY);
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                event.consume();
            }
        });

        graphPane.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            graphContentGroup.setScaleX(graphContentGroup.getScaleX() * zoomFactor);
            graphContentGroup.setScaleY(graphContentGroup.getScaleY() * zoomFactor);
            event.consume();
        });
    }

    @FXML
    private void handleBuscarRuta() {
        Aeropuerto origen = origenComboBox.getValue();
        Aeropuerto destino = destinoComboBox.getValue();

        if (origen == null || destino == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Debe seleccionar un aeropuerto de origen y destino.");
            return;
        }

        RadioButton rbSeleccionado = (RadioButton) criterioGroup.getSelectedToggle();
        Ruta.Criterio criterio = Ruta.Criterio.DISTANCIA;
        if (rbSeleccionado != null) {
            switch (rbSeleccionado.getText()) {
                case "Tiempo":
                    criterio = Ruta.Criterio.TIEMPO;
                    break;
                case "Costo":
                    criterio = Ruta.Criterio.COSTO;
                    break;
            }
        }

        Ruta ruta = sistema.buscarRutaMasCorta(origen.getCodigoIATA(), destino.getCodigoIATA(), criterio);
        resultadoLabel.setText(ruta.toString());
        resaltarRuta(ruta);
    }

    @FXML
    private void handleAnadirAeropuerto() {
        Dialog<Aeropuerto> dialog = new Dialog<>();
        dialog.setTitle("Añadir Nuevo Aeropuerto");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField codigo = new TextField();
        codigo.setPromptText("Ej: GYE");
        TextField nombre = new TextField();
        nombre.setPromptText("Ej: José Joaquín de Olmedo");
        TextField ciudad = new TextField();
        ciudad.setPromptText("Ej: Guayaquil");

        grid.add(new Label("Código IATA:"), 0, 0);
        grid.add(codigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(nombre, 1, 1);
        grid.add(new Label("Ciudad:"), 0, 2);
        grid.add(ciudad, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (codigo.getText().isEmpty() || nombre.getText().isEmpty() || ciudad.getText().isEmpty()) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "Todos los campos son obligatorios.");
                    return null;
                }
                if (sistema.getAeropuertoPorCodigo(codigo.getText().toUpperCase()) != null) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "El código IATA ya existe.");
                    return null;
                }
                return new Aeropuerto(codigo.getText().toUpperCase(), nombre.getText(), ciudad.getText());
            }
            return null;
        });

        Optional<Aeropuerto> result = dialog.showAndWait();
        result.ifPresent(aeropuerto -> {
            sistema.agregarAeropuerto(aeropuerto.getCodigoIATA(), aeropuerto.getNombre(), aeropuerto.getCiudad());
            sistema.guardarAeropuertoEnArchivo(aeropuerto);
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto añadido correctamente.");
        });
    }

    @FXML
    private void handleAnadirVuelo() {
        Dialog<Vuelo> dialog = crearDialogoVuelo(null);
        Optional<Vuelo> result = dialog.showAndWait();
        result.ifPresent(vuelo -> {
            sistema.agregarVuelo(vuelo.getOrigen().getCodigoIATA(), vuelo.getDestino().getCodigoIATA(), vuelo.getDistancia(), vuelo.getTiempo(), vuelo.getCosto());
            sistema.guardarVueloEnArchivo(vuelo);
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo añadido correctamente.");
        });
    }

    @FXML
    private void handleVerEstadisticas() {
        String masConectado = "Aeropuerto más conectado: " + sistema.getAeropuertoMasConectado().toString();
        String menosConectado = "Aeropuerto menos conectado: " + sistema.getAeropuertoMenosConectado().toString();
        String conexiones = "\nConexiones por Aeropuerto:\n" + sistema.getEstadisticasConexiones();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de la Red");
        alert.setHeaderText(masConectado + "\n" + menosConectado);
        alert.setContentText(conexiones);
        alert.showAndWait();
    }

    @FXML
    private void handleGestionarVuelos() {
        Stage stage = new Stage();
        stage.setTitle("Gestión de Vuelos");

        TableView<Vuelo> tableView = new TableView<>();
        tableView.setItems(FXCollections.observableArrayList(sistema.getTodosLosVuelos()));

        TableColumn<Vuelo, String> origenCol = new TableColumn<>("Origen");
        origenCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrigen().getCodigoIATA()));

        TableColumn<Vuelo, String> destinoCol = new TableColumn<>("Destino");
        destinoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDestino().getCodigoIATA()));

        TableColumn<Vuelo, Integer> distanciaCol = new TableColumn<>("Distancia (km)");
        distanciaCol.setCellValueFactory(new PropertyValueFactory<>("distancia"));

        TableColumn<Vuelo, Integer> tiempoCol = new TableColumn<>("Tiempo (min)");
        tiempoCol.setCellValueFactory(new PropertyValueFactory<>("tiempo"));

        TableColumn<Vuelo, Double> costoCol = new TableColumn<>("Costo ($");
        costoCol.setCellValueFactory(new PropertyValueFactory<>("costo"));

        TableColumn<Vuelo, Void> accionesCol = new TableColumn<>("Acciones");
        accionesCol.setCellFactory(param -> new TableCell<>() {
            private final Button editarBtn = new Button("Editar");
            private final Button eliminarBtn = new Button("Eliminar");
            private final HBox pane = new HBox(5, editarBtn, eliminarBtn);

            {
                editarBtn.setOnAction(event -> {
                    Vuelo vuelo = getTableView().getItems().get(getIndex());
                    handleEditarVuelo(vuelo, tableView);
                });
                eliminarBtn.setOnAction(event -> {
                    Vuelo vuelo = getTableView().getItems().get(getIndex());
                    handleEliminarVuelo(vuelo, tableView);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableView.getColumns().addAll(origenCol, destinoCol, distanciaCol, tiempoCol, costoCol, accionesCol);

        BorderPane layout = new BorderPane(tableView);
        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void actualizarVistas() {
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        origenComboBox.getItems().setAll(aeropuertos);
        destinoComboBox.getItems().setAll(aeropuertos);
        dibujarGrafo();
    }

    private void dibujarGrafo() {
        graphContentGroup.getChildren().clear();
        nodosVisuales.clear();
        posiciones.clear();
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        if (aeropuertos.isEmpty()) return;

        double width = graphPane.getWidth() > 0 ? graphPane.getWidth() : 1200;
        double height = graphPane.getHeight() > 0 ? graphPane.getHeight() : 1000;
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) * 0.9;

        Aeropuerto pkx = sistema.getAeropuertoPorCodigo("PKX");
        if (pkx != null) {
            posiciones.put(pkx, new Pair<>(centerX, centerY));
            aeropuertos.remove(pkx);
        }

        for (int i = 0; i < aeropuertos.size(); i++) {
            double angle = 2 * Math.PI * i / aeropuertos.size();
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            posiciones.put(aeropuertos.get(i), new Pair<>(x, y));
        }
        if (pkx != null) aeropuertos.add(pkx);

        for (Vuelo vuelo : sistema.getTodosLosVuelos()) {
            Pair<Double, Double> posOrigen = posiciones.get(vuelo.getOrigen());
            Pair<Double, Double> posDestino = posiciones.get(vuelo.getDestino());
            if (posOrigen == null || posDestino == null) continue;

            double x1 = posOrigen.getKey();
            double y1 = posOrigen.getValue();
            double x2 = posDestino.getKey();
            double y2 = posDestino.getValue();

            Line linea = new Line(x1, y1, x2, y2);
            linea.setStroke(Color.DARKGRAY);
            linea.setStrokeWidth(1.5);

            double angulo = Math.atan2(y2 - y1, x2 - x1);
            double tamFlecha = 10.0;
            double xFlecha1 = x2 - tamFlecha * Math.cos(angulo - Math.PI / 6);
            double yFlecha1 = y2 - tamFlecha * Math.sin(angulo - Math.PI / 6);
            double xFlecha2 = x2 - tamFlecha * Math.cos(angulo + Math.PI / 6);
            double yFlecha2 = y2 - tamFlecha * Math.sin(angulo + Math.PI / 6);

            Line flecha1 = new Line(x2, y2, xFlecha1, yFlecha1);
            Line flecha2 = new Line(x2, y2, xFlecha2, yFlecha2);
            flecha1.setStroke(Color.DARKGRAY);
            flecha2.setStroke(Color.DARKGRAY);
            flecha1.setStrokeWidth(1.5);
            flecha2.setStrokeWidth(1.5);

            Text textoPeso = new Text((x1 + x2) / 2, (y1 + y2) / 2 - 5, String.valueOf(vuelo.getDistancia()));
            textoPeso.setFill(Color.BLUE);

            graphContentGroup.getChildren().addAll(linea, flecha1, flecha2, textoPeso);
        }

        for (Aeropuerto apt : posiciones.keySet()) {
            Pair<Double, Double> pos = posiciones.get(apt);
            Circle circulo = new Circle(pos.getKey(), pos.getValue(), 15, apt.getCodigoIATA().equals("PKX") ? Color.CRIMSON : Color.STEELBLUE);
            circulo.setStroke(Color.BLACK);

            Text texto = new Text(pos.getKey() - 15, pos.getValue() - 20, apt.getCodigoIATA());
            texto.setMouseTransparent(true);

            circulo.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    handleMostrarVuelosDeAeropuerto(apt);
                }
            });

            ContextMenu contextMenu = new ContextMenu();
            MenuItem editarItem = new MenuItem("Editar Aeropuerto");
            editarItem.setOnAction(e -> handleEditarAeropuerto(apt));
            MenuItem eliminarItem = new MenuItem("Eliminar Aeropuerto");
            eliminarItem.setOnAction(e -> handleEliminarAeropuerto(apt));
            contextMenu.getItems().addAll(editarItem, eliminarItem);
            circulo.setOnContextMenuRequested(e -> contextMenu.show(circulo, e.getScreenX(), e.getScreenY()));

            nodosVisuales.put(apt, circulo);
            graphContentGroup.getChildren().addAll(circulo, texto);
        }
    }

    private void resaltarRuta(Ruta ruta) {
        nodosVisuales.forEach((apt, node) -> {
            Color colorOriginal = apt.getCodigoIATA().equals("PKX") ? Color.CRIMSON : Color.STEELBLUE;
            ((Circle) node).setFill(colorOriginal);
        });
        for (Aeropuerto apt : ruta.getAeropuertos()) {
            Node nodo = nodosVisuales.get(apt);
            if (nodo != null) {
                ((Circle) nodo).setFill(Color.ORANGE);
            }
        }
    }

    private void handleMostrarVuelosDeAeropuerto(Aeropuerto aeropuerto) {
        List<Vuelo> vuelos = sistema.getVuelosDeAeropuerto(aeropuerto.getCodigoIATA());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vuelos de " + aeropuerto.getCodigoIATA());
        dialog.setHeaderText("Mostrando todos los vuelos de salida y llegada para " + aeropuerto.getNombre());

        ListView<String> salidasListView = new ListView<>();
        ListView<String> llegadasListView = new ListView<>();

        List<String> salidas = vuelos.stream()
                .filter(v -> v.getOrigen().getCodigoIATA().equals(aeropuerto.getCodigoIATA()))
                .map(v -> "-> " + v.getDestino().getCodigoIATA() + " (Dist: " + v.getDistancia() + " km)")
                .collect(Collectors.toList());

        List<String> llegadas = vuelos.stream()
                .filter(v -> v.getDestino().getCodigoIATA().equals(aeropuerto.getCodigoIATA()))
                .map(v -> "<- " + v.getOrigen().getCodigoIATA() + " (Dist: " + v.getDistancia() + " km)")
                .collect(Collectors.toList());

        salidasListView.setItems(FXCollections.observableArrayList(salidas));
        llegadasListView.setItems(FXCollections.observableArrayList(llegadas));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("Vuelos de Salida:"), 0, 0);
        grid.add(salidasListView, 0, 1);
        grid.add(new Label("Vuelos de Llegada:"), 1, 0);
        grid.add(llegadasListView, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleEditarAeropuerto(Aeropuerto aeropuerto) {
        Dialog<Aeropuerto> dialog = new Dialog<>();
        dialog.setTitle("Editar Aeropuerto");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField codigo = new TextField(aeropuerto.getCodigoIATA());
        codigo.setDisable(true);
        TextField nombre = new TextField(aeropuerto.getNombre());
        TextField ciudad = new TextField(aeropuerto.getCiudad());

        grid.add(new Label("Código IATA:"), 0, 0);
        grid.add(codigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(nombre, 1, 1);
        grid.add(new Label("Ciudad:"), 0, 2);
        grid.add(ciudad, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (nombre.getText().isEmpty() || ciudad.getText().isEmpty()) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "Nombre y ciudad no pueden estar vacíos.");
                    return null;
                }
                aeropuerto.setNombre(nombre.getText());
                aeropuerto.setCiudad(ciudad.getText());
                return aeropuerto;
            }
            return null;
        });

        Optional<Aeropuerto> result = dialog.showAndWait();
        result.ifPresent(apt -> {
            sistema.editarAeropuerto(apt.getCodigoIATA(), apt.getNombre(), apt.getCiudad());
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto '" + apt.getCodigoIATA() + "' actualizado.");
        });
    }

    private void handleEliminarAeropuerto(Aeropuerto aeropuerto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Aeropuerto: " + aeropuerto.getCodigoIATA());
        alert.setContentText("¿Está seguro de que desea eliminar este aeropuerto? Se borrarán todos los vuelos asociados.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sistema.eliminarAeropuerto(aeropuerto);
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto eliminado.");
        }
    }

    private void handleEditarVuelo(Vuelo vuelo, TableView<Vuelo> tableView) {
        Dialog<Vuelo> dialog = crearDialogoVuelo(vuelo);
        Optional<Vuelo> result = dialog.showAndWait();
        result.ifPresent(vueloEditado -> {
            sistema.editarVuelo(vuelo, vueloEditado.getDistancia(), vueloEditado.getTiempo(), vueloEditado.getCosto());
            actualizarVistas();
            tableView.setItems(FXCollections.observableArrayList(sistema.getTodosLosVuelos()));
            tableView.refresh();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo actualizado.");
        });
    }

    private void handleEliminarVuelo(Vuelo vuelo, TableView<Vuelo> tableView) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Vuelo: " + vuelo.getOrigen().getCodigoIATA() + " -> " + vuelo.getDestino().getCodigoIATA());
        alert.setContentText("¿Está seguro de que desea eliminar este vuelo?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sistema.eliminarVuelo(vuelo);
            actualizarVistas();
            tableView.setItems(FXCollections.observableArrayList(sistema.getTodosLosVuelos()));
            tableView.refresh();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo eliminado.");
        }
    }

    private Dialog<Vuelo> crearDialogoVuelo(Vuelo vueloExistente) {
        Dialog<Vuelo> dialog = new Dialog<>();
        dialog.setTitle(vueloExistente == null ? "Añadir Nuevo Vuelo" : "Editar Vuelo");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Aeropuerto> origenCombo = new ComboBox<>(FXCollections.observableArrayList(sistema.getTodosLosAeropuertos()));
        ComboBox<Aeropuerto> destinoCombo = new ComboBox<>(FXCollections.observableArrayList(sistema.getTodosLosAeropuertos()));
        TextField distanciaField = new TextField();
        TextField tiempoField = new TextField();
        TextField costoField = new TextField();

        if (vueloExistente != null) {
            origenCombo.setValue(vueloExistente.getOrigen());
            destinoCombo.setValue(vueloExistente.getDestino());
            origenCombo.setDisable(true);
            destinoCombo.setDisable(true);
            distanciaField.setText(String.valueOf(vueloExistente.getDistancia()));
            tiempoField.setText(String.valueOf(vueloExistente.getTiempo()));
            costoField.setText(String.valueOf(vueloExistente.getCosto()));
        } else {
            distanciaField.setPromptText("Distancia en km");
            tiempoField.setPromptText("Tiempo en minutos");
            costoField.setPromptText("Costo en USD");
        }

        grid.add(new Label("Origen:"), 0, 0);
        grid.add(origenCombo, 1, 0);
        grid.add(new Label("Destino:"), 0, 1);
        grid.add(destinoCombo, 1, 1);
        grid.add(new Label("Distancia (km):"), 0, 2);
        grid.add(distanciaField, 1, 2);
        grid.add(new Label("Tiempo (min):"), 0, 3);
        grid.add(tiempoField, 1, 3);
        grid.add(new Label("Costo ($):"), 0, 4);
        grid.add(costoField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    int distancia = Integer.parseInt(distanciaField.getText());
                    int tiempo = Integer.parseInt(tiempoField.getText());
                    double costo = Double.parseDouble(costoField.getText());
                    return new Vuelo(origenCombo.getValue(), destinoCombo.getValue(), new PesoVuelo(distancia, tiempo, costo));
                } catch (NumberFormatException e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "Los campos de distancia, tiempo y costo deben ser números válidos.");
                    return null;
                }
            }
            return null;
        });
        return dialog;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}