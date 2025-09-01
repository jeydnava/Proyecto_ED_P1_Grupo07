package proyectoaeropuerto;

import java.util.Objects;

public class PesoVuelo {
    final int distancia;
    final int tiempo;
    final double costo;
    private int demanda;

    public PesoVuelo(int distancia, int tiempo, double costo) {
        this.distancia = distancia;
        this.tiempo = tiempo;
        this.costo = costo;
        this.demanda = 0;
    }

    public PesoVuelo(int distancia, int tiempo, double costo, int demanda) {
        this.distancia = distancia;
        this.tiempo = tiempo;
        this.costo = costo;
        this.demanda = demanda;
    }

    public int getDistancia() {
        return distancia;
    }

    public int getTiempo() {
        return tiempo;
    }

    public double getCosto() {
        return costo;
    }

    public int getDemanda() {
        return demanda;
    }

    public void incrementarDemanda() {
        this.demanda++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PesoVuelo pesoVuelo = (PesoVuelo) o;
        return distancia == pesoVuelo.distancia &&
                tiempo == pesoVuelo.tiempo &&
                Double.compare(pesoVuelo.costo, costo) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distancia, tiempo, costo);
    }
}