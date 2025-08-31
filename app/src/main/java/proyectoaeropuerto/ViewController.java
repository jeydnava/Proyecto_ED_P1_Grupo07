package proyectoaeropuerto;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewController {

    private SistemaDeVuelos sistema;
    private final Map<Aeropuerto, Node> nodosDelGrafo = new HashMap<>();

    // --- Componentes inyectados desde el FXML ---
    @FXML
    private ComboBox<Aeropuerto> origenComboBox;
    @FXML
    private ComboBox<Aeropuerto> destinoComboBox;
    @FXML
    private ToggleGroup criterioGroup;
    @FXML
    private RadioButton rbDistancia; // Asegúrate de poner este fx:id en tu FXML
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
            mostrarAlerta("Error", "Debe seleccionar un aeropuerto de origen y destino.");
            return;
        }

        RadioButton rbSeleccionado = (RadioButton) criterioGroup.getSelectedToggle();
        Ruta.Criterio criterio = Ruta.Criterio.DISTANCIA; // Por defecto
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

    /**
     * Muestra un diálogo para añadir un nuevo aeropuerto.
     */
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
            mostrarAlerta("Éxito", "Aeropuerto añadido correctamente.");
        });
    }

    /**
     * Muestra un diálogo para añadir un nuevo vuelo.
     */
    @FXML
    private void handleAnadirVuelo() {
        // Implementación similar a handleAnadirAeropuerto, pero con ComboBoxes y campos para los pesos.
        // Por brevedad, se omite, pero seguiría el mismo patrón de diálogo.
        mostrarAlerta("Info", "Funcionalidad para añadir vuelo no implementada en este ejemplo.");
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
        // Poblar ComboBoxes
        List<Aeropuerto> aeropuertos = sistema.getTodosLosAeropuertos().stream().collect(Collectors.toList());
        origenComboBox.getItems().clear();
        destinoComboBox.getItems().clear();
        origenComboBox.getItems().addAll(aeropuertos);
        destinoComboBox.getItems().addAll(aeropuertos);
        
        // Redibujar el grafo
        dibujarGrafo();
    }

    /**
     * Dibuja el grafo completo en el Pane.
     */
    private void dibujarGrafo() {
        graphPane.getChildren().clear();
        nodosDelGrafo.clear();
        List<Aeropuerto> aeropuertos = sistema.getTodosLosAeropuertos().stream().collect(Collectors.toList());
        List<Vuelo> vuelos = sistema.getTodosLosVuelos();

        if (aeropuertos.isEmpty()) return;

        // --- Dibujar Vuelos (líneas) ---
        for(Vuelo vuelo : vuelos) {
            Line linea = new Line();
            // Lógica para posicionar las líneas basada en la posición de los círculos
            // Se actualiza después de crear los círculos
            graphPane.getChildren().add(linea);
        }

        // --- Dibujar Aeropuertos (círculos y texto) ---
        double width = graphPane.getWidth();
        double height = graphPane.getHeight();
        if (width == 0) width = 800; // Valor por defecto
        if (height == 0) height = 600;

        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) * 0.4;
        int i = 0;
        for (Aeropuerto apt : aeropuertos) {
            double angle = 2 * Math.PI * i / aeropuertos.size();
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            Circle circulo = new Circle(x, y, 15, Color.SKYBLUE);
            Text texto = new Text(x - 15, y - 20, apt.getCodigoIATA());
            nodosDelGrafo.put(apt, circulo); // Guardamos el círculo para futuras referencias
            
            graphPane.getChildren().addAll(circulo, texto);
            i++;
        }
        
        // Actualizar la posición de las líneas de los vuelos
        for (Node node : graphPane.getChildren()) {
            if (node instanceof Line) {
                // Esta parte requiere encontrar el vuelo asociado a la línea y
                // obtener las coordenadas de los círculos de origen y destino.
                // Es una implementación compleja que se puede detallar más adelante.
            }
        }
    }

    /**
     * Resalta la ruta encontrada en el grafo.
     */
    private void resaltarRuta(Ruta ruta) {
        // Primero, restaurar el color de todos los nodos
        for (Node node : nodosDelGrafo.values()) {
            ((Circle) node).setFill(Color.SKYBLUE);
        }
        // Luego, pintar los nodos de la ruta
        for (Aeropuerto apt : ruta.getAeropuertos()) {
            Node nodo = nodosDelGrafo.get(apt);
            if (nodo != null) {
                ((Circle) nodo).setFill(Color.ORANGE);
            }
        }
    }
    
    /**
     * Muestra un diálogo de alerta simple.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

