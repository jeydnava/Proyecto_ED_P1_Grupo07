package proyectoaeropuerto;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SistemaDeVuelos {
    private Grafo<Aeropuerto, PesoVuelo> redDeVuelos;
    private ArbolAVL<Aeropuerto> arbolAeropuertos;
    private static final String ARCHIVO_PERSISTENCIA = "app/datos_adicionales.csv";

    public SistemaDeVuelos() {
        this.arbolAeropuertos = new ArbolAVL<>(Comparator.comparing(Aeropuerto::getCodigoIATA));
        this.redDeVuelos = new Grafo<>(Comparator.comparingDouble(Grafo.Nodo::getPesoAcumulado));
    }
    
    // --- MÉTODOS DE MANIPULACIÓN DE DATOS ---
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

    public void editarAeropuerto(String codigoIATA, String nuevoNombre, String nuevaCiudad) {
        Aeropuerto aeropuerto = getAeropuertoPorCodigo(codigoIATA);
        if (aeropuerto != null) {
            aeropuerto.setNombre(nuevoNombre);
            aeropuerto.setCiudad(nuevaCiudad);
            reescribirArchivoPersistencia();
        }
    }

    public void editarVuelo(Vuelo vueloAntiguo, int nuevaDistancia, int nuevoTiempo, double nuevoCosto) {
        if (vueloAntiguo == null) return;

        // 1. Eliminar la arista antigua del grafo
        PesoVuelo pesoAntiguo = new PesoVuelo(vueloAntiguo.getDistancia(), vueloAntiguo.getTiempo(), vueloAntiguo.getCosto());
        redDeVuelos.eliminarArista(vueloAntiguo.getOrigen(), vueloAntiguo.getDestino(), pesoAntiguo);

        // 2. Agregar la nueva arista al grafo
        PesoVuelo pesoNuevo = new PesoVuelo(nuevaDistancia, nuevoTiempo, nuevoCosto);
        redDeVuelos.agregarArista(vueloAntiguo.getOrigen(), vueloAntiguo.getDestino(), pesoNuevo);

        // 3. Guardar el estado actualizado
        reescribirArchivoPersistencia();
    }
    
    // --- MÉTODOS DE CONSULTA ---
    public Aeropuerto getAeropuertoPorCodigo(String codigoIATA) {
        return arbolAeropuertos.buscar(new Aeropuerto(codigoIATA, "", ""));
    }
    
    public Collection<Aeropuerto> getTodosLosAeropuertos() {
        return arbolAeropuertos.getDatosEnOrden();
    }

    public List<Vuelo> getTodosLosVuelos() {
        return redDeVuelos.getTodosLosVuelos();
    }

    public List<Vuelo> getVuelosDeAeropuerto(String codigoIATA) {
        List<Vuelo> vuelosDelAeropuerto = new ArrayList<>();
        if (codigoIATA == null || codigoIATA.isEmpty()) {
            return vuelosDelAeropuerto; // Return empty list if code is invalid
        }
        for (Vuelo vuelo : getTodosLosVuelos()) {
            if (vuelo.getOrigen().getCodigoIATA().equals(codigoIATA) || 
                vuelo.getDestino().getCodigoIATA().equals(codigoIATA)) {
                vuelosDelAeropuerto.add(vuelo);
            }
        }
        return vuelosDelAeropuerto;
    }

    public Ruta buscarRutaMasCorta(String codigoOrigen, String codigoDestino, Ruta.Criterio criterio) {
        Aeropuerto origen = arbolAeropuertos.buscar(new Aeropuerto(codigoOrigen, "", ""));
        Aeropuerto destino = arbolAeropuertos.buscar(new Aeropuerto(codigoDestino, "", ""));
        return redDeVuelos.encontrarRutaMasCorta(origen, destino, criterio);
    }
    
    // --- MÉTODOS DE ESTADÍSTICAS ---

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

    // --- MÉTODOS DE PERSISTENCIA (CON CAMBIOS IMPORTANTES) ---
    
    public void eliminarAeropuerto(Aeropuerto aeropuerto) {
        if (aeropuerto == null) return;
        arbolAeropuertos.eliminar(aeropuerto);
        redDeVuelos.eliminarVertice(aeropuerto);
        reescribirArchivoPersistencia();
    }

    public void eliminarVuelo(Vuelo vuelo) {
        if (vuelo == null) return;
        PesoVuelo peso = new PesoVuelo(vuelo.getDistancia(), vuelo.getTiempo(), vuelo.getCosto());
        redDeVuelos.eliminarArista(vuelo.getOrigen(), vuelo.getDestino(), peso);
        reescribirArchivoPersistencia();
    }

    public void cargarDatosDesdeArchivos() {
        // Carga desde el archivo de recursos (el que está dentro del programa)
        try (InputStream inputStream = SistemaDeVuelos.class.getResourceAsStream("/proyectoaeropuerto/datos_vuelos.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            if (inputStream == null) {
                System.err.println("¡Error Crítico! No se encontró el archivo de recursos 'datos_vuelos.csv'");
                return;
            }
            cargarDatos(reader);
        } catch (IOException | NullPointerException e) {
            System.err.println("Error al cargar datos desde recursos:");
            e.printStackTrace();
        }

        // Carga desde el archivo de persistencia (el que guarda los cambios del usuario)
        try (InputStream fis = new FileInputStream(ARCHIVO_PERSISTENCIA);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            cargarDatos(reader);
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró archivo de persistencia, se creará uno nuevo al guardar.");
        } catch (IOException e) {
            System.err.println("Error al cargar datos desde persistencia:");
            e.printStackTrace();
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

    public void guardarAeropuertoEnArchivo(Aeropuerto aeropuerto) {
        String linea = "AEROPUERTO," + aeropuerto.getCodigoIATA() + "," + aeropuerto.getNombre() + "," + aeropuerto.getCiudad() + "\n";
        escribirEnArchivo(linea, true); // true para añadir al final
    }

    public void guardarVueloEnArchivo(Vuelo vuelo) {
        String linea = "VUELO," + vuelo.getOrigen().getCodigoIATA() + "," + vuelo.getDestino().getCodigoIATA() + "," + vuelo.getDistancia() + "," + vuelo.getTiempo() + "," + vuelo.getCosto() + "\n";
        escribirEnArchivo(linea, true); // true para añadir al final
    }

    private void escribirEnArchivo(String data, boolean append) {
        // Usamos try-with-resources para asegurar que el archivo se cierre
        try (FileWriter fw = new FileWriter(ARCHIVO_PERSISTENCIA, append);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {
            out.print(data);
        } catch (IOException e) {
            System.err.println("Error al escribir en archivo de persistencia:");
            e.printStackTrace();
        }
    }
    
    private void reescribirArchivoPersistencia() {
        List<String> lineas = new ArrayList<>();
        for (Aeropuerto apt : getTodosLosAeropuertos()) {
            lineas.add("AEROPUERTO," + apt.getCodigoIATA() + "," + apt.getNombre() + "," + apt.getCiudad());
        }
        for (Vuelo vuelo : getTodosLosVuelos()) {
            lineas.add("VUELO," + vuelo.getOrigen().getCodigoIATA() + "," + vuelo.getDestino().getCodigoIATA() + "," + vuelo.getDistancia() + "," + vuelo.getTiempo() + "," + vuelo.getCosto());
        }

        // Sobrescribimos el archivo con los nuevos datos
        try (FileWriter fw = new FileWriter(ARCHIVO_PERSISTENCIA, false); // false para sobrescribir
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {
            for (String linea : lineas) {
                out.println(linea);
            }
        } catch (IOException e) {
            System.err.println("Error al reescribir archivo de persistencia:");
            e.printStackTrace();
        }
    }
}