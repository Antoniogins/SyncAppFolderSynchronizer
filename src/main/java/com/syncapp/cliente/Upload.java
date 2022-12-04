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

public class Upload implements Runnable{
    
    SyncApp server;
    int id_file;
    Archivo ruta;
    Path abs;
    TokenUsuario tu;
    LectorArchivos la;
    int ultimo_enviado;

    public Upload(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario tu) throws IOException {
        this.server = server;
        this.tu = tu;

        Path path = Paths.get(ruta.ruta);
        Path wfold = Paths.get(pathlocal);
        this.ruta = new Archivo( path , wfold);
        this.abs = wfold;
        this.abs = wfold.resolve(path);



        this.la = new LectorArchivos(abs, "r");
        ultimo_enviado = -1; //Todavia no se ha enviado ninguno
    }

    public void run() {
        try {
            id_file = server.abrirArchivo(tu, ruta, "rw");
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        System.out.println("subiendo file="+id_file+" ruta="+ruta.toString());

        la.id_file = id_file;
        boolean keep = true;
        BloqueBytes bb = null;
        boolean saltarLectura = false;
        boolean reintentarLocal = false;

        while(keep) {
            // Si se activa intentarRemoto es porque ha fallado al enviar el ultimo bloque,
            // por tanto no leemos e intentamos reenviar ese bloque fallido, que todavia lo
            // tenemos en memoria
            if (!saltarLectura) {
                try {
                    if(reintentarLocal) {
                        bb = la.leerBloqueBytesEnPosicion((ultimo_enviado + 1)* Utilidades.MAX_BYTES_IN_BLOCK );
                        // Cuando reintentamos enviar un bloque, sabemos que su posicion inicial es su
                        // identificador*MAX_BYTES
                        // Pero como unicamente conocemos el ultimo bloque enviado, si le sumamos uno
                        // tenemos el bloque que estamos enviando. Explicado en txt

                        reintentarLocal = false;
                    } else {
                        // Indicamos -1 para que el lector de archivos se encarge de gestionar la
                        // posicion
                        bb = la.leerBloqueBytesEnPosicion(-1);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("reintentando leer file="+id_file+" bloque="+(ultimo_enviado+1));
                    reintentarLocal = true;
                }

                // Comprobamos si bloque es null o su tamaño es 0, esto indica que hemos acabado
                // de leer el archivo, salimos
                if (bb == null || bb.size == 0) {
                    keep = false;
                    continue;
                }

            }
            

            

            try {
                int posicionALeer = (saltarLectura)? 1 : -1 ;
                server.escribirBloqueBytes(id_file, bb, posicionALeer);

                // Si se ejecuta este bloque es porque escribirBloqueBytes no ha fallado,
                // dejamos de reintentarlo
                saltarLectura = false;
                ultimo_enviado++;
                // System.out.println(bb.toString());


            } catch (RemoteException e) {
                e.printStackTrace();
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

        System.out.println("Archivo file="+id_file+" cargado");



    }
}
