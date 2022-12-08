package com.syncapp.cliente;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.Archivo;
import com.syncapp.model.LocalRemote;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.Utilidades;
import com.syncapp.utility.VariablesGlobales;

/**
 * Esta clase reune los metodos necesarios para acceder al servicio SyncApp.
 */
public class SyncAppCliente {

    //constantes para acceder a argumentos

    /**
     * Representa en que posicion se debe indicar el argumento ip para que se introduzca correctamente.
     */
    public static final int ARG_IP = 0;

    /**
     * Representa en que posicion se debe indicar el argumento puerto para que se introduzca correctamente.
     */
    public static final int ARG_PUERTO = 1;

    /**
     * Representa en que posicion se debe indicar el argumento usuario para que se introduzca correctamente.
     */
    public static final int ARG_USUARIO = 2;

    /**
     * Representa en que posicion se debe indicar el argumento carpeta para que se introduzca correctamente.
     */
    public static final int ARG_CARPETA = 3;

    /**
     * Representa en que posicion se debe indicar el argumento hilos para que se introduzca correctamente.
     */
    public static final int ARG_HILOS = 4;

    //args para cliente:
        //  ip
        //  puerto
        //  usuario
        //  carpeta
        //  threads




    //Parametros del cliente

    /**
     * Direccion del registro RMI del que obtendremos el servidor.
     */
    private String serverIP;

    /**
     * Puerto del registro RMI.
     */
    private int puerto;

    /**
     * Token de usuario con el que iniciaremos sesion.
     */
    private TokenUsuario user;

    /**
     * Desfase del cliente respecto del servidor, para compensar a la hora de comparar las horas de modificacion de los archivos.
     * Este puede ser negativo debido al uso de diferentes zonas horarias.
     */
    private long timeOffset;

    /**
     * Error tipico al calcular el desfase con el servidor.
     */
    private long timeError;

    /**
     * Carpeta de la maquina cliente que se quiere sincronizar.
     */
    private Path workingPath;

    /**
     * Numero de hilos concurrentes para transmitir archivos.
     */
    private int hilos;




    //trabajadores

    /**
     * Servidor remoto con el que sincronizaremos la carpeta local. Dado que se trata de un objeto remoto, realmente
     * es un proxy (stub) del servidor remoto enmascarado bajo la interfaz SyncApp, ya que ambos realizan las mismas
     * tareas, pero el proxy utiliza una conexion tcp para que el objeto remoto las realice por el.
     */
    private SyncApp remoteServer;

    /**
     * Servicio para monitorizar los cambios en la carpeta de sincronizacion.
     */
    private ServicioMonitorizacion monitor;

    /**
     * Gestor de tareas, para encolar tareas de transmision y que se ejecuten automaticamente bajo disponibilidad.
     */
    private ExecutorService exec;




    //esto pasara al controlador

    /**
     * Gestor de las tareas fijas, como son el servicio de monitor y el servicio de consola.
     */
    private ExecutorService fixedSubRutines;
    // private InterfazCliente interfaz;




    //Constructores

