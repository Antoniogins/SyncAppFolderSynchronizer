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
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.Utilidades;
import com.syncapp.utility.Operaciones;

public class SyncAppCliente {

    //constantes para acceder a argumentos
    public static final int ARG_IP = 0;
    public static final int ARG_PUERTO = 1;
    public static final int ARG_USUARIO = 2;
    public static final int ARG_CARPETA = 3;
    public static final int ARG_HILOS = 4;

    //args para cliente:
        //  ip
        //  puerto
        //  usuario
        //  carpeta
        //  threads




    //Parametros del cliente
    private String serverIP;
    private TokenUsuario user;
    private long timeOffset;
    private long timeError;
    private Path workingPath;
    private int puerto;




    //trabajadores
    private SyncApp remoteServer;
    private ServicioMonitorizacion monitor;
    private ExecutorService exec;




    //esto pasara al controlador
    private ExecutorService fixedSubRutines;
    // private InterfazCliente interfaz;




    //Constructores
    public SyncAppCliente(String[] args) {
        if(args == null || args.length != 5) return;


        fixedSubRutines = Executors.newFixedThreadPool(2);

        System.out.println("estableciendo parametros");
        serverIP = args[ARG_IP];
        puerto = Integer.parseInt(args[ARG_PUERTO]);
        user = new TokenUsuario(args[ARG_USUARIO]);
        setWorkingPath(args[ARG_CARPETA]);
        exec = Executors.newFixedThreadPool(  Integer.parseInt(args[ARG_HILOS])  );



    }















    //GETTERS
    public SyncApp getRemoteServer() { return remoteServer; }
    public TokenUsuario getUsuario() { return user; }
    // public ServicioMonitorizacion getMonitor() { return monitor; }
    public Path getWorkingPath() { return workingPath; }//creamos el path asi para que no se pueda modificar el path interno desde el exterior
    public int getPort() { return puerto; }
    public String getIP() { return new String(serverIP); }
    public long getTimeOffset() { return timeOffset; }

















    //SETTERS
    
    public void setServerIP(String serverIP) {
        if(serverIP == null || serverIP.split(".").length != 4) return;
        for (int i=0 ; i < 4 ; i++) {
            int val = Integer.parseInt(serverIP.split(".")[i]);
            if( val < 1 || val > 255) return;
        }
        this.serverIP = new String(serverIP);
        //cuando hacemos ip.spit(".") separamos el texto ip en subtextos, buscando "." dentro del texto para separar
        //si tras separar, el array resultante no es 4 significa que no es una ip
        // "192.168.0.1" -> {"192" , "168" , "0" , "1"} , cualquier otra tamaño de array no representa una ip

    }


    public void setUser(TokenUsuario user) {
        if(user == null || user.name == null || user.name.equals("")) return;
        this.user = new TokenUsuario(user.name);
    }


    public void setWorkingPath(Path workingPath) {
        if(workingPath == null) return;

        if(workingPath.isAbsolute()) {
            this.workingPath = workingPath;
        } else {
            this.workingPath = Paths.get( System.getProperty("user.home") ).resolve(workingPath);
        }


    }

    public void setWorkingPath(String path) {
        setWorkingPath(Paths.get(path));
    }


    public void setPuerto(short puerto) {
        if(puerto < 1000) return; //short va de 0 a 65535 (justo la cantidad de puertos que existen)
        this.puerto = puerto;
    }

    public void setTimeOffset(long offset) {
        if(offset < 0 ) return;
        timeOffset = offset;
    }





















    //Servicios de iniciacion
    public void iniciarServidor() throws MalformedURLException, RemoteException, NotBoundException {
        remoteServer = (SyncApp) Naming.lookup("rmi://"+serverIP+":"+puerto+"/SyncApp");
    }
    

    public void iniciarUsuario() throws RemoteException {
        if(user != null) {
            System.out.println("Iniciando sesion como <"+user.name +"> ...");
            user.session_id = remoteServer.iniciarUsuario(user);
            System.out.println("Sesion iniciada con exito, sesion_id="+user.session_id);
        }
    }
    

