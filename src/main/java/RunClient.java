import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

import com.syncapp.cliente.ClienteCLI;

public class RunClient {
    
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

        boolean executeInConsole = false;


        if(args != null) {
            for (int i = 0; i < args.length; i++) {
                switch(args[i]) {
    
                    case "ip" : {
                        ip = args[i+1];
                        i++; //aumentamos otro valor mas porque de args hemos usado el parametro "i" e "i+1"
                        break;
                    }
                    case "port" : {
                        puerto = args[i+1];
                        i++;
                        break;
                    }
                    case "folder" : {
                        String rawFolder = args[i+1];
                        carpeta = (rawFolder.charAt(0) == '"')? rawFolder.substring(1, rawFolder.length()-2) : rawFolder ;
                        //si la carpeta se introduce como "folder" quitamos las comillas "" -> substring
                        i++;
                        break;
                    }
                    case "user" : {
                        usuario = args[i+1];
                        i++;
                        break;
                    }
                    case "threads" : {
                        hilos = args[i+1];
                        i++;
                        break;
                    }
                    case "console" :{
                        executeInConsole = true;
                        break;
                    }
                }
            }
    
        }
        
        if(ip == null || ip.length() <1) {
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

        
        //aqui toca pasar los argumentos al controlador
        if(executeInConsole) {
            ClienteCLI.main(newFixedArgs);
        } else {
            ClienteCLI.main(newFixedArgs); //ESTE SE CAMBIARA CUANDO TENGAMOS LA INTERFAZ
            //ClienteGUI.main(newFixedArgs); //AQUI SE EJECUTARA EL CLIENTE POR CONSOLA
        }

    }
    
}
