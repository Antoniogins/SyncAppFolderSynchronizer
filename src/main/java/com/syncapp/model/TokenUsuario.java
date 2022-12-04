package com.syncapp.model;

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
        return "["+name+"#"+session_id+"]";
    }
}
