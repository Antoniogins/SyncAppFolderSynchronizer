import java.net.MalformedURLException;
import java.rmi.RemoteException;

import com.syncapp.server.SyncAppServer;

public class RunServer {
    
    public static void main(String[] args) throws RemoteException, MalformedURLException {
        SyncAppServer.main(args);
    }

}
