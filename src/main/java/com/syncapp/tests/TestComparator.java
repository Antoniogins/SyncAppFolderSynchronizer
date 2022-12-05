package com.syncapp.tests;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.syncapp.model.Archivo;
import com.syncapp.utility.Utilidades;

public class TestComparator {
    public static void main(String[] args) {
        Path folder = Paths.get("/home", "antonio", "pruebas");

        ArrayList<Archivo> listaArchivos = Utilidades.listFiles(folder);
        System.out.println("Lista de archivos:");
        listaArchivos.forEach(System.out::println);

        ArrayList<Archivo> archivosConInformacion = Utilidades.obtenerMultiplesMetadatos(listaArchivos, folder);
        System.out.println("\nLista de archivos");
        archivosConInformacion.forEach(System.out::println);
    }
}
