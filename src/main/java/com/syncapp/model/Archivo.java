package com.syncapp.model;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Archivo implements Serializable {
    public final String ruta; //Ruta a un archivo de forma realitva a la carpeta que queremos sincronizar
    public String hash;
    public long timeMilisLastModified; //TODO CAMBIAR LISTAR ARCHIVOS PARA QUE INCLUYA TIME
    public long sizeInBytes;


    public Archivo(String s) {
        ruta = new String(s);
    }

    public Archivo(Path t) {
        ruta = t.toString();
    }

    @Override
    public String toString() {
        return "[ruta="+ruta+", hash="+hash+", lastModified="+timeMilisLastModified+"]";
    }

    public Path toPath(Path parents) {
        if(parents == null) return null;

        return Paths.get(parents.toString(), ruta);
    }

    public File toFile(Path parents) {
        if(parents == null) return null;

        return Paths.get(parents.toString(), ruta).toFile();
    }
}
