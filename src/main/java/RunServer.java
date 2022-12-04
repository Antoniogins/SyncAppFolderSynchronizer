import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import com.syncapp.server.SyncAppServer;

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
     *             <b>ip dirIp</b> -> para indicar la direccion ip del registro RMI. Valor por defecto es <i>localhost</i>:
     *         </li>
     *         <li>
     *             <b>port</b> -> para indicar el puerto del registro RMI. Valor por defecto es <i>1099</i>.
     *         </li>
     *     </ul>
     * </p>
     *
     *
     * @param args
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws RemoteException, MalformedURLException {

        SyncAppServer sap = new SyncAppServer();
        Naming.rebind("rmi://localhost:1099/SyncApp", sap);
        System.out.println("ready to operate");


    }

}
