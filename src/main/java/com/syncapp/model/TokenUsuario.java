package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;

import java.io.Serializable;

// import org.json.JSONObject;

public class TokenUsuario implements Serializable{
    public final String name;
    public String session_id;


    public TokenUsuario(String name) {
        this.name = new String(name);
    }

    @Override
    public String toString() {
        return VariablesGlobales.COLOR_GREEN +"["+name+"#"+session_id+"]"+VariablesGlobales.COLOR_WHITE;
    }
}
