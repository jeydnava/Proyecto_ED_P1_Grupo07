package com.example.proyectoaeropuerto;

import java.util.LinkedList;
import java.util.Locale;

public class Ruta {
    private LinkedList<Aeropuerto> aeropuertos;
    private double pesoTotal;
    private Criterio criterio;
    public enum Criterio { DISTANCIA, TIEMPO, COSTO }

    public Ruta(LinkedList<Aeropuerto> aeropuertos, double pesoTotal, Criterio criterio) {
        this.aeropuertos = aeropuertos;
        this.pesoTotal = pesoTotal;
        this.criterio = criterio;
    }
    public LinkedList<Aeropuerto> getAeropuertos() {
        return aeropuertos;
    }

    @Override
    public String toString() {
        if (aeropuertos == null || aeropuertos.isEmpty() || pesoTotal == Double.POSITIVE_INFINITY) {
            return "No se encontró una ruta.";
        }
        StringBuilder sb = new StringBuilder("Ruta encontrada (");
        switch (criterio) {
            case DISTANCIA: sb.append(String.format(Locale.US, "%.0f km", pesoTotal)); break;
            case TIEMPO: sb.append(String.format(Locale.US, "%.0f min", pesoTotal)); break;
            case COSTO: sb.append(String.format(Locale.US, "$%.2f", pesoTotal)); break;
        }
        sb.append("): ");
        for (int i = 0; i < aeropuertos.size(); i++) {
            sb.append(aeropuertos.get(i).getCodigoIATA());
            if (i < aeropuertos.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }
}
