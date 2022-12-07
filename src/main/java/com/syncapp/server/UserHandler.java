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

    /**
     * Almacena un {@link HashMap} de {@link String identificador de archivo}/{@link String nombre de usuario} que lo ha
     * abierto. Esto es util cuando un usuario cierra sesion mientras tiene archivos abiertos en escritura, y poder
     * liberar esos archivos para que otras sesiones puedan abrirlos.
     */
    HashMap< String, String > archivosActivosEnSesion; //fileid - usuario












    // Constructor

    /**
     * Constructor de {@link UserHandler} inicia los registros de los usuarios.
     */
    public UserHandler(){
        userFromSessionId = new HashMap<>();
        sessionIdFromUsername = new HashMap<>();
        memoriaDeSesiones = new ArrayList<>();
        archivosActivosEnSesion = new HashMap<>();

    }
















    // Iniciadores

    /**
     * Este metodo permite a un usuario iniciar una sesion en el servidor. Para ellos, se genera un {@link String identificador}
     * unico de sesion, que se devolvera posteriormente al usuario. Este identificador es unico y aleatorio.
     * @param usuario {@link TokenUsuario} que quiere iniciar sesion.
     * @return {@link String identificador} unico de la sesion.
     */
    public String iniciarSesion(TokenUsuario usuario) {

        // Generamos un identificador hasta que comprobemos que ese identificador no esta en uso.
        String id = "";
        do {
            id = ""+( (int) (Math.random()*10000) );
        } while (memoriaDeSesiones.contains(id));

        // Añadimos el usuario y su identificador de sesion a los registros de usuarios
        userFromSessionId.put(id, usuario);
        sessionIdFromUsername.put(usuario.name, id);
        memoriaDeSesiones.add(id);

        // Devolvemos el identificador
        return id;
    }


    /**
     * Este metodo permite enlazar un archivo con una sesion.
     * @param sessionId {@link String identificador} de la sesion que quiere abrir el archivo.
     * @param fileId {@link String identificador} del archivo que se quiere abrir.
     */
    public void abrirArchivo(String sessionId, int fileId) {
        archivosActivosEnSesion.put(""+fileId, sessionId);
    }






















    // Comprobadores

    /**
     * Este metodo comprueba si un archivo esta abierto por otra sesion.
     * @param fileId {@link String identificador} del archivo que quermos comprobar.
     * @return {@link Boolean} indicando si el archivo esta abierto(true) o no.
     */
    public boolean isOpen(int fileId) {
        return archivosActivosEnSesion.containsKey(""+fileId);
    }

    /**
     * Este metodo nos permite comprobar si una sesion esta activa.
     * @param sessionId {@link String identificador} de la sesion.
     * @return {@link Boolean} indicando si la sesion esta activa (true) o no.
     */
    public boolean isSessionActive(String sessionId) {
        return memoriaDeSesiones.contains(sessionId);
    }















    // Finalizadores

    /**
     * Este metodo nos permite cerrar de forma segura una sesion, cerrando todos los archivos asociados a la sesion.
     * @param sessionId {@link String identificador} de la sesion.
     */
    public void cerrarSesion(String sessionId) {
        // Para cada archivo, comprobamos si la sesion que ha abierto el archivo es la sesion que queremos cerrar
        archivosActivosEnSesion.forEach( (a,b) -> {
            if(b.equals(sessionId)) {
                archivosActivosEnSesion.remove(a);
            }
        } );

        // Removemos la sesion de todos los registros
        sessionIdFromUsername.remove( userFromSessionId.get(sessionId).name );
        userFromSessionId.remove(sessionId);
        memoriaDeSesiones.remove(sessionId);
    }

    /**
     * Este metodo nos permite cerrar un archivo que hubiera abierto una sesion.
     * @param fileId {@link String identificador} del archivo.
     */
    public void cerrarArchivo(int fileId) {
        archivosActivosEnSesion.remove(""+fileId);
    }




















    // Obtener recursos

    /**
     * Este metodo nos permite obtener una lista de todos los archivos que estan abiertos por una sesion.
     * @param sessionId {@link String identificador} de la sesion.
     * @return {@link ArrayList} {@link String identificador} de los archivos abiertos por la sesion.
     */
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
