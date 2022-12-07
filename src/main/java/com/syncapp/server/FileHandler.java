package com.syncapp.server;

import com.syncapp.model.Archivo;
import com.syncapp.utility.LectorArchivos;
import com.syncapp.utility.Utilidades;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Esta clase nos permite controlar los archivos que se encuentran en el servidor. Para ello, acada archivo (ruta) se le
 * asigna un identificador unico, y se almacena en registros.
 * <br>
 * Ademas se encarga de comprobar sobre la existencia de archivos y archivos que estan siendo escritos (para que no
 * colapsen), lo cual ayuda a obtener un nivel extra de abstraccion a la hora de implementar la gestion de archivos en el servidor.
 */
public class FileHandler {






    // Registros

    /**
     * Se almacena la pareja {@link String fileId}/{@link String ruta}. Esto nos permite obtener la ruta de un archivo
     * a partir de su identificador unico.
     */
    private final HashMap< String, String> pathFromFileID;

    /**
     * Se almacena la pareja {@link String ruta}/{@link String fileId}. Esto nos permite obtener un fileId a partir de
     * una ruta.
     */
    private final HashMap< String, String> fileIdFromPath;

    /**
     * Almacena la {@link String ruta} de los archivo que estan siendo escritos. Esto es especialmente util cuando varias
     * sesiones intentan escribir un archivo de forma simultanea, lo cual esta prohibido dentro de nuestro servicio.
     */
    private final ArrayList<String> openedFiles;

    /**
     * Almacen de {@link LectorArchivos lectores de archivos}, almacena la pareja {@link String ruta}/{@link LectorArchivos},
     * para cuando un cliente quiere escribir un bloque de datos, se pueda obtener facilmente el lector.
     */
    private final HashMap< String, LectorArchivos> openedFilesWriter;

    /**
     * Variable global, que se usa como contador de archivos, para asignar una id unica a cada archivo.
     */
    private static int globalFileId;























    // Constructor

    /**
     * Constructor de FileHander. FileHandler se debe crear a partir de una ruta de trabajo, pues cuando abrimos un
     * archivo, estamos usando una ruta relativa a la ruta de trabajo.
     * <br>
     * En el constructor iniciaremos los registros indicados anteriormente, y recorreremos la ruta de trabajo indicada
     * para realizar un primer barrido de los archivos que existen, y asignarels un identificador unico.
     * @param workingPath ruta de trabajo del servidor.
     */
    public FileHandler(Path workingPath) {
        pathFromFileID = new HashMap<>();
        fileIdFromPath = new HashMap<>();
        globalFileId = 0;

        openedFiles = new ArrayList<>();
        openedFilesWriter = new HashMap<>();


        ArrayList<Archivo> fileList = Utilidades.listFiles(workingPath);
        if(fileList == null) {return;}

        // Recorremos la ruta de trabajo para saber que archivos ya existen.
        fileList.forEach(c->{
            Path fullPath = Paths.get(workingPath.toString() , c.ruta);
            int fileId = globalFileId++;

            pathFromFileID.put(""+fileId, fullPath.toString());
            fileIdFromPath.put(fullPath.toString(), ""+fileId);
        });
    }

























    // Getters

    /**
     * Este metodo nos permite obtener el identificador de un archivo a partir de su ruta relativa (usuario+rutaArchivo).
     * @param path del archivo cuyo identificador queremos obtener.
     * @return {@link Integer identificador} del archivo en cuestion.
     */
    public int getFileId(String path) {
        return Integer.parseInt( fileIdFromPath.get(path) );
    }

    /**
     * Este metodo nos permite obtener la ruta (en forma de String) de un archivo a partir de su identificador unico.
     * @param fileId {@link Integer identificador} del archivo.
     * @return {@link String ruta} del archivo en cuestion.
     */
    public String getStringFilePath(int fileId) {
        return pathFromFileID.get(""+fileId);
    }

    /**
     * Este metodo nos permite obtener la ruta (en forma de Path) de un archivo a partir de su identificador unico.
     * @param fileId {@link Integer identificador} del archivo.
     * @return {@link Path ruta} del archivo en cuestion.
     */
    public Path getFilePath(int fileId) {
        return Paths.get(pathFromFileID.get(""+fileId));
    }


