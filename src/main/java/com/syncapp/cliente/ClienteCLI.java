package com.syncapp.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import com.syncapp.model.TokenUsuario;
import com.syncapp.utility.Utilidades;


/**
 * Cliente por consola. Permite iniciar el {@link SyncAppCliente} en linea de comandos, permitiendo ejecutar comandos
 * posteriormente a la primera sincronizacion con el servidor. Es el principal cliente.
 */
public class ClienteCLI {


    // args para cliente:
    // args[0] -> ip
    // args[1] -> puerto
    // args[2] -> usuario
    // args[3] -> carpeta
    // args[4] -> threads

    /**
     * Este array de String permite almacenar los parametros con los que ejecutar el {@link SyncAppCliente}. En una
     * primera instancia, se almacenan los argumentos indicados al ejecutar el cliente, y posteriormente, almacenara
     * los valores que se introduzcan por consola, para cuando el usuario pida volver a sincronizar toda la carpeta,
     * se ejecute el cliente con estos parametros.
     * <br>
     * Estos parametros se almacenan segun las directivas de {@link SyncAppCliente}:
     * <ul>
     *     <li>
     *         Direccion IP se almacena en la posicion {@link SyncAppCliente#ARG_IP}.
     *     </li>
     *     <li>
     *         Puerto se almacena en la posicion {@link SyncAppCliente#ARG_PUERTO}.
     *     </li>
     *     <li>
     *         Nombre de usuario se almacena en la posicion {@link SyncAppCliente#ARG_USUARIO}.
     *     </li>
     *     <li>
     *         Carpeta de sincronizacion se almacena en la posicion {@link SyncAppCliente#ARG_CARPETA}.
     *     </li>
     *     <li>
     *         Numero de hilos se almacena en la posicion {@link SyncAppCliente#ARG_HILOS}.
     *     </li>
     * </ul>
     */
    static String[] lastParams;


    /**
     * Metodo principal para ejecutar el cliente.
     * @param args argumentos de entrada listados anteriormente. Son NECESARIOS.
     * @throws RemoteException si ocurre algun fallo al ejecutar RMI.
     * @throws MalformedURLException si se introduce una direccion incorrecta.
     * @throws NotBoundException si se intenta ejecutar un servicio que no esta disponible (o no existe) en el registro RMI.
     */
    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        if(args == null || args.length != 5) return;

        // Guardamos los argumentos iniciales y los mostramos por pantalla
        lastParams = args;
        System.out.println("Args iniciales:");
        for (String ar : args) {
            System.out.print(ar+", ");
        }
        System.out.println();






        // Creamos el objeto cliente
        SyncAppCliente cliente = new SyncAppCliente(lastParams);

        // Iniciamos el servidor
        cliente.iniciarServidor();
        System.out.println("servidor esta vivo");

        // Ejecutamos Algoritmo de Cristian para obtener nuestro offset con el
        // servidor (10 intentos deberia ser suficiente)
        cliente.calcularOffset(10);
        System.out.println("offset calculado");

        // Nos anunciamos en el servidor
        cliente.iniciarSesion();
        System.out.println("sesion iniciada");








        // Comenzamos a comparar que archivos hay que sincronizar

        cliente.sincronizarConServidor();


        // Bloqueamos la aplicacion hasta que se termine de descargar todos los archivos.
        // Realizamos esta operacion, ya que si hay muchos archivos que transmitir, el
        // monitor de carpetas detectara como nuevos/modificados los archivos que se descarguen
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
                                        "\n\tuser:value    ->  cambia de usuario a valor \"value\"  " +
                                        "\n\tip:a.b.c.d:p  ->  establece una nueva ip de servidor (puerto se puede omitir, 1099 por defecto)" +
                                        "\n\tthreads:n     ->  establece a \"n\" el maximo de archivos transferibles simultaneamente" +
                                        "\n\tfolder:value  ->  cambia la carpeta a transferir" +
                                        "\n\tsincronizar   ->  ejecuta el Algoritmo de Cristian para refrescar el TimeOffset" +
                                        "\n\tdelete:file   ->  borra file en cliente y servidor" +
                                        "\n\tlist:lri      ->  muestra una lista con los archivos de la siguiente forma:" +
                                        "\n\t                        -l: lista de archivos remotos" +
                                        "\n\t                        -r: lista de archivos locales" +
                                        "\n\t                        -i: obtener hash" +
                                        "\n\t                  para ello escriba las letras de la informacion que quiera mostrar" +
                                        "\n\thelp          ->  muestra este texto xD  ";
                        System.out.println(helped);
                    }


                    //CADA VEZ QUE EDITEMOS UN PARAMETRO HAY QUE RECORDAR CAMBIAR lastParams
                    case "close" -> {
                        System.out.println("cerrando aplicacion");
                        keepWorking = false;
                        cliente.close();

                    }
                    case "user" -> {
                        cliente.setUser(new TokenUsuario(sentencia[1]));
                        lastParams[SyncAppCliente.ARG_USUARIO] = sentencia[1];
                    }
                    case "ip" -> {
                        cliente.setServerIP(sentencia[1]);
                        cliente.setPuerto(sentencia[2]);
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
                        cliente.esperarHastaTerminarTransmisiones();
                        cliente.sincronizarConServidor();
                    }
                    case "delete" -> {
                    }
                    case "list" -> {
                        boolean listaLocal = false;
                        boolean listaRemota = false;
                        boolean conHash = false;

                        // Leemos los caracteres indicados
                        for (char ind : sentencia[1].toCharArray()) {
                            if (ind == 'l') {
                                listaLocal = true;
                            } else if (ind == 'r') {
                                listaRemota = true;
                            } else if (ind == 'i') {
                                conHash = true;
                            }
                        }

                        // POR IMPLEMENTAR ESTA PARTE




                    }
                    default -> {
                        System.out.print("comando desconocido, intenta de nuevo: ");
                    }
                }
    

                
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        cliente.close();

        
    }


    
}
