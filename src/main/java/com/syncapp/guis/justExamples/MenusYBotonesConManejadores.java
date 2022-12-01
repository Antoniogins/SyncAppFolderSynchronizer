package com.syncapp.guis.justExamples;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
    - Crea una ventana visible.
    - A�ade una Barra de Men� que a su vez incluye
       - Dos men�s, uno de ellos con submen�s.
       - 2 Botones.
 
   @author TIC-LSI 
 */
 
public class MenusYBotonesConManejadores extends JFrame implements ActionListener {

    private static final long serialVersionUID = 5110341197899182147L;

    JMenuBar barraDelMenu;
    JMenu menu_1, menu_2, menu_3;
    JMenuItem item_1, item_2, item_3, item_4, item_5;
    JButton b1, b2, b3;


    public MenusYBotonesConManejadores(){

       super("Menus y botones con manejadores de eventos");
       
       construirMenus();
       setSize (300,240);
       setVisible(true);       
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                       
    }

    private void construirMenus(){
    
       barraDelMenu = new JMenuBar();
       
       menu_1 = new JMenu("menu 1");
       menu_2 = new JMenu("menu 2");  
       menu_3 = new JMenu("menu 3");  
         
       // Creamos entradas de menus y suscribimos la ventana
       // a los eventos producidos en dichas entradas.
       item_1 = new JMenuItem("item_1");   
       item_1.addActionListener(this);
       item_2 = new JMenuItem("item_2");  
       item_2.addActionListener(this);
       item_3 = new JMenuItem("item_3");  
       item_3.addActionListener(this);
       item_4 = new JMenuItem("item_4");  
       item_4.addActionListener(this);
       item_5 = new JMenuItem("item_5");     
       item_5.addActionListener(this);
       
       // Creamos un manejador especifico para los botones.   
       Manejador m = new Manejador();
       
       // Creamos los botones y suscribimos el manejador 'm' a los
       // eventos producidos por dichos botones.
       b1 = new JButton("b1");
       b1.addActionListener(m);
       b2 = new JButton("b2");  
       b2.addActionListener(m);
       
       // Creamos el botón b3, un manejador anónimo para el mismo y
       // suscribimos el manejador anónimo a su botón.
       b3 = new JButton("b3");   
       b3.addActionListener(
            new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae){
                    System.out.println("b3 !!!!!!!!!!!!!!!!!: ");
                }
        });
  
       // A�adimos elementos a barra de men�.
       barraDelMenu.add(menu_1);
       barraDelMenu.add(menu_2);
       barraDelMenu.add(b1);
       barraDelMenu.add(b2);
             
       // A�adimos elementos a men� 1.
       menu_1.add(item_1); 
       menu_1.addSeparator();
       menu_1.add(menu_3);  
       
       // A�adimos elementos a men� 3.
       menu_3.add(item_2); 
       menu_3.add(item_3); 
       
       // A�adimos elementos a men� 2.              
       menu_2.add(item_4); 
       menu_2.add(item_5);  
       menu_2.add(b3);
       
       // Le ponemos un borde a la barra de men� y lo a�adimos a la ventana.
       barraDelMenu.setBorder(BorderFactory.createLineBorder(Color.blue));
       setJMenuBar(barraDelMenu);             
    }
    
    @Override
    public void actionPerformed(ActionEvent evento) {
	if (evento.getSource() == item_1) {
            System.out.println("item_1 seleccionado.");
	}
	else if (evento.getSource() == item_2) {
            System.out.println("item_2 seleccionado.");
	}
	else if (evento.getSource() == item_3) {
            System.out.println("item_3 seleccionado.");
	}
	else if (evento.getSource() == item_4) {
            System.out.println("item_4 seleccionado.");
	}
	else if (evento.getSource() == item_5) {
            System.out.println("item_5 seleccionado.");
	}	
    }
    
    public static void main(String [] args){
       MenusYBotonesConManejadores gui = new MenusYBotonesConManejadores();
    }
}

class Manejador implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent evento) {
	System.out.println("Pulsado bot�n: " + evento.getActionCommand());
    }
}