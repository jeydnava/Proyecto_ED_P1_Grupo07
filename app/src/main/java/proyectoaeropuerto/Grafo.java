package proyectoaeropuerto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class Grafo<V, E> {
    private final LinkedList<Nodo<V, E>> nodos;
    private final Comparator<Nodo<V, E>> comparadorDijkstra;

    public static class Nodo<V, E> {
        final V data;
        final LinkedList<Arista<V, E>> adyacentes = new LinkedList<>();
        double pesoAcumulado = Double.POSITIVE_INFINITY;
        Nodo<V, E> predecesor = null;
        boolean visitado = false;

        Nodo(V data) { 
            this.data = data; 
        }
        
        void reiniciarParaDijkstra() {
            pesoAcumulado = Double.POSITIVE_INFINITY;
            predecesor = null;
            visitado = false;
        }

        public double getPesoAcumulado() {
            return pesoAcumulado;
        }
    }

    private static class Arista<V, E> {
        final Nodo<V, E> destino;
        final E peso;
        Arista(Nodo<V, E> destino, E peso) {
            this.destino = destino;
            this.peso = peso;
        }
    }

    public Grafo(Comparator<Nodo<V, E>> comparadorDijkstra) {
        this.nodos = new LinkedList<>();
        this.comparadorDijkstra = comparadorDijkstra;
    }

    private Nodo<V, E> buscarNodo(V data) {
        for (Nodo<V, E> nodo : nodos) {
            if (nodo.data.equals(data)) return nodo;
        }
        return null;
    }

    public void agregarVertice(V data) {
        if (buscarNodo(data) == null) {
            nodos.add(new Nodo<>(data));
        }
    }

    public void agregarArista(V origenData, V destinoData, E peso) {
        Nodo<V, E> origen = buscarNodo(origenData);
        Nodo<V, E> destino = buscarNodo(destinoData);
        if (origen != null && destino != null) {
            origen.adyacentes.add(new Arista<>(destino, peso));
        }
    }

    public void eliminarVertice(V data) {
        Nodo<V, E> nodoAEliminar = buscarNodo(data);
        if (nodoAEliminar == null) return;
        nodos.remove(nodoAEliminar);
        for (Nodo<V, E> nodo : nodos) {
            nodo.adyacentes.removeIf(arista -> arista.destino.equals(nodoAEliminar));
        }
    }

    public void eliminarArista(V origenData, V destinoData, E peso) {
        Nodo<V, E> origen = buscarNodo(origenData);
        if (origen != null) {
            origen.adyacentes.removeIf(arista -> 
                arista.destino.data.equals(destinoData) && arista.peso.equals(peso)
            );
        }
    }

    public int getNumeroDeConexiones(V data) {
        Nodo<V, E> nodo = buscarNodo(data);
        return (nodo != null) ? nodo.adyacentes.size() : 0;
    }

    public List<Vuelo> getTodosLosVuelos() {
        List<Vuelo> todosLosVuelos = new ArrayList<>();
        for (Nodo<V, E> nodo : nodos) {
            for (Arista<V, E> arista : nodo.adyacentes) {
                todosLosVuelos.add(new Vuelo((Aeropuerto) nodo.data, (Aeropuerto) arista.destino.data, (PesoVuelo) arista.peso));
            }
        }
        return todosLosVuelos;
    }

    public Ruta encontrarRutaMasCorta(V origenData, V destinoData, Ruta.Criterio criterio) {
        Nodo<V, E> nodoInicio = buscarNodo(origenData);
        Nodo<V, E> nodoDestino = buscarNodo(destinoData);

        if (nodoInicio == null || nodoDestino == null) {
            return new Ruta(new LinkedList<>(), -1, criterio); // Devuelve ruta vacía si no existen los nodos
        }

        // 1. Inicializar todos los nodos
        for(Nodo<V,E> nodo : nodos) {
            nodo.reiniciarParaDijkstra();
        }

        // 2. Establecer el nodo de inicio
        nodoInicio.pesoAcumulado = 0;
        PriorityQueue<Nodo<V, E>> colaPrioridad = new PriorityQueue<>(comparadorDijkstra);
        colaPrioridad.add(nodoInicio);

        // 3. Bucle principal de Dijkstra
        while (!colaPrioridad.isEmpty()) {
            Nodo<V, E> nodoActual = colaPrioridad.poll();

            if (nodoActual.visitado) {
                continue;
            }
            nodoActual.visitado = true;

            // Si hemos llegado al destino, podemos parar
            if (nodoActual.equals(nodoDestino)) {
                break;
            }

            for (Arista<V, E> arista : nodoActual.adyacentes) {
                Nodo<V, E> vecino = arista.destino;

                if (!vecino.visitado) {
                    // Determinar el peso de la arista según el criterio
                    PesoVuelo pesoVuelo = (PesoVuelo) arista.peso;
                    double pesoArista;
                    switch (criterio) {
                        case TIEMPO: pesoArista = pesoVuelo.getTiempo(); break;
                        case COSTO: pesoArista = pesoVuelo.getCosto(); break;
                        default: pesoArista = pesoVuelo.getDistancia(); break;
                    }

                    double nuevoPesoAcumulado = nodoActual.pesoAcumulado + pesoArista;

                    if (nuevoPesoAcumulado < vecino.pesoAcumulado) {
                        vecino.pesoAcumulado = nuevoPesoAcumulado;
                        vecino.predecesor = nodoActual;
                        colaPrioridad.add(vecino);
                    }
                }
            }
        }

        // 4. Reconstruir el camino desde el destino hacia atrás
        LinkedList<Aeropuerto> camino = new LinkedList<>();
        if (nodoDestino.pesoAcumulado != Double.POSITIVE_INFINITY) {
            Nodo<V, E> paso = nodoDestino;
            while (paso != null) {
                camino.addFirst((Aeropuerto) paso.data);
                paso = paso.predecesor;
            }
        }
        return new Ruta(camino, nodoDestino.pesoAcumulado, criterio);
    }

    public List<Ruta> encontrarRutasAlternativas(V origenData, V destinoData, Ruta.Criterio criterio, int maxRutas) {
        Nodo<V, E> nodoInicio = buscarNodo(origenData);
        Nodo<V, E> nodoDestino = buscarNodo(destinoData);
        List<Ruta> rutasEncontradas = new ArrayList<>();

        if (nodoInicio == null || nodoDestino == null) {
            return rutasEncontradas;
        }

        List<LinkedList<Nodo<V, E>>> todosLosCaminos = new ArrayList<>();
        LinkedList<Nodo<V, E>> caminoActual = new LinkedList<>();
        caminoActual.add(nodoInicio);

        buscarCaminosDFS(nodoInicio, nodoDestino, caminoActual, todosLosCaminos);

        for (LinkedList<Nodo<V, E>> camino : todosLosCaminos) {
            LinkedList<Aeropuerto> aeropuertosEnRuta = new LinkedList<>();
            double pesoTotal = 0.0;
            Nodo<V, E> previo = null;

            for (Nodo<V, E> paso : camino) {
                aeropuertosEnRuta.add((Aeropuerto) paso.data);
                if (previo != null) {
                    Arista<V, E> arista = encontrarArista(previo, paso);
                    if (arista != null) {
                        pesoTotal += calcularPesoArista(arista, criterio);
                    }
                }
                previo = paso;
            }
            rutasEncontradas.add(new Ruta(aeropuertosEnRuta, pesoTotal, criterio));
        }

        rutasEncontradas.sort(Comparator.comparingDouble(Ruta::getPesoTotal));
        return rutasEncontradas.subList(0, Math.min(maxRutas, rutasEncontradas.size()));
    }

    private void buscarCaminosDFS(Nodo<V, E> actual, Nodo<V, E> destino, LinkedList<Nodo<V, E>> caminoActual, List<LinkedList<Nodo<V, E>>> todosLosCaminos) {
        if (actual.equals(destino)) {
            todosLosCaminos.add(new LinkedList<>(caminoActual));
            return;
        }

        for (Arista<V, E> arista : actual.adyacentes) {
            Nodo<V, E> vecino = arista.destino;
            if (!caminoActual.contains(vecino)) {
                caminoActual.addLast(vecino);
                buscarCaminosDFS(vecino, destino, caminoActual, todosLosCaminos);
                caminoActual.removeLast();
            }
        }
    }

    private Arista<V, E> encontrarArista(Nodo<V, E> origen, Nodo<V, E> destino) {
        for (Arista<V, E> arista : origen.adyacentes) {
            if (arista.destino.equals(destino)) {
                return arista;
            }
        }
        return null;
    }

    private double calcularPesoArista(Arista<V, E> arista, Ruta.Criterio criterio) {
        PesoVuelo pesoVuelo = (PesoVuelo) arista.peso;
        switch (criterio) {
            case TIEMPO: return pesoVuelo.getTiempo();
            case COSTO: return pesoVuelo.getCosto();
            default: return pesoVuelo.getDistancia();
        }
    }
}