    /**
     * Este metodo nos permite obtener el {@link LectorArchivos} asociando a un archivo.
     * @param fileId {@link String identificador} unico del archivo.
     * @return {@link LectorArchivos} asociado al archivo.
     */
    public LectorArchivos getWriter(int fileId) {
        return openedFilesWriter.get(  pathFromFileID.get(""+fileId)  );
    }





























    // Iniciadores

    /**
     * Este metodo nos permite abstraernos de la creacion de un archivo. Para crear un archvo necesitamos conocer su
     * ruta, la cual se indicara en forma de String.
     * <br>
     * Para ello, el metodo comprueba:
     * <ul>
     *     <li>
     *         Si el archivo existe: lo eliminamos y creamos uno vacio. Esto se debe a que este metodo lo usaremos
     *         cuando queramos sobreescribir un archivo.
     *     </li>
     * </ul>
     * <br>
     * En cualquier caso, se devuelve el identificador unico del archivo, y quedara almacenado en las listas de archivos.
     * @param path {@link String ruta} del archivo que queremos crear.
     * @return {@link Integer identificador} del archivo en cuestion.
     */
    public int newFile(String path) {
        Path toWork = Paths.get(path);

        if(Files.exists(toWork)) {

            // Si el archivo ya existia, lo borramos y lo creamos desde cero
            try {
                Files.delete(toWork);
                Files.createFile(toWork);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Integer.parseInt(fileIdFromPath.get(path));
        }

        // En caso de que no existiera, lo añadimos a los registros
        int fileId = globalFileId++;
        pathFromFileID.put("" + fileId, path);
        fileIdFromPath.put(path, "" + fileId);

        try {
            Files.createFile(toWork);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileId;
    }


    /**
     * Este metodo nos permite abrir un archivo en modo escritura. Para ello comprueba que no este abierto, en cuyo caso
     * crea un nuevo {@link LectorArchivos} asociado a ese archivo, y devuelve el identificador unico del archivo.
     * En el caso que el archivo ya estuviera abierto, se devuelve -1.
     * @param path {@link String ruta} del archivo que queremos abrir.
     * @param opMode {@link String modo} de operacion: "rw" para escribir.
     * @return {@link String identificador} del archivo.
     */
    public int openFile(String path, String opMode) {

        // Si el archivo ya esta abierto, devolvemos -1
        if(isFileOpened(path)) {
            return -1;
        }


        // Obtenemos el identificador del archivo
        int fileid;
        if(!fileIdFromPath.containsKey(path)) {
            fileid = newFile(path);
        } else {
            fileid = Integer.parseInt(fileIdFromPath.get(path));
        }



        if(opMode.equals("rw")) {
            try{

                // Si se ha abierto en modo escritura, creamos el nuevo lector y lo almacenamos
                LectorArchivos la = new LectorArchivos(Paths.get(path), "rw", Integer.parseInt(fileIdFromPath.get(path))  );

                // Registramos el archivo como abierto, para que no lo intenten abrir posteriormente
                openedFiles.add(path);
                openedFilesWriter.put(path, la);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        return fileid;



    }



































    // Comprobadores

    /**
     * Este metodo nos permite comprobar sin un archivo esta siendo abierto para escritura.
     * @param path {@link String ruta} del archivo que queremos comprobar.
     * @return {@link Boolean} indicando si esta abierto.
     */
    public boolean isFileOpened(String path) {
        return openedFiles.contains(path);
    }
























    // Finalizadores

    /**
     * Este metodo nos permite cerrar un archivo, para ellos, cerramos su {@link LectorArchivos} y borramos el
     * archivo de los registros, para que quede libre para otras sesiones.
     * @param fileId {@link String identificador} del archivo que queremos cerrar.
     */
    public void closeFile(int fileId) {
        // Cerramos el lector de archivos
        try {
            openedFilesWriter.get( pathFromFileID.get(""+fileId) ).cerrarArchivo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Removemos el archivo de los registros de archivos activos
        openedFiles.remove(""+fileId);
        openedFilesWriter.remove(  pathFromFileID.get(""+fileId)  );
    }

}
