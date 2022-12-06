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
import com.syncapp.utility.VariablesGlobales;


public class Download implements Runnable {
    
    SyncApp server;
    int id_file;
    Archivo ruta;
    Path abs;
    TokenUsuario usuario;
    LectorArchivos la;
    int ultimo_recibido;
    long posicion;

    public Download(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario usuario) throws IOException {
        this.server = server;
        this.usuario = usuario;

        Path path = Paths.get(ruta.ruta);
        Path wfold = Paths.get(pathlocal);
        this.ruta = new Archivo( path , wfold);
        this.abs = wfold.resolve(path);


        this.la = new LectorArchivos(abs, "rw", 0);
        ultimo_recibido = -1; //Replica de upload
        posicion = 0;
    }


    public void run() {
        try {
            id_file = server.abrirArchivo(usuario, ruta, "r");
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        la.id_file = id_file;
        System.out.println("bajando file="+id_file+" ruta="+ruta.toString());


        boolean keepTransmission = true;
        BloqueBytes bb = null;
        boolean leerBytes = true;

        while (keepTransmission) {
            if (leerBytes) {

                try {
                    bb = server.leerBloqueBytes(id_file, posicion);
                    posicion += bb.size;

                } catch (RemoteException e) {
                    e.printStackTrace();
                    System.out.println("reintentando recibir file="+id_file+" bloque="+(ultimo_recibido+1));
                }



                if(bb == null) {
                    continue;
                } else if(bb.size < VariablesGlobales.MAX_BYTES_IN_BLOCK) {
                    keepTransmission = false;
                }

            }


            // Si se activa intentarRemoto es porque ha fallado al enviar el ultimo bloque,
            // por tanto no leemos e intentamos reenviar ese bloque fallido, que todavia lo
            // tenemos en memoria

            try {

                la.escribirBloqueBytes(bb);

                // Si se ejecuta este bloque es porque escribirBloque no ha fallado,
                // dejamos de reintentarlo
                leerBytes = true;
                ultimo_recibido++;
                // System.out.println(bb.toString());

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Reintentando escribir file="+id_file+" bloque="+(ultimo_recibido+1));
                leerBytes = false;
                // Queremos que salte la lectura para que intente escribir el ultimo bloque, que
                // todavia tenemos en memoria
            }

        }


        
        try {
            la.cerrarArchivo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Archivo file="+id_file+" descargado");



    }
}
