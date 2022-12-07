package com.syncapp.interfaces;

import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.syncapp.cliente.SyncAppCliente;
import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.server.SyncAppServer;


/**
 * Servicio SyncApp.
 * <br>
 * <p>
 *     Esta clase define el servicio distribuido SyncApp, sus funciones y su comportamiento.
 * </p>
 * <p>
 *     Para conocer en detalle como se implementan los diferentes metodos, a nivel codigo, revise {@link SyncAppServer}.
 * </p>
 * <br>
 * <p>
 *     El objetivo del servicio distribuido SyncApp es "sincronizar" una carpeta que indique un {@link com.syncapp.cliente.SyncAppCliente Cliente},
 *     con una carpeta dentro de un {@link SyncAppServer Servidor}. Para ello un {@link com.syncapp.cliente.SyncAppCliente Cliente} debe:
 *     <ol>
 *         <li>
 *             {@link #listaArchivos(TokenUsuario) Otener} su {@link ArrayList<Archivo> lista} de {@link Archivo Archivos} en su
 *             {@link #listaArchivos(TokenUsuario) carpeta dentro del servidor} y su {@link com.syncapp.utility.Utilidades#listFiles(Path) carpeta a sincronizar}.
 *         </li>
 *         <li>
 *             Comprobar que archivos no existen en la maquina remota o local
 *         </li>
 *         <li>
 *             Para esos archivos, cargalors/descargarlos sin mas comprobaciones
 *         </li>
 *         <li>
 *             Para aquellos archivos que estan presentes en ambas maquinas, obtener el hash local y hash remoto, y si
 *             los dos hashes son diferentes, el archivo ha cambiado. Para saber cual cargar/descargar, necesitamos saber
 *             cual es el mas receiente, haciendo uso de {@link Archivo#timeMilisLastModified}.
 *             <br>
 *             Para el archivo mas reciente (tiene mayor milis) lo cargamos/descargamos (dependiendo de donde proceda ese archivo).
 *         </li>
 *     </ol>
 * </p>
 * <br>
 * <p>
 *     Una vez que el cliente sepa que archivos quiere cargar/descargar, necesita ejecutar las siguientes tareas para poder
 *     subirlo/bajarlo:
 *     <ol>
 *         <li>
 *             {@link #abrirArchivo(TokenUsuario, Archivo, String) Preparar} devuelve el identificador unico del archivo
 *             y, si la operacion indicada es "rw", se prepara el archivo para la escritura. Esto es importante, pues
 *             multiples sesiones pueden leer el mismo archivo simultaneamente, pero unicamente uno puede escribir en el.
 *             <br>
 *             Si se quiere abrir el archivo en modo "rw", pero esta siendo usado, se devuelve -1.
 *         <li>
 *             {@link #leerBloqueBytes Obtener}/{@link #escribirBloqueBytes(int, BloqueBytes) escribir} el siguiente {@link BloqueBytes}
 *             correspondiente hasta que no queden bloques por obtener/escribir. Cuando obtenemos bloques, si no quedan bloques por leer, se devuelve -1.
 *         </li>
 *         <li>
 *             {@link #cerrarArchivo(int, TokenUsuario) Cerrar} el archivo, liberando recursos, y permitiendo que otros usuarios puedan acceder al arachivo.
 *         </li>
 *     </ol>
 * </p>
 * <br>
 * <p>
 *     Para que un cliente pueda acceder al servicio tiene que seguir las siguientes instrucciones:
 *     <ol>
 *         <li>
 *             Obtener este propio servicio mediante {@link Naming#lookup(String) Naming.lookup("rmi://<i>ip_rmiregistry:puerto_rmi</i>/SyncApp")}.
 *         </li>
 *         <li>
 *             Sincronizarse temporalmente con el servidor {@link SyncAppServer#obtenerHora()}.
 *         </li>
 *         <li>
 *             Iniciar sesion, indicando su token de usuario {@link SyncAppServer#iniciarSesion(TokenUsuario)}.
 *         </li>
 *         <li>
 *             Ya esta listo para ejecutar alguna de las funciones ofrecidas.
 *         </li>
 *     </ol>
 * </p>
 * <p>
 *     Para conocer en detalle como se implementan los diferentes metodos, revise {@link SyncAppServer}.
 * </p>
 */


public interface SyncApp extends Remote {





    // Metodos iniciadores

