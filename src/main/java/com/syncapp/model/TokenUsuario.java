package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;
import java.io.Serializable;


/**
 * Esta clase representa a un usuario dentro del servicio. Un usuario puede usar el servicio desde multiples maquinas (o
 * procesos) simultaneamente, para ello, cada usuario puede tener multiples sesiones. En este objeto (que sera unico para
 * cada proceso) se almacenara tanto el usuario como la sesion que le corresponde a ese proceso.<br>
 * De esta clase se puede implementar un control de acceso, uso de contraseñas almacenadas por hash y pseudo hashs, pero
 * requeriria un mayor control y encriptacion de la informacion, lo cual abarca mas alla de este proyecto.
 */
public class TokenUsuario implements Serializable{

    /**
     * Almacena el nombre de usuario con el que se quiere trabajar.
     */
    public final String name;

    /**
     * Contiene el identificador de sesion que el servidor le proporcione. Al principio esta vacio, hasta que se inicie
     * sesion.
     */
    public String session_id;


    /**
     * Crea un TokenUsuario a partir del nombre de usuario.
     * @param name {@link String} que representa el nombre de usuario. Puede ser cualquier cadena de caracteres.
     */
    public TokenUsuario(String name) {
        this.name = new String(name);
    }

    /**
     * Nos permite tener una representacion visual del token de usuario.
     * @return {@link String} cadena de texto representativa del token.
     */
    @Override
    public String toString() {
        return VariablesGlobales.COLOR_GREEN +"["+name+"#"+session_id+"]"+VariablesGlobales.COLOR_WHITE;
    }
}
