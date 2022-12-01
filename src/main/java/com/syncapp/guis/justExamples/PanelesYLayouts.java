package com.syncapp.guis.justExamples;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.*;

 
public class PanelesYLayouts extends JFrame implements ActionListener {

    private static final long serialVersionUID = 4915693836532900203L;

    JButton btGL2x3, btGL3x2, btFL, btBL, btAddPanel;
    JPanel pnButtons;
    JPanel pnSouth;
    JPanel pnCenter;
    JPanel p[] = new JPanel[5];
    
    JLabel lbInfo;
    

    public PanelesYLayouts(){

        super("Paneles y layouts");
       
        btAddPanel = new JButton("Add Panel");
        btAddPanel.addActionListener(this);      
        lbInfo = new JLabel("Waiting for action");
        lbInfo.setHorizontalAlignment(SwingConstants.CENTER);
       
        pnSouth = new JPanel();
        pnSouth.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        pnSouth.setLayout( new GridLayout(3,1));
        pnSouth.add(btAddPanel);
        pnSouth.add(lbInfo);
       
        getContentPane().add(pnSouth, BorderLayout.SOUTH);
       
        for (int i = 0; i < 5; i++){
            p[i] = new JPanel();
            Border b = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.blue), "Panel_" + i);
            p[i].setBorder(b);
            p[i].setPreferredSize(new Dimension(100,100));
        }
              
       setSize (480,480);
       setVisible(true);     
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                  
    }


    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btAddPanel) {
            
            if (pnCenter == null){                
                pnCenter = new JPanel();
                pnCenter.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));                
                for (int i = 0; i < p.length; i++){
                    pnCenter.add(p[i]);
                }                
                getContentPane().add(pnCenter);
            }            
            else if (pnButtons == null){
                        
                pnButtons = new JPanel();
                pnButtons.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
                
                btGL2x3 = new JButton("Grid 2x3");
                btGL2x3.addActionListener(this);
                pnButtons.add(btGL2x3);
                
                btGL3x2 = new JButton("Grid 3x2");
                btGL3x2.addActionListener(this);
                pnButtons.add(btGL3x2);
                
                btFL = new JButton("Flow Layout");
                btFL.addActionListener(this);
                pnButtons.add(btFL);
                
                btBL = new JButton("Border Layout");
                btBL.addActionListener(this);
                pnButtons.add(btBL);
                
                pnSouth.add(pnButtons);           
            }
        }
        else if (ae.getSource() == btGL2x3){
            pnCenter.setLayout(new GridLayout(2,3));
        }
        else if (ae.getSource() == btGL3x2){
            pnCenter.setLayout(new GridLayout(3,2));
        }
        else if (ae.getSource() == btFL){
            pnCenter.setLayout(new FlowLayout());
        }
        else if (ae.getSource() == btBL){
            pnCenter.setLayout(new BorderLayout());
            pnCenter.add(p[0], BorderLayout.SOUTH);
            pnCenter.add(p[1], BorderLayout.EAST);
            pnCenter.add(p[2], BorderLayout.NORTH);
            pnCenter.add(p[3], BorderLayout.WEST);
            pnCenter.add(p[4]);
        }        
        this.validate();
    }
    
    public static void main(String [] args){
       PanelesYLayouts gui = new PanelesYLayouts();
    }  
}