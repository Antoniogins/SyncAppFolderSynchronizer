package com.syncapp.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.syncapp.model.BloqueBytes;
import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;



public interface SyncApp extends Remote {
    
    void iniciar_usuario(TokenUsuario u) throws RemoteException;
    void cerrar_usuario(TokenUsuario u) throws RemoteException;
    ArrayList<Archivo> lista_archivos(TokenUsuario u) throws RemoteException;
    ArrayList<Archivo> obtenerParametrosSimultaneos(TokenUsuario tu, ArrayList<Archivo> lista) throws RemoteException;
    Archivo obtenerParametros(TokenUsuario tu, Archivo a) throws RemoteException;
    int abrirArchivo(TokenUsuario tu, Archivo a, String op_mode) throws RemoteException;
    void cerrarArchivo(int id_file) throws RemoteException;
    BloqueBytes leerBloqueBytes(int id_file, long position) throws RemoteException;
    void escribirBloqueBytes(int id_file, BloqueBytes bloq_bytes, int pos) throws RemoteException;
    String hash(int file_id) throws RemoteException;
    String sayHello() throws RemoteException;
    long obtenerHora() throws RemoteException;
    byte calcularTmin(byte b) throws RemoteException;


}
