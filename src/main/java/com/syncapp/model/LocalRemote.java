package com.syncapp.model;


public class LocalRemote {
    public boolean presentInLocal;
    public boolean presentInRemote;

    public String hashLocal;
    public String hashRemoto;

    public long timeMilisLocal; 
    public long timeMilisRemote;
    
    public LocalRemote(){
        presentInLocal = false;
        presentInRemote = false;
    }
}
