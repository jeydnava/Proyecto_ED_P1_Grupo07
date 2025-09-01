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
            guardarEstado();
        }
    }

    public void agregarVuelo(String codigoOrigen, String codigoDestino, int distancia, int tiempo, double costo, int demanda) {
        Aeropuerto origen = arbolAeropuertos.buscar(new Aeropuerto(codigoOrigen, "", ""));
        Aeropuerto destino = arbolAeropuertos.buscar(new Aeropuerto(codigoDestino, "", ""));
        if (origen != null && destino != null) {
            redDeVuelos.agregarArista(origen, destino, new PesoVuelo(distancia, tiempo, costo, demanda));
            guardarEstado();
        }
    }

    public void eliminarAeropuerto(Aeropuerto aeropuerto) {
        if (aeropuerto == null) return;
        arbolAeropuertos.eliminar(aeropuerto);
        redDeVuelos.eliminarVertice(aeropuerto);
        guardarEstado();
    }

    public void eliminarVuelo(Vuelo vuelo) {
        if (vuelo == null) return;
        PesoVuelo peso = new PesoVuelo(vuelo.getDistancia(), vuelo.getTiempo(), vuelo.getCosto(), vuelo.getPeso().getDemanda());
        redDeVuelos.eliminarArista(vuelo.getOrigen(), vuelo.getDestino(), peso);
        guardarEstado();
    }

    public void editarAeropuerto(String codigoIATA, String nuevoNombre, String nuevaCiudad) {
        Aeropuerto aeropuerto = getAeropuertoPorCodigo(codigoIATA);
        if (aeropuerto != null) {
            aeropuerto.setNombre(nuevoNombre);
            aeropuerto.setCiudad(nuevaCiudad);
            guardarEstado();
        }
    }

    public void editarVuelo(Vuelo vueloAntiguo, int nuevaDistancia, int nuevoTiempo, double nuevoCosto) {
        if (vueloAntiguo == null) return;

        // 1. Eliminar la arista antigua del grafo
        PesoVuelo pesoAntiguo = new PesoVuelo(vueloAntiguo.getDistancia(), vueloAntiguo.getTiempo(), vueloAntiguo.getCosto(), vueloAntiguo.getPeso().getDemanda());
        redDeVuelos.eliminarArista(vueloAntiguo.getOrigen(), vueloAntiguo.getDestino(), pesoAntiguo);

        // 2. Agregar la nueva arista al grafo
        PesoVuelo pesoNuevo = new PesoVuelo(nuevaDistancia, nuevoTiempo, nuevoCosto, vueloAntiguo.getPeso().getDemanda());
        redDeVuelos.agregarArista(vueloAntiguo.getOrigen(), vueloAntiguo.getDestino(), pesoNuevo);

        // 3. Guardar el estado actualizado
        guardarEstado();
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
            return vuelosDelAeropuerto;
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
        Ruta ruta = redDeVuelos.encontrarRutaMasCorta(origen, destino, criterio);
        if (ruta != null && !ruta.getAeropuertos().isEmpty()) {
            incrementarDemandaRuta(ruta);
        }
        return ruta;
    }

    public List<Ruta> buscarRutasAlternativas(String codigoOrigen, String codigoDestino, Ruta.Criterio criterio, int maxRutas) {
        Aeropuerto origen = arbolAeropuertos.buscar(new Aeropuerto(codigoOrigen, "", ""));
        Aeropuerto destino = arbolAeropuertos.buscar(new Aeropuerto(codigoDestino, "", ""));
        List<Ruta> rutas = redDeVuelos.encontrarRutasAlternativas(origen, destino, criterio, maxRutas);
        for (Ruta ruta : rutas) {
            incrementarDemandaRuta(ruta);
        }
        return rutas;
    }

    private void incrementarDemandaRuta(Ruta ruta) {
        List<Aeropuerto> aeropuertos = ruta.getAeropuertos();
        if (aeropuertos.size() < 2) return;

        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            Aeropuerto origen = aeropuertos.get(i);
            Aeropuerto destino = aeropuertos.get(i + 1);
            // Encontrar el vuelo específico en el grafo y actualizar su peso
            for (Vuelo v : redDeVuelos.getTodosLosVuelos()) {
                if (v.getOrigen().equals(origen) && v.getDestino().equals(destino)) {
                    v.getPeso().incrementarDemanda();
                    break;
                }
            }
        }
        guardarEstado();
    }
    

    // --- MÉTODOS DE ESTADÍSTICAS ---
    public List<Vuelo> getVuelosMasDemandados(int n) {
        List<Vuelo> todosLosVuelos = getTodosLosVuelos();
        todosLosVuelos.sort(Comparator.comparingInt((Vuelo v) -> v.getPeso().getDemanda()).reversed());
        return todosLosVuelos.subList(0, Math.min(n, todosLosVuelos.size()));
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

    // --- MÉTODOS DE PERSISTENCIA ---
    public void cargarDatosDesdeArchivos() {
        File archivoPersistencia = new File(ARCHIVO_PERSISTENCIA);
        if (archivoPersistencia.exists()) {
            // Si el archivo de persistencia existe, se carga desde él.
            try (InputStream fis = new FileInputStream(archivoPersistencia);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                cargarDatos(reader);
            } catch (IOException e) {
                System.err.println("Error al cargar datos desde persistencia:");
                e.printStackTrace();
            }
        } else {
            // Si no, se carga desde el archivo de recursos (datos iniciales).
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
            int demanda = (datosVuelo.length > 6) ? Integer.parseInt(datosVuelo[6].trim()) : 0;
            agregarVuelo(origen, destino, distancia, tiempo, costo, demanda);
        }
    }

    public void guardarEstado() {
        List<String> lineas = new ArrayList<>();
        for (Aeropuerto apt : getTodosLosAeropuertos()) {
            lineas.add("AEROPUERTO," + apt.getCodigoIATA() + "," + apt.getNombre() + "," + apt.getCiudad());
        }
        for (Vuelo vuelo : getTodosLosVuelos()) {
            lineas.add("VUELO," + vuelo.getOrigen().getCodigoIATA() + "," + vuelo.getDestino().getCodigoIATA() + "," + vuelo.getDistancia() + "," + vuelo.getTiempo() + "," + vuelo.getCosto() + "," + vuelo.getPeso().getDemanda());
        }

        try {
            File archivo = new File(ARCHIVO_PERSISTENCIA);
            File parentDir = archivo.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter fw = new FileWriter(archivo, false);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
                for (String linea : lineas) {
                    out.println(linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al reescribir archivo de persistencia:");
            e.printStackTrace();
        }
    }
}
