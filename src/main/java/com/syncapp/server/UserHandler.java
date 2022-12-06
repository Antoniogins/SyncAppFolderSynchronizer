package com.syncapp.server;

import com.syncapp.model.TokenUsuario;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Esta clase nos permite controlar los usuarios que han iniciado sesion, los archivos que han abierto, etc.
 */
public class UserHandler {


    /**
     * Almacena un {@link TokenUsuario} para un {@link String sessionId}.
     */
    HashMap< String, TokenUsuario> userFromSessionId;

    /**
     * Almacena un {@link String sessionId} para un {@link String nombre de usuario}.
     */
    HashMap< String, String> sessionIdFromUsername;

    /**
     * Almacena todos los {@link String idSesiones} que se han usado, para no asignar dos mismos id a diferentes
     * usuarios, ya que los id son aleatorios.
     */
    ArrayList<String> memoriaDeSesiones;

    HashMap< String, String > archivosActivosEnSesion; //fileid - usuario


    public UserHandler(){
        userFromSessionId = new HashMap<>();
        sessionIdFromUsername = new HashMap<>();
        memoriaDeSesiones = new ArrayList<>();
        archivosActivosEnSesion = new HashMap<>();

    }


    public String iniciarSesion(TokenUsuario usuario) {
        String id = "";
        do {
            id = ""+( (int) (Math.random()*10000) );
        } while (memoriaDeSesiones.contains(id));


        userFromSessionId.put(id, usuario);
        sessionIdFromUsername.put(usuario.name, id);
        memoriaDeSesiones.add(id);


        return id;
    }


    public void abrirArchivo(String sessionId, int fileId) {
        archivosActivosEnSesion.put(""+fileId, sessionId);
    }

    public boolean isOpen(int fileId) {
        return archivosActivosEnSesion.containsKey(""+fileId);
    }

    public void cerrarSesion(String sessionId) {
        archivosActivosEnSesion.forEach( (a,b) -> {
            if(b.equals(sessionId)) {
                archivosActivosEnSesion.remove(a);
            }
        } );

        sessionIdFromUsername.remove( userFromSessionId.get(sessionId).name );
        userFromSessionId.remove(sessionId);
        memoriaDeSesiones.remove(sessionId);
    }

    public void cerrarArchivo(int fileId) {
        archivosActivosEnSesion.remove(""+fileId);
    }

    public boolean isSessionActive(String sessionId) {
        return memoriaDeSesiones.contains(sessionId);
    }

    public ArrayList<Integer> listFilesForSession(String sessionId) {
        ArrayList<Integer> listaArchivosAbierto = new ArrayList<>();
        archivosActivosEnSesion.forEach( (a,b) -> {
            if(b.equals(sessionId)) {
                listaArchivosAbierto.add(  Integer.parseInt(a)  );
            }
        } );

        return listaArchivosAbierto;
    }






}
