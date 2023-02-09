package com.syncapp.server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.LectorArchivos;
import com.syncapp.utility.Utilidades;


/**
 * Implementacion de SyncApp.
 * <br>
 * <p>
 *     Esta clase implementa el servicio distribuido SyncApp, sus funciones y su comportamiento.
 *     <br>
 *     Se trata de un objeto remoto ({@link UnicastRemoteObject}), que se registra en el registro rmi, y los
 *     clientes pueden invocarlo remotamente para acceder a sus funciones.
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
 *             Comprobar que archivos no existen en la maquina remota o local.
 *         </li>
 *         <li>
 *             Para esos archivos, cargarlos/descargarlos sin mas comprobaciones.
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
 *         <li>
 *             {@link #leerBloqueBytes Obtener}/{@link #escribirBloqueBytes(int, BloqueBytes) escribir} el siguiente {@link BloqueBytes}
 *             correspondiente hasta que no queden bloques por obtener/escribir. Cuando obtenemos bloques, si no quedan bloques por leer, se devuelve null.
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
 */


public class SyncAppServer extends UnicastRemoteObject implements SyncApp{

    /**
     * Representa la carpeta (Path) donde se almacenaran las carpetas de los usuarios. En el pc en el que ejecute, esta
     * carpeta sera {@code "/home/<usuario>/sync_app/cloud_containers"} en linux, o {@code "C:\Users\<usuario>\sync_app\cloud_containers"}.
     */
    private static final Path usersContainers = Paths.get(System.getProperty("user.home")).resolve("sync_app").resolve("cloud_containers");


    /**
     * Nos permite almacenar contenido a un log, por ejemplo inicio de sesion, archivos cargados, errores, etc.
     */
    Logger logger;

    /**
     * Este objeto nos permite manejar que archivos estan siendo abierto (en escritura), y manjera los recursos sobre
     * estos archivos (sus {@link LectorArchivos}).
     */
    FileHandler fileHandler;

    /**
     * Este objeto nos permite tener un control sobre las sesiones de los usuarios, facilitando la tarea de iniciar sesiones,
     * cerrar sesiones etc.
     * <br>
     * Ademas mantiene un control sobre los archivos abiertos por una sesion (abiertos en modo escritura).
     */
    UserHandler userHandler;
    







    // Constructores

    /**
     * Inicializa los registros necesarios para el servidor y el identificador global de archivos.
     * @throws RemoteException si ocurre algun problema en la ejecucion del constructor.
     */
    public SyncAppServer() throws RemoteException {
        super();

        // Inicializamos el registro de archivos
        fileHandler = new FileHandler(usersContainers);

        // Inicializamos el registro de usuarios
        userHandler = new UserHandler();

        // Inicializamos el Logger
        logger = Logger.getLogger("Logger");


    }












    // Metodos iniciadores

