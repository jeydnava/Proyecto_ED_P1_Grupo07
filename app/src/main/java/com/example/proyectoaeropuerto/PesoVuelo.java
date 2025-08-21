package com.example.proyectoaeropuerto;

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
}
