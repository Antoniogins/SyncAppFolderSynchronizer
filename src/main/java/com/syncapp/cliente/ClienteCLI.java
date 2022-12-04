package com.syncapp.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.syncapp.model.Archivo;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.Utilidades;

public class ClienteCLI {


    // args para cliente:
    // args[0] -> ip
    // args[1] -> puerto
    // args[2] -> usuario
    // args[3] -> carpeta
    // args[4] -> threads

    static String[] lastParams;


    static void load(SyncAppCliente cliente) throws RemoteException {

        ArrayList<Archivo> pendientes = cliente.primeraIteracion();
        while(pendientes != null) {
            pendientes = cliente.siguienteIteracion(pendientes);
        }

    }


    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Args iniciales:");
        for (String ar : args) {
            System.out.println(ar);
        }
        if(args == null || args.length != 5) return;
        
        if(lastParams == null) {
            lastParams = args;
        }




        // Creamos el objeto cliente
        SyncAppCliente cliente = new SyncAppCliente(lastParams);

        // Iniciamos el servidor
        cliente.iniciarServidor();

        // Ejecutamos Algoritmo de Cristian para obtener nuestro offset con el
        // servidor (10 intentos deberia ser suficiente)
        cliente.calcularOffset(10);

        // Nos anunciamos en el servidor
        cliente.iniciarUsuario();








        // Comenzamos a comparar que archivos hay que sincronizar
        // Primero obtenemos una lista con los archivos que no tienen presencia local o
        // remota, ya que sabemos que estos los tenemos que sincronizar.

        // Segundo, tras ejecutar el primer paso, obtendremos una lista con los archivos
        // que necesitan mas informacion para determinar si hay que sincronizarlos. Para
        // ello, a esa lista de archivo pedimos al servidor que nos devulva esa misma
        // lista, pero con la informacion necesaria. Realizamos la misma operacion pero
        // en local, con esa misma lista. Una vez tengamos esas dos listas con
        // informacion, decidimos que operacion se necesita ejecutar.

        // Hay que tener en cuenta, que tras obtener estas listas, automaticamente se
        // ejecuta la funcion "ejecutarOperaciones", que vera que operacion se ha
        // determinado para cada uno de los archivos, y ejecuta la operacion necesaria

        load(cliente);


        // Bloqueamos la aplicacion hasta que se termine de descargar todos los archivos.
        // Realizamos esta operacion, ya que si hay muchos archivos que transmitir, el
        // monitor de carpetas detectara como nuevos/modificados los archivos que se descargue
        // y los volvera a cargar (esto no debe pasar)
        cliente.esperarHastaTerminarTransmisiones();
        System.out.println("se acabaron las transmisiones");



        // Una vez se han sincronizado todos los archivos (o por lo menos ya tienen
        // asignada una operacion) esperamos 30 segundos y empezamos a observar los
        // cambios de archivos y carpetas dentro de nuestra carpeta (este tiempo podemos
        // incrementarlo si necesitamos)
        cliente.iniciarServicioObservadorDeCarpetas();






        // Iniciamos una consola, para poder introducir comandos e interactuar con el
        // programa. 
        BufferedReader bis = new BufferedReader( new InputStreamReader(System.in) );


        System.out.println("Ejecutando el lector de consola ... ");
        System.out.println("Introduce help para mas informacion ...");
        String line;
        boolean keepWorking = true;

        while(keepWorking) {
            try {
                line = bis.readLine();
                String[] sentencia = line.split(":");
    
                //Introducimos los comandos como "comando:algo mas"


                switch (sentencia[0]) {

                    //TODO terminarlo
                    case "help" -> {
                        String helped =
                                "\ncomandos disponibles: " +
                                        "\n\tclose         ->  cierra la aplicacion  " +
                                        "\n\treload        ->  vuelve a sincronizar las carpetas desde cero  " +
                                        "\n\tuser:value    ->  cambia de usuario a valor \"value\"  " +
                                        "\n\tip:a.b.c.d:p  ->  establece una nueva ip de servidor (puerto se puede omitir, 1099 por defecto)" +
                                        "\n\tthreads:n     ->  establece a \"n\" el maximo de archivos transferibles simultaneamente" +
                                        "\n\tfolder:value  ->  cambia la carpeta a transferir" +
                                        "\n\tsincronizar   ->  ejecuta el Algoritmo de Cristian para refrescar el TimeOffset" +
                                        "\n\tdelete:file   ->  borra file en cliente y servidor" +
                                        "\n\tlist:lri      ->  muestra una lista con los archivos de la siguiente forma:" +
                                        "\n\t                        -l: lista de archivos remotos" +
                                        "\n\t                        -r: lista de archivos locales" +
                                        "\n\t                        -i: mostrar informacion (hash, ultima modificacion)" +
                                        "\n\t                  para ello escriba las letras de la informacion que quiera mostrar" +
                                        "\n\thelp          ->  muestra este texto xD  ";
                        System.out.println(helped);
                    }


                    //CADA VEZ QUE EDITEMOS UN PARAMETRO HAY QUE RECORDAR CAMBIAR lastParams
                    case "close" -> {
                        System.out.println("cerrando aplicacion");
                        //sac.cerrarCliente
                        keepWorking = false;

                    }
                    case "reload" -> {
                        main(lastParams);
                        keepWorking = false;
                    }
                    case "user" -> {
                        cliente.setUser(new TokenUsuario(sentencia[1]));
                        lastParams[SyncAppCliente.ARG_USUARIO] = sentencia[1];
                    }
                    case "ip" -> {
                        cliente.setServerIP(sentencia[1]);
                        cliente.setPuerto(Short.parseShort(sentencia[2]));
                        lastParams[SyncAppCliente.ARG_IP] = sentencia[1];
                        lastParams[SyncAppCliente.ARG_PUERTO] = sentencia[2];
                    }
                    case "threads" -> {
                        lastParams[SyncAppCliente.ARG_HILOS] = sentencia[1];
                    }
                    case "folder" -> {
                        cliente.setWorkingPath(Path.of(sentencia[1]));
                        lastParams[SyncAppCliente.ARG_CARPETA] = sentencia[1];
                    }
                    case "sincronizar" -> {
                        load(cliente);
                    }
                    case "delete" -> {
                    }
                    case "list" -> {
                        Utilidades.listFiles(cliente.getWorkingPath()).forEach(System.out::println);
                        for (String s : sentencia) {
                            if (sentencia[1].charAt(0) == 'l') {
                                //.listFolders(cliente.getWorkingPath());
                            } else if (sentencia[1].charAt((0)) == 'r') {
                                Utilidades.listFolders(cliente.getWorkingPath());
                            } else if (sentencia[1].charAt((0)) == 'i') {
                                //
                            }
                        }


                    }
                    default -> {
                        System.out.println("comando desconocido, intenta de nuevo ... ");
                    }
                }
    

                
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        cliente.close();

        
    }


    
}
