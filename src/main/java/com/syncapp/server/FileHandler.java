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

public class FileHandler {

    private final HashMap< String, String> pathFromFileID;
    private final HashMap< String, String> fileIdFromPath;
    private final ArrayList<String> openedFiles;
    private final HashMap< String, LectorArchivos> openedFilesWriter;
    private static int globalFileId;


    public FileHandler(Path workingPath) {
        pathFromFileID = new HashMap<>();
        fileIdFromPath = new HashMap<>();
        globalFileId = 0;

        openedFiles = new ArrayList<>();
        openedFilesWriter = new HashMap<>();


        ArrayList<Archivo> fileList = Utilidades.listFiles(workingPath);
        if(fileList == null) {return;}


        fileList.forEach(c->{
            Path fullPath = Paths.get(workingPath.toString() , c.ruta);
            int fileId = globalFileId++;

            pathFromFileID.put(""+fileId, fullPath.toString());
            fileIdFromPath.put(fullPath.toString(), ""+fileId);
        });
    }


    public int getFileId(String path) {
        return Integer.parseInt( fileIdFromPath.get(path) );
    }

    public String getStringFilePath(int fileId) {
        return pathFromFileID.get(""+fileId);
    }

    public Path getFilePath(int fileId) {
        return Paths.get(pathFromFileID.get(""+fileId));
    }

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


        int fileId = globalFileId++;
        pathFromFileID.put("" + fileId, path);
        fileIdFromPath.put(path, "" + fileId);
        return fileId;
    }

    public boolean isFileOpened(String path) {
        return openedFiles.contains(path);
    }

    public int openFile(String path, String opMode) {

        // Si el archivo ya esta abierto, devolvemos -1
        if(isFileOpened(path)) {
            return -1;
        }

        int fileid = Integer.parseInt(fileIdFromPath.get(path));

        if(opMode.equals("rw")) {
            try{

                LectorArchivos la = new LectorArchivos(Paths.get(path), "rw", Integer.parseInt(fileIdFromPath.get(path))  );

                openedFiles.add(path);
                openedFilesWriter.put(path, la);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        return fileid;



    }


    public LectorArchivos getWriter(int fileId) {
        return openedFilesWriter.get(  pathFromFileID.get(""+fileId)  );
    }

    public void closeFile(int fileId) {
        try {
            openedFilesWriter.get( pathFromFileID.get(""+fileId) ).cerrarArchivo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        openedFiles.remove(""+fileId);
        openedFilesWriter.remove(  pathFromFileID.get(""+fileId)  );
    }

}
