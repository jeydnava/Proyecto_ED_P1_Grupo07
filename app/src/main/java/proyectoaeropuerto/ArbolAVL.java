package proyectoaeropuerto;

import java.util.Comparator;
import java.util.LinkedList;

public class ArbolAVL<T extends Comparable<T>> {
    private NodoAVL raiz;
    private Comparator<T> comparador;

    public ArbolAVL(Comparator<T> comparador) {
        this.comparador = comparador;
    }

    private class NodoAVL {
        T data;
        NodoAVL izquierdo;
        NodoAVL derecho;
        int altura;
        NodoAVL(T data) {
            this.data = data;
            this.altura = 1;
        }
    }

    //Metodos vistos en clase
    private int altura(NodoAVL n) {
        return (n == null) ? 0 : n.altura;
    }

    private int getBalance(NodoAVL n) {
        return (n == null) ? 0 : altura(n.derecho) - altura(n.izquierdo);
    }

    private NodoAVL rotacionDerecha(NodoAVL y) {
        NodoAVL x = y.izquierdo;
        NodoAVL T2 = x.derecho;
        x.derecho = y;
        y.izquierdo = T2;
        y.altura = Math.max(altura(y.izquierdo), altura(y.derecho)) + 1;
        x.altura = Math.max(altura(x.izquierdo), altura(x.derecho)) + 1;
        return x;
    }

    private NodoAVL rotacionIzquierda(NodoAVL x) {
        NodoAVL y = x.derecho;
        NodoAVL T2 = y.izquierdo;
        y.izquierdo = x;
        x.derecho = T2;
        x.altura = Math.max(altura(x.izquierdo), altura(x.derecho)) + 1;
        y.altura = Math.max(altura(y.izquierdo), altura(y.derecho)) + 1;
        return y;
    }

    public void insertar(T data) {
        raiz = insertarRecursivo(raiz, data);
    }

    private NodoAVL insertarRecursivo(NodoAVL nodo, T data) {
        if (nodo == null) return new NodoAVL(data);
        int cmp = comparador.compare(data, nodo.data);
        if (cmp < 0) nodo.izquierdo = insertarRecursivo(nodo.izquierdo, data);
        else if (cmp > 0) nodo.derecho = insertarRecursivo(nodo.derecho, data);
        else return nodo;

        nodo.altura = 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
        int balance = getBalance(nodo);

        if (balance < -1 && comparador.compare(data, nodo.izquierdo.data) < 0) return rotacionDerecha(nodo);
        if (balance > 1 && comparador.compare(data, nodo.derecho.data) > 0) return rotacionIzquierda(nodo);
        if (balance < -1 && comparador.compare(data, nodo.izquierdo.data) > 0) {
            nodo.izquierdo = rotacionIzquierda(nodo.izquierdo);
            return rotacionDerecha(nodo);
        }
        if (balance > 1 && comparador.compare(data, nodo.derecho.data) < 0) {
            nodo.derecho = rotacionDerecha(nodo.derecho);
            return rotacionIzquierda(nodo);
        }
        return nodo;
    }

    public T buscar(T dataKey) {
        NodoAVL nodo = buscarRecursivo(raiz, dataKey);
        return (nodo != null) ? nodo.data : null;
    }

    private NodoAVL buscarRecursivo(NodoAVL nodo, T dataKey) {
        if (nodo == null) return null;
        int cmp = comparador.compare(dataKey, nodo.data);
        if (cmp == 0) return nodo;
        return cmp < 0 ? buscarRecursivo(nodo.izquierdo, dataKey) : buscarRecursivo(nodo.derecho, dataKey);
    }

    public void eliminar(T data) {
        raiz = eliminarRecursivo(raiz, data);
    }

    private NodoAVL eliminarRecursivo(NodoAVL nodo, T data) {
        if (nodo == null) return null;
        int cmp = comparador.compare(data, nodo.data);
        if (cmp < 0) nodo.izquierdo = eliminarRecursivo(nodo.izquierdo, data);
        else if (cmp > 0) nodo.derecho = eliminarRecursivo(nodo.derecho, data);
        else {
            if ((nodo.izquierdo == null) || (nodo.derecho == null)) {
                nodo = (nodo.izquierdo != null) ? nodo.izquierdo : nodo.derecho;
            } else {
                NodoAVL temp = encontrarNodoMinimo(nodo.derecho);
                nodo.data = temp.data;
                nodo.derecho = eliminarRecursivo(nodo.derecho, temp.data);
            }
        }
        if (nodo == null) return null;
        nodo.altura = Math.max(altura(nodo.izquierdo), altura(nodo.derecho)) + 1;
        int balance = getBalance(nodo);
        if (balance < -1 && getBalance(nodo.izquierdo) <= 0) return rotacionDerecha(nodo);
        if (balance < -1 && getBalance(nodo.izquierdo) > 0) {
            nodo.izquierdo = rotacionIzquierda(nodo.izquierdo);
            return rotacionDerecha(nodo);
        }
        if (balance > 1 && getBalance(nodo.derecho) >= 0) return rotacionIzquierda(nodo);
        if (balance > 1 && getBalance(nodo.derecho) < 0) {
            nodo.derecho = rotacionDerecha(nodo.derecho);
            return rotacionIzquierda(nodo);
        }
        return nodo;
    }

    private NodoAVL encontrarNodoMinimo(NodoAVL nodo) {
        NodoAVL actual = nodo;
        while (actual.izquierdo != null) {
            actual = actual.izquierdo;
        }
        return actual;
    }

    //Entrega los nodos del arbol en forma de lista
    public LinkedList<T> getDatosEnOrden() {
        LinkedList<T> lista = new LinkedList<>();
        inOrderRecursivo(raiz, lista);
        return lista;
    }
    
    //Recorre el arbol en "EnOrden" para agregarlos a la lista
    private void inOrderRecursivo(NodoAVL nodo, LinkedList<T> lista) {
        if (nodo != null) {
            inOrderRecursivo(nodo.izquierdo, lista);
            lista.add(nodo.data);
            inOrderRecursivo(nodo.derecho, lista);
        }
    }
}
