package com.syncapp.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.syncapp.interfaces.SyncApp;
import com.syncapp.model.LocalRemote;
import com.syncapp.model.Archivo;



public class Util {

    public static final int MAX_BLOCKS = 16;
    public static final int MAX_BYTES_IN_BLOCK = 8000000; //Por defecto 80KB = 80 000


    /**
     * Esta clase statica toma como parametro de entrada un directorio (Path) y
     * devuelve el contenido
     * del directorio en un arraylist de Path.
     * 
     * 
     * @param Path 
     * @return ArrayList<Path> , o null si el directorio no existe
     * @throws IOException
     */

    public static ArrayList<Archivo> listFiles(Path pathToList) {

        if (pathToList == null)
            return null;

        if(!pathToList.toFile().exists()) {
            try {
                Files.createDirectory(pathToList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Archivo> lista = new ArrayList<>();

        try {
            Files.walk(pathToList).forEach(c -> {
                c.normalize();
                Path m = pathToList.relativize(c);
                
                if (c.toFile().isFile() && c.toFile().length() > 0){
                    lista.add(new Archivo(m , pathToList));
                }
                
            });
        } catch (IOException ioe) {
            System.out.println("excepcion en utils al listar archivos");
            ioe.printStackTrace();
        }

        return lista;

    }



    //Si se crean carpetas durante la ejecucion del programa hay que reiniciar o ya implementare alguna funcion
    public static ArrayList<Path> listFolders(Path pathToList) {

        if (pathToList == null)
            return null;

        ArrayList<Path> lista = new ArrayList<>();

        try {
            Files.walk(pathToList).forEach(c -> {

                if (c.toFile().isDirectory()) {
                    lista.add(c);
                }

            });
        } catch (IOException ioe) {
            System.out.println("excepcion en utils al listar carpetas");
            ioe.printStackTrace();
        }

        return lista;

    }

    public static Archivo getParameters(Archivo a, Path workingFolder) throws IOException {
        
        Archivo deVuelta = new Archivo(  a.toRelativePath(), workingFolder);
        
        deVuelta.hash = checkSumhash( deVuelta.toFile() );
        
        FileTime ft = Files.getLastModifiedTime( deVuelta.toPath() );
        deVuelta.timeMilisLastModified = ft.toMillis();

        return deVuelta;
    }

    public static ArrayList<Archivo> obtenerParametrosSimultaneos(ArrayList<Archivo> lista, Path containerFolder) {
        if(lista == null || lista.size() == 0) {
            return null;
        }
        ArrayList<Archivo> listaFinal = new ArrayList<>();
        lista.forEach(c -> {
            try {
                listaFinal.add( getParameters( c, containerFolder) );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return listaFinal;
    }





    /**
     * Este metodo nos permite crear un checksum a un File de entrada, usango el
     * algoritmo md5
     * 
     * @param f File al que le realizaremos el md5sum
     * @return un String que contiene el resultado del algoritmo md5
     * @throws FileNotFoundException cuando no existe el archivo indicado
     * @throws IOException           cuando se produce un error al leer el archivo
     *                               {@code Path}
     */
    public static String checkSumhash(File f) {

        // Comprobamos errores antes de intentar realizar ninguna operacion
        if (f == null)
            return null;

        // Obtenemos el Stream para poder leer el archivo
        // BufferedInputStream bif = null;
        RandomAccessFile raf = null;

        try {
            // bif = new BufferedInputStream(new FileInputStream(f.ruta));
            raf = new RandomAccessFile(f, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Usamos un StringBuilder para formatear el resultado del md5
        StringBuilder sb = new StringBuilder();

        // Algoritmo md5
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] chunk = new byte[800000]; // Leemos 800KB a cada iteraccion para que no se llene memoria (archivos
                                             // grandes)
            int bytesRead = 0;

            while ((bytesRead = raf.read(chunk)) != -1) { // Cuando no hay mas bytes por leer devuelve -1
                if (bytesRead != chunk.length) {
                    byte[] fin = new byte[bytesRead];
                    System.arraycopy(chunk, 0, fin, 0, bytesRead);

                    md5.update(fin);
                } else {
                    md5.update(chunk);
                }

            }

            byte[] result = md5.digest();

            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }

        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            sb = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Devolvemos el hash md5 del archivo
        return (sb == null) ? null : sb.toString();

    }


    public String hashmd5(byte[] bytes) {

        return null;
    }



    //ESTA SOLO COMPARA PRESENCIA LOCAL O REMOTA, TA BIEN PARA OPTIMIZAR 
    public static HashMap<Archivo , Integer> operacionesIniciales(ArrayList<Archivo> local, ArrayList<Archivo> remoto, Path workginPath) {
        if(   (local == null || local.size() < 1) && (remoto == null || remoto.size() < 1)  ) return null;


        HashMap<Archivo , Integer> operaciones = new HashMap<>();

        //si no hay archivos remotos, pero si locales, cargamos todos los que hay en local
        if(   (remoto == null || remoto.size() < 1 )    && local.size() >= 1) {
            for(Archivo r : local) {
                operaciones.put(r, Ops.UPLOAD);
            }
            return operaciones;
        } 

        
        //si no hay archivos locals, pero si remotos, descargamos todos los que hay en remoto
        if(   (local == null || local.size() < 1 )    && remoto.size() >= 1){
            for(Archivo r : remoto) {
                operaciones.put(r, Ops.DOWNLOAD);
            }
            return operaciones;
        }




        HashMap<Archivo , Integer> operacionesMixtas = new HashMap<>();
        HashMap<String , LocalRemote> listaIndices = new HashMap<>();

        local.forEach(c -> {
            LocalRemote lr = new LocalRemote();
            lr.presentInLocal = true;
            listaIndices.put(c.ruta, lr);
        });
        
        remoto.forEach(c->{
            if(listaIndices.containsKey(c.ruta)){
                listaIndices.get(c.ruta).presentInRemote = true;
            } else {
                LocalRemote lr = new LocalRemote();
                lr.presentInRemote = true;
                listaIndices.put(c.ruta, lr);
            }
        });
        
        listaIndices.forEach((a,b)->{

            //Primero comparamos si un archivo esta presente unicamente en local o en remoto
            if(b.presentInLocal && !b.presentInRemote) {
                operacionesMixtas.put(new Archivo( Paths.get(a) , workginPath), Ops.UPLOAD); //Presente en local y no presente en remoto -> subir archivo

            } else if(!b.presentInLocal && b.presentInRemote) {
                operacionesMixtas.put(new Archivo( Paths.get(a) , workginPath), Ops.DOWNLOAD); //Presente en remoto y no presente en local -> descargar archivo

            } else if(b.presentInLocal && b.presentInRemote) {
                operacionesMixtas.put(new Archivo( Paths.get(a) , workginPath), Ops.MORE_INFO);

            }
        });

        return operacionesMixtas;
    }


    public static HashMap< Archivo, Integer> compararParametrosSimultaneos(ArrayList<Archivo> local, ArrayList<Archivo> remoto, long offset, Path workingPath) {
        HashMap<Archivo , Integer> operaciones = new HashMap<>();

        if(remoto == null || remoto.size() == 0) {
            for(Archivo r : local) {
                operaciones.put(r, Ops.UPLOAD);
            }
            return operaciones;
        } 




        HashMap<String , LocalRemote> listaIndices = new HashMap<>();

        local.forEach(c -> {

            LocalRemote lr = new LocalRemote();
            lr.hashLocal = c.hash;
            lr.timeMilisLocal = c.timeMilisLastModified + offset;
            listaIndices.put(c.ruta, lr);
            
        });
        
        remoto.forEach(c->{
            if(listaIndices.containsKey(c.ruta)){
                listaIndices.get(c.ruta).hashRemoto = c.hash;
                listaIndices.get(c.ruta).timeMilisRemote = c.timeMilisLastModified;
            } else {
                LocalRemote lr = new LocalRemote();
                lr.hashRemoto = c.hash;
                lr.timeMilisRemote = c.timeMilisLastModified;
                listaIndices.put(c.ruta, lr);
            }
        });


        listaIndices.forEach( (a,b) -> {  // a=archivo (en String)  b=localRemote

            if(!b.hashLocal.equals(b.hashRemoto)) {
                if(b.timeMilisLocal < b.timeMilisRemote) {
                    operaciones.put(new Archivo( Paths.get(a) , workingPath), Ops.DOWNLOAD);
                } else {
                    operaciones.put(new Archivo( Paths.get(a) , workingPath), Ops.UPLOAD);
                }
            }

        } );


        return operaciones;
    }


    public static Set<PosixFilePermission> getPerms() {
        Set<PosixFilePermission> peerms = new HashSet<>();

        peerms.add(PosixFilePermission.GROUP_EXECUTE);
        peerms.add(PosixFilePermission.GROUP_READ);
        peerms.add(PosixFilePermission.GROUP_WRITE);

        peerms.add(PosixFilePermission.OTHERS_READ);
        peerms.add(PosixFilePermission.OTHERS_WRITE);
        peerms.add(PosixFilePermission.OWNER_EXECUTE);

        peerms.add(PosixFilePermission.OWNER_READ);
        peerms.add(PosixFilePermission.OWNER_WRITE);
        peerms.add(PosixFilePermission.OWNER_EXECUTE);






        return peerms;
    }












    


    

    






}