    /**
     * Permite crear un cliente a partir de argumentos. Estos deben de tener un orden especifico, y deben estar indicados
     * todos obligatoriamente para un correcto funcionamiento. <br>
     * Se deben indicar en el siguiente orden:
     * <ul>
     *     <li>
     *         Direccion IP se almacena en la posicion {@link SyncAppCliente#ARG_IP}.
     *     </li>
     *     <li>
     *         Puerto se almacena en la posicion {@link SyncAppCliente#ARG_PUERTO}.
     *     </li>
     *     <li>
     *         Nombre de usuario se almacena en la posicion {@link SyncAppCliente#ARG_USUARIO}.
     *     </li>
     *     <li>
     *         Carpeta de sincronizacion se almacena en la posicion {@link SyncAppCliente#ARG_CARPETA}.
     *     </li>
     *     <li>
     *         Numero de hilos se almacena en la posicion {@link SyncAppCliente#ARG_HILOS}.
     *     </li>
     * </ul>
     * @param args argumentos indicadosn anteriormente. Obligatorios todos.
     */
    public SyncAppCliente(String[] args) {
        if(args == null || args.length != 5) return;

        // Creamos el gestor para las tareas fijas
        fixedSubRutines = Executors.newFixedThreadPool(2);


        // Establecemos todos los parametros
        System.out.println("estableciendo parametros");
        setServerIP(args[ARG_IP]);
        System.out.println("direccion ip="+serverIP);

        setPuerto(args[ARG_PUERTO]);
        System.out.println("puerto="+puerto);

        setUser(new TokenUsuario((args[ARG_USUARIO])));
        System.out.println("usuario="+user.name);

        setWorkingPath(args[ARG_CARPETA]);
        System.out.println("working path="+workingPath.toString());

        setHilos(args[ARG_HILOS]);
        System.out.println("hilos="+hilos);

        // Creamos el gestor para las tareas de carga/descarga
        exec = Executors.newFixedThreadPool(  Integer.parseInt(args[ARG_HILOS])  );



    }















    //GETTERS

    /**
     * Devuelve la referencia de servidor remoto (proxy) que se usa en este cliente.
     * @return {@link SyncApp proxy} del servidor.
     */
    public SyncApp getRemoteServer() { return remoteServer; }

    /**
     * Devuelve el token de usuario de este cliente.
     * @return {@link TokenUsuario}.
     */
    public TokenUsuario getUsuario() { return user; }

    /**
     * Devuelve la carpeta que se esta sincronizando en el cliente.
     * @return {@link Path directorio} de sincronizacion.
     */
    public Path getWorkingPath() { return workingPath; }

    /**
     * Devuelve el puerto del registro rmi con el que hemos establecido conexion.
     * @return puerto del registro rmi.
     */
    public int getPort() { return puerto; }

    /**
     * Devuelve la direccion ip del registro rmi.
     * @return direccion ip version 4 del registro rmi.
     */
    public String getIP() { return new String(serverIP); }

    /**
     * Devuelve el desfase que existe entre el cliente y el servidor. Puede ser negativo (debido a zonas horarias).
     * @return offset en milisegundos.
     */
    public long getTimeOffset() { return timeOffset; }

















    //SETTERS

    /**
     * Establece una nueva direccion ip del registro rmi. Debe comprobarse con anterioridad que se trata de una direccion
     * ip valida.
     * @param serverIP direccion ip en formato v4.
     */
    public void setServerIP(String serverIP) {
        if(serverIP == null || serverIP.length() < 7 || serverIP.length() > 15) return;
        this.serverIP = new String(serverIP);
    }

    /**
     * Establece un nuevo usuario para este cliente.
     * @param user que se quiere establecer.
     */
    public void setUser(TokenUsuario user) {
        if(user == null || user.name == null || user.name.equals("")) return;
        this.user = new TokenUsuario(user.name);
    }

    /**
     * Establece una nueva carpeta de sincronizacion. Si el path no es absoluto, se toma como carpeta de origen la
     * carpeta de usuario ({@code "C:\\Users\\usuario\\<ruta indicada>" o "/home/usuario/<ruta indicada>" })
     * @param workingPath que se quiere establecer.
     */
    public void setWorkingPath(Path workingPath) {
        if(workingPath == null) return;

        if(workingPath.isAbsolute()) {
            this.workingPath = workingPath;
        } else {
            this.workingPath = Paths.get( System.getProperty("user.home") ).resolve(workingPath);
        }

    }

    /**
     * Analogo a {@link #setWorkingPath(Path)} pero toma como entrada una ruta en cadena de texto. Se realiza la misma
     * comprobacion (se invoca al metodo mencionado).
     * @param path nuevo directorio de trabajo.
     */
    public void setWorkingPath(String path) {
        setWorkingPath(Paths.get(path));
    }

