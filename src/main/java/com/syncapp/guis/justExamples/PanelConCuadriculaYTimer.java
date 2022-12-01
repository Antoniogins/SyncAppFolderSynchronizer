/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.syncapp.guis.justExamples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

/**
 *
 * @author juanangel
 */
public class PanelConCuadriculaYTimer extends JFrame implements KeyListener, ActionListener {

    
    public static final int UP_KEY    = 38;
    public static final int DOWN_KEY  = 40;
    public static final int RIGTH_KEY = 39;
    public static final int LEFT_KEY  = 37;
    public static final int SPACE_KEY = 32;

    int lastKey = DOWN_KEY;
    
    public static final int CANVAS_WIDTH = 480;
    
    int boxSize = 40;
    int row, col;
    
    Canvas canvas;
    JPanel canvasFrame;
    JLabel positionLabel;
    
    Timer timer;
    int tick = 200;
    

    public PanelConCuadriculaYTimer() throws Exception{

       super("Panel Con Cuadricula y timer");
            
       positionLabel = new JLabel("[" + col + ", " + row + "]");
       positionLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)); 
       positionLabel.setPreferredSize(new Dimension(120,40));
       positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
       
       canvas = new Canvas(CANVAS_WIDTH, boxSize);
       canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_WIDTH));
       canvas.setBorder(BorderFactory.createLineBorder(Color.blue));
       
       canvasFrame = new JPanel();
       canvasFrame.setPreferredSize(new Dimension(CANVAS_WIDTH + 40, CANVAS_WIDTH + 40));
       canvasFrame.add(canvas);
       getContentPane().add(canvasFrame);
       getContentPane().add(positionLabel, BorderLayout.SOUTH);
       
       setSize (CANVAS_WIDTH + 40, CANVAS_WIDTH + 80);
       setResizable(false);
       setVisible(true);         
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    
       
       addKeyListener(this);
       System.out.println(this.getFocusableWindowState());
       this.setFocusable(true);
       timer = new Timer(tick, this);
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    // Version 1
    @Override
    public void keyPressed(KeyEvent ke) {
        lastKey = ke.getKeyCode(); 
        if (lastKey == SPACE_KEY){
            if (timer.isRunning()){
                    timer.stop();
                }
                else{
                    timer.start();
                }
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        System.out.println("code --> " + lastKey);
        switch (lastKey) {
            case UP_KEY:  
                row--;
                break;
            case DOWN_KEY:
                row++;                    
                break;
            case RIGTH_KEY:
                col++;
                break;
            case LEFT_KEY:
                col--;
                break; 
        }
        setInLimits();
        positionLabel.setText("[" + col + ", " + row + "]");
        canvas.setSquareCoordinates(col, row);  
    }
    
    class Canvas extends JPanel {
        
        int size, boxSize;
        int pX, pY;
        
        public Canvas(int size, int boxSize){
            this.size = size;
            this.boxSize = boxSize;
        }
        
        public void setSquareCoordinates(int x, int y){
            pX = x;
            pY = y;
            repaint();
        }
        
        public void paintComponent(Graphics g){
            super.paintComponent(g);   
            drawGrid(g); 
            g.setColor(Color.red);
            g.fillRect(pX*boxSize+4, pY*boxSize+4, boxSize-8, boxSize-8);
        }  

        private void drawGrid(Graphics g){
            Color c = g.getColor();
            // Grid
            g.setColor(Color.LIGHT_GRAY);
            int nLines = size/boxSize;
            for (int i = 1; i < nLines; i++){
               g.drawLine(i*boxSize, 0, i*boxSize, size);
               g.drawLine(0, i*boxSize, size, i*boxSize);
            } 
            g.setColor(c);
        }        
    }
    
    private void setInLimits(){
        
        int lastBox = (CANVAS_WIDTH/boxSize) - 1;
        
        if (col < 0){
            col = 0;
        }
        else if ( col > lastBox ){
            col = lastBox;
        }
        
        if (row < 0){
            row = 0;
        }
        else if ( row > lastBox){
            row = lastBox;
        } 
    }
    
    public static void main(String [] args) throws Exception{
       PanelConCuadriculaYTimer gui = new PanelConCuadriculaYTimer();
    }
}
