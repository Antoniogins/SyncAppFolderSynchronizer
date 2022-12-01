package com.syncapp.guis.justExamples;
import javax.swing.*;

/**
   Crea una ventana visible con un nombre.
  
   @author TIC-LSI 
 */
public class UnaVentana extends JFrame{

    private static final long serialVersionUID = -276938680268304313L;

    public UnaVentana(){
    
       super("Una Ventana");
    
       // Fijamos tama�o de la ventana.       
       setSize (400,240);
       
       // Hacemos la ventana visible.
       setVisible(true);  
       
       // Forzamos a que la aplicaci�n termine al cerrar la ventana.     
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                            
    }
    
    public static void main(String [] args){
       UnaVentana gui = new UnaVentana();
    }
}