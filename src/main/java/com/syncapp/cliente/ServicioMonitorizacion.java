package com.syncapp.cliente;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;

import com.syncapp.model.Archivo;
import com.syncapp.utility.VariablesGlobales;
import com.syncapp.utility.Utilidades;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Este objeto nos permite monitorizar la carpeta de sincronizacion, en busca de archivos que se creen/modifiquen.
 * En el caso de que un archivo se haya modificado/creado, se esperara 20s para subirlo. Esto se realiza, ya que para
 * cada modificacion de un archivo (por ejemplo escribir una linea) se crea un nuevo evento, entonces si para cada
 * evento cargamos el archivo, podemos producir problemas de integridad, y una redundancia de bytes (los archivos cada
 * vez que se carga, se cargan desde el primer hasta el ultimo byte, no por tramos). <br>
 * Ademas, para optimizar un poco mas el monitor, cuando se detecta un evento, se bloquea el servicio durante dos segundos,
 * ya que suelen ocurrir rafas de eventos redundantes. Para ello, detectamos un evento, en vez de obtener las claves
 * con el metodo bloqueante {@link WatchService#take()}, obtenemos las claves mediante {@link WatchService#poll()}, que
 * comprobara si hay alguna clave encolada, o devolvera null si no hay niguna. Una vez que terminemos de subir el archivo,
 * mediante {@link ServicioMonitorizacion#goToSleep}, haremos que el monitor obtenga las claves con el servicio bloqueante,
 * y se ira a dormir hasta que ocurra un evento.
 */
public class ServicioMonitorizacion implements Runnable {

    /**
     * Cliente al que se le realiza el servicio de monitor.
     */
    SyncAppCliente sac;

    /**
     * Monitor de eventos generico.
     */
    WatchService watcher;

    /**
     * Mapa de rutas registradas para una clave de evento.
     */
    HashMap< WatchKey, Path> pathsRegistrados;

    /**
     * Contadores para determinar si un archivo ha pasado el suficiente tiempo sin modificar como para que sea cargado
     * al servidor.
     */
    HashMap< Path, Long> reloj;

    /**
     * Variable que permite controlar cuando el monitor debe seguir trabajando o finalizar.
     */
    boolean keep;

    /**
     * Variable que determina si el monitor debe leer una clave mediante el metodo bloqueante o mediante el metodo
     * no bloqueante.
     */
    boolean goToSleep;












    // Constructor

    /**
     * Con el constructor, podemos crear un servicio de monitorizacion a partir del cliente sobre el que quiere monitorizar.
     * Para ello, desde el cliente {@link SyncAppCliente#getWorkingPath() obtenemos} la carpeta de sincronizacion, y la
     * iteramos, para conocer los subdirectorios, y cada uno de ellos lo registramos en el monitor de eventos (ademas
     * de registrar la carpeta de sincronizacion misa, para los archivos raiz).
     * @param sac {@link SyncAppCliente cliente} que quiere monitorizar su carpeta.
     * @throws IOException si ocurre un fallo al iterar las subcarpetas.
     */
    public ServicioMonitorizacion(SyncAppCliente sac) throws IOException {
        // mejor que lance excepcion, asi el cliente sabe que se ha producido un error y
        // actue consecuentemente

        // Comprobamos que el cliente no sea nulo
        if (sac == null) {
            return;
        }

        // Añadimos el cliente a las variables del monitor
        this.sac = sac;

        // Creamos un nuevo mapa para el reloj de los archivos
        reloj = new HashMap<>();

        // Indicamos que funcione
        keep = true;

        // Obtenemos un monitor a partir del sistema
        this.watcher = FileSystems.getDefault().newWatchService();
        System.out.println("\niniciando servicio de monitorizacion");

        // Creamos el mapa de carpetas registradas
        pathsRegistrados = new HashMap<>();

        // Buscamos los subdirectorios y los añadimos
        actualizarCarpetasRegistradas();

        // Indicamos que obtenga claves de forma bloqueante
        goToSleep = true;
    }





















    /**
     * Este metodo permite actualizar las carpetas registradas. Para ello, cuando se observa que se ha creado una nueva
     * carpeta, se invoca este metodo, que recorrera todas las carpetas, dentro de la carpeta de sincronizacion, y
     * registrara aquellas que no estuvieran registradas.
     * @throws IOException si ocurre algun problema recorriendo las carpetas.
     */

    public void actualizarCarpetasRegistradas() throws IOException {

        System.out.println("working path="+sac.getWorkingPath().toString()); //TESTS

        // Obtenemos la lista de carpetas dentro del directorio de trabajo
        ArrayList<Path> listaCarpetas = Utilidades.listFolders(sac.getWorkingPath());

        // Recorremos la lista de carpetas
        listaCarpetas.forEach(c -> {

            // Obtenemos la ruta absoluta de la carpeta
            Path pathAbsoluto = sac.getWorkingPath().resolve(c);

            // Comprobamos si la carpeta ya estaba registrada, en caso afirmativo, registramos la carpeta
            if (!pathsRegistrados.containsValue(pathAbsoluto)) {
                try {

                    // Registramos el path con el monitor, y le indicamos que escuche los eventos ENTRY_CREATE y
                    // ENTRY_MODIFY para esta carpeta
                    WatchKey wk = pathAbsoluto.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                    // Añadimos la carpeta registrada al registro de carpetas
                    pathsRegistrados.put(wk, pathAbsoluto);

                    // Mostramos por pantalla que se ha registrado la carpeta
                    System.out.println("registrado "+VariablesGlobales.COLOR_YELLOW+"[READ,MODIFY]"+VariablesGlobales.COLOR_WHITE+" \"" + pathAbsoluto.toString() + "\"");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }





        });

    }


    /**
     * Con este metodo, podemos observar si ha ocurrido algun evento dentro de las carpetas que estamos observando.
     * Tiene dos modos de funcionamiento:
     * <ol>
     *     <li>
     *         Cuando obtenemos la clave que se ha modificado, se bloquea hasta que exista el evento. Este modo de
     *         operacion se realiza cuando {@link #goToSleep} es true.
     *     </li>
     *     <li>
     *         Cuando obtenemos la clave que se ha modificado, obtenemos un valor instantaneo de las claves modificadas,
     *         no bloqueando el monitor. En caso de que existan claves, las devuelve, y en caso de que no existan claves,
     *         devuelve null. Este modo de operacion se realiza cuando {@link #goToSleep} es false.
     *     </li>
     * </ol>
     * @throws InterruptedException
     */
    public void observar() throws InterruptedException {

        // Indicamos la variable para poder usarla despues
        WatchKey key = null;

        // Comprobamos el modo de observacion, bloqueante o no bloqueante
        if(goToSleep) {
            key = watcher.take(); //take() se bloquea (duerme) hasta que ocurre un evento
        } else {
            key = watcher.poll(); //poll() obtiene las claves modificadas encoladas en ese momento, o null si no
                                  // hay ninguna.
        }

        // Si la clave es nula retorna (pues no hay clave que observar)
        if(key == null) {
            return;
        }

        // Obtenemos la lista de eventos que han ocurrido para la clave obtenida. Iteramos esta lista
        for (WatchEvent<?> we : key.pollEvents()) {

            // Obtenemos el tipo de evento generado
            WatchEvent.Kind<?> tipoEvento = we.kind();

            // Como sabemos que los eventos son generados por paths, transformamos la lista de eventos genericos
            // en lista de eventos de paths. Para realizar esta operacion debemos estar seguros que unicamente
            // hemos registrado eventos sobre paths, pues el casteo de otro tipo de evento a path produciria error.
            WatchEvent<Path> observables = (WatchEvent<Path>) we;


            // Obtenemos el nombre de la carpeta que ha generado el evento
            Path carpetaPadre = pathsRegistrados.get(key);

            // Obtenemos la ruta completa del archivo. Para ello obtenemos el contexto (archivo modificado/creado) y
            // lo derivamos de la carpeta padre
            Path archivo = carpetaPadre.resolve(observables.context());


            // Mostramos el evento ocurrido
            System.out.println("evento "+VariablesGlobales.COLOR_YELLOW+tipoEvento+VariablesGlobales.COLOR_WHITE+" "+archivo);



            // Comprobamos si el evento que ha ocurrido es un directorio, actualizamos carpetas y reseteamos la clave
            if (archivo.toFile().isDirectory()) {
                try {
                    actualizarCarpetasRegistradas();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Reseteamos la clave para que se sigan escuchando eventos de esta clave
                key.reset();
                continue;
            }





            // Comprobamos si el archivo es un archivo oculto -> estos no se deben
            // sincronizar (problemas)
            String filename = archivo.getFileName().toString();
            if (filename.charAt(0) == '.' || filename.charAt(0) == '~') {
                key.reset();
                continue;
            }






            try {

                // Comprobamos si el evento que se ha generado es tipo CREATE o MODIFY
                if (tipoEvento.equals(ENTRY_CREATE)  || tipoEvento.equals(ENTRY_MODIFY)) {


                    // Actualizamos la ultima vez que se ha observado una modificacion del archivo
                    reloj.put(archivo, System.currentTimeMillis());


                    //Ya que vamos a comprobar a los 20s si el archivo ha vuelto a ser modificado, ponemos que el
                    // cliente no se vaya a bloquear, para que cada 2s compruebe los valores del registro del reloj
                    goToSleep = false;


                } else if (tipoEvento == ENTRY_DELETE) {
                    //TODO si se esta ejecutando el cliente y se elimina un archivo sabemos que ha sido eliminado?
                    throw new IOException(); // TEMPORAL
                }


            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            if (!key.reset()) {
                break;
            }






        }

        

        

    }


    /**
     * Este metodo nos permite actualizar los valores de reloj de los archivo. Esto se hace para controlar cuando subir
     * un archivo. Dado que un archivo se modifica linea a linea, o byte a byte, mientras se esta trabajando con un archivo
     * se van a generar multiples eventos, y transmitir constantemente el archivo por completo es redundante e ineficiente.
     */
    public void actualizar() {

        // Creamos una lista para los archivos que hayan pasado mas de 20s, y queremos removerlos de los relojes
        ArrayList<Path> listaDeRelojesABorrarSincronamente = new ArrayList<>();

        // Obtenemos la hora actual del cliente
        long actual = System.currentTimeMillis();

        // Iteramos el reloj, para saber cual de los archivos ha completado el tiempo de espera
        reloj.forEach((a, b) -> { // a=archivo b=tiempo

            // Comparamos la hora actual con la hora en que se modifico el archivo por ultima vez. Si el resultado
            // de esta comparacion es que han pasado mas de 20s, enviamos el archivo al servidor
            if (actual - b > 20000) {


                // 20000ms = 20s -> actualizamos cada 20s para que no tengamos que enviar
                // informacion innecesaria (cada byte o linea que se escribe de un archivo lanza
                // un evento , entonces si actualziamos cada vez que semodifica un byte estamos
                // enviando el resto de bytes redundantemente)

                // Comprobamos que al archivo exista, debido a que puede ocurrir que el archivo se elimine antes
                // de que se transmite
//                System.out.println("prueba1: ruta="+a.toString()+" existe="+a.toFile().exists());
                if (!a.toFile().exists()) {

                    // añadimos el archivo a la lista de archivo a eliminar
                    listaDeRelojesABorrarSincronamente.add(a);
                    return;
                    // salta esta iteracion, dado que esto se ejecuta asincronamente puede que el
                    // archivo que estaba encolado se haya eliminado antes de que se compruebe su
                    // evento
                }



                // Intentamos enviar el archivo
                try {
                    System.out.println("enviando archivo="+a);
                    sac.ejecutarOperacion(new Archivo(a , sac.getWorkingPath()) , VariablesGlobales.UPLOAD);
                    listaDeRelojesABorrarSincronamente.add(a);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Una vez acabamos de iterar todos los relojes, eliminamos del reloj la lista de archivos indicada
        listaDeRelojesABorrarSincronamente.forEach(reloj::remove); //como c -> reloj.remove(c)


        // Si tras iterar, el reloj queda vacio, es porque no quedan eventos pendientes, por tanto pasamos el monitor
        // al modo dormir hasta que ocurra un nuevo evento
        if(reloj.isEmpty()) {
            //Dado que no hay mas relojes de archivo que observar (no queda ningun temporizador) mandamos a dormir
            goToSleep = true;
        }


    }


    /**
     * Este metodo nos permite cerrar el monitor, cancelando todos los monitores de paths, cancelando sus claves
     */
    public void dejarDeObservar() {
        pathsRegistrados.forEach((a, b) -> a.cancel());
        pathsRegistrados.clear();
        keep = false;
    }


    /**
     * Tarea de ejecucion para permitir al monitor ejecutarse en un hilo aparte del hilo principal del cliente. Esta
     * tarea se ejecuta de forma indefinida hasta que se indique finalizar el monitor. <br>
     * La funcion principal de esta tarea, es observar los eventos y realizar las funciones descrtas para esta clase.
     */
    @Override
    public void run() {
        try {
            

            // Mientras se indique que funcione, se ejecuta
            while(keep) {

                // Dormimos el hilo 2s, para optimizar el monitor
                Thread.sleep(2000);

                // Observamos si ha ocurrido algun evento
                observar();

                // Actualizamos el reloj, comprobando si algun evento a alcanzado su fin
                actualizar();
            }



        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }





}
