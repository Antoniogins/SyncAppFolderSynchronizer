/**
 * Servicio distribuido SyncApp.
 * <br>
 * <p>
 *     El conjunto de clases escritas en este proyecto implementan un servicio distribuido, diseñado para que usuarios
 *     que utilicen un {@link com.syncapp.cliente.SyncAppCliente} puedan sincronizar una de las carpetas en su pc con el servidor.
 * </p>
 * <p>
 *     Este servicio esta implementado en JavaRMI, y permite que multiples usuarios puedan acceder a sus archivos en varios
 *     ordenadores simultaneamente, mediante el uso de sesiones, para identificar cada uno de estos ordenadores. Aunque pueden
 *     acceder a los archivos de forma simultanea, no se puede acceder al mismo archivo concreto, debido a limitaciones
 *     con el manejo de archivos, que requeririan una mayor implementacion arquitectonica.
 * </p>
 * <p>
 *     Aunque se indica que se "sincronizan", no se realiza una "sincronizacion" propia, los clientes pueden subir y bajar archivos,
 *     pero no pueden ser eliminados de forma inmediata cuando se elimina en alguno de los dispositivos, debido tambien a que se
 *     requiere una mayor implementacion arquitectonica que abarca mas alla del objetivo de este proyecto.
 * </p>
 * <p>
 *     Este {@link run} permite ejecutar de forma sencilla tanto el cliente como el servidor del serivio SyncApp.
 * </p>
 */
package run;