package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;

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
    public String remoteID;





    public Archivo(Path archivo, Path workingPath) {
        if(  archivo.isAbsolute()  ) {
            ruta = (workingPath.relativize(archivo)).toString();
        } else {
            ruta = archivo.toString();
        }

        workingFOlder = workingPath.toString();

        timeMilisLastModified = -1;

    }

    public Archivo(String pathRelativo) {
        this.ruta = new String(pathRelativo);
        timeMilisLastModified = -1;
    }



    @Override
    public String toString() {
        String toRet = "["+ VariablesGlobales.COLOR_CYAN;
        toRet = toRet.concat(ruta+VariablesGlobales.COLOR_WHITE+",");
        toRet = toRet.concat(  (hash == null)? "no_hash," : (hash+",") );
        toRet = toRet.concat(  (timeMilisLastModified < 0)? "no_time," : (""+timeMilisLastModified)  );

        float kb = (float) sizeInBytes/1000; //Para saber cuantos kB
        float mb = kb/1000;
        float gb = mb/1000;

        String sizeText = (gb<1) ? ( (mb<1) ? ( (kb<1) ? (sizeInBytes+"B") : kb+"KB" )  : (mb+"MB")) : (gb+"GB")  ;



        return toRet+sizeText+"]";
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
