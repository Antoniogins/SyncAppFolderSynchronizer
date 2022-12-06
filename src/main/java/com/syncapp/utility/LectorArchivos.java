package com.syncapp.utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import com.syncapp.model.BloqueBytes;


/**
 * Clase que permite leer y escribir archivos mediante {@link BloqueBytes bloques de bytes}.
 */
public class LectorArchivos {





    // Parametros

    /**
     * {@link Path} sobre el que estamos trabajando.
     */
    Path path;

    /**
     * {@link RandomAccessFile} para poder leer el archivo en bloques sin necesidad de leerlos secuencialmente.
     */
    RandomAccessFile raf;

    /**
     * {@link Long posicion} del lectores, esto es, el siguiente byte que se va a leer/escribir.
     */

    /**
     * {@link Integer ultimo enviado} nos sirve para tener un poco de control sobre el ultimo bloque que se ha enviado.
     */

    /**
     * {@link Integer ID} del archivo que se esta leyendo/escribiendo
     */
    public int id_file;


















    // Constructores

    /**
     * Para crear un lector de archivos, se necesita indicar la ruta completa del archivo, el modo de operacion,
     * "r" para lectura y "rw" para escritura; y el identificador del archivo. <br>
     * Al crear el lector de archivos, se comprobara si el archivo existe, y en caso de que no exista, se crea el archivo.
     * Para ello, si los directorios padres no existen, tambien se crean. Esto se debe a que si estamos descargando
     * un archivo que no existe en nuestra maquina, necesitamos crear todas las rutas precedentes al archivo, en caso
     * de que no existan.
     * @param path
     * @param op_mode
     * @param id_file
     * @throws IOException
     */
    public LectorArchivos(Path path, String op_mode, int id_file) throws IOException { //r:read rw:read and write
        if(id_file <0 ) return;

        //SOLUCION TEMPORAL

        // Comprobamos si el modo de operacion es escritura
        if(op_mode.equals("rw")){

            // Comprobamos si el archivo no existe
            if (!Files.exists(path)) {
                // En este punto sabemos que el archivo no existe, asi que lo mas probable es que los directorios superiores
                // no existan, por tanto los creamos. Esta operacion es segura, ya que no se intentara crear todos
                // los directiores, unicamente creara aquellos que no existan, por tanto, no va a intentar crear directorios
                // que ya existen, lo cual produciria un error

                // Obtenemos el directorio padre del archivo
                Path tmp = path.getParent();

                // Creamos todos los directorios superiores que no existan
                Files.createDirectories(tmp);

                // Finalmente creamos el archivo
                Files.createFile(path);

            }
            // Si el archivo ya existia, lo eliminamos y lo volvemos a crear. En este punto no hace falta que creemos
            // los directorios padre, ya que al existir el archivo, existiran los directorios padres.
            else {
                Files.delete(path);
                Files.createFile(path);
            }
        }

        // Si el modo de operacion es modo lectura no tenemos que realizar ninguna operacion extra, ya que si el archivo
        // no existe, al crear el RandomAccessFile se lanzara una excepcion


        // Establecemos los parametros
        this.id_file = id_file;
        this.path = path;

        // Inicializamos el RandomAccessFile
        raf = new RandomAccessFile(path.toFile(), op_mode);
    }















    // Finalizadores de servicio

    /**
     * Este metodo nos permite cerrar un archivo de forma segura, liberando asi los recursos.
     * @throws IOException si ocurre un problema durante el cierre del archivo.
     */
    public void cerrarArchivo() throws IOException {
        if(raf != null) {
            raf.close();
            raf = null;
        }
    }
    


























    // Transmision de datos

    /**
     * Con este metodo podemos leer un {@link BloqueBytes} del archivo con el que se creo el {@link LectorArchivos},
     * indicando la {@link Long posicion} a partir de la cual queremos leer. Para leer bloques es necesario indicar la
     * posicion de lectura, pues esta no se almacena, ya que varios usuarios pueden leer del mismo archivo simultaneamente.
     * <br>
     * Un dato importante, es que cuando se lee el ultimo bloque de bytes, se creara un {@link BloqueBytes} de tamaño distinto
     * al indicado en {@link VariablesGlobales#MAX_BYTES_IN_BLOCK}, porque no se deben llenar archivos con bytes vacios,
     * pues esto cambiara totalmente su hash. <br>
     * Si el archivo no tiene mas bytes para leer, se devuelve null.
     * @param posicion {@link Long} indicando el primer byte que se quiere leer.
     * @return {@link BloqueBytes} del tamaño de bytes que se hayan podido leer. Si hay suficientes bytes, se leeran hasta
     * {@link VariablesGlobales#MAX_BYTES_IN_BLOCK}, en caso contrario, se leeran todos los bytes que queden.
     * @throws IOException si ocurre un problema al leer el archivo.
     */
    public BloqueBytes leerBloqueBytes(long posicion) throws IOException {
        if(path == null || !path.toFile().exists() || raf == null) return null;


        try {
            // Posicionamos el RandomAcccessFile en la posicion que queremos leer
            raf.seek(posicion);

            
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Esta variable la usaremos para saber cuantos bytes se han leido
        int realBytes = 0;

        // Creamos un array en el que leermos los bytes del archivo, de tamaño unicial MAX_BYTES
        byte[] bytes = new byte[VariablesGlobales.MAX_BYTES_IN_BLOCK];

        // Leemos el archivo en el array
        realBytes = raf.read(bytes); //Lectura

        // Comprobamos si se han leido algun byte, en caso contrario devolvemos null
        if(realBytes < 1) return null;

        // Creamos la variable realInfo, en la que guardaremos los bytes que contienen informacion tras leer
        byte[] realinfo;

        // Comprobamos si se han leido MAX_BYTES o menos. En caso que se hayan leido menos bytes que MAX_BYTES
        // copiamos los bytes que contienen informacion al array realInfo, para guardar exclusivamente esos bytes
        if(realBytes != bytes.length) {
            realinfo = new byte[realBytes];
            System.arraycopy(bytes, 0, realinfo, 0, realBytes);
        } else {
            realinfo = bytes;
        }


        // Creamos el bloque de bytes que estan pidiendo
        BloqueBytes chunk = new BloqueBytes();
        chunk.data = realinfo;
        chunk.position = posicion;
        chunk.size = realinfo.length;
        chunk.fileID = id_file;

//        System.out.println(chunk); //TESTS

        return chunk;
    }




    /**
     * Con este metodo podemos escribir un {@link BloqueBytes} dentro de un archivo. El bloque debe contener en su interior
     * la posicion en la que quiere escribir.
     * <br>
     * Se escribiran hasta {@link BloqueBytes#size} bytes de datos.
     * @param bb
     * @throws IOException
     */
    public void escribirBloqueBytes(BloqueBytes bb) throws IOException {
        if (path == null || bb == null || bb.size < 1 || raf == null) return;

        // Movemos el puntero del RandomAccessFile hasta la posicion del bloque
        raf.seek(bb.position);

        // Escribimos el array de bytes que contiene
        raf.write(bb.data);

        System.out.println("escribiendo el bloque"+bb); //TESTS
    }




    public void setFileId(int fileId) {
        this.id_file = fileId;
    }








}
