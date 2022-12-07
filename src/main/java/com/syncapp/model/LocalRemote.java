package com.syncapp.model;


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
     * Archivo local.
     */
    private Archivo local;

    /**
     * Archivo remoto.
     */
    private Archivo remoto;


















    // Constructor - antigua implementacion

    /**
     * Constructo para la antigua implementacion.
     * @deprecated - antigua implementacion.
     */
    public LocalRemote(){
        presentInLocal = false;
        presentInRemote = false;
    }










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
     *         Verdadero - si hay archivo local y no hay archivo remoto.
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
     *         Verdadero - si hay archivo remoto y no hay archivo local.
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
