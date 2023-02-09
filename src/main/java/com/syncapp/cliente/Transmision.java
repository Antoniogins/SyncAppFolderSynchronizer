package com.syncapp.cliente;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.LectorArchivos;

import java.io.IOException;
import java.nio.file.Path;

public class Transmision implements Runnable {

    /**
     * Servidor del que se va a cargar el archivo.
     */
    SyncApp server;

    /**
     * Identificador del archivo que se va a cargar. Inicialmente no se conoce, hasta que se le pide al servidor el mismo.
     */
    int fileId;

    /**
     * Ruta relativa del archivo que se quiere cargar.
     */
    Archivo ruta;

    /**
     * Directorio de trabajo del cliente, para poder obtener la ruta completa del archivo que hay que escribir en la
     * maquina local.
     */
    Path abs;

    /**
     * Usuario que quiere cargar el archivo.
     */
    TokenUsuario usuario;

    /**
     * Lector de archivos que se usara para escribir el archivo en la maquina local.
     */
    LectorArchivos lectorArchivos;

    /**
     * Posicion del bloque que se esta enviando. Es especialmente util cuando se interrumpe una transmision, y queremos
     * asegurarnos de obtener el bloque completo.
     */
    long posicionActual;

    /**
     * Indica el tipo de operacion a realizar. El tipo de operacion viene recogido en la enumeracion {@link TipoDeTransmisiones}.
     */
    private final TipoDeTransmisiones tipoOperacion;

    /**
     * Enumeracion para almacenar los tipos de transmisiones que se pueden realizar.
     */
    public enum TipoDeTransmisiones{
        UPLOAD,
        DOWNLOAD
    }




    // Constructor

    /**
     * En el constructor necesitamos indicar los parametros necesarios para realizar la transmision del archivo.
     * @param server {@link SyncApp servidor} con el que queremos transmitir.
     * @param ruta {@link Archivo} que queremos transmitir.
     * @param pathlocal {@link String directorio} de trabajo del cliente.
     * @param usuario {@link TokenUsuario usuario} con que el que iniciaremos la transmision. Previamente necesitara
     *                                            haber iniciado sesion.
     * @throws IOException si ocurre un problema al crear el {@link LectorArchivos}.
     */

    public Transmision(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario usuario, TipoDeTransmisiones tipo) throws IOException {

        // Guardamos los valores
        this.server = server;
        this.usuario = usuario;
        this.ruta = ruta;
        this.ruta.parentFolder = pathlocal;
        this.abs = this.ruta.toPath();
        this.tipoOperacion = tipo;

        if(tipo == TipoDeTransmisiones.UPLOAD) {
            this.lectorArchivos = new LectorArchivos(abs, "r", 999);
        } else {
            this.lectorArchivos = new LectorArchivos(abs, "rw", 999);
        }



        // Ponemos la posicion en 0, para comenzar desde el principio.
        posicionActual = 0;
    }





    @Override
    public void run() {

    }
}