    public void iniciarServicioObservadorDeCarpetas() {
        try {
            monitor = new ServicioMonitorizacion(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fixedSubRutines.execute(monitor);
    }


























    //Servicios de cierre

    public void cerrar_usuario() {
        System.out.println("Cerrando usuario ...");
        try {
            remoteServer.cerrarUsuario(user);
            System.out.println("Sesion cerrada con exito");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        cerrar_usuario();

        monitor.dejarDeObservar();
        monitor = null;
        
        
        

    }























    //Servicios de funcionalidad

    public ArrayList<Archivo> ejectuarOperaciones(HashMap< Archivo, Integer> lista) {
        if(lista == null || lista.size() < 1 ) return null;

        ArrayList<Archivo> pendientes = new ArrayList<>();

        System.out.println("\nOperaciones a realizar:");
        lista.forEach((a , b) ->{ //a = Archivo  b = int(operacion)
            try{

                if( ejecutarOperacion(a, b) < 1) {
                    pendientes.add(a);
                } 

            } catch(IOException ioe) {
                ioe.printStackTrace();
            }

        } );


        return pendientes;
    }




    //devuelve >0 si la operacion ya se ha ejecutado y <0 si el archivo necesita mas informacion para decidir que ejecutar
    public int ejecutarOperacion(Archivo a, int operacion) throws IOException {
        int returner = 1;
        System.out.println(Operaciones.toString(operacion)+": "+a.ruta);
        switch(operacion){
            case Operaciones.UPLOAD : {
                exec.execute(new Upload(remoteServer, a, workingPath.toString() , user)); 
                break;
            }
            case Operaciones.DOWNLOAD : {
                exec.execute(new Download(remoteServer, a, workingPath.toString() , user));
                break;
            }
            case Operaciones.MORE_INFO : {
                returner = -1;
                break;
            }
            default : {
                System.out.println("operacion desconocida");
            }
        }

        return returner;
    }

    



    public ArrayList<Archivo> primeraIteracion() throws RemoteException {

        //Obtenemos listas iniciales, sin parametros
        ArrayList<Archivo> local = Utilidades.listFiles(workingPath);
        if (local != null) {
            local.sort((a, b) -> a.ruta.compareTo(b.ruta));
            System.out.println("tamaño lista local="+local.size()); //TESTS
            local.forEach(c-> System.out.println(c.ruta)); //TESTS
        }

        ArrayList<Archivo> remota = remoteServer.listaArchivos(user);
        if (remota != null) {
            remota.sort((a, b) -> a.ruta.compareTo(b.ruta));
            System.out.println("\ntamaño lista remota="+remota.size()); //TESTS
            remota.forEach(c-> System.out.println(c.ruta)); //TESTS
        }
        


        
        

        HashMap<Archivo , Integer> pendientes = Utilidades.operacionesIniciales(local, remota, workingPath);

        return ejectuarOperaciones(pendientes);
    }


    public ArrayList<Archivo> siguienteIteracion(ArrayList<Archivo> pendientes) throws RemoteException {
        //Seguda parte, intercambiar archivos ya existentes
        if(pendientes == null || pendientes.size() < 1) return null;

        ArrayList<Archivo> local = Utilidades.obtenerParametrosSimultaneos(pendientes, workingPath);

        System.out.println("info local");
        local.forEach(System.out::println);

        ArrayList<Archivo> remota = remoteServer.obtenerParametrosSimultaneos(user, pendientes);

        System.out.println("info remota");
        remota.forEach(System.out::println);
        HashMap< Archivo, Integer> operaciones = Utilidades.compararParametrosSimultaneos(local, remota, timeOffset, workingPath);
        
        if(operaciones == null || operaciones.size() == 0) return null;

        return ejectuarOperaciones(operaciones);
    }


    public void calcularOffset(int maxIntentos) throws RemoteException {
        long[] rttS = new long[maxIntentos];
        long[] horaServ = new long[maxIntentos];
        long[] t1S = new long[maxIntentos];
        int menor = 0;

        // Calculamos varios RTT y nos quedamos con el menor, y la hora del servidor
        // asociada a ese RTT
        for (int i = 0; i < t1S.length; i++) {
            long t0 = System.currentTimeMillis();
            long tr = remoteServer.obtenerHora();
            long t1 = System.currentTimeMillis();

            rttS[i] = t1 - t0;
            horaServ[i] = tr;
            t1S[i] = t1;

            // System.out.println("intento "+i+" -> hora servidor="+tr+"ms,
            // RTT="+rttS[i]+"ms");

            if (rttS[i] < rttS[menor]) {
                menor = i;
            }
        }

        // Calculamos Tmin
        // Tmin es el menor tiempo que puede durar en llegar un mensaje al servidor
        // (tiempo minimo de propagacion). Para ello enviamos la menor unidad de
        // informacion (1 byte) y nos dara el menor tiempo que podemos tardar en llegar
        // al servidor

        // En realidad es el tiempo de ida y vuelta, para obtener Tmin, ese tiempo de
        // ida y vuelta (RTT) lo dividimos entre 2
        
        long tMin = Long.MAX_VALUE;

        for (int i = 0; i < 4; i++) {

            long t0 = System.currentTimeMillis();
            remoteServer.ping();
            long t1 = System.currentTimeMillis();

            long rttActual = t1 - t0;
            long tActual = rttActual/2;

            // System.out.println("intento "+i+" -> tmin="+rttActual+"ms");
            // double cambio = rttNano/1000000;
            // System.out.println("intento "+i+" -> tminNano="+rttNano+"ns ->
            // "+cambio+"ms");

            if (tActual < tMin) {
                tMin = tActual;
            }

        }

        long hs = horaServ[menor] + (rttS[menor] / 2); //horaServer
        long error = (rttS[menor] / 2) - tMin;         //|error tipico| del algoritmo de cristian
        long moffset = t1S[menor] - hs;                // offset = hora del cliente(t1s) - hora del servidor (c)
        System.out.println("resultado de algoritmo Christian: hora de servidor=(" + hs + " +-" + error + ")ms  y offset="
                + moffset + "ms");

        this.timeOffset = moffset;
        this.timeError = error;
    }









    public void esperarHastaTerminarTransmisiones() {
        exec.shutdown();
        boolean transmissionFinished = false;
        while(!transmissionFinished) {
            try {
                transmissionFinished = exec.awaitTermination(300, TimeUnit.SECONDS);
                //BLOQUEAMOS HASTA QUE TERMINEN TODAS LAS TAREAS DE exec U OCURRA UN TIMOUT DE 300s;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }








    
    
    





    
















    

    
}