    /**
     * Permite iniciar sesion a un usuario. Esto permite que pueda acceder/escribir archivos en su carpeta contenedora. <br>
     * Dado que varios usuarios pueden acceder al servicio de forma simultanea, cada usuario tendra un {@link Integer id de sesion},
     * con el que podra acceder al resto de servicios.
     * @param usuario {@link TokenUsuario} que quiere iniciar sesion.
     * @return id {@link String id de sesion} asignado al usuario. Si el usuario ofrecido al metodo es nulo o no contiene nombre, se devuelve null, indicando
     * asi que hay fallos.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    String iniciarSesion(TokenUsuario usuario) throws RemoteException;




    /**
     * Este metodo permite inicializar la lectura/escritura de un archivo, reservando los recursos para el mismo.
     * <br>
     * El metodo comprueba las siguientes situaciones
     * <ul>
     *     <li>
     *         Alguno de los parametros dados no sea nulo.
     *     </li>
     *     <li>
     *         La operacion indicada sea "r" o "rw".
     *     </li>
     *     <li>
     *         El usuario que haya invocado el metodo tenga una sesion activa.
     *     </li>
     *     <li>
     *         El archivo este en el registro de archivos abiertos.
     *     </li>
     * </ul>
     * <br>
     * En el caso que no se cumpla alguna de estas condiciones, se devuelve -1;
     *
     * @param usuario {@link TokenUsuario Usuario} que quiere abrir el archivo.
     * @param archivo {@link Archivo} que se quiere abrir.
     * @param op_mode {@link String} que indica en que modo se quiere abrir el archivo -> "r" para leer, "rw" para escribir.
     * @return {@link Integer} identificador unico (temporal) del archivo en cuestion. Necesario para posteriormente transferir datos sobre este archivo.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    int abrirArchivo(TokenUsuario usuario, Archivo archivo, String op_mode) throws RemoteException;






















    // Metodos finalizadores

    /**
     * Permite cerrar sesion a un usuario.
     * @param usuario {@link TokenUsuario} que quiere cerrar sesion.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    void cerrarSesion(TokenUsuario usuario) throws RemoteException;



    /**
     * Este metodo permite cerrar la lectura/escritura de un archivo, liberando asi el recurso para otros usuarios.
     * @param id_file identificador unico del archivo.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    void cerrarArchivo(int id_file, TokenUsuario usuario) throws RemoteException;

























    // Metodos para obtener informacion

    /**
     * Devuelve un {@link ArrayList} de {@link Archivo}, que representa la lista de archivos que un {@link TokenUsuario usuario}
     * contiene en su carpeta contenedora dentro del servidor.
     * <br>
     * Esta lista de archivos no contiene metadatos sobre los archivos (hash, hora ultima modificacion), para ello se debe
     * ejecutar {@link #obtenerMetadatos(TokenUsuario, ArrayList)}.
     *
     * @param usuario {@link TokenUsuario Usuario} cuya lista se quiere obtener. Previamente debe haber iniciado sesion.
     * @return {@link ArrayList} de {@link Archivo} sin metadatos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    ArrayList<Archivo> listaArchivos(TokenUsuario usuario) throws RemoteException;

    /**
     * Devuelve la lista de archivos indicada, pero cada archivo contiene los metadatos.
     * @param usuario {@link TokenUsuario Usuario} que pide la informacion.
     * @param lista {@link ArrayList} de {@link Archivo} de la que se quiere obtener los metadatos.
     * @return {@link ArrayList} de {@link Archivo} con metadatos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    ArrayList<Archivo> obtenerMetadatos(TokenUsuario usuario, ArrayList<Archivo> lista) throws RemoteException;


    /**
     * Devuelve el hash de un archivo disponible en el servidor.
     * @param acalcular {@link Archivo} al cual calcular el hash md5.
     * @return {@link String hasmd5} del archivo especificado.
     * @throws RemoteException si ocurre un problema durante la ejecucion el metodo.
     */
    String calcularHash(Archivo acalcular, TokenUsuario usuario) throws RemoteException;























    // Metodos para transferir datos

    /**
     * Permite leer un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se debe indicar la posicion
     * (offset de bytes) a partir de la cual leer el bloque.
     * <br>
     * Para ello, el que invoque el metodo debera tener un control sobre estas posiciones.
     * <br>
     * Los bloques se leen a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta lectura con offset, etc.
     *
     * @param id_file identificador unico del archivo.
     * @param position posicion a partir de la cual se lee el archivo.
     * @return {@link BloqueBytes} a transimitir.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    BloqueBytes leerBloqueBytes(int id_file, long position) throws RemoteException;

    /**
     * Permite escribir un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se debe indicar la posicion
     * (offset de bytes) a partir de la cual escribir el bloque.
     * <br>
     * El bloque a escribir, debe contener la posicion en la que se va a escribir el bloque ({@link BloqueBytes#position}).
     * <br>
     * Los bloques se escriben a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta escritura con offset, etc.
     * @param id_file identificador unico del archivo.
     * @param bloq_bytes {@link BloqueBytes} a escribir.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    void escribirBloqueBytes(int id_file, BloqueBytes bloq_bytes) throws RemoteException;




















    // Metodos para el funcionamiento del servicio

    /**
     * Permite al cliente saber cual es la hora actual (en {@link Long milisegundos}) del servidor, para poder sincronizarse
     * y saber la fecha relativa de un archivo.
     *
     * @return {@link Long ms} de la hora actual del servidor.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    long obtenerHora() throws RemoteException;

    /**
     * Permite al usuario realizar un ping para saber si el servidor esta funcionando. Ademas, se le puede dar un uso especifico,
     * que es para obtener el tiempo minimo de transimision entre el cliente y el servidor.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    void ping() throws RemoteException;


}
