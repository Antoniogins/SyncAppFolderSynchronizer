package com.syncapp.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.syncapp.cliente.SyncAppCliente;
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
 *             {@link com.syncapp.utility.Utilidades#operacionesIniciales(ArrayList, ArrayList, Path) Comprobar} que
 *             archivos faltan en el servidor o en su pc. Esto devolvera una {@link ArrayList<Archivo> lista} de {@link Archivo Archivos}
 *             con aquellos archivos que necesitan mas informacion ({@link String hash}, {@link Long ultima hora modificacion}) para determinar si cargalos o descargarlos.
 *         </li>
 *         <li>
 *             {@link SyncAppCliente#primeraIteracion() Cargar} aquellos archivos que seguro sabemos que necesitan cargarse/descargarse.
 *         </li>
 *         <li>
 *             Para aquellos archivos que necesitan mas informacion, {@link #obtenerParametrosSimultaneos(TokenUsuario, ArrayList) obtenerla}.
 *         </li>
 *         <li>
 *             Cargar/descargar aquellos archivos que se consideren necesarios.
 *         </li>
 *     </ol>
 * </p>
 * <br>
 * <p>
 *     Una vez que el cliente sepa que archivos quiere cargar/descargar, necesita ejecutar las siguientes tareas para poder
 *     subirlo/bajarlo:
 *     <ol>
 *         <li>
 *             {@link #abrirArchivo(TokenUsuario, Archivo, String) Preparar} el archivo en el servidor para ser leido/escrito . Esto devuelve
 *             un {@link Integer identificador unico} para este archivo, con el que posteriormente podra obtener un {@link BloqueBytes} del archivo.
 *             Si el archivo ha sido abierto por otro usuario, el metodo devuelve -1.
 *         </li>
 *         <li>
 *             {@link #leerBloqueBytes Obtener}/{@link #escribirBloqueBytes(int, BloqueBytes, int) escribir} el siguiente {@link BloqueBytes}
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
 *             Iniciar sesion, indicando su token de usuario {@link SyncAppServer#iniciarUsuario(TokenUsuario)}.
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
     * Representa una lista de usuaios activos, que
     */
    ArrayList<String> usuariosActivos;

    /**
     * Registro para almacenar las sesiones de usuario activas. <br>
     * Se almacena la pareja {@link Integer identificador unico de sesion}/{@link TokenUsuario usuario que realiza la sesion}
     */
    HashMap< Integer, TokenUsuario> sesionesUsuarioActivas;

    /**
     * Registro para almacenar los identificadores de sesiones que ya han sido usados (int).
     */
    ArrayList<Integer> memoriaDeSesiones;

    /**
     * Registro para almacenar que archivos estan siendo leidos/escritos.
     * <br>
     * Se almacena la pareja {@link Integer identificador del archivo}/{@link Path ruta del archivo}
     */
    HashMap< Integer , Path> archivosActivos;

    /**
     * Registro para llevar un control sobre la posicion de lectura/escritura de un archivo.
     * <br>
     * Se almacena la pareja {@link Integer identificador de archivo}/{@link Long posicion del primer byte} que se necesita leer/escribir
     */
    HashMap< Integer, Long> posicionByteActual;

    /**
     * Registro para llevar un control sobre cual es el ultimo bloque de datos que ha sido enviado.
     * <br>
     * Se almacena la pareja {@link Integer identificador de archivo}/{@link Integer identificador del ultimo bloque enviado}
     */
    HashMap< Integer, Integer> ultimoBloqueEnviado;

    /**
     * Registro para almacenar la referencia al lector de archivos de un archivo dado.
     * <br>
     * Se almacena la pareja {@link Integer identificador de archivo}/{@link LectorArchivos}
     */
    HashMap< Integer, LectorArchivos> lectoresArchivo;


    /**
     * Registro de archivos activos para una sesion de usuario. Esto es conviniente cuando existen varias sesiones de un
     * mismo usuario. <br>
     * Se almacena la pareja {@link Integer id_sesion}/{@link ArrayList}<{@link Path}>
     */
    HashMap< Integer, ArrayList<String> > archivosActivosEnSesion;

    /**
     * Identificador global de archivos, que sirve para poner un id a cada uno de los archivos, de forma ordenada.
     */
    static int globalFileID;

    /**
     * Nos permite almacenar contenido a un log, por ejemplo inicio de sesion, archivos cargados, archivos descargados, etc.
     */
    Logger logger;
    







    // Constructores

    /**
     * Inicializa los registros necesarios para el servidor y el identificador global de archivos.
     * @throws RemoteException si ocurre algun problema en la ejecucion del constructor.
     */
    public SyncAppServer() throws RemoteException {
        super();

        // Inicializamos los registros de usuarios
        usuariosActivos = new ArrayList<>();
        sesionesUsuarioActivas = new HashMap<>();
        memoriaDeSesiones = new ArrayList<>();


        // Inicializamos los registros de archivos
        archivosActivos = new HashMap<>();
        globalFileID = 0;
        archivosActivosEnSesion = new HashMap<>();


        // Inicializamos el registro de lectores de archivos
        lectoresArchivo = new HashMap<>();
        ultimoBloqueEnviado = new HashMap<>();
        posicionByteActual = new HashMap<>();


    }












    // Metodos iniciadores

    /**
     * Permite iniciar sesion a un usuario. Esto permite que pueda acceder/escribir archivos en su carpeta contenedora. <br>
     * Dado que varios usuarios pueden acceder al servicio de forma simultanea, cada usuario tendra un {@link Integer id de sesion},
     * con el que podra acceder al resto de servicios.
     * @param usuario {@link TokenUsuario} que quiere iniciar sesion.
     * @return id {@link Integer id de sesion} asignado al usuario. Si el usuario ofrecido al metodo es nulo o no contiene nombre, se devuelve -1, indicando
     * asi que hay fallos.
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public int iniciarUsuario(TokenUsuario usuario) throws RemoteException {
        if(usuario == null) return -1;
        if(usuario.name.length() <1) return -1;

        // Asignamos un identificador unico al usuario
        // Para ello, creamos un numero aleatorio, y reintentamos hasta que el numero obtenido no haya sido
        // anteriormente seleccionado a un usuario (memoriaDeSesiones)
        int id = 0;
        do {
            id = (int) (Math.random()*Integer.MAX_VALUE);
        } while (memoriaDeSesiones.contains(id));

        // Añadimos el usuario al registro de sesiones activas, y al registro de identificadores de usuario ya usados
        memoriaDeSesiones.add(id);
        sesionesUsuarioActivas.put(id, usuario);
        archivosActivosEnSesion.put(id, new ArrayList<>() );




        System.out.println("Usuario: "+usuario.name+" ha iniciado sesion");

        //DEPRECATED
        usuariosActivos.add(usuario.name);

        return id;
        
    }


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
    @Override
    public int abrirArchivo(TokenUsuario usuario, Archivo archivo, String op_mode) throws RemoteException { // op_mode is read or write
        if(usuario == null || archivo == null || !(op_mode.equals("r") || op_mode.equals("rw"))){ return -1; }
        if(!sesionesUsuarioActivas.containsKey(usuario.session_id)) { return -1; }


        // Obtenemos el path absoluto dentro del archivo, dentro del servidor
        Path filepath = usersContainers.resolve(usuario.name).resolve(archivo.ruta);

        // Comprobamos si este archivo ya estaba en el registro de archivos activos
        if(archivosActivos.containsKey(filepath)) {
            return -1; // Si el archivo ya esta abierto devolvemos -1, ya que no se puede acceder al mismo archivo de forma simultanea
        }

        // Asignamos un identificador unico al archivo, y lo almacenamos en el registro de archivos activos
        int file_id = globalFileID++;
        archivosActivos.put(file_id, filepath);

        // Indicamos que el ultimo bloque enviado es -1, para el correcto funcionamiento del transmisor
        ultimoBloqueEnviado.put(file_id, -1);

        // Intentamos crear el lector de archivos y añadirlo al mapa de lector de archivo para el archivo dado
        try {
            lectoresArchivo.put(file_id, new LectorArchivos(filepath, op_mode, file_id));
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new RemoteException("Error al abrir archivo en servidor");
        }

        // Añadimos este archivo a la lista de archivos activos para una sesion
        archivosActivosEnSesion.get(usuario.session_id).add(""+file_id);



        System.out.println("abriendo file="+file_id+" ruta="+ archivo.toString()+"  mode="+op_mode);
        return file_id;
    }



















    // Metodos finalizadores

    /**
     * Permite cerrar sesion a un usuario.
     * @param usuario {@link TokenUsuario} que quiere cerrar sesion.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void cerrarUsuario(TokenUsuario usuario) throws RemoteException {
        if(usuario == null) return;

        // Eliminamos el usuario de la lista de usuarios activos
        usuariosActivos.remove(usuario.name);

        // Eliminamos el usuario de los registros de sesion, y eliminamos su id de sesion para que quede libre para otro usuario
        sesionesUsuarioActivas.remove(usuario.session_id);
        memoriaDeSesiones.remove(usuario.session_id);

        // Comprobamos si hay archivos en la lista de archivos activos de un usuario.
        if(archivosActivosEnSesion.containsKey(usuario.session_id)) {
            archivosActivosEnSesion.get(usuario.session_id).forEach(c -> {
                archivosActivosEnSesion.get(usuario.session_id).remove(c);
            });
            archivosActivosEnSesion.remove(usuario.session_id);
        }

        System.out.println("Usuario: "+ usuario.name +" ha cerrado sesion");
    }


    /**
     * Este metodo permite cerrar la lectura/escritura de un archivo, liberando asi el recurso para otros usuarios.
     * @param id_file identificador unico del archivo.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void cerrarArchivo(int id_file, TokenUsuario usuario) throws RemoteException {
        if(id_file <0 ) return;

        // Removemos el archivo de la lista de archivo para la sesion x
        archivosActivosEnSesion.get(usuario.session_id).remove(""+id_file);



        // Removemos el lector de archivos y la entrada del mismo, para el archivo dado
        try {
            lectoresArchivo.get(id_file).cerrarArchivo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lectoresArchivo.remove(id_file);

        // Removemos el archivo dado de la lista de archivos activos
        archivosActivos.remove(id_file);



        System.out.println("cerrando archivo "+id_file);
    }
























    // Metodos para obtener informacion

    /**
     * Devuelve un {@link ArrayList} de {@link Archivo}, que representa la lista de archivos que un {@link TokenUsuario usuario}
     * contiene en su carpeta contenedora dentro del servidor.
     * <br>
     * Esta lista de archivos no contiene metadatos sobre los archivos (hash, hora ultima modificacion), para ello se debe
     * ejecutar {@link #obtenerParametrosSimultaneos(TokenUsuario, ArrayList)}.
     *
     * @param usuario {@link TokenUsuario Usuario} cuya lista se quiere obtener. Previamente debe haber iniciado sesion.
     * @return {@link ArrayList} de {@link Archivo} sin metadatos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public ArrayList<Archivo> listaArchivos(TokenUsuario usuario) throws RemoteException {
        if(!sesionesUsuarioActivas.containsKey(usuario.session_id)) return null;
        Path toWalk = Paths.get(usersContainers.toString() , usuario.name);

        if(!toWalk.toFile().exists()) return null;

        ArrayList<Archivo> listaRet = Utilidades.listFiles( toWalk );
        System.out.println("listando archivos de <"+ usuario.name +">"+" cantidad de elementos: "+listaRet.size());
        System.out.println("listando archivos de <"+ usuario.name +">"+" -> "+Paths.get(usersContainers.toString() , usuario.name)+" cantidad de elementos: "+listaRet.size());
        return listaRet;
    }


    /**
     * Devuelve la lista de archivos indicada, pero cada archivo contiene los metadatos.
     * @param usuario {@link TokenUsuario Usuario} que pide la informacion.
     * @param lista {@link ArrayList} de {@link Archivo} de la que se quiere obtener los metadatos.
     * @return {@link ArrayList} de {@link Archivo} con metadatos.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public ArrayList<Archivo> obtenerParametrosSimultaneos(TokenUsuario usuario, ArrayList<Archivo> lista)
            throws RemoteException {

        if(!sesionesUsuarioActivas.containsKey(usuario.session_id) || lista == null || lista.size() == 0){
            return null;
        }

        System.out.println(Paths.get(usersContainers.toString() , usuario.name));
        System.out.println("obteniendo parametros de archivos pedidos");
        return Utilidades.obtenerParametrosSimultaneos(lista, Paths.get(usersContainers.toString() , usuario.name));
        
    }



















    // Metodos para transferrir datos

    /**
     * Permite leer un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se permite indicar la posicion
     * (offset de bytes) a partir de la cual leer el bloque.
     * <br>
     * Para ello, cuando se indica posicion -1 se lee el siguiente bloque a partir de la posicion que le corresponde
     * , y cuando posicion >=0 se lee el archivo a partir de esa posicion.
     * <br><br>
     * Los bloques se leen a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta lectura con offset, etc.
     *
     * @param id_file identificador unico del archivo.
     * @param position posicion a partir de la cual se lee el archivo:
     *                 <ul>
     *                 <li>
     *                 position = -1  se lee de forma normal.
     *                 </li>
     *                 <li>
     *                 position >=0  se lee a partir de la posicion indicada.
     *                 </li>
     *                 </ul>
     * @return {@link BloqueBytes} a transimitir.
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public BloqueBytes leerBloqueBytes(int id_file, long position) throws RemoteException {
        LectorArchivos archivoActual = lectoresArchivo.get(id_file);

        try {
            System.out.println("leyendo file="+id_file+" pos="+position);
            return archivoActual.leerBloqueBytesEnPosicion(position);

        } catch (IOException e) {
            System.out.println("aqui falla?");
            e.printStackTrace();
            throw new RemoteException("Error al leer un bloque de bytes");
        }
        
    }


    /**
     * Permite escribir un {@link BloqueBytes} para un archivo especificado con su identificador unico.
     * Dado que pueden ocurrir errores en la transimision de un bloque, se permite indicar la posicion
     * (offset de bytes) a partir de la cual escribir el bloque.
     * <br>
     * Para ello, cuando se indica posicion -1 se escribe el siguiente bloque a partir de la posicion que le corresponde
     * , y cuando posicion >=0 se escribe el archivo a partir de esa posicion.
     * <br><br>
     * Los bloques se escriben a traves de un {@link com.syncapp.utility.LectorArchivos}, que nos permite
     * realizar esta escritura con offset, etc.
     *
     * @param id_file identificador unico del archivo.
     * @param bloq_bytes {@link BloqueBytes} a transimitir.
     * @param pos posicion a partir de la cual se escribe el archivo:
     *                   <ul>
     *                   <li>
     *                   position = -1  se escribe de forma normal.
     *                   </li>
     *                   <li>
     *                   position >=0  se escribe a partir de la posicion indicada.
     *                   </li>
     *                   </ul>
     *
     * @throws RemoteException si ocurre un problema durante la ejecucion del metodo.
     */
    @Override
    public void escribirBloqueBytes(int id_file, BloqueBytes bloq_bytes, int pos) throws RemoteException {
        if(bloq_bytes == null || bloq_bytes.size < 1 || id_file < 0) return;

        LectorArchivos actual = lectoresArchivo.get(id_file);
        try {
            System.out.println("escribiendo "+bloq_bytes.toString());
            actual.escribirBloqueBytesEnPosicion(bloq_bytes, pos);
        } catch(IOException ioe) {
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
        
        // ESTO ES PARA PRUEBAS, simulamos el tiempo que tarda en viajar un byte tanto
        // tiempo que tarda en llegar a servidor como tiempo que tarda en llegar a
        // cliente que seria 2*tmin

        // Como al llegar la peticion no se realiza ninguna accion, 2*tmin = RTT
        
        // int rand = (int) (Math.random()*10);
        // simularRetardo(rand);
        // simularRetardo(rand);
        return ;
    }


    

    







































    public static void main(String[] args) throws RemoteException, MalformedURLException {
        SyncAppServer sap = new SyncAppServer();
        Naming.rebind("rmi://localhost:1099/SyncApp", sap);
        System.out.println("ready to operate");
        
    }






    






    






    






   






    


}
