package com.syncapp.cliente;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;

import com.syncapp.model.Archivo;
import com.syncapp.utility.Ops;
import com.syncapp.utility.Util;

import static java.nio.file.StandardWatchEventKinds.*;

public class ServicioMonitorizacion implements Runnable {

    SyncAppCliente sac;
    WatchService watcher;
    HashMap< WatchKey, Path> pathsRegistrados;
    HashMap< Path, Long> reloj;
    boolean keep;
    boolean goToSleep;

    boolean containsPath;

    public ServicioMonitorizacion(SyncAppCliente sac) throws IOException {
        // mejor que lance excepcion, asi el cliente sabe que se ha producido un error y
        // actue consecuentemente

        if (sac == null)
            return;
        this.sac = sac;
        reloj = new HashMap<>();
        keep = true;
        this.watcher = FileSystems.getDefault().newWatchService();
        System.out.println("\niniciando servicio de monitorizacion");
        pathsRegistrados = new HashMap<>();
        goToSleep = true;
        actualizarCarpetasRegistradas();
    }

    public void actualizarCarpetasRegistradas() throws IOException {

        System.out.println("working path="+sac.getWorkingPath().toString()); //TESTS
        ArrayList<Path> listaCarpetas = Util.listFolders(sac.getWorkingPath());
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
                    System.out.println("registrado [READ , MODIFY] \"" + pathAbsoluto.toString() + "\"");


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
            System.out.println("carpeta padre=\""+carpetaPadre.toString()+"\"");
            Path archivo = carpetaPadre.resolve(observables.context());
            System.out.println("ruta del archivo modificado=\""+archivo.toString());



            System.out.println("evento " + tipoEvento + " \"" + archivo.toString() + "\"");



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
                    System.out.println("llegamos a test, con archivo=\"" + archivo + "\"");


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
                    sac.ejecutarOperacion(new Archivo(a , sac.getWorkingPath()) , Ops.UPLOAD);
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