    /**
     * Permite iniciar sesion a un usuario. Esto permite que pueda acceder/escribir archivos en su carpeta contenedora. <br>
     * Dado que varios usuarios pueden acceder al servicio de forma simultanea, cada usuario tendra un {@link Integer id de sesion},
     * con el que podra acceder al resto de servicios.
     * @param usuario {@link TokenUsuario} que quiere iniciar sesion.
     * @return id {@link String id de sesion} asignado al usuario. Si el usuario ofrecido al metodo es nulo o no contiene nombre, se devuelve null, indicando
     * asi que hay fallos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public String iniciarSesion(TokenUsuario usuario) throws RemoteException {
        if(usuario == null) return null;
        if(usuario.name.length() <1) return null;

        String sessionId = userHandler.iniciarSesion(usuario);
        usuario.session_id = sessionId;

        logger.info("usuario "+usuario+" ha iniciado sesion");
        return sessionId;

        
    }


    /**
     * Este metodo permite obtener el {@link Integer identificador unico} del archivo. Si el archivo se quiere
     * abrir en modo escritura, se comprueba que ningun usuario lo tenga abierto para escritura.
     * <br>
     * {@link SyncApp#abrirArchivo(TokenUsuario, Archivo, String)} para mas informacion.
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
    @Override
    public int abrirArchivo(TokenUsuario usuario, Archivo archivo, String op_mode) throws RemoteException { // op_mode is read or write
        if(usuario == null || archivo == null || !(op_mode.equals("r") || op_mode.equals("rw"))){
            return -1;
        }

        // Comprobamos que el usuario tenga una sesion activa
        if(!userHandler.isSessionActive(usuario.session_id)) { return -1; }

        // Obtenemos el path absoluto dentro del archivo, dentro del servidor
        Path filepath = usersContainers.resolve(usuario.name).resolve(archivo.childFolder);



        // Loggeamos la apertura del archivo
        logger.info("abriendo archivo " + archivo + " modo " + op_mode);

        // Devolvemos el fileId. FileHandler comprobara si el archivo se puede abrir o no, devolviendo -1 en caso engativo.
        return fileHandler.openFile(filepath.toString(), op_mode);

    }



















    // Metodos finalizadores

    /**
     * Permite cerrar sesion a un usuario. <br>
     * Se cierra la sesion de usuario asociada al usuario, eliminando todos los registros asociados a este, liberando
     * asi recursos de memoria.
     * @param usuario {@link TokenUsuario} que quiere cerrar sesion.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void cerrarSesion(TokenUsuario usuario) throws RemoteException {
        if(usuario == null) return;

        ArrayList<Integer> archivosACerrar = userHandler.listFilesForSession(usuario.session_id);
        if(archivosACerrar != null) {
            archivosACerrar.forEach(fileHandler::closeFile);
        }

        userHandler.cerrarSesion(usuario.session_id);

        logger.info("usuario "+usuario+" ha cerrado sesion");
    }


    /**
     * Este metodo permite cerrar la lectura/escritura de un archivo, liberando asi el recurso para otros usuarios.
     * @param fileId identificador unico del archivo.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void cerrarArchivo(int fileId, TokenUsuario usuario) throws RemoteException {
        if(fileId <0 ) return;
        Archivo enCuestion = new Archivo( fileHandler.getFilePath(fileId).toString() );


        // Mostramos informacion
        logger.info("cerrando archivo "+enCuestion);

        userHandler.cerrarArchivo(fileId);
        fileHandler.closeFile(fileId);

    }
























    // Metodos para obtener datos

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
    @Override
    public ArrayList<Archivo> listaArchivos(TokenUsuario usuario) throws RemoteException {

        // Comprobamos que el usuario tenga una sesion activa
        if(!userHandler.isSessionActive(usuario.session_id)) return null;

        // Obtenemos la carpeta contenedora del usuario
        Path toWalk = Paths.get(usersContainers.toString() , usuario.name);

        // Si no existe dicha carpeta devolvemos null (primera vez que inicia sesion)
        if(!toWalk.toFile().exists()) return null;

        // Obtenemos la lista de archivos hay en la carpeta contenedora del usuario
        ArrayList<Archivo> listaRet = Utilidades.listFiles( toWalk );

        // Mostramos informacion
        logger.info("listando archivos para "+usuario+" total de elementos="+listaRet.size());

        return listaRet;
    }


    /**
     * Devuelve la lista de archivos indicada, pero cada archivo contiene los metadatos.
     * @deprecated - el sistema ha cambiado, pero se puede seguir usando.
     * @param usuario {@link TokenUsuario Usuario} que pide la informacion.
     * @param lista {@link ArrayList} de {@link Archivo} de la que se quiere obtener los metadatos.
     * @return {@link ArrayList} de {@link Archivo} con metadatos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public ArrayList<Archivo> obtenerMetadatos(TokenUsuario usuario, ArrayList<Archivo> lista)
            throws RemoteException {

        if(!userHandler.isSessionActive(usuario.session_id) || lista == null || lista.size() == 0){
            return null;
        }

        logger.info("obteniendo informacion de archivos de "+usuario+" con "+lista.size()+" elementos");

        return Utilidades.obtenerMultiplesMetadatos(lista, Paths.get(usersContainers.toString() , usuario.name));
        
    }


    /**
     * Devuelve el hash de un archivo disponible en el servidor.
     * @param acalcular {@link Archivo} al cual calcular el hash md5.
     * @return {@link String hasmd5} del archivo especificado.
     * @throws RemoteException si ocurre un problema durante la ejecucion el metodo.
     */
    @Override
    public String calcularHash(Archivo acalcular, TokenUsuario usuario) throws RemoteException {
        acalcular.parentFolder = usersContainers.resolve(usuario.name).toString();
        return Utilidades.checkSumhash(acalcular.toFile());
    }
























