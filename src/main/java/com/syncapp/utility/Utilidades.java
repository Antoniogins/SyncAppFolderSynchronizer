package com.syncapp.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
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
        Path real = pathToList.normalize();

        // Iteramos el directorio indicado
        try {
            Files.walk(real).forEach(c -> {

                // Dado que unicamente queremos poner al archivo la ruta respecto al path de original, lo relativizamos
                // Por ejemplo: real = /home/usuario/carpeta
                //                 c = /home/usuario/carpeta/documentos/renta.pdf
                //                 m = documentos/renta.pdf
                Path m = real.relativize(c);

                // Si el path c corresponde a un archivo, y su tamaño es mas de 0bytes, lo añadimos a la lista de archivos
                long fileSize = c.toFile().length();
//                System.out.println(fileSize);
                if (c.toFile().isFile() && fileSize > 0){
//                    System.out.println("tiene mas de 0bytes asi que añado");
                    Archivo toAdd = new Archivo(m, real);
                    toAdd.sizeInBytes = fileSize;
                    lista.add(toAdd);
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
     * Este metodo nos permite obtener metadatos de archivos de la maquina que lo ejecuta. Estos metadatos se
     * obtienen para cada uno de los archivos de la lista de entrada. Los metadatos que se obtienen son: hashmd5
     * ({@link String}) y la ultima fecha/hora de modificacion ({@link Long})
     * <br>
     * Dado que los archivos contienen rutas relativas (a la carpeta de sincronizacion), es necesario que se indique
     * dicha carpeta para acceder poder acceder al archivo real.
     *
     * @param lista {@link ArrayList}<{@link Archivo}> lista de los que se quiere obtener metadatos.
     * @param containerFolder carpeta contenedora de esos archivos (directorio de la carpeta de sincronizacion)
     * @return {@link ArrayList}<{@link Archivo}> lista de los archivos pedidos, pero contienen los metadatos.
     */
    public static ArrayList<Archivo> obtenerMultiplesMetadatos(ArrayList<Archivo> lista, Path containerFolder) {

        // Comprobamos que la lista contenga elementos
        if(lista == null || lista.size() == 0) {
            return null;
        }

        // Creamos una lista temporal en la que añadiremos los archivos conforme vayamos leyendo
        ArrayList<Archivo> listaFinal = new ArrayList<>();

        // Para cada uno de los archivos de entrada, calculamos el hash y obtenemos la ultima fecha de modificacion.
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
     * Este metodo nos permite obtener los metadatos de un archivo en concreto.
     * <br>
     * Se genera un checksum tipo md5, realizado mediante la clase {@link MessageDigest}.<br>
     * Este MessageDigest consume todos los bytes que tiene un archivo (los vamos introduciendo por bloques) y
     * tras consumir todos, genera un array de bytes representando el hash. Se necesita transformar ese array de bytes
     * en un String, para ello usamos un {@link StringBuilder}, que convertira cada uno de los bytes archivo un caracter y
     * construira la cadena de texto.
     * <br>
     * Para la ultima fecha de modificacion, el sistema de archivos almacena automaticamente la ultima vez que se ha
     * modificado ese archivo, asi que se obtiene usando una clase, y se obtiene en forma de milisegundos. Estos
     * milisegundos, son una cuenta en milisegundos desde 1970, y que nos permite sincronizarnos a una hora concreta.
     * <br>
     * Para comparar las horas de modificacion, se tiene en cuenta el offset de tiempo que el cliente tiene respecto
     * al servidor, para ello, inicialmente, se ha ejecutado el Algoritmo de Cristian para obtener nuestro offset
     * respecto al servidor.
     * @param archivo {@link Archivo} sobre el que se quiere consultar los metadatos.
     * @param workingFolder carpeta de trabajo del cliente, ya que las rutas de los archivos son relativas a esa carpeta.
     * @return {@link Archivo} con los metadatos en su interior.
     * @throws IOException si ocurre un problema al leer los metadatos del fichero.
     */
    public static Archivo obtenerMetadatos(Archivo archivo, Path workingFolder) throws IOException {

        // Creamos un nuevo archivo, que sera donde incluiremos los metadatos
        Archivo deVuelta = new Archivo(  archivo.toRelativePath(), workingFolder);

        // Obtenemos el hash del fichero y se lo añadimos al fichero de vuelta
        deVuelta.hash = checkSumhash( deVuelta.toFile() );

        // Obtenemos la hora de ultima modificacion
        FileTime ft = Files.getLastModifiedTime( deVuelta.toPath() );

        // Le añadimos la hora al archivo de vuelta
        deVuelta.timeMilisLastModified = ft.toMillis();

        return deVuelta;
    }



    /**
     * Este metodo nos permite crear un checksum a un File de entrada, usango el
     * algoritmo md5.<br>
     * Para ello, el {@link File archivo} de entrada se separa en bloques de 800KB (para mejorar la rapidez del algoritmo)
     * y lo pasamos al obtejo {@link MessageDigest} para que lo consuma y vaya produciendo el hash. Una vez que se
     * ha consumido el total de bytes del archivo, se invoca el metodo {@link MessageDigest#digest()} que nos devolvera
     * un array de bytes representativo del hash. Este array de bytes lo tenemos que convertir a un String, dado que
     * es como se representa un hashmd5. Para ello usamos un {@link StringBuilder}, al que le pasaremos el array de bytes,
     * y le indicaremos una funcion que convierte esos bytes en texto.
     * 
     * @param f {@link File} al que le realizaremos el md5sum.
     * @return un String que contiene el resultado del algoritmo md5.
     * @throws FileNotFoundException si existe el archivo indicado
     * @throws IOException           cuando se produce un error al leer el archivo
     */
    public static String checkSumhash(File f) {

        // Comprobamos errores antes de intentar realizar ninguna operacion
        if (f == null || !f.exists()){
            return  null;
        }

        // Obtenemos el RandomAccessFile para poder leer el archivo
        RandomAccessFile raf = null;

        try {
            // Indicamos "rw" como modo de apertura, aunque seria valid con "r"
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

            // Mientras que bytesRead sea distinto de null (lo cual indica que no quedan mas bytes por leer del archivo)
            while ((bytesRead = raf.read(chunk)) != -1) { // Cuando no hay mas bytes por leer devuelve -1
                if (bytesRead != chunk.length) {

                    // Normalmente se dara el caso en que el tamaño del array no coincida con la cantidad de bytes que
                    // habia disponibles para leer. Es MUY IMPORTANTE que los bytes vacios sean eliminados, ya que que
                    // el algoritmo los consumira y producira un hash TOTALMENTE DIFERENTE

                    // Por tanto creamos un array del tamaño de bytes que se han leido, y copiamos esos bytes al nuevo
                    // array, que contendra la informacion real
                    byte[] fin = new byte[bytesRead];

                    // Indicamos que copie desde el chunk[0] hasta chunk[bytesRead] en fin[0] hasta fin[bytesRead]
                    System.arraycopy(chunk, 0, fin, 0, bytesRead);

                    // Actualizamos el hash generado
                    md5.update(fin);
                } else {

                    // Si se habian leido 800KB, simplemente actualizamos el hash con el array original
                    md5.update(chunk);
                }

            }

            // Finalizamos el resultado del algoritmo, y lo almacenamos en un array
            byte[] result = md5.digest();

            // Construimos el String del hash
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
                // Cerramos el RandomAccessFile para que el recurso quede libre
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
     * Analogo a {@link #checkSumhash(File)} pero, en vez de trabajar sobre un fichero, se trabaja con un array de bytes,
     * que pueden ser un String convertido a bytes[], un entero, un Objeto, etc.
     * <br>
     * Este metodo nos permite generar identificadores unicos seguros para el manejo de constraseñas e informacion
     * sensible (no implementado)
     * @param bytes {@link Byte[]} array de bytes sobre el que calcular el md5.
     * @return {@link String} que representa el hash md5.
     */
    public String hashmd5(byte[] bytes) {
        try {

            // Analogo a la funcion anterior, pero mas sencilla, consulte la funcion checksumhash()

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

    /**
     * Este metodo se encarga de comparar dos listas de archivos, y decidir cual es la operacion que debe realizar el
     * cliente que invoca el metodo, sobre los archivos indicados en las listas.<br>
     * Tiene dos modos de operacion:
     * <ul>
     *     <li>
     *         Sin comprobacion de metadatos: compara las listas y decide las operaciones exclusivamente en funcion
     *         de que archivos estan presentes en una lista u otra.
     *     </li>
     *     <li>
     *         Con comprobacion de metadatos: comapra las listas y decide las operaciones basandose en:
     *         <ol>
     *             <li>
     *                 Cual es la presencia de los archivos -> estan en remoto, en local o en ambas?
     *             </li>
     *             <li>
     *                 Cual es el hash en local y en remoto -> el archivo ha cambiado? si el archivo ha cambiado su hash
     *                 sera distinto en las dos listas.
     *             </li>
     *             <li>
     *                 Ultima fecha de modificacion -> si el archivo ha cambiado, cual es la ultima version? Sera la
     *                 mas reciente (cuya fecha sea mayor).
     *             </li>
     *         </ol>
     *     </li>
     *
     * </ul>
     * <br>
     * Una vez se sabe cual es la operacion, se añade en un {@link HashMap}<{@link Archivo},{@link Integer}> ,que indica
     * para cada archivo una operacion. Las operaciones estan definidas en la clase {@link VariablesGlobales}.
     * <br><br>
     * Un punto a tener en cuenta es la tecnica para realizar estas comparaciones: dado que en un entorno real estas
     * listas pueden contener de miles, incluso cientos de miles, de archivos; por tanto comparar elemento a elemento de la lista
     * se hace inviable. Por ello se usa un {@link HashMap}<{@link String},{@link LocalRemote}>. Esta clase LocalRemote,
     * es una clase auxiliar que permite almacenar si un archivo tiene presencia local, presencia remota, permite almacenar
     * su hash local, hash remoto, su fecha de ultima modificacion local y la remota. Y se usa un String para almacenar
     * ruta del archivo de forma inequivoca, pues, para la misma ruta, unicamente existira una entrada en el mapa.
     * <br>
     * El funcionamiento es el siguiente:
     * <ol>
     *     <li>
     *         Se recorre la lista de archivos locales. Para cada archivo de la lista creamos un {@link LocalRemote} indicando
     *         que presenciaLocal=true, hashLocal=hash, ultimaModificacionLocal=ultimaModificacion+<b>offset</b>. Es muy
     *         importante indicar el offset respecto al servidor, pues esto puede cambiar totalmente el escenario.
     *     </li>
     *     <li>
     *         Se recorre la lista de archivos remotos. Para cada archivo se comprueba si existe ya una entrada con ese
     *         archivo.
     *         <ul>
     *             <li>
     *                 Si ya habia una entrada, obtenemos el objeto LocalRemote e indicamos presenciaRemota=true,
     *                 hashRemoto=hash, ultimaModificacionRemota=ultimaModificacion, mateniendo los valores anteriores
     *                 de presenciaLocal, hashLocal y ultimaModificacionLocal. Este paso es muy importante, pues estariamos
     *                 omitiendo informacion.
     *             </li>
     *             <li>
     *                 Si no existia una entrada asociada con el archivo, creamos un nuevo objeto LocalRemote e introducimos
     *                 los mismo valores que en el paso anterior.
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Recorremos el mapa de archivos y localRemote. Para cada archivo comprobamos:
     *         <ol>
     *             <li>
     *                 Si tiene presencia local pero no remota, añadimos el archivo a la lista de operaciones, indicando
     *                 que la operacion es {@link VariablesGlobales#UPLOAD}.
     *             </li>
     *             <li>
     *                 Si tiene presencia remota pero no local, añadimos el archivo a la lista de operaciones, indicando
     *                 que la operacion es {@link VariablesGlobales#DOWNLOAD}.
     *             </li>
     *             <li>
     *                 En caso que no se quieran comprobar metadatos (operaciones iniciales) el archivo lo añadimos a la
     *                 lista de operaciones con la operacion {@link VariablesGlobales#MORE_INFO}, para que posteriormente
     *                 se obtenga los metadatos y se procese el archivo.
     *             </li>
     *             <li>
     *                 Comprobamos si el hashLocal es distinto que el hashRemoto. Esto indica que los archivos han cambiado,
     *                 por tanto tenemos que comparar cual es el mas reciente.
     *             </li>
     *             <li>
     *                 Comprobamos si la ultima modificaion local es menor que la ultima modificacion remota, en ese caso
     *                 añadimos el archivo con operacion {@link VariablesGlobales#DOWNLOAD}, y en caso contrario
     *                 {@link VariablesGlobales#UPLOAD}.
     *             </li>
     *         </ol>
     *     </li>
     * </ol>
     * <br>
     * @deprecated - actualmente se implementa en cliente.
     * @param local {@link ArrayList}<{@link Archivo}> que se encuentran en la carpeta local del usuario.
     * @param remoto {@link ArrayList}<{@link Archivo}> que se encuentran en la carpeta remota del usuario.
     * @param offset diferencia de tiempo entre el cliente y el servidor.
     * @param comprobarMetadatos para indicar si unicamente queremos comprobar la presencia de los archivos en estas
     *                           listas, no sobre sus metadatos.
     * @return {@link HashMap}<{@link Archivo},{@link Integer}> la lista de operaciones que el cliente debe realizar
     * para poder sincronizar su carpeta local con su carpeta remota.
     */
    public static HashMap< Archivo, Integer> compararListas(ArrayList<Archivo> local, ArrayList<Archivo> remoto, long offset, boolean comprobarMetadatos) {
        HashMap<Archivo , Integer> operaciones = new HashMap<>();


        boolean directUpload = (remoto == null || remoto.size() < 1 )    &&   local.size() >= 1;
        //si no hay archivos remotos, pero si locales, cargamos todos los que hay en local sin comparar ningun parametro extra
        if(directUpload) {
            for(Archivo r : local) {
                operaciones.put(r, VariablesGlobales.UPLOAD);
            }
            return operaciones;
        }



        boolean directDownload = (local == null || local.size() < 1 )    && remoto.size() >= 1;
        //si no hay archivos locales, pero si remotos, descargamos todos los que hay en remoto sin comparar ningun parametro extra
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
            lr.sizeLocal = c.sizeInBytes;

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
            lr.sizeRemote = c.sizeInBytes;

            // Añadimos el LocalRemote a la lista, por si no lo contuviera
            listaIndices.put(c.ruta, lr);

        });

        // Hay que tener en cuenta, que si estamos realizando la primera comprobacion, no se indican los metadatos
        // de los archivos





        // Iteramos la lista para saber que archivos hay que cargar/descargar/mas operaciones
        listaIndices.forEach( (a,b) -> {  // a=archivo (en String)  b=localRemote
            Archivo fin = new Archivo(a);

            // Si el archivo tiene presencia remota, pero no local, no necesitamos comparar mas metadatos, ya que sabemos
            // que se tienen que descargar
            if(b.presentInRemote && !b.presentInLocal) {
                fin.sizeInBytes = b.sizeRemote;
                operaciones.put(fin , VariablesGlobales.DOWNLOAD);

                // Con return saltamos a la siguiente iteracion de foreach
                return;
            }

            // Analogamente al caso anterior, si el archivo tiene presencia local pero no remota, directamente lo cargamos
            if(b.presentInLocal && !b.presentInRemote) {
                fin.sizeInBytes = b.sizeLocal;
                operaciones.put(fin, VariablesGlobales.UPLOAD);

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
                fin.sizeInBytes = local.size();
                operaciones.put(fin, VariablesGlobales.MORE_INFO);

                // Con return saltamos a la siguiente iteracion de foreach
                return;
            }


            // Comparamos si el hash local es igual que el hash remoto. Si ambos hashes son identicos, es porque
            // el archivo no ha sido modificado en niguno de los equipos -> no realizamos niguna operacion sobre ese
            // archivo
            if(!b.hashLocal.equals(b.hashRemoto)) {

                // Comparamos la ultima modificacion local (con offset) vs la ultima modificacion remota.
                if(b.timeMilisLocal < b.timeMilisRemote) {
                    fin.sizeInBytes = b.sizeRemote;
                    operaciones.put(fin, VariablesGlobales.DOWNLOAD);
                } else {
                    fin.sizeInBytes = b.sizeLocal;
                    operaciones.put(fin, VariablesGlobales.UPLOAD);
                }
            }

        } );


        return operaciones;
    }



}
