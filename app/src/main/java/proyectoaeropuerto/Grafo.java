package proyectoaeropuerto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class Grafo<V, E> {
    private final LinkedList<Nodo<V, E>> nodos;
    private final Comparator<Nodo<V, E>> dijkstraComparator;

    public static class Nodo<V, E> {
        final V data;
        final LinkedList<Arista<V, E>> adyacentes = new LinkedList<>();
        double pesoAcumulado = Double.POSITIVE_INFINITY;
        Nodo<V, E> predecesor = null;
        boolean visitado = false;
        Nodo(V data) { this.data = data; }
        void resetDijkstra() {
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

    public Grafo(Comparator<Nodo<V, E>> dijkstraComparator) {
        this.nodos = new LinkedList<>();
        this.dijkstraComparator = dijkstraComparator;
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
        Nodo<V, E> origen = buscarNodo(origenData);
        Nodo<V, E> destino = buscarNodo(destinoData);
        if (origen == null || destino == null) return new Ruta(new LinkedList<>(), -1, criterio);

        nodos.forEach(Nodo::resetDijkstra);
        origen.pesoAcumulado = 0;
        PriorityQueue<Nodo<V, E>> cola = new PriorityQueue<>(dijkstraComparator);
        cola.add(origen);

        while (!cola.isEmpty()) {
            Nodo<V, E> actual = cola.poll();
            assert actual != null;
            if (actual.visitado) continue;
            actual.visitado = true;
            if (actual.equals(destino)) break;

            for (Arista<V, E> arista : actual.adyacentes) {
                Nodo<V, E> vecino = arista.destino;
                if (!vecino.visitado) {
                    PesoVuelo pesoVuelo = (PesoVuelo) arista.peso;
                    double pesoArista;
                    switch (criterio) {
                        case TIEMPO: pesoArista = pesoVuelo.tiempo; break;
                        case COSTO: pesoArista = pesoVuelo.costo; break;
                        default: pesoArista = pesoVuelo.distancia; break;
                    }
                    double nuevoPeso = actual.pesoAcumulado + pesoArista;
                    if (nuevoPeso < vecino.pesoAcumulado) {
                        vecino.pesoAcumulado = nuevoPeso;
                        vecino.predecesor = actual;
                        cola.add(vecino);
                    }
                }
            }
        }

        LinkedList<Aeropuerto> camino = new LinkedList<>();
        if (destino.pesoAcumulado != Double.POSITIVE_INFINITY) {
            Nodo<V, E> paso = destino;
            while (paso != null) {
                camino.addFirst((Aeropuerto) paso.data);
                paso = paso.predecesor;
            }
        }
        return new Ruta(camino, destino.pesoAcumulado, criterio);
    }
}
