package ed_p1_grupo07;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
// Importa aquí los demás componentes que necesites (Button, ComboBox, etc.)

public class ViewController {

    // 1. Instancia de tu lógica de negocio
    private SistemaDeVuelos sistema;

    // 2. Inyección de componentes desde el FXML
    // La anotación @FXML conecta esta variable con el componente que tiene
    // el mismo fx:id en el archivo FXML.
    @FXML
    private Pane graphPane;
    
    // Aquí declararías los demás componentes:
    // @FXML private ComboBox<Aeropuerto> origenComboBox;
    // @FXML private Label resultadoLabel;

    // 3. Método de inicialización
    // Se ejecuta automáticamente después de que se cargan los componentes FXML.
    // Es el equivalente a "onCreate" de Android.
    @FXML
    public void initialize() {
        // Inicializamos el sistema y cargamos los datos
        sistema = new SistemaDeVuelos();
        sistema.cargarDatosDesdeArchivos();

        // Aquí configurarías tus ComboBox, etc.
        System.out.println("Vista inicializada. Aeropuertos cargados: " + sistema.getTodosLosAeropuertos().size());
        
        // Lógica para dibujar el grafo inicial en el graphPane
        dibujarGrafo();
    }

    // 4. Métodos manejadores de eventos (vinculados desde el FXML)
    @FXML
    private void handleAnadirAeropuerto() {
        System.out.println("Botón 'Añadir Aeropuerto' presionado.");
        // Aquí abrirías un diálogo para añadir un aeropuerto
    }

    @FXML
    private void handleAnadirVuelo() {
        System.out.println("Botón 'Añadir Vuelo' presionado.");
        // Lógica para el diálogo de añadir vuelo
    }

    @FXML
    private void handleVerEstadisticas() {
        System.out.println("Botón 'Ver Estadísticas' presionado.");
        // Lógica para mostrar las estadísticas en un diálogo (Alert)
    }

    // 5. Métodos para actualizar la UI
    private void dibujarGrafo() {
        // Limpia el panel antes de dibujar
        graphPane.getChildren().clear();

        // Aquí irá toda la lógica para recrear tu "GraphView".
        // Por ahora, solo imprimimos un mensaje.
        System.out.println("Dibujando el grafo...");
        // Ejemplo: podrías crear y añadir un círculo por cada aeropuerto.
    }
}
