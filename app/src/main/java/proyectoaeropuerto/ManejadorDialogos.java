package proyectoaeropuerto;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManejadorDialogos {

    private final SistemaDeVuelos sistema;

    public ManejadorDialogos(SistemaDeVuelos sistema) {
        this.sistema = sistema;
    }

    public void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public Optional<Aeropuerto> mostrarDialogoAnadirAeropuerto() {
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

        return dialog.showAndWait();
    }

    public Optional<Vuelo> mostrarDialogoAnadirVuelo() {
        return crearDialogoVuelo(null).showAndWait();
    }

    public void mostrarDialogoEstadisticas() {
        List<String> choices = Arrays.asList("Conexiones por Aeropuerto", "Rutas Más Demandadas");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Conexiones por Aeropuerto", choices);
        dialog.setTitle("Seleccionar Estadísticas");
        dialog.setHeaderText("¿Qué estadísticas desea visualizar?");
        dialog.setContentText("Elija una opción:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            if (choice.equals("Conexiones por Aeropuerto")) {
                mostrarEstadisticasConexiones();
            } else if (choice.equals("Rutas Más Demandadas")) {
                mostrarDialogoRutasDemandadas();
            }
        });
    }

    private void mostrarEstadisticasConexiones() {
        String masConectado = "Aeropuerto más conectado: " + sistema.getAeropuertoMasConectado().toString();
        String menosConectado = "Aeropuerto menos conectado: " + sistema.getAeropuertoMenosConectado().toString();
        String conexiones = "\nConexiones por Aeropuerto:\n" + sistema.getEstadisticasConexiones();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de Conexiones");
        alert.setHeaderText(masConectado + "\n" + menosConectado);
        alert.setContentText(conexiones);
        alert.showAndWait();
    }

    private void mostrarDialogoRutasDemandadas() {
        List<Vuelo> vuelosPopulares = sistema.getVuelosMasDemandados(10);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Top 10 Rutas Más Demandadas");
        dialog.setHeaderText("Mostrando las rutas de vuelo más populares según la demanda.");

        ListView<String> listView = new ListView<>();
        List<String> vuelosComoTexto = new ArrayList<>();
        for (Vuelo vuelo : vuelosPopulares) {
            vuelosComoTexto.add(
                String.format("%s -> %s (Demanda: %d)",
                    vuelo.getOrigen().getCodigoIATA(),
                    vuelo.getDestino().getCodigoIATA(),
                    vuelo.getPeso().getDemanda())
            );
        }
        listView.setItems(FXCollections.observableArrayList(vuelosComoTexto));

        VBox vbox = new VBox(listView);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    public void mostrarDialogoVuelosDeAeropuerto(Aeropuerto aeropuerto) {
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

    public Optional<Aeropuerto> mostrarDialogoEditarAeropuerto(Aeropuerto aeropuerto) {
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

        return dialog.showAndWait();
    }

    public Optional<ButtonType> mostrarConfirmacionEliminarAeropuerto(Aeropuerto aeropuerto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Aeropuerto: " + aeropuerto.getCodigoIATA());
        alert.setContentText("¿Está seguro de que desea eliminar este aeropuerto? Se borrarán todos los vuelos asociados.");
        return alert.showAndWait();
    }

    public Optional<Vuelo> mostrarDialogoEditarVuelo(Vuelo vuelo) {
        return crearDialogoVuelo(vuelo).showAndWait();
    }

    public Optional<ButtonType> mostrarConfirmacionEliminarVuelo(Vuelo vuelo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Vuelo: " + vuelo.getOrigen().getCodigoIATA() + " -> " + vuelo.getDestino().getCodigoIATA());
        alert.setContentText("¿Está seguro de que desea eliminar este vuelo?");
        return alert.showAndWait();
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
    
    public void mostrarRutasAlternativas(List<Ruta> rutas) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rutas Alternativas");
        dialog.setHeaderText("Se encontraron varias rutas. La mejor ruta está resaltada en el mapa.");

        ListView<String> listView = new ListView<>();
        List<String> rutasComoTexto = new ArrayList<>();
        for (int i = 0; i < rutas.size(); i++) {
            rutasComoTexto.add("Ruta " + (i + 1) + ": " + rutas.get(i).toString());
        }
        listView.setItems(FXCollections.observableArrayList(rutasComoTexto));

        VBox vbox = new VBox(listView);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
