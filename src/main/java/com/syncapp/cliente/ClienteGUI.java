package com.syncapp.cliente;


import java.awt.*;
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

	public class ClienteGUI extends JFrame implements ActionListener {

		// Variables
		private SyncAppCliente sac;
		private JLabel label_informacion, label_info_velocidad;
		private JPanel panel_central, panel_botones, panel_informacion, panel_inferior;
		private JPanel panel_cremota, panel_x1, panel_x2;
		private JButton boton_servidor, boton_usuario, boton_carpeta_local, boton_carpeta_remota, boton_restart, boton_incognita_dos;
	
		// Al constructor de la clase vista le pasamos el cliente
		public ClienteGUI(SyncAppCliente sac) {
			super("Cliente SyncApp");
			if (sac == null)
				return;

			this.sac = sac;

			Container contenedor = getContentPane();
			contenedor.setLayout(new BorderLayout());
		
	//Ahora creamos los paneles auxiliares para pasarlos al panel central del borderlayout
		
		panel_central = new JPanel (new BorderLayout());//este contiene todos los paneles que vamos a crear
		panel_botones = new JPanel (new FlowLayout());
		panel_inferior = new JPanel (new FlowLayout());
		
		//panel de información central
		panel_informacion = new JPanel (new BorderLayout());
		panel_central.add(panel_informacion);

		//creamos los botones y los añadimos al panel
		boton_servidor = new JButton ("Seleccionar servidor");
		boton_servidor.addActionListener(this);
		
		boton_usuario = new JButton ("Seleccionar usuario");
		boton_usuario.addActionListener(this);
		
		boton_carpeta_local = new JButton ("Seleccionar carpeta local");
		boton_carpeta_local.addActionListener (this);
		
		boton_carpeta_remota = new JButton ("seleccionar carpeta remota");
		boton_carpeta_remota.addActionListener(this);
		
		boton_restart = new JButton ("Poner nombre uno");
		boton_incognita_dos = new JButton ("Poner nombre dos");
		
		panel_botones.add(boton_servidor);
		panel_botones.add(boton_usuario);
		panel_botones.add(boton_carpeta_local);
		panel_botones.add(boton_carpeta_remota);
		panel_botones.add(boton_restart);
		panel_botones.add(boton_incognita_dos);
		
		
		
		
	//Se lo añadimos al borderlayout	
		contenedor.add(panel_central, BorderLayout.CENTER);
		contenedor.add(panel_botones, BorderLayout.NORTH);
		contenedor.add(panel_inferior, BorderLayout.SOUTH);
		
	//Esto siempre hay que ponerlo
		
		setTitle ("Ponte titulo");
		setSize (800,800);
		setVisible(true);

		}
	
		
		
		
		//HASTA AQUI ES LO DEL PROYECTO
		
		
		




		@Override
		public void actionPerformed(ActionEvent e) {
			JButton origen = (JButton) e.getSource();
			


			if(origen == boton_servidor) {
				String ip = JOptionPane.showInputDialog(origen, "Introduce la ip", "localhost");
				//obtener IP y pasarla como parametro
				//sac.restart
				
			}
			else if(origen == boton_carpeta_local) {
				//sac.reload

			}
			else if(origen == boton_carpeta_remota) {
				//sac.reload


			}
			else if(origen == boton_usuario) {
				//sac.restart

			}
			else if(origen == boton_restart) {
				//sac.restart

			}
			else if(origen == boton_incognita_dos) {
				
			}


			
		}
		
		
		
		
		
}
