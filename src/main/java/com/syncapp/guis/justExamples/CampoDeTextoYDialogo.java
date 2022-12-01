package com.syncapp.guis.justExamples;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
   Ejemplo que:
   
    - Tiene una etiqueta y un campo de texto.
    - Maneja los eventos que se producen en el campo de texto.
    - Muestra un cuadro de di�logo que informa de los errores
      del operador.
  
   @author TIC-LSI 
 */
public class CampoDeTextoYDialogo extends JFrame implements ActionListener{
    
    
    private JLabel etiqueta;
    private JTextField campoTexto;  
    

    public CampoDeTextoYDialogo(){

       super("Campo de Texto");
       
       // Creamos etiqueta.
       etiqueta = new JLabel("Introduzca Nro mayor que 0:");
       etiqueta.setHorizontalAlignment(SwingConstants.LEFT); 
       etiqueta.setPreferredSize(new Dimension(250,30)); 
       etiqueta.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));  
             
       // Creamos campo de texto.      
       campoTexto = new JTextField(20);
       campoTexto.setPreferredSize(new Dimension(150,30));     
       
       // La ventana se suscribe a los eventos del campo de texto.  
       campoTexto.addActionListener(this);          
              
       // A�adimos etiqueta y campo de texto.
       getContentPane().setLayout(new FlowLayout());
       getContentPane().add(etiqueta);
       getContentPane().add(campoTexto);
             
        
       // Fijamos tama�o de la ventana.       
       setSize (520,120);
       
       // Hacemos la ventana visible.
       setVisible(true);     
       
       // Forzamos a que la aplicaci�n termine al cerrar la ventana.     
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                  
    }
 
    public void actionPerformed(ActionEvent ae){
    
       	try{
            int num = Integer.parseInt(campoTexto.getText());
            System.out.println("Numero = " + num);
            if (num <= 0)
                JOptionPane.showMessageDialog(this, "El n�mero debe ser > 0"); 
       	}
        catch(Exception e){
            System.out.println(e.toString());
            JOptionPane.showMessageDialog(this,
                                           e.toString(),
                                           "Input error",
                                           JOptionPane.ERROR_MESSAGE);
        } 
    }
    
    public static void main(String [] args){
       CampoDeTextoYDialogo gui = new CampoDeTextoYDialogo();
    }
}