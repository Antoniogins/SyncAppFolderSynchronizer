package com.syncapp.cliente;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.LectorArchivos;

public class Upload implements Runnable{
    
    SyncApp server;
    int fileId;
    Archivo ruta;
    Path abs;
    TokenUsuario usuario;
    LectorArchivos lectorArchivos;
//    int ultimo_enviado;
    long posicionActual;

    public Upload(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario usuario) throws IOException {
        this.server = server;
        this.usuario = usuario;

        this.ruta = ruta;
        this.ruta.workingFOlder = pathlocal;
        abs = this.ruta.toPath();



        this.lectorArchivos = new LectorArchivos(abs, "r", 999); // el id lo cambiaremos en cuanto dispongamos
        posicionActual = 0;
    }




    boolean siguienteBloque() {
        BloqueBytes bloque = new BloqueBytes();

        boolean reintentarLectura = true;
        while(reintentarLectura) {
            try {
                bloque = lectorArchivos.leerBloqueBytes(posicionActual);

                // Llegamos unicamente a este putno si leerBloqueBytes tiene exito
                reintentarLectura = false;
            } catch (IOException e) {
                System.out.println("reintentando leer file="+ fileId +" pos="+posicionActual);
                e.printStackTrace();
            }
        }

        if(bloque == null) {
            return false;
        }


        boolean reintentarEnvio = true;
        while (reintentarEnvio) {
            try {
                server.escribirBloqueBytes(fileId, bloque);
                System.out.println("enviando bloque "+bloque);

                // Si llegamos a este punto, es que escribir el bloque en el servidor ha tenido exito
                reintentarEnvio = false;
                posicionActual += bloque.size;


            } catch (RemoteException e) {
                System.out.println("reintentando leer file=\""+ fileId +"\" pos=\""+posicionActual);
            }
        }

        // si todas las operaciones se han realizado con exito, llegaremos a este punto
        return true;
    }









    public void run() {
        try {
            fileId = server.abrirArchivo(usuario, ruta, "rw");
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        System.out.println("subiendo file="+ fileId +" "+ruta);

        lectorArchivos.setFileId(fileId);
        ruta.remoteID = ""+fileId;


        boolean quedanBloques;
        do {
            quedanBloques = siguienteBloque();
        } while (quedanBloques);


        // Llegamos a este punto cuando no quedan bloques por enviar, liberamos recursos

        try {
            server.cerrarArchivo(fileId, usuario);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            lectorArchivos.cerrarArchivo();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Archivo file="+ fileId +" cargado");















//        lectorArchivos.id_file = fileId;
//        boolean keep = true;
//        BloqueBytes bb = null;
//        boolean saltarLectura = false;
//        boolean reintentarLocal = false;
//
//        boolean reintentarLectura = true;
//        while(reintentarLectura) {
//            try {
//                bb = lectorArchivos.leerBloqueBytes(posicionActual);
//
//                // Llegamos unicamente a este putno si leerBloqueBytes tiene exito
//                reintentarLectura = false;
//            } catch (IOException e) {
//                System.out.println("reintentando leer file="+ fileId +" bloque="+(ultimo_enviado+1));
//                e.printStackTrace();
//            }
//        }
//
//
//
//        while(keep) {
//            // Si se activa intentarRemoto es porque ha fallado al enviar el ultimo bloque,
//            // por tanto no leemos e intentamos reenviar ese bloque fallido, que todavia lo
//            // tenemos en memoria
//            if (!saltarLectura) {
//                try {
//                    if(!reintentarLocal) {
//                        bb = lectorArchivos.leerBloqueBytes(posicionActual);
//                    }
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    System.out.println("reintentando leer file="+ fileId +" bloque="+(ultimo_enviado+1));
//                    reintentarLocal = true;
//                }
//
//                // Comprobamos si bloque es null o su tamaño es 0, esto indica que hemos acabado
//                // de leer el archivo, salimos
//                if (bb == null || bb.size == 0) {
//                    keep = false;
//                    continue;
//                }
//
//            }
//
//
//
//
//            try {
//                server.escribirBloqueBytes(fileId, bb);
//
//                // Si se ejecuta este bloque es porque escribirBloqueBytes no ha fallado,
//                // dejamos de reintentarlo
//                saltarLectura = false;
//                posicionActual += bb.size;
//                ultimo_enviado++;
//                // System.out.println(bb.toString());
//
//
//            } catch (RemoteException e) {
//                e.printStackTrace();
//                saltarLectura = true;
//                // Queremos que salte la lectura para que intente reenvia el ultimo bloque que
//                // todavia tenemos en memoria
//            }
//
//
//        }
//
//
//
//        try {
//            server.cerrarArchivo(fileId, tu);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            lectorArchivos.cerrarArchivo();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Archivo file="+ fileId +" cargado");



    }
}
