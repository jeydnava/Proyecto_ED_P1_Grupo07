package proyectoaeropuerto;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ViewController {

    private SistemaDeVuelos sistema;
    private ManejadorGrafico manejadorGrafico;
    private ManejadorDialogos manejadorDialogos;

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

        manejadorDialogos = new ManejadorDialogos(sistema);
        manejadorGrafico = new ManejadorGrafico(graphPane, sistema, this::handleMostrarVuelosDeAeropuerto, this::mostrarMenuContextualAeropuerto);

        actualizarVistas();
        resultadoLabel.setText("Bienvenido al Sistema de Gestión de Vuelos.");
    }

    public SistemaDeVuelos getSistemaDeVuelos() {
        return sistema;
    }

    private void actualizarVistas() {
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        origenComboBox.getItems().setAll(aeropuertos);
        destinoComboBox.getItems().setAll(aeropuertos);
        manejadorGrafico.dibujarGrafo();
    }

    @FXML
    private void handleBuscarRuta() {
        Aeropuerto origen = origenComboBox.getValue();
        Aeropuerto destino = destinoComboBox.getValue();

        if (origen == null || destino == null) {
            manejadorDialogos.mostrarAlerta(Alert.AlertType.ERROR, "Error", "Debe seleccionar un aeropuerto de origen y destino.");
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
        resultadoLabel.setText("Mejor Ruta: " + ruta.toString());
        manejadorGrafico.resaltarRuta(ruta);

        List<Ruta> rutasAlternativas = sistema.buscarRutasAlternativas(origen.getCodigoIATA(), destino.getCodigoIATA(), criterio, 5);
        if (rutasAlternativas.size() > 1) {
            manejadorDialogos.mostrarRutasAlternativas(rutasAlternativas);
        }
    }

    @FXML
    private void handleAnadirAeropuerto() {
        Optional<Aeropuerto> result = manejadorDialogos.mostrarDialogoAnadirAeropuerto();
        result.ifPresent(aeropuerto -> {
            sistema.agregarAeropuerto(aeropuerto.getCodigoIATA(), aeropuerto.getNombre(), aeropuerto.getCiudad());
            actualizarVistas();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto añadido correctamente.");
        });
    }

    @FXML
    private void handleAnadirVuelo() {
        Optional<Vuelo> result = manejadorDialogos.mostrarDialogoAnadirVuelo();
        result.ifPresent(vuelo -> {
            sistema.agregarVuelo(vuelo.getOrigen().getCodigoIATA(), vuelo.getDestino().getCodigoIATA(), vuelo.getDistancia(), vuelo.getTiempo(), vuelo.getCosto(), 0);
            actualizarVistas();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo añadido correctamente.");
        });
    }

    @FXML
    private void handleVerEstadisticas() {
        manejadorDialogos.mostrarDialogoEstadisticas();
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

    private void handleMostrarVuelosDeAeropuerto(Aeropuerto aeropuerto) {
        manejadorDialogos.mostrarDialogoVuelosDeAeropuerto(aeropuerto);
    }

    private void mostrarMenuContextualAeropuerto(Aeropuerto aeropuerto, ContextMenuEvent event) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar Aeropuerto");
        editarItem.setOnAction(e -> handleEditarAeropuerto(aeropuerto));
        MenuItem eliminarItem = new MenuItem("Eliminar Aeropuerto");
        eliminarItem.setOnAction(e -> handleEliminarAeropuerto(aeropuerto));
        contextMenu.getItems().addAll(editarItem, eliminarItem);
        contextMenu.show(graphPane.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }

    private void handleEditarAeropuerto(Aeropuerto aeropuerto) {
        Optional<Aeropuerto> result = manejadorDialogos.mostrarDialogoEditarAeropuerto(aeropuerto);
        result.ifPresent(apt -> {
            sistema.editarAeropuerto(apt.getCodigoIATA(), apt.getNombre(), apt.getCiudad());
            actualizarVistas();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto '" + apt.getCodigoIATA() + "' actualizado.");
        });
    }

    private void handleEliminarAeropuerto(Aeropuerto aeropuerto) {
        Optional<ButtonType> result = manejadorDialogos.mostrarConfirmacionEliminarAeropuerto(aeropuerto);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sistema.eliminarAeropuerto(aeropuerto);
            actualizarVistas();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Aeropuerto eliminado.");
        }
    }

    private void handleEditarVuelo(Vuelo vuelo, TableView<Vuelo> tableView) {
        Optional<Vuelo> result = manejadorDialogos.mostrarDialogoEditarVuelo(vuelo);
        result.ifPresent(vueloEditado -> {
            sistema.editarVuelo(vuelo, vueloEditado.getDistancia(), vueloEditado.getTiempo(), vueloEditado.getCosto());
            actualizarVistas();
            tableView.setItems(FXCollections.observableArrayList(sistema.getTodosLosVuelos()));
            tableView.refresh();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo actualizado.");
        });
    }

    private void handleEliminarVuelo(Vuelo vuelo, TableView<Vuelo> tableView) {
        Optional<ButtonType> result = manejadorDialogos.mostrarConfirmacionEliminarVuelo(vuelo);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sistema.eliminarVuelo(vuelo);
            actualizarVistas();
            tableView.setItems(FXCollections.observableArrayList(sistema.getTodosLosVuelos()));
            tableView.refresh();
            manejadorDialogos.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Vuelo eliminado.");
        }
    }
}