    /**
     * Establece el puerto del registro rmi al que queremos acceder.
     * @param puerto del registro rmi.
     */
    public void setPuerto(String puerto) {
        this.puerto = Short.parseShort(puerto);
    }

    /**
     * Establece el numero de hilos para las conexiones simultaneas.
     * @param source numero de hilos indicado en forma de texto.
     */
    public void setHilos(String source) {
        hilos = Integer.parseInt(source);
    }





















    //Servicios de iniciacion

    /**
     * Inicia una conexion con el registro rmi, y le pide obtener el servidor SyncApp. Establece la conexion con los
     * parametros introducidos al cliente.
     * @throws MalformedURLException si la direccion al registro rmi no esta correctamente formada.
     * @throws RemoteException si ocurre un problema remoto.
     * @throws NotBoundException si ocurre un problema al obtener el objeto remoto servidor.
     */
    public void iniciarServidor() throws MalformedURLException, RemoteException, NotBoundException {
        System.out.println("intentando conectar a rmi://"+serverIP+":"+puerto+"/SyncApp");
        remoteServer = (SyncApp) Naming.lookup("rmi://"+serverIP+":"+puerto+"/SyncApp");
    }

    /**
     * Inicia sesion en el servidor remoto, bajo el usuario que tenga el cliente.
     * @throws RemoteException si ocurre un problema remoto.
     */
    public void iniciarSesion() throws RemoteException {
        if(user != null) {
            System.out.println("Iniciando sesion como <"+user.name +"> ...");
            user.session_id = remoteServer.iniciarSesion(user);
            System.out.println("Sesion iniciada con exito, sesion_id="+user.session_id);
        }
    }

