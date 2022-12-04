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
import com.syncapp.utility.Utilidades;


public class Download implements Runnable {
    
    SyncApp server;
    int id_file;
    Archivo ruta;
    Path abs;
    TokenUsuario usuario;
    LectorArchivos la;
    int ultimo_recibido;

    public Download(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario usuario) throws IOException {
        this.server = server;
        this.usuario = usuario;

        Path path = Paths.get(ruta.ruta);
        Path wfold = Paths.get(pathlocal);
        this.ruta = new Archivo( path , wfold);
        this.abs = wfold.resolve(path);


        this.la = new LectorArchivos(abs, "rw");
        ultimo_recibido = -1; //Replica de upload
    }


    public void run() {
        try {
            id_file = server.abrirArchivo(usuario, ruta, "r");
            // System.out.println("id_File"+id_file);
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        System.out.println("bajando file="+id_file+" ruta="+ruta.toString());


        boolean keep = true;
        BloqueBytes bb = null;
        boolean saltarLectura = false;
        boolean reintentarRemoto = false;

        while (keep) {
            if (!saltarLectura) {

                try {
                    if (reintentarRemoto) {

                        // Analgo a Upload (se nota cual hicimos primero????)
                        bb = server.leerBloqueBytes(id_file, (ultimo_recibido + 1)* Utilidades.MAX_BYTES_IN_BLOCK );
                        reintentarRemoto = false;

                    } else {

                        // Indicamos -1 para que el lector de archivos se encarge de gestionar la
                        // posicion
                        bb = server.leerBloqueBytes(id_file, -1);

                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                    System.out.println("reintentando recibir file="+id_file+" bloque="+(ultimo_recibido+1));
                    reintentarRemoto = true;
                }

                // Comprobamos si bloque es null o su tamaño es 0, esto indica que hemos acabado
                // de leer el archivo, salimos
                if (bb == null || bb.size == 0) {
                    keep = false;
                    continue;
                }

            }

            // Si se activa intentarRemoto es porque ha fallado al enviar el ultimo bloque,
            // por tanto no leemos e intentamos reenviar ese bloque fallido, que todavia lo
            // tenemos en memoria

            try {

                la.escribirBloqueBytes(bb);

                // Si se ejecuta este bloque es porque escribirBloque no ha fallado,
                // dejamos de reintentarlo
                saltarLectura = false;
                ultimo_recibido++;
                // System.out.println(bb.toString());

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Reintentando escribir file="+id_file+" bloque="+(ultimo_recibido+1));
                saltarLectura = true;
                // Queremos que salte la lectura para que intente reenvia el ultimo bloque que
                // todavia tenemos en memoria
            }

        }

        try {
            server.cerrarArchivo(id_file);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        
        try {
            la.cerrarArchivo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Archivo file="+id_file+" descargado");



    }
}
