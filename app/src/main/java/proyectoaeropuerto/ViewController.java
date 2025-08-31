package proyectoaeropuerto;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViewController {

    private SistemaDeVuelos sistema;
    private final Map<Aeropuerto, Node> nodosVisuales = new HashMap<>();
    private final Map<Aeropuerto, Pair<Double, Double>> posiciones = new HashMap<>();

    // --- Componentes inyectados desde el FXML ---
    @FXML
    private ComboBox<Aeropuerto> origenComboBox;
    @FXML
    private ComboBox<Aeropuerto> destinoComboBox;
    @FXML
    private ToggleGroup criterioGroup;
    @FXML
    private RadioButton rbDistancia;
    @FXML
    private Pane graphPane;
    @FXML
    private Label resultadoLabel;

    /**
     * Se ejecuta al iniciar la aplicación. Carga los datos y configura la UI.
     */
    @FXML
    public void initialize() {
        sistema = new SistemaDeVuelos();
        sistema.cargarDatosDesdeArchivos();
        actualizarVistas();
        resultadoLabel.setText("Bienvenido al Sistema de Gestión de Vuelos.");
    }

    /**
     * Maneja el evento del botón "Buscar Ruta".
     */
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
        Dialog<Pair<String, String>> dialog = new Dialog<>();
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
                if (!codigo.getText().isEmpty() && !nombre.getText().isEmpty() && !ciudad.getText().isEmpty()) {
                    return new Pair<>(codigo.getText().toUpperCase(), nombre.getText() + "," + ciudad.getText());
                }
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(datos -> {
            String[] nombreCiudad = datos.getValue().split(",");
            sistema.agregarAeropuerto(datos.getKey(), nombreCiudad[0], nombreCiudad[1]);
            sistema.guardarAeropuertoEnArchivo(new Aeropuerto(datos.getKey(), nombreCiudad[0], nombreCiudad[1]));
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Vuelo ya existente");
        });
    }

    @FXML
    private void handleAnadirVuelo() {
        Dialog<Vuelo> dialog = new Dialog<>();
        dialog.setTitle("Añadir Nuevo Vuelo");
        dialog.setHeaderText("Configure los detalles del nuevo vuelo.");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        ComboBox<Aeropuerto> origenCombo = new ComboBox<>(FXCollections.observableArrayList(aeropuertos));
        ComboBox<Aeropuerto> destinoCombo = new ComboBox<>(FXCollections.observableArrayList(aeropuertos));
        TextField distanciaField = new TextField();
        distanciaField.setPromptText("Distancia en km");
        TextField tiempoField = new TextField();
        tiempoField.setPromptText("Tiempo en minutos");
        TextField costoField = new TextField();
        costoField.setPromptText("Costo en USD");

        grid.add(new Label("Origen:"), 0, 0);
        grid.add(origenCombo, 1, 0);
        grid.add(new Label("Destino:"), 0, 1);
        grid.add(destinoCombo, 1, 1);
        grid.add(new Label("Distancia:"), 0, 2);
        grid.add(distanciaField, 1, 2);
        grid.add(new Label("Tiempo:"), 0, 3);
        grid.add(tiempoField, 1, 3);
        grid.add(new Label("Costo:"), 0, 4);
        grid.add(costoField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Aeropuerto origen = origenCombo.getValue();
                Aeropuerto destino = destinoCombo.getValue();

                if (origen == null || destino == null) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "Debe seleccionar un origen y un destino.");
                    return null;
                }
                if (origen.equals(destino)) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "El origen y el destino no pueden ser el mismo.");
                    return null;
                }
                try {
                    int distancia = Integer.parseInt(distanciaField.getText());
                    int tiempo = Integer.parseInt(tiempoField.getText());
                    double costo = Double.parseDouble(costoField.getText());
                    return new Vuelo(origen, destino, new PesoVuelo(distancia, tiempo, costo));
                } catch (NumberFormatException e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error de validación", "Los campos de distancia, tiempo y costo deben ser números válidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<Vuelo> result = dialog.showAndWait();
        result.ifPresent(vuelo -> {
            sistema.agregarVuelo(vuelo.getOrigen().getCodigoIATA(), vuelo.getDestino().getCodigoIATA(), vuelo.getDistancia(), vuelo.getTiempo(), vuelo.getCosto());
            sistema.guardarVueloEnArchivo(vuelo);
            actualizarVistas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo añadido correctamente.");
        });
    }

    /**
     * Muestra las estadísticas de la red de vuelos.
     */
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
    
    /**
     * Actualiza todos los componentes de la UI, como los ComboBox y el grafo.
     */
    private void actualizarVistas() {
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        origenComboBox.getItems().setAll(aeropuertos);
        destinoComboBox.getItems().setAll(aeropuertos);
        dibujarGrafo();
    }

    /**
     * Dibuja el grafo completo en el Pane.
     */
    private void dibujarGrafo() {
        graphPane.getChildren().clear();
        nodosVisuales.clear();
        posiciones.clear();
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        if (aeropuertos.isEmpty()) return;

        // --- 1. Calcular posiciones de los nodos (PKX en el centro) ---
        double width = graphPane.getWidth() > 0 ? graphPane.getWidth() : 800;
        double height = graphPane.getHeight() > 0 ? graphPane.getHeight() : 600;
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) * 0.4;

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

        // --- 2. Dibujar Vuelos (Arcos, Flechas y Pesos) ---
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
            graphPane.getChildren().add(linea);
            
            double angulo = Math.atan2(y2 - y1, x2 - x1);
            // Aumentamos el tamaño de la flecha
            double tamFlecha = 20.0; 
            
            double xFlecha1 = x2 - tamFlecha * Math.cos(angulo - Math.PI / 6);
            double yFlecha1 = y2 - tamFlecha * Math.sin(angulo - Math.PI / 6);
            double xFlecha2 = x2 - tamFlecha * Math.cos(angulo + Math.PI / 6);
            double yFlecha2 = y2 - tamFlecha * Math.sin(angulo + Math.PI / 6);
            
            Line flecha1 = new Line(x2, y2, xFlecha1, yFlecha1);
            Line flecha2 = new Line(x2, y2, xFlecha2, yFlecha2);
            
            // Hacemos las líneas de la flecha más gruesas
            flecha1.setStrokeWidth(1.5);
            flecha2.setStrokeWidth(1.5);
            flecha1.setStroke(Color.DARKGRAY);
            flecha2.setStroke(Color.DARKGRAY);

            graphPane.getChildren().addAll(flecha1, flecha2);
            // ==========================================================

            Text textoPeso = new Text((x1 + x2) / 2 + 5, (y1 + y2) / 2 - 5, String.valueOf(vuelo.getDistancia()));
            textoPeso.setFill(Color.BLUE);
            graphPane.getChildren().add(textoPeso);
        }

        // --- 3. Dibujar Aeropuertos (Nodos) encima de las líneas ---
        for (Aeropuerto apt : posiciones.keySet()) {
            Pair<Double, Double> pos = posiciones.get(apt);
            double x = pos.getKey();
            double y = pos.getValue();
            
            Color colorNodo = apt.getCodigoIATA().equals("PKX") ? Color.CRIMSON : Color.STEELBLUE;
            Circle circulo = new Circle(x, y, 15, colorNodo);
            circulo.setStroke(Color.BLACK);
            
            Text texto = new Text(x - 15, y - 20, apt.getCodigoIATA());
            
            nodosVisuales.put(apt, circulo);
            graphPane.getChildren().addAll(circulo, texto);
        }
    }

    private void resaltarRuta(Ruta ruta) {
        // Restaurar color original de todos los nodos
        nodosVisuales.forEach((apt, node) -> {
            Color colorOriginal = apt.getCodigoIATA().equals("PKX") ? Color.CRIMSON : Color.STEELBLUE;
            ((Circle) node).setFill(colorOriginal);
        });
        // Resaltar nodos de la ruta
        for (Aeropuerto apt : ruta.getAeropuertos()) {
            Node nodo = nodosVisuales.get(apt);
            if (nodo != null) {
                ((Circle) nodo).setFill(Color.ORANGE);
            }
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

