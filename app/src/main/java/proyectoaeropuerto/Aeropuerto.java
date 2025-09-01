package proyectoaeropuerto;

import java.util.Objects;

public class Aeropuerto implements Comparable<Aeropuerto> {
    //Atributos de identificación de un aeropuerto
    private String codigoIATA;
    private String nombre;
    private String ciudad;

    //Atributo a utlizar para mostrar los nodos en la interfaz (clase ViewController)
    public float x, y;

    //Constructor
    public Aeropuerto(String codigoIATA, String nombre, String ciudad) {
        this.codigoIATA = codigoIATA;
        this.nombre = nombre;
        this.ciudad = ciudad;
    }

    //Getters y setters
    public String getCodigoIATA() {
        return codigoIATA;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCodigoIATA(String codigoIATA) {
        this.codigoIATA = codigoIATA;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    //Define el orden natural de Aeropuertos por su codigo IATA
    @Override
    public int compareTo(Aeropuerto otro) {
        return this.codigoIATA.compareTo(otro.codigoIATA);
    }

    //Define como se consideran dos aeropuertos iguales
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return codigoIATA.equals(((Aeropuerto) obj).codigoIATA);
    }

    //Genera un hash para el Aeropuerto basado en el codigo IATA
    @Override
    public int hashCode() {
        return Objects.hash(codigoIATA);
    }

    @Override
    public String toString() {
        return nombre + " (" + codigoIATA + ")";
    }
}


