package com.syncapp.guis.justExamples;
import java.awt.Color;
import javax.swing.*;

/**
    - Crea una ventana visible.
    - A�ade una Barra de Men� que a su vez incluye
       - Dos men�s, uno de ellos con submen�s.
       - 2 Botones.
 
   @author TIC-LSI 
 */
 
public class MenusYBotones extends JFrame{
   
    private static final long serialVersionUID = 5110341197899182147L;

    JMenuBar barraDelMenu;
    JMenu menu_1, menu_2, menu_3;
    JMenuItem item_1, item_2, item_3, item_4, item_5;
    JButton b1, b2, b3;

    public MenusYBotones(){

       super("Menus y botones");
       
       construirMenus();
         
       setSize (400,240);
       setVisible(true);           
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                       
    }

    private void construirMenus(){
    
       // Creamos Barra de Menu.   
       barraDelMenu = new JMenuBar();
       
       // Creamos Menus.
       menu_1 = new JMenu("menu 1");
       menu_2 = new JMenu("menu 2");  
       menu_3 = new JMenu("menu 3");  
         
       // Creamos entradas de menus.
       item_1 = new JMenuItem("item_1");       
       item_2 = new JMenuItem("item_2");  
       item_3 = new JMenuItem("item_3");        
       item_4 = new JMenuItem("item_4");         
       item_5 = new JMenuItem("item_5");                
       
       // Creamos botones.    
       b1 = new JButton("b1");
       b2 = new JButton("b2");  
       b3 = new JButton("b3");         
  
       // Anadimos elementos a barra de menu.
       barraDelMenu.add(menu_1);
       barraDelMenu.add(menu_2);
       barraDelMenu.add(b1);
       barraDelMenu.add(b2);
             
       // Anadimos elementos a menu 1.
       menu_1.add(item_1); 
       menu_1.addSeparator();
       menu_1.add(menu_3);  
       
       // Anadimos elementos a menu 3.
       menu_3.add(item_2); 
       menu_3.add(item_3); 
       
       // Anadimos elementos a men� 2.              
       menu_2.add(item_4); 
       menu_2.add(item_5);  
       menu_2.add(b3);
       
       // Le ponemos un borde a la barra de menu y lo anadimos a la ventana.
       barraDelMenu.setBorder(BorderFactory.createLineBorder(Color.blue));
       setJMenuBar(barraDelMenu);             
    }
    
    public static void main(String [] args){
       MenusYBotones gui = new MenusYBotones();
    }
}