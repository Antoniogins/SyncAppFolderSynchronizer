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

    /**
     * Variable que se usa de forma auxiliar entre un metodo y una funcion lambda.
     */
    boolean containsPath;










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
     *
     * @throws IOException
     */

    public void actualizarCarpetasRegistradas() throws IOException {

        System.out.println("working path="+sac.getWorkingPath().toString()); //TESTS
        ArrayList<Path> listaCarpetas = Utilidades.listFolders(sac.getWorkingPath());
        listaCarpetas.forEach(c -> {

            Path pathAbsoluto = sac.getWorkingPath().resolve(c);

            containsPath = false;

            pathsRegistrados.forEach( (a,b) -> {
                if(  pathAbsoluto.toString().equals(b.toString())  ) {
                    containsPath = true;
                    return;
                }
            });

            if (!containsPath) {
                try {

                    WatchKey wk = pathAbsoluto.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                    pathsRegistrados.put(wk, pathAbsoluto);
                    System.out.println("registrado "+VariablesGlobales.COLOR_YELLOW+"[READ,MODIFY]"+VariablesGlobales.COLOR_WHITE+" \"" + pathAbsoluto.toString() + "\"");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

    }




    public void observar() throws InterruptedException {

        WatchKey key = null;

        if(goToSleep) {
            key = watcher.take(); //take() se bloquea (duerme) hasta que ocurre un evento
        } else {
            key = watcher.poll(); //poll() obtiene las claves modificadas encoladas en ese momento, o null si no
                                  // hay ninguna.
        }


        if(key == null) {
            return;
        }


        for (WatchEvent<?> we : key.pollEvents()) {

            WatchEvent.Kind<?> tipoEvento = we.kind();
            WatchEvent<Path> observables = (WatchEvent<Path>) we;



            Path carpetaPadre = pathsRegistrados.get(key);
//            System.out.println("carpeta padre=\""+carpetaPadre.toString()+"\"");
            Path archivo = carpetaPadre.resolve(observables.context());
//            System.out.println("ruta del archivo modificado=\""+archivo.toString());



            System.out.println("evento "+VariablesGlobales.COLOR_YELLOW+tipoEvento+VariablesGlobales.COLOR_WHITE+" "+archivo);



            //Si el evento que ha ocurrido es un directorio, actualizamos carpetas y reseteamos la clave
            if (archivo.toFile().isDirectory()) {
                try {
                    actualizarCarpetasRegistradas();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                key.reset();
                continue;
            }





            // comprobamos si el archivo es un archivo oculto -> estos no se deben
            // sincronizar (problemas)
            String filename = archivo.getFileName().toString();
            if (filename.charAt(0) == '.' || filename.charAt(0) == '~') {
                key.reset();
                continue;
            }






            try {

                if (tipoEvento.equals(ENTRY_CREATE)  || tipoEvento.equals(ENTRY_MODIFY)) {
//                    System.out.println("llegamos a test, con archivo=\"" + archivo + "\"");


                    // Actualizamos la ultima vez que ha cambiado el archivo observado
                    reloj.put(archivo, System.currentTimeMillis());


                    //Ya que vamos a comprobar a los 20s si el archivo ha vuelto a ser modificado, ponemos que el
                    // cliente no se vaya a bloquear
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

    public void actualizar() {

        ArrayList<Path> listaDeRelojesABorrarSincronamente = new ArrayList<>();

        long actual = System.currentTimeMillis();
        reloj.forEach((a, b) -> { // a=archivo b=tiempo
            if (actual - b > 20000) {
                System.out.println("prueba 2:"+(actual - b > 20000));
                // 20000ms = 20s -> actualizamos cada 20s para que no tengamos que enviar
                // informacion innecesaria (cada byte o linea que se escribe de un archivo lanza
                // un evento , entonces si actualziamos cada vez que semodifica un byte estamos
                // enviando el resto de bytes redundantemente)

                System.out.println("prueba1: ruta="+a.toString()+" existe="+a.toFile().exists());
                if (!a.toFile().exists()) {
                    listaDeRelojesABorrarSincronamente.add(a);
                    return;
                    // salta esta iteracion, dado que esto se ejecuta asincronamente puede que el
                    // archivo que estaba encolado se haya eliminado antes de que se compruebe su
                    // evento
                }



                //ACTUALIZAR ESTOOOO
                try {
//                    Path relativo = sac.getWorkingPath().relativize(a);
                    System.out.println("enviando archivo="+a);
                    sac.ejecutarOperacion(new Archivo(a , sac.getWorkingPath()) , VariablesGlobales.UPLOAD);
                    listaDeRelojesABorrarSincronamente.add(a);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        listaDeRelojesABorrarSincronamente.forEach(reloj::remove); //como c -> reloj.remove(c)


        if(reloj.isEmpty()) {
            //Dado que no hay mas relojes de archivo que observar (no queda ningun temporizador) mandamos a dormir
            goToSleep = true;
        }


    }





    public void dejarDeObservar() {
        pathsRegistrados.forEach((a, b) -> a.cancel());
        pathsRegistrados.clear();
        keep = false;
    }







    @Override
    public void run() {
        try {


            while(keep) {
                Thread.sleep(2000);
                observar();
                actualizar();
            }



        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }





}