    // Metodos para transferrir datos

    /**
     * Permite leer un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se debe indicar la posicion
     * (offset de bytes) a partir de la cual leer el bloque.
     * <br>
     * Los bloques se leen a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta lectura con offset, etc.
     * <br>
     * Para cada peticion, se abre y se cierra un nuevo {@link LectorArchivos} nuevo, para leer el bloque
     * de bytes.
     *
     * @param fileId identificador unico del archivo.
     * @param posicion posicion a partir de la cual se lee el archivo:
     * @return {@link BloqueBytes} a transimitir.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public BloqueBytes leerBloqueBytes(int fileId, long posicion) throws RemoteException {


        LectorArchivos archivoActual = null;
        BloqueBytes bb = null;

        try {
            // Creamos un nuevo Lector para poder leer los bytes
            archivoActual = new LectorArchivos(fileHandler.getFilePath(fileId), "r" , fileId);

        } catch (IOException e) {
            logger.severe("error al leer el bloque "+" position del archivo "+fileId);
            e.printStackTrace();
            throw new RemoteException("Error al leer un bloque de bytes");
        }


        try {
            // Obtenemos el bloque de bytes
            bb = archivoActual.leerBloqueBytes(posicion);
            logger.info("leyendo bloque "+bb+" del archivo "+fileId);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            // Intentamos cerrar ese Lector de archivos
            archivoActual.cerrarArchivo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Devolvemos el bloque leido
        return bb;
    }


    /**
     * Permite escribir un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se debe indicar la posicion
     * (offset de bytes) a partir de la cual escribir el bloque.
     * <br>
     * Los bloques se escriben a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta escritura con offset, etc.
     *
     * @param fileId identificador unico del archivo.
     * @param bloq_bytes {@link BloqueBytes} a transimitir.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void escribirBloqueBytes(int fileId, BloqueBytes bloq_bytes) throws RemoteException {
        if(bloq_bytes == null || bloq_bytes.size < 1 || fileId < 0) return;

        // Obtenemos el lector de archivos que se creo al abrir el archivo (debido a que es escritura)
        LectorArchivos actual = fileHandler.getWriter(fileId);
        try {
            // Escribimos el bloque de bytes en el archivo
            logger.info("escribiendo el bloque "+bloq_bytes);
            actual.escribirBloqueBytes(bloq_bytes);

        } catch(IOException ioe) {
            logger.severe("error al escribir el bloque "+bloq_bytes);
            ioe.printStackTrace();
            throw new RemoteException("Error al escribir el bloque de bytes en servidor");
        }

    }






















    // Metodos para el funcionamiento del servicio

    /**
     * Permite al cliente saber cual es la hora actual (en {@link Long milisegundos}) del servidor, para poder sincronizarse
     * y saber la fecha relativa de un archivo.
     *
     * @return {@link Long ms} de la hora actual del servidor.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public long obtenerHora() throws RemoteException {
        
        // int rand = (int) (Math.random()*100);
        // simularRetardo(rand);

        long hora = System.currentTimeMillis();

        // simularRetardo(rand);
        
        return hora;
    }


    /**
     * Permite al usuario realizar un ping para saber si el servidor esta funcionando. Ademas, se le puede dar un uso especifico,
     * que es para obtener el tiempo minimo de transimision entre el cliente y el servidor.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void ping() throws RemoteException {
        try {
            logger.info("ping de "+getClientHost());
        } catch (ServerNotActiveException e) {
            throw new RuntimeException(e);
        }
    }


    

    














































    






    






    






   






    


}
