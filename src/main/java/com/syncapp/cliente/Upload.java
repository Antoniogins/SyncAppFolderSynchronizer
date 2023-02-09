package com.syncapp.cliente;

import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.LectorArchivos;


/**
 * Tarea de carga de un archivo. Permite al cliente ejecutar la carga de un archivo mediante un hilo, puediendo
 * optimizar el cliente.
 */
public class Upload implements Runnable{

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

    public Upload(SyncApp server, Archivo ruta, String pathlocal, TokenUsuario usuario) throws IOException {

        // Guardamos los valores
        this.server = server;
        this.usuario = usuario;
        this.ruta = ruta;
        this.ruta.parentFolder = pathlocal;
        this.abs = this.ruta.toPath();

        this.lectorArchivos = new LectorArchivos(abs, "r", 999);

        // Ponemos la posicion en 0, para comenzar desde el principio.
        posicionActual = 0;
    }










    /**
     * Este metodo nos permite leer de forma controlada un bloque de bytes hacia el servidor. Se debe ejecutar hasta
     * que devuelva falso, que significara que ha terminado de cargar el archivo.
     * @return
     * <ul>
     *     <li>
     *         Verdadero - si quedan bloques por escribir para completar el archivo.
     *     </li>
     *     <li>
     *         Falso - si ya se han escrito todos los bloques.
     *     </li>
     * </ul>
     */
    boolean siguienteBloque() {

        // Creamos la variable que representa el bloque a descargar
        BloqueBytes bloque = null;

        // Utilizamos esta variable para reintentar leer el bloque de bytes hasta que se tenga exito
        boolean reintentarLectura = true;
        while(reintentarLectura) {
            try {
                // Leemos el bloque de bytes del lector de archivos
                bloque = lectorArchivos.leerBloqueBytes(posicionActual);

                // Llegamos unicamente a este putno si leerBloqueBytes tiene exito, por tanto dejamos de reintentar
                reintentarLectura = false;

            } catch (IOException e) {
                System.out.println("reintentando leer file="+ fileId +" pos="+posicionActual);
                e.printStackTrace();
            }
        }

        // Si ocurre algun problema, se devuelve falso
        if(bloque == null) {
            return false;
        }


        // Utilizamos esta variable para reintentar escribir el bloque en la maquina remota
        boolean reintentarEnvio = true;
        while (reintentarEnvio) {
            try {

                // Intentamos escribir el bloque de bytes
                server.escribirBloqueBytes(fileId, bloque);
                System.out.println("enviando bloque "+bloque);

                // Si llegamos a este punto, es que se ha escrito el bloque con exito en la maquina remota

                // Dejamos de reintentar escribir el bloque
                reintentarEnvio = false;

                // Como hemos conseguido escribir el bloque con exito, aumentamos la posicion que se debe leer
                posicionActual += bloque.size;


            } catch (RemoteException e) {
                System.out.println("reintentando leer file=\""+ fileId +"\" pos=\""+posicionActual);
            }
        }

        // si todas las operaciones se han realizado con exito, llegaremos a este punto
        return true;
    }









    /**
     * Tarea de carga del archivo. Con este metodo, que proviene de implementar la interfaz Runnable, permitimos
     * que un gestor de tareas, ejecute la tarea en un nuevo hilo, permitiendo asi poder transmitir archivos de
     * forma concurrente.
     * <br>
     * Para descargar el archivo, seguimos las instrucciones indicadadas en la {@link SyncApp interfaz del servicio}, en
     * el apartado de carga/descarga de archivos.
     */
    public void run() {

        // Obtenemos el identificador del archivo
        try {
            fileId = server.abrirArchivo(usuario, ruta, "rw");
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        System.out.println("subiendo file="+ fileId +" "+ruta);

        // Le inticamos al lector de archivo y al propio archivo el identificador remoto
        lectorArchivos.setFileId(fileId);
        ruta.remoteID = ""+fileId;


        // Troceamos el archivo en bloques, y enviamos bloques hasta que no quede ninguno por enviar
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






    }
}
