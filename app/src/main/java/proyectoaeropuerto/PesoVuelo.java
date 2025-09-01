package proyectoaeropuerto;

import java.util.Objects;

public class PesoVuelo {
    final int distancia;
    final int tiempo;
    final double costo;

    public PesoVuelo(int distancia, int tiempo, double costo) {
        this.distancia = distancia;
        this.tiempo = tiempo;
        this.costo = costo;
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