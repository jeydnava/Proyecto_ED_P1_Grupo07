package proyectoaeropuerto;

import java.util.Objects;

public class Aeropuerto implements Comparable<Aeropuerto> {
    //Atributos de identificación de un aeropuerto
    private String codigoIATA;
    private String nombre;
    private String ciudad;

    //Atributo a utlizar para mostrar los nodos en la app (clase GraphView)
    public float x, y;

    public Aeropuerto(String codigoIATA, String nombre, String ciudad) {
        this.codigoIATA = codigoIATA;
        this.nombre = nombre;
        this.ciudad = ciudad;
    }

    public String getCodigoIATA() {
        return codigoIATA;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCiudad() {
        return ciudad;
    }

    @Override
    public int compareTo(Aeropuerto otro) {
        return this.codigoIATA.compareTo(otro.codigoIATA);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return codigoIATA.equals(((Aeropuerto) obj).codigoIATA);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoIATA);
    }

    @Override
    public String toString() {
        return nombre + " (" + codigoIATA + ")";
    }
}


