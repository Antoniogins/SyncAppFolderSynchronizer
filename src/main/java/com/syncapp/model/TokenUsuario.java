package com.syncapp.model;

import java.io.Serializable;

// import org.json.JSONObject;

public class TokenUsuario implements Serializable{
    public final String token;
    private String password; //Password in hash save
    private String pseudPassword;

    public TokenUsuario(String token) {
        this.token = new String(token);
    }
    
    
    // public TokenUsuario(String jsonRawFormat) {
    //     JSONObject jsn = new JSONObject(jsonRawFormat);
    //     token = jsn.getString("token");

    // }

    public void cambiarPassword(String oldPassword, String newPassword) {
        //generar hash de oldPasswrod y si coincide con el password antiguo, entonces se genera un hash del nuevo;
    }



    // public TokenUsuario(JSONObject jsn) {
    //     token = jsn.getString("token");
    //     password = jsn.getString("psswd");
    //     pseudPassword = jsn.getString("pspsswd");
    // }


    // public JSONObject toJSON() {
    //     JSONObject thisis = new JSONObject();
    //     thisis.put("token", this.token);
    //     thisis.put("psswd", password);
    //     thisis.put("pspsswd", pseudPassword);
    //     return thisis;
    // }

}
