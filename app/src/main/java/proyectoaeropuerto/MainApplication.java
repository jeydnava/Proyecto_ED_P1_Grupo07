package proyectoaeropuerto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    private FXMLLoader fxmlLoader;

    @Override
    public void start(Stage stage) throws IOException {
        // Carga del archivo FXML con la interfaz
        fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        
        // Crea la escena con el contenido del FXML
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Configura y muestra la ventana principal
        stage.setTitle("Gestor de Vuelos del Aeropuerto");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        ViewController controller = fxmlLoader.getController();
        controller.getSistemaDeVuelos().guardarEstado();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}