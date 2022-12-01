package com.syncapp.guis.justExamples;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


public class PanelConScrollYEventosDeRaton extends JFrame{

    
    PanelImagen2 panelImagen;
    Image miImagen;
    JScrollPane marcoImagen;
           
    // Etiqueta informativa de la posicion del raton.
    JLabel infoLabel;
    
    public PanelConScrollYEventosDeRaton(){

       super("Eventos de Rat�n");
       
       infoLabel = new JLabel("Waiting for mouse event");
       infoLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)); 
       infoLabel.setPreferredSize(new Dimension(150,50));
       
       // Construimos y a�adimos paneles.
       construirPaneles();
         
       setSize (600,600);
       setResizable(false);
       setVisible(true);  
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);              
    }

    private void construirPaneles(){

       // Creamos una imagen a partir de un fichero.
       String path = "src/main/resources/images/PUERTO.jpg"; 
       ImageIcon imi = new ImageIcon(path);   
       miImagen = imi.getImage();

       // Creamos un panel donde colocar la imagen
       panelImagen = new PanelImagen2(miImagen); 
       panelImagen.setInfoLabel(infoLabel);   
     
       // Creamos un panel con scroll donde colocaremos el panel 
       // que contiene la imagen.
       // Las dimensiones del panel con scroll delimitan la parte 
       // visible de la imagen.
       JScrollPane marcoImagen = new JScrollPane(panelImagen);
       marcoImagen.setPreferredSize(new Dimension(450, 450));
       marcoImagen.setViewportBorder(
                BorderFactory.createLineBorder(Color.black));
       marcoImagen.getVerticalScrollBar().setUnitIncrement(10);
       marcoImagen.getHorizontalScrollBar().setUnitIncrement(10);
       
       // Añadimos el marco y la etiqueta informativa al content Pane
       getContentPane().setLayout(new FlowLayout());
       getContentPane().add(marcoImagen);
       getContentPane().add(infoLabel);          
    }
    
      
    public static void main(String [] args){
       PanelConScrollYEventosDeRaton gui = new PanelConScrollYEventosDeRaton();
    }
}

/**
   Panel con una imagen que maneja los eventos de rat�n que se producen sobre 
   el mismo.
   
  */     
class PanelImagen2 extends JPanel implements MouseListener, MouseMotionListener {

   private Image miImagen;
   private JLabel infoLabel;
   
   public PanelImagen2(Image miImagen){
	   
      this.miImagen = miImagen;
      
      // Se fijan las dimensiones del tablero de acuerdo
      // con las dimensiones del dibujo que contiene.
      int xd = miImagen.getWidth(this);
      int yd = miImagen.getHeight(this);         
      setPreferredSize(new Dimension(xd, yd));   
      
      addMouseListener(this);
      addMouseMotionListener(this);

   }
   
   public void paintComponent(Graphics g) {
   	   super.paintComponent(g);           
         g.drawImage(miImagen, 0, 0, this);
   }
   
   public void setInfoLabel(JLabel infoLabel){
	   this.infoLabel = infoLabel;
   }   
   
   @Override
   public void mouseClicked(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseClicked en x = " + e.getX() + " y = " + e.getY());
	     else
	        System.out.println("mouseClicked en x = " + e.getX() + " y = " + e.getY());
   }
   
   @Override
   public void mousePressed(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mousePressed en x = " + e.getX() + " y = " + e.getY());
	     else
   	     System.out.println("mousePressed en x = " + e.getX() + " y = " + e.getY());	   
   }   

   @Override
   public void mouseReleased(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseReleased en x = " + e.getX() + " y = " + e.getY());
	     else
	        System.out.println("mouseReleased en x = " + e.getX() + " y = " + e.getY());
   } 
   
   @Override
   public void mouseEntered(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseEntered en x = " + e.getX() + " y = " + e.getY());
	     else
           System.out.println("MouseEntered en x = " + e.getX() + " y = " + e.getY());
   } 
   
   @Override
   public void mouseExited(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseExited");
	     else	   
           System.out.println("mouseExited");   
   }   
   
   @Override
   public void mouseDragged(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseDragged: [" + e.getX() + ", " + e.getY() + "]");
	     else
           System.out.println("mouseDragged en x = " + e.getX() + " y = " + e.getY());
   } 
   
   @Override
   public void mouseMoved(MouseEvent e){
   	  if (infoLabel != null)
	        infoLabel.setText("mouseMoved: [" + e.getX() + ", " + e.getY() + "]");
	     else
           System.out.println("mouseMoved: [" + e.getX() + ", " + e.getY() + "]");   
   }     
}




