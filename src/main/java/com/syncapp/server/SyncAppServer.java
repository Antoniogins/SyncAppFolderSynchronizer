package com.syncapp.server;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.LectorArchivos;
import com.syncapp.utility.Util;

public class SyncAppServer extends UnicastRemoteObject implements SyncApp{

    /**
     * 
     * 
     * 
     * 
     */
    private static final Path usersContainers = Paths.get(System.getProperty("user.home")).resolve("sync_app").resolve("cloud_containers");
    ArrayList<String> usuariosActivos;
    HashMap< Integer , Path> archivosActivos;
    HashMap< Integer, Long> position;
    HashMap< Integer, Integer> ultimoEnviado;
    HashMap< Integer, LectorArchivos> manejo;
    FileWriter bw;
    int logId;
    static int global_id;
    

    protected SyncAppServer() throws RemoteException {
        super();
        usuariosActivos = new ArrayList<>();
        archivosActivos = new HashMap<>();
        position = new HashMap<>();
        // global_id = (int) (Math.random()*1000);
        global_id = 0;
        logId = 0;
        ultimoEnviado = new HashMap<>();
        manejo = new HashMap<>();
        
        boolean keep = true;
        logId = 0;
        Path logger = Paths.get("");
        while (keep) {
            logger = Paths.get(System.getProperty("user.home")).resolve("sync_app").resolve("logs")
                    .resolve(logId + ".txt");

            if (logger.toFile().exists()) {
                logId++;
            } else {

                try {
                    Path tmp = logger.getParent();
                    if (!tmp.toFile().exists()) {
                        Files.createDirectories(tmp);
                    }
                    Files.createFile(logger);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                keep = false;
            }

        }

        try {
            if(logger.toFile().isFile()) {
                bw = new FileWriter( logger.toFile() );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    @Override
    public void iniciar_usuario(TokenUsuario u) throws RemoteException {
        if(u == null) return;
        usuariosActivos.add(u.token);
        System.out.println("Usuario: "+u.token+" ha iniciado sesion");
        log("Usuario: "+u.token+" ha iniciado sesion");
        // System.out.println("Objeto remoto="+u); //TESTS
        
    }

    @Override
    public void cerrar_usuario(TokenUsuario u) throws RemoteException {
        if(u == null) return;
        usuariosActivos.remove(u.token);
        System.out.println("Usuario: "+u.token+" ha cerrado sesion");
        log("Usuario: "+u.token+" ha cerrado sesion");
        // System.out.println("Objeto remoto="+u); //TESTS
    }


    @Override
    public int abrirArchivo(TokenUsuario tu, Archivo a, String op_mode) throws RemoteException { // op_mode is read or write
        int file_id = global_id++;
        Path filepath = Paths.get(usersContainers.toString() , tu.token , a.ruta);
        long len = filepath.toFile().length();
        System.out.println("abriendo file="+file_id+" ruta="+a.toString()+" _"+op_mode+"_ bytes="+len);
        log("abriendo file="+file_id+" ruta="+filepath.toString()+" _"+op_mode+"_ bytes="+len);
        archivosActivos.put(file_id, filepath);
        ultimoEnviado.put(file_id, -1);

        try {
            manejo.put(file_id, new LectorArchivos(filepath, op_mode));
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new RemoteException("Error al abrir archivo en servidor");
        }
  

        manejo.get(file_id).id_file = file_id;
        return file_id;
    }

    @Override
    public void cerrarArchivo(int id_file) throws RemoteException {
        System.out.println("cerrando archivo "+id_file);
        log("cerrando archivo "+id_file);
        // lectorArchivos.cerrarArchivo( archivosActivos.get(id_file) );
        try {
            manejo.get(id_file).cerrarArchivo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        manejo.remove(id_file);
        archivosActivos.remove(id_file);

        
    }




    @Override
    public String hash(int file_id) throws RemoteException {
        System.out.println("realizando hash a "+file_id);
        log("realizando hash a "+file_id);
        return Util.checkSumhash( archivosActivos.get(file_id).toFile() );
    }

    @Override
    public String sayHello() throws RemoteException {
        return "Hello world!";
    }

    

    @Override
    public ArrayList<Archivo> lista_archivos(TokenUsuario u) throws RemoteException {
        if(!usuariosActivos.contains(u.token)) return null;
        Path toWalk = Paths.get(usersContainers.toString() , u.token );
        if(!toWalk.toFile().exists()) return null;
        ArrayList<Archivo> listaRet = Util.listFiles( toWalk );
        System.out.println("listando archivos de <"+u.token+">"+" cantidad de elementos: "+listaRet.size());
        log("listando archivos de <"+u.token+">"+" -> "+Paths.get(usersContainers.toString() , u.token )+" cantidad de elementos: "+listaRet.size());
        return listaRet;
    }

    @Override
    public ArrayList<Archivo> obtenerParametrosSimultaneos(TokenUsuario tu, ArrayList<Archivo> lista)
            throws RemoteException {

        if(!usuariosActivos.contains(tu.token) || lista == null || lista.size() == 0){
            return null;
        }

        log("obteniendo parametros de archivos pedidos");
        return Util.obtenerParametrosSimultaneos(lista, Paths.get(usersContainers.toString() , tu.token));
        
    }

    @Override
    public Archivo obtenerParametros(TokenUsuario tu, Archivo a) throws RemoteException {
        if(!usuariosActivos.contains(tu.token) || a == null) {
            return null;
        }

        try {
            log("obteniendo parametros de archivo "+a.ruta);
            return Util.getParameters(a, Paths.get(usersContainers.toString() , tu.token));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }







    /**
     * 
     * si position < 0 se leera de la ultima posicion que tenia el lector de
     *      archivos.
     * si position >= 0 se leera desde esa posicion. La posicion actual del lector
     *      de archivos se actualizara al siguiente byte a leer despues de leer este
     *      bloque.
     */

    @Override
    public BloqueBytes leerBloqueBytes(int id_file, long position) throws RemoteException {
        LectorArchivos archivoActual = manejo.get(id_file);

        try {
            log("leyendo file="+id_file+" pos="+position);
            return archivoActual.leerBloqueBytesEnPosicion(position);

        } catch (IOException e) {
            System.out.println("aqui falla?");
            e.printStackTrace();
            throw new RemoteException("Error al leer un bloque de bytes");
        }
        
    }


    @Override
    public void escribirBloqueBytes(int id_file, BloqueBytes bloq_bytes, int pos) throws RemoteException {
        if(bloq_bytes == null || bloq_bytes.size < 1 || id_file < 0) return;

        LectorArchivos actual = manejo.get(id_file);
        try {
            log("escribiendo "+bloq_bytes.toString());
            actual.escribirBloqueBytesEnPosicion(bloq_bytes, pos);
        } catch(IOException ioe) {
            ioe.printStackTrace();
            throw new RemoteException("Error al escribir el bloque de bytes en servidor");
        }

    }


    

    @Override
    public long obtenerHora() throws RemoteException {
        
        // int rand = (int) (Math.random()*100);

        // simularRetardo(rand);
        long hora = System.currentTimeMillis();
        // simularRetardo(rand);
        
        return hora;
    }

    @Override
    public byte calcularTmin(byte b) throws RemoteException {
        
        // ESTO ES PARA PRUEBAS, simulamos el tiempo que tarda en viajar un byte tanto
        // tiempo que tarda en llegar a servidor como tiempo que tarda en llegar a
        // cliente que seria 2*tmin

        // Como al llegar la peticion no se realiza ninguna accion, 2*tmin = RTT
        
        // int rand = (int) (Math.random()*10);
        // simularRetardo(rand);
        // simularRetardo(rand);
        return 0;
    }

    void simularRetardo(int milis) {
        try {
            Thread.sleep((long) milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    void log(String info) {
        if(bw == null) return;

        Date fecha = Date.from(Instant.now());
        String info0xf = fecha.toString(); 
        try {
            bw.write("["+info0xf+"] "+info);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    







































    public static void main(String[] args) throws RemoteException, MalformedURLException {
        SyncAppServer sap = new SyncAppServer();
        Naming.rebind("rmi://localhost:1099/SyncApp", sap);
        System.out.println("ready to operate");
        sap.log("ready to operate");
        
    }






    






    






    






   






    


}
