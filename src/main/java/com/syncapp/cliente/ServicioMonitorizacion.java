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
    HashMap<Path, WatchKey> pathsRegistrados;
    HashMap<Path, Long> reloj;
    boolean keep;

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
        actualizarCarpetasRegistradas();
    }

    public void actualizarCarpetasRegistradas() throws IOException {

        ArrayList<Path> listaCarpetas = Util.listFolders(sac.getWorkingPath());
        listaCarpetas.forEach(c -> {

            if (!pathsRegistrados.containsKey(c)) {
                try {
                    WatchKey wk = c.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    pathsRegistrados.put(c, wk);
                    System.out.println("registrado [READ , MODIFY] \"" + c.toString() + "\"");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public void observar() {

        WatchKey key = null;
        

        try {
            key = watcher.take();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (WatchEvent<?> we : key.pollEvents()) {

            WatchEvent.Kind<?> tipoEvento = we.kind();

            WatchEvent<Path> observables = (WatchEvent<Path>) we;
            Path rutaObservada = observables.context();
            Path rutaAbsoluta = Paths.get(rutaObservada.toString()).toAbsolutePath();

            System.out.println("evento " + tipoEvento + " \"" + rutaObservada.toString() + "\"");

            if (rutaObservada.toFile().isDirectory()) {
                try {
                    actualizarCarpetasRegistradas();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // comprobamos si el archivo es un archivo oculto -> estos no se deben
            // sincronizar
            String filename = rutaObservada.getFileName().toString();
            System.out.println("prueba 5: nombre="+filename);

            if (filename.charAt(0) == '.' || filename.charAt(0) == '~') {
                continue;
            }

            try {
                System.out.println("prueba 3: create="+tipoEvento.equals(ENTRY_CREATE)+" modify="+tipoEvento.equals(ENTRY_MODIFY));
                System.out.println("prueba 4, clases coinciden? -> create="+tipoEvento.getClass().equals(ENTRY_CREATE.getClass())+" modify="+tipoEvento.getClass().equals(ENTRY_MODIFY.getClass()));
                if (tipoEvento.equals(ENTRY_CREATE)  || tipoEvento.equals(ENTRY_MODIFY)) {
                    System.out.println(
                            "llegamos a test, con filename=" + filename + " y ruta observada=" + rutaObservada);
                    // sac.ejecutarOperacion( sac.newArchivoInFolder(rutaObservada), Ops.UPLOAD);
                    reloj.put(rutaObservada, System.currentTimeMillis());

                    // esto lo hacemos para que cada vez que el archivo se modifique cambie su
                    // tiempo, y si la ultima vez que se ha modificado es superior a 20s entonces
                    // enviamos el archivo, mejoramos en eficiencia
                } else if (tipoEvento == ENTRY_DELETE) {
                    //TODO si se esta ejecutando el cliente y se elimina un archivo sabemos que ha sido eliminado?
                    throw new IOException(); // TEMPORAL
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        }

        actualizar();

        if (!key.reset()) {

        }

    }

    public void actualizar() {


        long actual = System.currentTimeMillis();
        reloj.forEach((a, b) -> { // a=archivo b=tiempo
            if (actual - b > 20000) {
                System.out.println("prueba 2:"+(actual - b > 20000));
                // 20000ms = 20s -> actualizamos cada 20s para que no tengamos que enviar
                // informacion innecesaria (cada byte o linea que se escribe de un archivo lanza
                // un evento , entonces si actualziamos cada vez que semodifica un byte estamos
                // enviando el resto de bytes redundantemente)

                System.out.println("prueba1:"+a.toFile().exists());
                if (!a.toFile().exists()) {
                    return;
                    // salta esta iteracion, dado que esto se ejecuta asincronamente puede que el
                    // archivo que estaba encolado se haya eliminado antes de que se compruebe su
                    // evento
                }
                try {
                    sac.ejecutarOperacion(new Archivo(a), Ops.UPLOAD);
                    reloj.remove(a);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void dejarDeObservar() {
        pathsRegistrados.forEach((a, b) -> b.cancel());
        pathsRegistrados.clear();
    }

    @Override
    public void run() {
        try {
            actualizarCarpetasRegistradas();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (keep) {
            try {
                Thread.sleep(2000);

            } catch (InterruptedException e1) {

                e1.printStackTrace();
            }

            observar();
        }
    }

}
