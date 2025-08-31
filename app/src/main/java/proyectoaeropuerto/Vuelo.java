package proyectoaeropuerto;

//representaría los arcos en el grafo
public class Vuelo {
    private final Aeropuerto origen;
    private final Aeropuerto destino;
    private final PesoVuelo pesos;

    public Vuelo(Aeropuerto origen, Aeropuerto destino, PesoVuelo pesos) {
        this.origen = origen;
        this.destino = destino;
        this.pesos = pesos;
    }
    public Aeropuerto getOrigen() {
        return origen;
    }

    public Aeropuerto getDestino() {
        return destino;
    }

    public int getDistancia() {
        return pesos.getDistancia();
    }

    public int getTiempo() {
        return pesos.getTiempo();
    }

    public double getCosto() {
        return pesos.getCosto();
    }
}
