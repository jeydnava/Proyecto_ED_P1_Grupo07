package com.example.proyectoaeropuerto;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SistemaDeVuelos sistema;
    private AutoCompleteTextView actvOrigen, actvDestino;
    private Button btnBuscarRuta;
    private TextView tvResultado;
    private GraphView graphView;
    private FloatingActionButton fabAddAeropuerto, fabAddVuelo, fabStats;
    private LinearLayout layoutCabeceraBusqueda, layoutContenidoBusqueda;
    private ImageView ivFlechaDesplegable;
    private RadioGroup rgCriterio;

    private Aeropuerto aeropuertoOrigenSeleccionado;
    private Aeropuerto aeropuertoDestinoSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sistema = new SistemaDeVuelos();
        sistema.cargarDatosDesdeArchivo(this);

        vincularVistas();
        configurarAutocompletado();
        configurarSeccionDesplegable();
        graphView.setSistemaDeVuelos(sistema);

        btnBuscarRuta.setOnClickListener(v -> buscarRuta());
        fabAddAeropuerto.setOnClickListener(v -> mostrarDialogoAddAeropuerto());
        fabAddVuelo.setOnClickListener(v -> mostrarDialogoAddVuelo());
        fabStats.setOnClickListener(v -> mostrarDialogoEstadisticas());
        graphView.setOnNodeLongClickListener(this::mostrarDialogoEliminarAeropuerto);
    }

    private void vincularVistas() {
        actvOrigen = findViewById(R.id.actv_origen);
        actvDestino = findViewById(R.id.actv_destino);
        btnBuscarRuta = findViewById(R.id.btn_buscar_ruta);
        tvResultado = findViewById(R.id.tv_resultado);
        graphView = findViewById(R.id.graph_view);
        fabAddAeropuerto = findViewById(R.id.fab_add_aeropuerto);
        fabAddVuelo = findViewById(R.id.fab_add_vuelo);
        fabStats = findViewById(R.id.fab_stats);
        layoutCabeceraBusqueda = findViewById(R.id.layout_cabecera_busqueda);
        layoutContenidoBusqueda = findViewById(R.id.layout_contenido_busqueda);
        ivFlechaDesplegable = findViewById(R.id.iv_flecha_desplegable);
        rgCriterio = findViewById(R.id.rg_criterio);
    }

    private void configurarSeccionDesplegable() {
        layoutCabeceraBusqueda.setOnClickListener(v -> {
            boolean isVisible = layoutContenidoBusqueda.getVisibility() == View.VISIBLE;
            layoutContenidoBusqueda.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            ivFlechaDesplegable.setRotation(isVisible ? 0 : 180);
        });
    }

    private void configurarAutocompletado() {
        List<Aeropuerto> listaAeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        ArrayAdapter<Aeropuerto> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, listaAeropuertos);

        actvOrigen.setAdapter(adapter);
        actvDestino.setAdapter(adapter);

        actvOrigen.setOnItemClickListener((parent, view, position, id) -> aeropuertoOrigenSeleccionado = (Aeropuerto) parent.getItemAtPosition(position));
        actvDestino.setOnItemClickListener((parent, view, position, id) -> aeropuertoDestinoSeleccionado = (Aeropuerto) parent.getItemAtPosition(position));
    }

    private void buscarRuta() {
        if (aeropuertoOrigenSeleccionado == null || aeropuertoDestinoSeleccionado == null) {
            Toast.makeText(this, "Por favor, seleccione un origen y destino válidos de la lista.", Toast.LENGTH_SHORT).show();
            return;
        }

        Ruta.Criterio criterioSeleccionado = Ruta.Criterio.DISTANCIA;
        int checkedId = rgCriterio.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_tiempo) {
            criterioSeleccionado = Ruta.Criterio.TIEMPO;
        } else if (checkedId == R.id.rb_costo) {
            criterioSeleccionado = Ruta.Criterio.COSTO;
        }

        Ruta ruta = sistema.buscarRutaMasCorta(
                aeropuertoOrigenSeleccionado.getCodigoIATA(),
                aeropuertoDestinoSeleccionado.getCodigoIATA(),
                criterioSeleccionado
        );

        tvResultado.setText(ruta.toString());
        graphView.highlightRoute(ruta);
    }

    private void mostrarDialogoAddAeropuerto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_aeropuerto, null);
        builder.setView(dialogView);

        final EditText etCodigo = dialogView.findViewById(R.id.et_codigo_iata);
        final EditText etNombre = dialogView.findViewById(R.id.et_nombre_aeropuerto);
        final EditText etCiudad = dialogView.findViewById(R.id.et_ciudad_aeropuerto);

        builder.setPositiveButton(R.string.boton_añadir, (dialog, which) -> {
            String codigo = etCodigo.getText().toString().toUpperCase().trim();
            String nombre = etNombre.getText().toString().trim();
            String ciudad = etCiudad.getText().toString().trim();

            if (codigo.length() == 3 && !nombre.isEmpty() && !ciudad.isEmpty()) {
                Aeropuerto nuevoApt = new Aeropuerto(codigo, nombre, ciudad);
                sistema.agregarAeropuerto(codigo, nombre, ciudad);
                sistema.guardarAeropuertoEnArchivo(this, nuevoApt);
                actualizarVistas();
                Toast.makeText(this, "Aeropuerto añadido: " + codigo, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_datos_invalidos, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.boton_cancelar, null);
        builder.create().show();
    }

    private void mostrarDialogoAddVuelo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_vuelo, null);
        builder.setView(dialogView);

        final Spinner spinnerOrigen = dialogView.findViewById(R.id.spinner_origen);
        final Spinner spinnerDestino = dialogView.findViewById(R.id.spinner_destino);
        final EditText etDistancia = dialogView.findViewById(R.id.et_distancia_vuelo);
        final EditText etTiempo = dialogView.findViewById(R.id.et_tiempo_vuelo);
        final EditText etCosto = dialogView.findViewById(R.id.et_costo_vuelo);

        List<Aeropuerto> aeropuertos = new ArrayList<>(sistema.getTodosLosAeropuertos());
        ArrayAdapter<Aeropuerto> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, aeropuertos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrigen.setAdapter(adapter);
        spinnerDestino.setAdapter(adapter);

        builder.setPositiveButton(R.string.boton_añadir, (dialog, which) -> {
            Aeropuerto origen = (Aeropuerto) spinnerOrigen.getSelectedItem();
            Aeropuerto destino = (Aeropuerto) spinnerDestino.getSelectedItem();
            String distanciaStr = etDistancia.getText().toString().trim();
            String tiempoStr = etTiempo.getText().toString().trim();
            String costoStr = etCosto.getText().toString().trim();

            if (origen != null && destino != null && !distanciaStr.isEmpty() && !tiempoStr.isEmpty() && !costoStr.isEmpty() && !origen.equals(destino)) {
                int distancia = Integer.parseInt(distanciaStr);
                int tiempo = Integer.parseInt(tiempoStr);
                double costo = Double.parseDouble(costoStr);

                sistema.agregarVuelo(origen.getCodigoIATA(), destino.getCodigoIATA(), distancia, tiempo, costo);

                PesoVuelo pesos = new PesoVuelo(distancia, tiempo, costo);
                Vuelo nuevoVuelo = new Vuelo(origen, destino, pesos);
                sistema.guardarVueloEnArchivo(this, nuevoVuelo);

                actualizarVistas();
                Toast.makeText(this, "Vuelo añadido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_datos_invalidos, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.boton_cancelar, null);
        builder.create().show();
    }

    private void mostrarDialogoEstadisticas() {
        String masConectado = sistema.getAeropuertoMasConectado().toString();
        String menosConectado = sistema.getAeropuertoMenosConectado().toString();
        String todasLasConexiones = sistema.getEstadisticasConexiones();
        String mensaje = "Más Conectado:\n" + masConectado + "\n\n" +
                "Menos Conectado:\n" + menosConectado + "\n\n" +
                "Conexiones por Aeropuerto:\n" + todasLasConexiones;
        new AlertDialog.Builder(this)
                .setTitle("Estadísticas de la Red")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void mostrarDialogoEliminarAeropuerto(Aeropuerto aeropuerto) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Aeropuerto")
                .setMessage("¿Seguro que desea eliminar " + aeropuerto.getNombre() + " ("+ aeropuerto.getCodigoIATA() +") y todos sus vuelos asociados? Esta acción es permanente.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    sistema.eliminarAeropuerto(this, aeropuerto);
                    actualizarVistas();
                    Toast.makeText(this, aeropuerto.getCodigoIATA() + " eliminado.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.boton_cancelar, null)
                .show();
    }

    private void actualizarVistas() {
        actvOrigen.setText("");
        actvDestino.setText("");
        aeropuertoOrigenSeleccionado = null;
        aeropuertoDestinoSeleccionado = null;
        configurarAutocompletado();
        graphView.setSistemaDeVuelos(sistema);
    }
}
