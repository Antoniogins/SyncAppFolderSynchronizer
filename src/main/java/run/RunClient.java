package run;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

import com.syncapp.cliente.ClienteCLI;
import com.syncapp.cliente.ClienteGUI;


/**
 * Cliente para acceder al servicio de cloud SyncApp
 */
public class RunClient {


    /**
     * <p>
     *   Este metodo estatico nos permite iniciar el cliente de SyncApp. Existen dos modos para ejecutar la aplicacion:
     *
     *   <ul>
     *       <li>
     *           mediante linea de comandos: usando el cliente {@link ClienteCLI} (por defecto).
     *       </li>
     *       <li>
     *           mediante interfaz grafica: usando el cliente {@link ClienteGUI} (indicando "gui" en argumentos).
     *       </li>
     *   </ul>
     * </p>
     *
     * <br>
     *
     * <p>
     *     Se pueden indicar los siguientes argumentos:
     *
     *     <ul>
     *          <li>
     *              <b>ip a.b.c.d</b> -> para indicar que se quiere conectar al servidor con direccion ip <b>a.b.c.d</b>. El valor por defecto es <i>localhost</i>.
     *          </li>
     *          <li>
     *              <b>port portValue</b> -> para indicar el puerto del servidor al que queremos conectar. El valor por defecto es <i>1099</i>.
     *          </li>
     *          <li>
     *              <b>folder foldPath</b> -> para indicar que se quiere sincronizar la carpeta <b>foldPath</b>.
     *              Hay que tener especial cuidado con la carpeta que se introduce. Si contiene espacios en algun nombre
     *              de carpeta, esta hay que indicarla entre comillas ("foldPath"); y si no se tiene permisos de lectura/escritura
     *              el programa fallará.
     *              <br>
     *              Si la ruta indicada no es una ruta absoluta (que no comienza desde el directorio raiz "C:" o "/")
     *              se supondra que la carpeta indicada es una carpeta dentro del directorio del usuario ( $ruta_usuario/foldPath -> windows:  "C:\Users\ usuario\foldPath" , linux/macos: "/home/usuario/foldPath").
     *              <br>
     *              El valor por defecto es <i>"$ruta_usuario/syncappshared"</i>.
     *          </li>
     *          <li>
     *              <b>user username</b> -> para indicar el nombre de usuario que queremos utilizar. El valor por defecto es <i>$usuario</i>.
     *          </li>
     *          <li>
     *              <b>threads n</b> -> para indicar que se quiere transmitir <b>n</b> archivos simultaneamente. El valor por defecto es <i>4</i>.
     *          </li>
     *          <li>
     *              <b>gui</b> -> para indicar que se quiere iniciar el cliente en modo interfaz grafica. Para ello, se debe
     *              disponer un entorno java que tenga la libreria {@link java.awt}, en caso de fallo, intente solucionar esto. El valor por defecto es <i>nulo</i> (se ejecutara el cliente en modo consola).
     *          </li>
     *      </ul>
     *
     * </p>
     *
     * <br>
     *
     * <p>
     *     Estos parametros se pueden indicar en cualquier orden, siempre reservando un espacio para indicar el valor
     *     del parametro que se quiere indicar.
     *     Ejemplos:
     *     <ul>
     *         <li> <i>gui folder "/home/usuario/syncappsharefolder"</i> </li>
     *         <li> <i>user usuario ip localhost port 1099</i> </li>
     *     </ul>
     * </p>
     *
     *
     * @param args array de argumentos separados por espacios.
     * @throws NotBoundException Si la direccionIP:puerto indicados no representa un servicio RMI.
     * @throws IOException Si ocurre un fallo durante la ejecucion.
     */


    public static void main(String[] args) throws NotBoundException, IOException {

        System.out.println("llego hasta linea 10");
        //args para cliente:
        //  ip
        //  puerto
        //  usuario
        //  carpeta
        //  hilos

        String ip = null;
        String puerto = null;
        String usuario = null;
        String carpeta = null;
        String hilos = null;

        boolean executeInGUI = false;


        // Comprobamos los argumentos introducidos

        if(args != null) {
            for (int i = 0; i < args.length; ) {
                switch (args[i]) {
                    case "ip" -> {
                        ip = args[i + 1];
                        i += 2; //aumentamos otro valor mas porque de args hemos usado el parametro "i" e "i+1"
                    }
                    case "port" -> {
                        puerto = args[i + 1];
                        i += 2;
                    }
                    case "folder" -> {
                        String rawFolder = args[i + 1];
                        carpeta = (rawFolder.charAt(0) == '"') ? rawFolder.substring(1, rawFolder.length() - 2) : rawFolder;
                        //si la carpeta se introduce como "folder" quitamos las comillas "" -> substring
                        i += 2;
                    }
                    case "user" -> {
                        usuario = args[i + 1];
                        i += 2;
                    }
                    case "threads" -> {
                        hilos = args[i + 1];
                        i += 2;
                    }
                    case "gui" -> {
                        executeInGUI = true;
                        i += 1;
                    }
                }
            }
    
        }


        // Comprobamos los valores introducidos. En caso de que no se introduzca alguno de los parametros, ponemos
        // el valor por defecto

        if(ip == null || ip.length() <8 || ip.length() >16 ) {
            System.out.println("direccion ip no indicada, o direccion ip introducida no es valida. Usando localhost como ip");
            ip = "localhost";
        }

        if(puerto == null || puerto.length() <1) {
            puerto = "1099";
        }

        if(usuario == null || usuario.length() <1) {
            Path tmp = Paths.get(System.getProperty("user.home"));
            usuario = tmp.getFileName().toString();
        }

        if(carpeta == null || carpeta.length() <1) {
            carpeta = "syncappshared";
        }

        if(hilos == null || hilos.length() <1) {
            hilos = "4";
        }

        //Tenemos que introducir los parametros en el siguiente orden
        //args para cliente:
        //  ip
        //  puerto
        //  usuario
        //  carpeta
        //  hilos
        String[] newFixedArgs = new String[] {ip, puerto, usuario, carpeta, hilos};

        
        // Comprobamos en que modo se ejecutara el cliente, indicando ademas los argumentos para el cliente.
        if(executeInGUI) {
            ClienteGUI interfaz = new ClienteGUI(newFixedArgs);
        } else {
            ClienteCLI.main(newFixedArgs);
        }




    }
    
}
