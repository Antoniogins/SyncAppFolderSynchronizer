package com.syncapp.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import com.syncapp.model.LocalRemote;
import com.syncapp.model.Archivo;

/**
 * Esta clase permite trabajar con listas de archivos, y los metadatos del archivo. Permite
 * abstraer al cliente de la comparacion de listas de archivos.
 */

public class Utilidades {


    // Metodos para iterar carpetas y obtener listas de archivos y directorios

    /**
     * Este metodo devuelve un {@link ArrayList}<{@link Archivo}> (lista de archivos, exclusivamente archivos)
     * que existe bajo el directorio {@link Path de entrada}. Usa el metodo {@link Files#walk(Path, FileVisitOption...)} para
     * iterar el directorio y subdirectorios dados, buscando los archivos que realmente son un archivo, y los añade
     * a la lista.
     *
     * <pre>
     * La lista de archivos viene relativizada al path que queremos lista.
     * Por ejemplo: p = /home/usuario/carpeta
     *              c = /home/usuario/carpeta/documentos/renta.pdf
     *              r = documentos/renta.pdf   ->   path de "c" relativo a "p"
     * </pre>
     * Esto ocurre para todos los archivos de la lista, respecto al path de entrada.
     * 
     * @param pathToList directorio sobre el que se quiere listar los archivos.
     * @return {@link ArrayList}<{@link Archivo}> dentro del directorio indicado.
     * @throws IOException si ocurre un problema al iterar los directorios.
     */
    public static ArrayList<Archivo> listFiles(Path pathToList) {

        // Comprobamos que el path indicado cumpla las condiciones para poder listar los archivos.
        // En este caso para que un directorio se pueda iterar, debe ser no nulo y debe ser absoluto
        if (  !(pathToList != null && pathToList.isAbsolute()) ){
            return null;
        }

        // Comprobamos si el directorio indicado es un archivo
        if(pathToList.toFile().isFile()) {
            System.out.println("el directorio indicado es un archivo, no se puede iterar");
            return null;
        }

        // Comprobamos si el directorio existe, en caso contrario lo creamos.
        if(!pathToList.toFile().exists()) {
            try {
                Files.createDirectory(pathToList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // Creamos la lista en la que añadiremos los archivos
        ArrayList<Archivo> lista = new ArrayList<>();

        // Por motivos de seguridad, normalizamos el path a iterar, esto se debe a que se puede indicar un directorio
        // superior indicando "..", lo cual en un servidor puede acceder a directorios que los usuarios no deberian acceder.
        pathToList.normalize();

        // Iteramos el directorio indicado
        try {
            Files.walk(pathToList).forEach(c -> {

                // Dado que unicamente queremos poner al archivo la ruta respecto al path de original, lo relativizamos
                // Por ejemplo: pathToList = /home/usuario/carpeta
                //                       c = /home/usuario/carpeta/documentos/renta.pdf
                //                       m = documentos/renta.pdf
                Path m = pathToList.relativize(c);

                // Si el path c corresponde a un archivo, y su tamaño es mas de 0bytes, lo añadimos a la lista de archivos
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



    /**
     * Devuelve un {@link ArrayList}<{@link Path}> con los directiorios dentro del path de entrada. <br>
     * Itera el path de entrada, hasta que no encuentra mas archivos/directorios.
     * @param pathToList {@link Path} del que queremos conocer los subdirectorios.
     * @return {@link ArrayList}<{@link Path}> con los subdirectorios dentro del path de entrada.
     */
    public static ArrayList<Path> listFolders(Path pathToList) {

        // Comprobamos si cumple los requisistos para ser iterado, en caso contrario devolvemos null
        if (  !(pathToList != null && pathToList.isAbsolute())  ) {
            return null;
        }

        // Creamos la lista donde añadiremos los directorios encontrados
        ArrayList<Path> lista = new ArrayList<>();

        // Iteramos el path de entrada, buscando los paths que sean directorios
        try {
            Files.walk(pathToList).forEach(c -> {

                // Si el path actual es un directorio, lo añadimos a la lista
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





















    // Metodos para obtener metadatos de una lista de archivos

    /**
     *
     * @param lista
     * @param containerFolder
     * @return
     */
    public static ArrayList<Archivo> obtenerMultiplesMetadatos(ArrayList<Archivo> lista, Path containerFolder) {
        if(lista == null || lista.size() == 0) {
            return null;
        }
        ArrayList<Archivo> listaFinal = new ArrayList<>();
        lista.forEach(c -> {
            try {
                listaFinal.add( obtenerMetadatos( c, containerFolder) );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return listaFinal;
    }






















    // Metodos para obtener metadatos sobre un archivo

    /**
     *
     * @param a
     * @param workingFolder
     * @return
     * @throws IOException
     */
    public static Archivo obtenerMetadatos(Archivo a, Path workingFolder) throws IOException {

        Archivo deVuelta = new Archivo(  a.toRelativePath(), workingFolder);

        deVuelta.hash = checkSumhash( deVuelta.toFile() );

        FileTime ft = Files.getLastModifiedTime( deVuelta.toPath() );
        deVuelta.timeMilisLastModified = ft.toMillis();

        return deVuelta;
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
























    // Metodos para obtener hashes sobre objetos (no archivos)

    /**
     *
     * @param bytes
     * @return
     */
    public String hashmd5(byte[] bytes) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            byte[] result = md5.digest();

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }

            return  sb.toString();




        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }























    // Metodos para comparar listas de archivos

    public static HashMap< Archivo, Integer> compararListas(ArrayList<Archivo> local, ArrayList<Archivo> remoto, long offset, boolean comprobarMetadatos) {
        HashMap<Archivo , Integer> operaciones = new HashMap<>();


        boolean directUpload = (remoto == null || remoto.size() < 1 )    &&   local.size() >= 1;
        //si no hay archivos remotos, pero si locales, cargamos todos los que hay en local
        if(directUpload) {
            for(Archivo r : local) {
                operaciones.put(r, VariablesGlobales.UPLOAD);
            }
            return operaciones;
        }



        boolean directDownload = (local == null || local.size() < 1 )    && remoto.size() >= 1;
        //si no hay archivos locales, pero si remotos, descargamos todos los que hay en remoto
        if(directDownload){
            for(Archivo r : remoto) {
                operaciones.put(r, VariablesGlobales.DOWNLOAD);
            }
            return operaciones;
        }


        // Usamos un mapa para almacenar la ruta de un archivo y sus metadatos
        // Es inviable comparar cada elemento de una lista con el resto de elementos de la otra lista para saber
        // si tiene presencia en la otra lista. Usamos este mapa, que para una ruta (que viene identificada unica
        // e inequivocamente por un string) tendra un unico valor asociado en el mapa. Este valor asociado es un
        // objeto LocalRemote, que permite almacenar temporalmente si una ruta tiene presencia local, presencia remota,
        // y los metadatos remotos y locales
        HashMap<String , LocalRemote> listaIndices = new HashMap<>();



        local.forEach(c -> {

            // Dado que iteramos sobre la lista local, sabemos que tiene presencia local, e indicamos sus metadatos locales
            LocalRemote lr = new LocalRemote();
            lr.presentInLocal = true;
            lr.hashLocal = c.hash;
            lr.timeMilisLocal = c.timeMilisLastModified + offset;

            // Para esta ruta, introducimos en el mapa su objeto LocalRemote, asi podremos compararlo posteriormente
            listaIndices.put(c.ruta, lr);
            
        });

        remoto.forEach(c -> {

            // En este caso, sabemos que estamos iterando la lista remota, por tanto sabemos que cada ruta que iteremos
            // tendra presencia remota

            // Dado a que ya hemos introducido valores a listaIndices, para esta ruta concreta puede que ya exista
            // un LocalRemote asociado (debido a que tuviera presencia local), asi que el LocalRemote sobre el que
            // tenemos que trabajar es con ese que habiamos creado previamente, pues contiene los metadatos locales
            // del archivo, y son necesarios para poder comparar local con remota posteriormente
            LocalRemote lr = (listaIndices.containsKey(c.ruta)) ? listaIndices.get(c.ruta) : new LocalRemote();

            // Indicamos a localRemote que tenemos presencia remota, e indicamos los metadatos remotos
            lr.presentInRemote = true;
            lr.hashRemoto = c.hash;
            lr.timeMilisRemote = c.timeMilisLastModified;

            // Añadimos el LocalRemote a la lista, por si no lo contuviera
            listaIndices.put(c.ruta, lr);

        });

        // Hay que tener en cuenta, que si estamos realizando la primera comprobacion, no se indican los metadatos
        // de los archivos





        // Iteramos la lista para saber que archivos hay que cargar/descargar/mas operaciones
        listaIndices.forEach( (a,b) -> {  // a=archivo (en String)  b=localRemote

            // Si el archivo tiene presencia remota, pero no local, no necesitamos comparar mas metadatos, ya que sabemos
            // que se tienen que descargar
            if(b.presentInRemote && !b.presentInLocal) {
                operaciones.put(new Archivo(a) , VariablesGlobales.DOWNLOAD);

                // Con return saltamos a la siguiente iteracion de foreach
                return;
            }

            // Analogamente al caso anterior, si el archivo tiene presencia local pero no remota, directamente lo cargamos
            if(b.presentInLocal && !b.presentInRemote) {
                operaciones.put(new Archivo(a) , VariablesGlobales.UPLOAD);

                // Con return saltamos a la siguiente iteracion de foreach
                return;
            }

            // A este punto llegamos si el archivo en cuestion tiene tanto presencia local como remota
            // Existen dos situaciones
            //     1- estamos realizando la primera iteracion de comparar listas, que no tienen metadatos. En este
            //        caso no tenemos metadatos que comparar, asi que al archivo le asignamos la operacion obtener
            //        mas informacion.
            //
            //     2- estamos realizando la segunda iteracion de comparar listas, que ya contienen metadatos. En este
            //        caso ya se pueden comparar los metadatos de los archivos, y determinar si se tienen que
            //        cargar/descargar/no hacer nada
            if(!comprobarMetadatos) {
                // Este bloque se ejecutara cuando el archivo indicado tenga presencia local y remota, y ademas
                // no se quiera comparar los metadatos del archivo
                operaciones.put(new Archivo(a) , VariablesGlobales.MORE_INFO);

                // Con return saltamos a la siguiente iteracion de foreach
                return;
            }



            if(!b.hashLocal.equals(b.hashRemoto)) {
                if(b.timeMilisLocal < b.timeMilisRemote) {
                    operaciones.put(new Archivo(a), VariablesGlobales.DOWNLOAD);
                } else {
                    operaciones.put(new Archivo(a), VariablesGlobales.UPLOAD);
                }
            }

        } );


        return operaciones;
    }



}
