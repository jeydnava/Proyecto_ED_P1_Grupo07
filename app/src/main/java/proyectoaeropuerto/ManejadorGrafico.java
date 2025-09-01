package proyectoaeropuerto;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ManejadorGrafico {

    private final Pane graphPane;
    private final SistemaDeVuelos sistema;
    private final Consumer<Aeropuerto> onAeropuertoClick;
    private final BiConsumer<Aeropuerto, ContextMenuEvent> onAeropuertoContextMenu;

    private final Map<Aeropuerto, Node> nodosVisuales = new HashMap<>();
    private final Map<Aeropuerto, Pair<Double, Double>> posiciones = new HashMap<>();
    private final Group graphContentGroup = new Group();
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public ManejadorGrafico(Pane graphPane, SistemaDeVuelos sistema, Consumer<Aeropuerto> onAeropuertoClick, BiConsumer<Aeropuerto, ContextMenuEvent> onAeropuertoContextMenu) {
        this.graphPane = graphPane;
        this.sistema = sistema;
        this.onAeropuertoClick = onAeropuertoClick;
        this.onAeropuertoContextMenu = onAeropuertoContextMenu;

        this.graphPane.getChildren().add(graphContentGroup);
        setupPanningAndZooming();
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

    public void dibujarGrafo() {
        graphContentGroup.getChildren().clear();
        nodosVisuales.clear();
        posiciones.clear();
        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        if (aeropuertos.isEmpty()) return;

        double width = graphPane.getWidth() > 0 ? graphPane.getWidth() : 1200;
        double height = graphPane.getHeight() > 0 ? graphPane.getHeight() : 1000;
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) * 0.8;

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
            double tamFlecha = 25.0;
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

        for (Aeropuerto apt : aeropuertos) {
            Pair<Double, Double> pos = posiciones.get(apt);
            if (pos == null) continue;
            Circle circulo = new Circle(pos.getKey(), pos.getValue(), 15, apt.getCodigoIATA().equals("PKX") ? Color.CRIMSON : Color.STEELBLUE);
            circulo.setStroke(Color.BLACK);

            Text texto = new Text(pos.getKey() - 15, pos.getValue() - 20, apt.getCodigoIATA());
            texto.setMouseTransparent(true);

            circulo.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onAeropuertoClick.accept(apt);
                }
            });

            circulo.setOnContextMenuRequested(event -> onAeropuertoContextMenu.accept(apt, event));

            nodosVisuales.put(apt, circulo);
            graphContentGroup.getChildren().addAll(circulo, texto);
        }
    }

    public void resaltarRuta(Ruta ruta) {
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
}
