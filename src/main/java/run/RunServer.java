package run;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import com.syncapp.server.SyncAppServer;


/**
 * Clase que inicia el servicio "cloud" SyncApp
 */
public class RunServer {

    /**
     * <p>
     *     Este metodo estatico nos permite iniciar el servidor de SyncApp.
     * </p>
     * <p>
     *     Se pueden indicar los siguientes argumentos:
     *
     *     <ul>
     *         <li>
     *             <b>ip a.b.c.d</b> -> para indicar la direccion ip del registro RMI. Valor por defecto es <i>localhost</i>:
     *         </li>
     *         <li>
     *             <b>port puerto</b> -> para indicar el puerto del registro RMI. Valor por defecto es <i>1099</i>.
     *         </li>
     *     </ul>
     * </p>
     *
     *
     * @param args array de argumentos separados por espacios.
     * @throws RemoteException cuando ocurre algun error ejecutando los serivicios.
     * @throws MalformedURLException cuando la url indicada para el registro rmi no alcanza al registro rmi.
     */
    public static void main(String[] args) throws RemoteException, MalformedURLException {

        String ip = null;
        String puerto = null;


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
                }
            }

        }


        // Comprobamos los valores introducidos. En caso de que no se introduzca alguno de los parametros, ponemos
        // el valor por defecto

        if(ip == null || ip.length() <8 || ip.length() >16 ) {
            System.out.println("direccion ip no indicada, o direccion ip introducida no es valida. Usando localhost como ip");
            ip = "localhost";
        }

        if(puerto == null || puerto.length() <1 ) {
            puerto = "1099";
        }


        SyncAppServer sap = new SyncAppServer();
        System.out.println(ip+":"+puerto);
        Naming.rebind("rmi://"+ip+":"+puerto+"/SyncApp", sap);
        System.out.println("ready to operate");


    }

}
