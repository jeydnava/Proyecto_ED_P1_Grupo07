package com.example.proyectoaeropuerto;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SistemaDeVuelos {
    private Grafo<Aeropuerto, PesoVuelo> redDeVuelos;
    private ArbolAVL<Aeropuerto> arbolAeropuertos;
    private String ARCHIVO_PERSISTENCIA = "datos_adicionales.csv";

    public SistemaDeVuelos() {
        this.arbolAeropuertos = new ArbolAVL<>(Comparator.comparing(Aeropuerto::getCodigoIATA));
        this.redDeVuelos = new Grafo<>(Comparator.comparingDouble(Grafo.Nodo::getPesoAcumulado));
    }

    public void agregarAeropuerto(String codigoIATA, String nombre, String ciudad) {
        Aeropuerto dummyAeropuerto = new Aeropuerto(codigoIATA, "", "");
        if (arbolAeropuertos.buscar(dummyAeropuerto) == null) {
            Aeropuerto nuevoAeropuerto = new Aeropuerto(codigoIATA, nombre, ciudad);
            arbolAeropuertos.insertar(nuevoAeropuerto);
            redDeVuelos.agregarVertice(nuevoAeropuerto);
        }
    }

    public void agregarVuelo(String codigoOrigen, String codigoDestino, int distancia, int tiempo, double costo) {
        Aeropuerto origen = arbolAeropuertos.buscar(new Aeropuerto(codigoOrigen, "", ""));
        Aeropuerto destino = arbolAeropuertos.buscar(new Aeropuerto(codigoDestino, "", ""));
        if (origen != null && destino != null) {
            redDeVuelos.agregarArista(origen, destino, new PesoVuelo(distancia, tiempo, costo));
        }
    }

    public void eliminarAeropuerto(Context context, Aeropuerto aeropuerto) {
        if (aeropuerto == null) return;
        arbolAeropuertos.eliminar(aeropuerto);
        redDeVuelos.eliminarVertice(aeropuerto);
        reescribirArchivoPersistencia(context);
    }

    public Collection<Aeropuerto> getTodosLosAeropuertos() {
        return arbolAeropuertos.getDatosEnOrden();
    }

    public List<Vuelo> getTodosLosVuelos() {
        return redDeVuelos.getTodosLosVuelos();
    }

    public Ruta buscarRutaMasCorta(String codigoOrigen, String codigoDestino, Ruta.Criterio criterio) {
        Aeropuerto origen = arbolAeropuertos.buscar(new Aeropuerto(codigoOrigen, "", ""));
        Aeropuerto destino = arbolAeropuertos.buscar(new Aeropuerto(codigoDestino, "", ""));
        return redDeVuelos.encontrarRutaMasCorta(origen, destino, criterio);
    }

    public String getEstadisticasConexiones() {
        StringBuilder sb = new StringBuilder();
        for (Aeropuerto apt : getTodosLosAeropuertos()) {
            int conexiones = redDeVuelos.getNumeroDeConexiones(apt);
            sb.append(apt.getCodigoIATA()).append(": ").append(conexiones).append(" vuelos salientes\n");
        }
        return sb.toString();
    }

    public Aeropuerto getAeropuertoMasConectado() {
        return Collections.max(getTodosLosAeropuertos(), Comparator.comparingInt(redDeVuelos::getNumeroDeConexiones));
    }

    public Aeropuerto getAeropuertoMenosConectado() {
        return Collections.min(getTodosLosAeropuertos(), Comparator.comparingInt(redDeVuelos::getNumeroDeConexiones));
    }

    public void cargarDatosDesdeArchivo(Context context) {
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open("datos_vuelos.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            cargarDatos(reader);
        } catch (IOException e) {
            Log.e("SistemaDeVuelos", "Error al cargar desde assets", e);
        }

        try (FileInputStream fis = context.openFileInput(ARCHIVO_PERSISTENCIA);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            cargarDatos(reader);
        } catch (FileNotFoundException e) {
            // Es normal que no exista la primera vez
        } catch (IOException e) {
            Log.e("SistemaDeVuelos", "Error al cargar desde persistencia", e);
        }
    }

    private void cargarDatos(BufferedReader reader) throws IOException {
        String line;
        List<String[]> lineasVuelos = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] datos = line.split(",");
            if (datos.length > 0) {
                if (datos[0].equalsIgnoreCase("AEROPUERTO") && datos.length >= 4) {
                    agregarAeropuerto(datos[1].trim(), datos[2].trim(), datos[3].trim());
                } else if (datos[0].equalsIgnoreCase("VUELO") && datos.length >= 6) {
                    lineasVuelos.add(datos);
                }
            }
        }
        for (String[] datosVuelo : lineasVuelos) {
            String origen = datosVuelo[1].trim();
            String destino = datosVuelo[2].trim();
            int distancia = Integer.parseInt(datosVuelo[3].trim());
            int tiempo = Integer.parseInt(datosVuelo[4].trim());
            double costo = Double.parseDouble(datosVuelo[5].trim());
            agregarVuelo(origen, destino, distancia, tiempo, costo);
        }
    }

    public void guardarAeropuertoEnArchivo(Context context, Aeropuerto aeropuerto) {
        String linea = "AEROPUERTO," + aeropuerto.getCodigoIATA() + "," + aeropuerto.getNombre() + "," + aeropuerto.getCiudad() + "\n";
        escribirEnArchivo(context, linea);
    }

    public void guardarVueloEnArchivo(Context context, Vuelo vuelo) {
        String linea = "VUELO," + vuelo.getOrigen().getCodigoIATA() + "," + vuelo.getDestino().getCodigoIATA() + "," + vuelo.getDistancia() + "," + vuelo.getTiempo() + "," + vuelo.getCosto() + "\n";
        escribirEnArchivo(context, linea);
    }

    private void escribirEnArchivo(Context context, String data) {
        try (FileOutputStream fos = context.openFileOutput(ARCHIVO_PERSISTENCIA, Context.MODE_APPEND)) {
            fos.write(data.getBytes());
        } catch (IOException e) {
            Log.e("SistemaDeVuelos", "Error al escribir en archivo", e);
        }
    }

    private void reescribirArchivoPersistencia(Context context) {
        List<String> lineas = new ArrayList<>();
        Collection<Aeropuerto> aeropuertos = getTodosLosAeropuertos();

        List<Vuelo> vuelosExistentes = getTodosLosVuelos();

        for (Aeropuerto apt : aeropuertos) {
            lineas.add("AEROPUERTO," + apt.getCodigoIATA() + "," + apt.getNombre() + "," + apt.getCiudad());
        }
        for (Vuelo vuelo : vuelosExistentes) {
            lineas.add("VUELO," + vuelo.getOrigen().getCodigoIATA() + "," + vuelo.getDestino().getCodigoIATA() + "," + vuelo.getDistancia() + "," + vuelo.getTiempo() + "," + vuelo.getCosto());
        }

        try (FileOutputStream fos = context.openFileOutput(ARCHIVO_PERSISTENCIA, Context.MODE_PRIVATE)) {
            for (String linea : lineas) {
                fos.write((linea + "\n").getBytes());
            }
        } catch (IOException e) {
            Log.e("SistemaDeVuelos", "Error al reescribir archivo de persistencia", e);
        }
    }
}