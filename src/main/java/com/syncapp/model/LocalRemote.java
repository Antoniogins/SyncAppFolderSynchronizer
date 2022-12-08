package com.syncapp.model;

/**
 * Esta clase se implementa para facilitar el uso de comparar archivos. Para ello se guarda el mismo archivo pero
 * con dos instancias: la local y la remota. En este objeto, tras recorrer las listas de archivos, se ira almacenando
 * los archivos correspondientes en local o en remoto, y sera de gran ayuda para determinar si un archivo debe ser
 * cargado/descargado.<br>
 * Ademas se dispone de metodos que resuelven este problema.
 */
public class LocalRemote {

    // Desactualizados
    public boolean presentInLocal;
    public boolean presentInRemote;

    public String hashLocal;
    public String hashRemoto;

    public long timeMilisLocal; 
    public long timeMilisRemote;

    public long sizeLocal;
    public long sizeRemote;



    // Nueva implementacion

    /**
     * Archivo local. Incluye su ruta, sus metadatos, etc.
     */
    private Archivo local;

    /**
     * Archivo remoto. Incluye su ruta, sus metadatos, etc.
     */
    private Archivo remoto;

















    // SETTERS

    /**
     * Establece el archivo local.
     * @param local {@link Archivo} local.
     */
    public void setLocal(Archivo local) {
        this.local = local;
    }

    /**
     * Estabelece el archivo remoto.
     * @param remoto {@link Archivo} remoto.
     */
    public void setRemote(Archivo remoto) {
        this.remoto = remoto;
    }














    // LOGICOS

    /**
     * Comprueba si un archivo esta exclusivamente en la maquina local.
     * @return
     * <ul>
     *     <li>
     *         Verdadero - si hay archivo local y no hay archivo remoto. Permite determinar que el archivo se debe
     *         cargar al servidor.
     *     </li>
     *     <li>
     *         Falso - en caso contrario.
     *     </li>
     * </ul>
     */
    public boolean exclusivoLocal(){
        return (local != null && remoto == null);
    }


    /**
     * Comprueba si un archivo esta exclusivamente en la maquina remota.
     * @return
     * <ul>
     *     <li>
     *         Verdadero - si hay archivo remoto y no hay archivo local. Permite determinar que el archivo se debe
     *         descargar del servidor.
     *     </li>
     *     <li>
     *         Falso - en caso contrario.
     *     </li>
     * </ul>
     */
    public boolean exclusivoRemoto() {
        return (local == null && remoto != null);
    }
















    // GETTERS

    /**
     * Permite obtener el archivo local.
     * @return {@link Archivo} local.
     */
    public Archivo getLocal() {
        return  local;
    }

    /**
     * Permite obtener el archivo remoto.
     * @return {@link Archivo} remoto.
     */
    public Archivo getRemoto() {
        return remoto;
    }
}
