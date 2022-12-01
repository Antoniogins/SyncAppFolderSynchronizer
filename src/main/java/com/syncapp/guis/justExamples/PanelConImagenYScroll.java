package com.syncapp.guis.justExamples;
import java.awt.*;
import javax.swing.*;


public class PanelConImagenYScroll extends JFrame{


    // Imagen a dibujar:
    Image miImagen;
        
    // Panel donde incluir la imagen.
    PanelImagen panelImagen;
   
    // scroll pane en el que se incluye el panel de la imagen:
    JScrollPane marcoImagen;
       
 
    public PanelConImagenYScroll(){

       super("Imagen con Scroll");
       
       construirPaneles();
       setSize (400,400);
       setVisible(true);  
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);              
    }

    private void construirPaneles(){

       // Creamos la imagen a partir de un fichero.    
       String path = "src/main/resources/images/PUERTO.jpg";    
       ImageIcon imi = new ImageIcon(path); 
       miImagen = imi.getImage();

       // Creamos el panel que va a contener la imagen.
       panelImagen = new PanelImagen(miImagen);    
     
       // Creamos el marco en el que incluiremos el
       // panel que incluye a la imagen.
       // Las dimensiones de este marco delimitan la parte de la imagen
       // que es visible en la ventana.
       JScrollPane marcoImagen = new JScrollPane(panelImagen);
       marcoImagen.setPreferredSize(new Dimension(450, 450));
       marcoImagen.setViewportBorder(
                BorderFactory.createLineBorder(Color.black));
       marcoImagen.getVerticalScrollBar().setUnitIncrement(10);
       marcoImagen.getHorizontalScrollBar().setUnitIncrement(10);
               
       // Lo a�adimos a la ventana principal.
       getContentPane().add(marcoImagen);           
    }
    
    class PanelImagen extends JPanel {

        private Image miImagen;
   
        public PanelImagen(Image miImagen){
	   
            this.miImagen = miImagen;
          
            int xd = miImagen.getWidth(this);
            int yd = miImagen.getHeight(this);  
	           
            setPreferredSize(new Dimension(xd, yd));   
        }
   
        public void paintComponent(Graphics g) {
            super.paintComponent(g);           
            g.drawImage(miImagen, 0, 0, this);
            g.drawRect(0, 0, 100, 100); 
        }
    }    
    
    
    public static void main(String [] args){
       PanelConImagenYScroll gui = new PanelConImagenYScroll();
    }
}




