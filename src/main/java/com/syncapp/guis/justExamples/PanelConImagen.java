package com.syncapp.guis.justExamples;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
    - Crea una ventana visible.
    - A�ade dos paneles modelados como clases internas. 
    - En uno de ellos coloca una imagen.
    - En el otro 6 botones colocados seg�n un GridLayout.
    - Se utilizan diferentes gestores de dise�o. 
  
   @author TIC-LSI 
 */
 
public class PanelConImagen extends JFrame {
    
    PanelGrafica     panelGrafica;

    public PanelConImagen(){

        super("Panel con Imagen");
       
        // Construimos y a�adimos paneles.
        construirPaneles();
         
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize (800,640);
        setVisible(true);                       
    }


    private void construirPaneles(){
    
       // Creamos los dos paneles.
       panelGrafica =   new PanelGrafica();
             
       // Creamos bordes y se los a�adimos a los paneles.
       panelGrafica.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
              
       // A�adimos paneles a la ventana (a su contentPane).
       getContentPane().add(panelGrafica);
    }
    
    
    public static void main(String [] args){
       PanelConImagen gui = new PanelConImagen();
    }
    

   /**
    * Clase interna: 
    * Utiliza layout por defecto.
    * 
    */
    class PanelGrafica extends JPanel{

        JLabel lb;

        // Creamos la imagen a partir de un fichero.
        String path = "src/main/resources/images/PUERTO.jpg";  
        ImageIcon imi = new ImageIcon(path);
        Image miImagen = imi.getImage();

            public PanelGrafica(){
            lb = new JLabel("Aqui puede ir una imagen");
            add(lb);  
        } 

        public void paintComponent(Graphics g){
            super.paintComponent(g);
            g.drawImage(miImagen, 0, 0, this);         
        }                	     	    
    }     
}