    /**
     * Inicia el servicio de monitorizacion de carpetas, para comprobar cuando se crean/modifican archivos/carpetas
     * dentro de la carpeta de sincronizacion.
     */
    public void iniciarServicioObservadorDeCarpetas() {
        try {
            monitor = new ServicioMonitorizacion(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fixedSubRutines.execute(monitor);
    }


























    //Servicios de cierre

    /**
     * Cierra la sesion del usuario indicado en el cliente, en el servidor remoto.
     */
    public void cerrar_usuario() {
        System.out.println("Cerrando usuario ...");
        try {
            remoteServer.cerrarSesion(user);
            System.out.println("Sesion cerrada con exito");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cierra de forma segura el cliente.
     */
    public void close() {
        cerrar_usuario();

        monitor.dejarDeObservar();
        monitor = null;
    }























    //Servicios de funcionalidad

    /**
     * Le pide al gestor de tareas que lance una nueva tarea, del tipo indicado en los parametros, para el archivo tambien
     * indicado en los parametros.
     * @param archivo sobre el que ejecutar la tarea.
     * @param operacion {@link VariablesGlobales#UPLOAD}/{@link VariablesGlobales#DOWNLOAD}, operacion que se quiere ejecutar.
     * @throws IOException si ocurre un problema al ejecutar la operacion.
     */
    public void ejecutarOperacion(Archivo archivo, int operacion) throws IOException {

        System.out.println( VariablesGlobales.toString(operacion)+": "+archivo );
        switch (operacion) {
            case VariablesGlobales.UPLOAD -> {
                exec.execute(new Upload(remoteServer, archivo, workingPath.toString(), user));
            }
            case VariablesGlobales.DOWNLOAD -> {
                exec.execute(new Download(remoteServer, archivo, workingPath.toString(), user));
            }
        }

    }


    /**
     * Metodo principal, que ejecuta las instrucciones descritas {@link SyncApp} para obtener la lista de archivos, y determinar
     * que hacer con cada uno de ellos.
     * @throws RemoteException si ocurre un problema remoto que impida ejecutar el metood.
     */
    public void sincronizarConServidor() throws RemoteException {

        //Obtenemos listas iniciales, sin parametros

        // Obtenemos la lista local y la mostramos por pantall
        ArrayList<Archivo> local = Utilidades.listFiles(workingPath);
        if (local != null) {
            System.out.println("tamaño lista local=" + local.size()); //TESTS
            local.forEach(System.out::println); //TESTS
        }

        // Obtenemos la lsita remota y la mostramos por pantalla
        ArrayList<Archivo> remota = remoteServer.listaArchivos(user);
        if (remota != null) {
            System.out.println("\ntamaño lista remota=" + remota.size()); //TESTS
            remota.forEach(System.out::println); //TESTS
        }

        // Establecemos dos condiciones que permiten determinar si la lista
        // local o remota contiene elementos que sincronizar
        boolean hayElementosEnLocal = (local != null && local.size() > 1);
        boolean hayElementosEnRemoto = (remota != null && remota.size() > 1);


        // Si no hay archivos en local, pero si en remoto, descargamos todos los archivos de la lista
        if (!hayElementosEnLocal && hayElementosEnRemoto) {
            for (Archivo archivo : remota) {
                try {
                    ejecutarOperacion(archivo, VariablesGlobales.DOWNLOAD);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }


        // Si no hay en remota, pero si en local, cargamos todos los archivos de la lista.
        if (!hayElementosEnRemoto && hayElementosEnLocal) {
            for (Archivo archivo : local) {
                try {
                    ejecutarOperacion(archivo, VariablesGlobales.UPLOAD);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }


        // Si local y remota estan vacias, volvemos, para asi no ejecutar ninguna operacion y esperar que se cree algun
        // archivo para subirlo (monitor de archivos)
        if (!hayElementosEnLocal && !hayElementosEnRemoto) {
            return;
        }


        // LLegamos aqui si hay archivos tanto en remota como en local, por tanto tenemos que comparar ambas listas
        // para determinar que archivos deben cargarse/descargarse

        // Establecemos un mapa auxiliar, que nos permitira almacenar una ruta relativa, en forma de String para que sea
        // unica, y un objeto tipo LocalRemote, que nos permitira establecer si el archivo dado esta en la lista local,
        // en la lista remota, o en ambas. Ademas, como LocalRemote contien tanto el archivo remoto como local, podemos
        // comparar sus metadatos para determinar cual es el mas reciente

        // Creamos el hashmap auxiliar
        HashMap<String, LocalRemote> presencia = new HashMap<>();



        // Para cada elemento de la lista local, almacenamos la pareja ruta (en forma String) y su Localremote, añadiendo
        // el archivo iterado como archivo local.
        local.forEach(c -> {

            // Creamos un LocalRemote, y añadimos el archivo iterado como archivo local
            LocalRemote lr = new LocalRemote();
            lr.setLocal(c);

            // Añadimos la pareja de valores al mapa
            presencia.put(c.ruta, lr);
        });



        // Para cada archivo remoto, repetimos el mismo proceso anterior, pero en este caso comprobamos si el objeto
        // LocalRemote ya existia para este archivo. Esto es muy importante, porqe si creamos un nuevo LocalRemote
        // estamos borrando la informacion que podria existir de la lista local
        remota.forEach(c -> {

            // Obtenemos la ruta relativa
            String rutaRelativa = c.ruta;


            // Si el LocalRemote para este archivo ya existia, lo obtenemos y añadimos el archivo remoto
            if (presencia.containsKey(rutaRelativa)) {
                LocalRemote lr = presencia.get(rutaRelativa);
                lr.setRemote(c);
            }
            // Si el LocalRemote para este archivo no existia, creamos uno nuevo, añadimos archivo remoto, y lo
            // introducimos en el mapa
            else {
                LocalRemote lr = new LocalRemote();
                lr.setRemote(c);
                presencia.put(rutaRelativa, lr);
            }
        });



        // Iteramos el mapa para determinar las operaciones. Para ello usamos como ayuda los propios metodos que contiene
        // el objeto LocalRemote, que permiten comparar los metadatos de los archivos
        presencia.forEach((rutaRelativa, localRemote) -> { // a= rutaRelativa (string)  b= LocalRemote
            try {

                // Si el archivo esta exclusivamente en local, lo cargamos al servidor
                if (localRemote.exclusivoLocal()) {
                    ejecutarOperacion(localRemote.getLocal(), VariablesGlobales.UPLOAD);
                }

                // Si el archivo esta exclusivamente en remoto, lo descargamos del servidor
                else if (localRemote.exclusivoRemoto()) {
                    ejecutarOperacion(localRemote.getRemoto(), VariablesGlobales.DOWNLOAD);
                }


                // Si el archivo esta en ambas maquinas, pedimos el hash para poder comparar
                else {
                    String hashLocal = Utilidades.checkSumhash(localRemote.getLocal().toFile());
                    String hashRemoto = remoteServer.calcularHash(localRemote.getRemoto(), user);

                    // Si los hashes son diferentes es porque el alguno de los archivos (o los dos) ha cambiado
                    // Comparamos la ultima fecha de modificacion en ese caso
                    if (!hashLocal.equals(hashRemoto)) {

                        // Comparamos la ultima fecha de modificacion. Si la fecha local es menor que la remota, es porque
                        // la fecha remota es mas reciente (la fecha en milis incrementa al pasar el tiempo)º
                        if (localRemote.getLocal().timeMilisLastModified < localRemote.getRemoto().timeMilisLastModified) {
                            ejecutarOperacion(localRemote.getRemoto(), VariablesGlobales.DOWNLOAD);
                        } else {
                            ejecutarOperacion(localRemote.getLocal(), VariablesGlobales.UPLOAD);
                        }


                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });


    }


    /**
     * Este metodo permite determinar el offset que existe entre el cliente y el servidor. Para ello, se ejecuta el
     * Algoritmo de Cristan, y se determina, para una serie de intentos, el menor offset posible.
     * @param maxIteraciones maximas iteraciones para este Algoritmo de Cristian.
     * @throws RemoteException si ocurre un problema al pedir la hora al servidor
     */
    public void calcularOffset(int maxIteraciones) throws RemoteException {

        // Establecemos unos arrays que nos permitiran almacenar los diferentes resultados de cada iteraccion del
        // algoritmo
        long[] rttS = new long[maxIteraciones];
        long[] horaServ = new long[maxIteraciones];
        long[] t1S = new long[maxIteraciones];

        // Establecemos un indice, que nos permitira determinar cual es el menor resultado del algoritmo
        int menor = 0;

        // Calculamos varios RTT y nos quedamos con el menor, y la hora del servidor
        // asociada a ese RTT
        for (int i = 0; i < t1S.length; i++) {

            // Obtenemos el tiempo de la maquina antes y despues de pedir al servidor la hora, para determinar cual
            // el tiempo de ida y vuelta (RoundTripTtime / RTT)
            long t0 = System.currentTimeMillis();
            long tr = remoteServer.obtenerHora();
            long t1 = System.currentTimeMillis();

            // Realizamos los calculos del algoritmo
            rttS[i] = t1 - t0;
            horaServ[i] = tr;
            t1S[i] = t1;

            // System.out.println("intento "+i+" -> hora servidor="+tr+"ms,
            // RTT="+rttS[i]+"ms");

            // Comprobamos si los resultados de esta iteraccion son menores que el que actualmente es el menor, para
            // determinar si es menor, tomamos como parametro de comparacion el RTT, pues es el mayor influyente a la
            // hora de determinar el desfase (offset) del cliente-servidor
            if (rttS[i] < rttS[menor]) {
                menor = i;
            }

        }

        // Calculamos Tmin
        // Tmin es el menor tiempo que puede durar en llegar un mensaje al servidor
        // (tiempo minimo de propagacion). Para ello realizamos una peticion al servidor con la menor informacion
        // posible, como es hacer un ping, pues simplemente se invoca al servidor remoto y cuando termina de
        // ejecutarse devuelve fin de metodo

        // En realidad ese calculo es el tiempo de ida y vuelta, para obtener Tmin, ese tiempo de
        // ida y vuelta (RTT) lo dividimos entre 2, que seria el tiempo minimo que se tarda en llegar de un
        // extremo a otro, y RTT es el tiempo que tarda de llegar desde un extremo al otro y volver

        // Establecemos como inicial que tMin es el mayor valor posible, pues si iniciamos a 0 nigun valor de RTT/2 sera
        // inferior a 0
        long tMin = Long.MAX_VALUE;

        // Realizamos 4 iteracciones (podrian ser mas)
        for (int i = 0; i < 4; i++) {

            // Analogamente al bloque de codigo anterior, calculamos RTT
            long t0 = System.currentTimeMillis();
            remoteServer.ping();
            long t1 = System.currentTimeMillis();

            // Realizamos los calculos
            long rttActual = t1 - t0;
            long tActual = rttActual/2;

            // Comparamos si el tMin obtenido en esta iteraccion es menor que el menor tMin hasta el momento. En caso
            // de que sea menor, actualizamos el valor a tmin de esta iteraccion
            if (tActual < tMin) {
                tMin = tActual;
            }

        }

        // Obtenemos los resultados del algoritmo y guardamos los resultados
        long hs = horaServ[menor] + (rttS[menor] / 2); //horaServer
        long error = (rttS[menor] / 2) - tMin;         //|error tipico| del algoritmo de cristian
        long moffset = t1S[menor] - hs;                // offset = hora del cliente(t1s) - hora del servidor (c)
        System.out.println("resultado de algoritmo Christian: hora de servidor=(" + hs + " +-" + error + ")ms  y offset="
                + moffset + "ms");

        this.timeOffset = moffset;
        this.timeError = error;
    }


    /**
     * Este metodo nos permite bloquear el cliente hasta que se terminen de sincronizar todos los archivos. Esto se
     * realiza debido a que hay caso en los que la descarga de un archivo toma mucho tiempo, y detecta que se esta
     * escribiendo un nuevo archivo (monitor no diferencia entre modificacion/creacion de archivos por parte del usuario
     * o por parte de un programa, pues solo detecta cambios en archvos). <br>
     * Para realizar este proposito, se usa el metodo {@link ExecutorService#shutdown()} del gestor de tareas, que
     * ejecutara todas las tareas  pendientes hasta que finalicen y no permitira encolar nuevas tareas.
     * Posteriormente se debera de crear un nuevo gestor de tareas para que se puedan ejecutar.
     * <br>
     * Posteriormente a shutdow(), utilizaremos la funcion {@link java.util.concurrent.ExecutorService#awaitTermination(long, TimeUnit)}
     * para que el hilo actual se bloquee hasta que todas las tareas pendientes tras shutdown() finalicen. Esto nos
     * permite detectar que la transmision de archivos ha finalizado.
     */
    public void esperarHastaTerminarTransmisiones() {

        // Invocamos el metodo shutdown() para finalice las tareas en orden y no permita encolar nuevas tareas
        exec.shutdown();

        // Creamos una variable auxiliar para saber si las tareas han finalizado
        boolean transmissionFinished = false;

        // Mientras las tareas no hayan finalizado, intentaremos bloquear el hilo actual
        while(!transmissionFinished) {
            try {

                // Bloqueamos el hilo hasta que las tareas pendientes de shutdow() finalicen su trascurso normal, o alcancemos
                // un timeout, en cuyo caso se devuelve true si se han finalizado todas las tareas, o false
                // si todavia queda alguna tarea pendiente de finalizar
                transmissionFinished = exec.awaitTermination(300, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Tras finalizar, creamos un nuevo gestor de tareas, debido a que el anterior no atendera nuevas peticiones
        exec = Executors.newFixedThreadPool(hilos);

    }

    

    
}
