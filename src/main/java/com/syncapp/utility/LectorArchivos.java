package com.syncapp.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import com.syncapp.model.BloqueBytes;

public class LectorArchivos {
    Path path;
    RandomAccessFile raf;
    long posittion; //cambiar mas tarde

    public int ultimoEnviado;
    public static int MAX_BYTES = Utilidades.MAX_BYTES_IN_BLOCK; //80kBytes aunque se puede aumentar
    public int id_file;

    

    /**
     * 
     * @param path
     * @param op_mode
     * @throws IOException 
     * @throws FileNotFoundException 
     *         
     */
    public LectorArchivos(Path path, String op_mode, int id_file) throws IOException { //r:read w:write rw:read and write
        if(id_file <0 ) return;

        //SOLUCION TEMPORAL
        if(op_mode.equals("rw")){
            if (!Files.exists(path)) {
                Path tmp = path.getParent();
                Files.createDirectories(tmp);
                // Files.setAttribute(path, op_mode, op_mode, null)
                Files.createFile(path);
                // Files.setPosixFilePermissions(path, Util.getPerms());
            } else {
                Files.delete(path);
                Files.createFile(path);
                // Files.setPosixFilePermissions(path, Util.getPerms());
            }
        } else {
            if (!Files.exists(path)) {
                Path tmp = path.getParent();
                Files.createDirectories(tmp);
                Files.createFile(path);
                // Files.setPosixFilePermissions(path, Util.getPerms());
            }
        }

        this.id_file = id_file;
        


        this.path = path;
        raf = new RandomAccessFile(path.toFile(), "rw");
        posittion = 0;
        ultimoEnviado = -1;
    }

    public void cerrarArchivo() throws IOException {
        if(raf != null && raf.getFilePointer() != 0) {
            raf.close();   
        }
    }
    

   


    public BloqueBytes leerSiguienteBloque() throws IOException {
        return leerBloqueBytesEnPosicion(-1); //Ahorramos codigo, indicando -1 le decimos que no mueva el offset
    }

    //importante, cuando movemos el offset, volvemos a empezar a leer desde ese punto?
    public BloqueBytes leerBloqueBytesEnPosicion(long posicion) throws IOException { //Indicar -1 cuando no se quiere desplazar el offset
        if(path == null || !path.toFile().exists() || raf == null) return null;


        try {
            // System.out.println("raf position="+raf.getFilePointer()+", file position="+posittion); //TESTS
            if(posicion >= 0 || raf.getFilePointer() != posittion) {
                // Comprobamos rac.getFilePointer, que es la posicion real de RandomAccessFile
                // con la posicion que nosotros sabemos que hemos leido correctamente
                // esta es una buena practica para saber si al leer anteriormente se quedo a
                // mitad de proceso, asi que tomamos el valor que deberia tener
                // y comenzamos por ahi
                raf.seek(posittion);
            }
            
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        
        int realBytes = 0;
        byte[] bytes = new byte[Utilidades.MAX_BYTES_IN_BLOCK];

        realBytes = raf.read(bytes); //Lectura
        if(realBytes < 1) return null;
        

        byte[] realinfo;
        if(realBytes != bytes.length) {
            realinfo = new byte[realBytes];
            // for (int i = 0; i < realinfo.length; i++) {
            //     realinfo[i] = bytes[i];
            // }
            System.arraycopy(bytes, 0, realinfo, 0, realBytes);


        } else {
            realinfo = bytes;
        }


        BloqueBytes chunk = new BloqueBytes();
        chunk.data = realinfo;
        chunk.position = posittion;
        chunk.size = realinfo.length; 
        chunk.id = ++ultimoEnviado;
        chunk.id_file = id_file;

        System.out.println(chunk.toString()); //TESTS
        // System.out.println("position in lector="+posittion); //TESTS
        posittion += realBytes; //Actualizamos position al siguiente byte a leer
        

        return chunk;
    }



    public void escribirBloqueBytesEnPosicion(BloqueBytes bb, int pos) throws IOException {
        if (path == null || bb == null || bb.size < 1 || raf == null) return;


        if(pos > -1) {
            raf.seek(bb.position);
        }


        raf.write(bb.data);

        //si llegamos a este punto se han escrito bien los bytes (no salta excepcion) actualizamos valor de position
        System.out.println(bb.toString()); //TESTS
        posittion += bb.size;
    }



    public void escribirBloqueBytes(BloqueBytes bb) throws IOException { 
        escribirBloqueBytesEnPosicion(bb, -1);
    }







}
