package com.syncapp.model;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Archivo implements Serializable {
    public final String ruta; //Ruta a un archivo de forma realitva a la carpeta que queremos sincronizar
    public String workingFOlder;
    public String hash;
    public long timeMilisLastModified; //TODO CAMBIAR LISTAR ARCHIVOS PARA QUE INCLUYA TIME
    public long sizeInBytes;



    public Archivo(Path archivo, Path workingPath) {
        if(  archivo.isAbsolute()  ) {
            ruta = (workingPath.relativize(archivo)).toString();
        } else {
            ruta = archivo.toString();
        }

        workingFOlder = workingPath.toString();

    }



    @Override
    public String toString() {
        return "[ruta="+ruta+", hash="+hash+", lastModified="+timeMilisLastModified+"]";
    }

    public Path toPath() {
        return Paths.get(workingFOlder , ruta);
    }

    public Path toRelativePath() {
        return Paths.get(ruta);
    }



    public File toFile() {
        return Paths.get(workingFOlder , ruta).toFile();
    }
}
