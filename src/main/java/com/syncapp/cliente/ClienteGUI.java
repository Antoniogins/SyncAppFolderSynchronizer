package com.syncapp.cliente;


import java.awt.*;
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClienteGUI extends JFrame implements ActionListener {

	// Variables
	private SyncAppCliente sac;
	private JLabel label_informacion, label_info_velocidad;

	private JLabel ip, puerto, usuario, password, carpetalocal, carpetaremota;
	private JPanel panel_cremota, panel_x1, panel_x2;
	private JButton bservidor, busuario, bcarpetalocal, bcarpetaremota, brestart, bejecutar;

	private PopupMenu pop_servidor, pop_usuario, pop_carpeta_local, pop_carpeta_remota, pop_restart, pop_ejecutar;

	// Al constructor de la clase vista le pasamos el cliente
	public ClienteGUI(SyncAppCliente sac) {
		super("Cliente SyncApp");
		if (sac == null)
			return;

		this.sac = sac;

		Container contenedor = getContentPane();
		contenedor.setLayout(new BorderLayout());


		JPanel pcentral = new JPanel(new BorderLayout());//este contiene todos los paneles que vamos a crear
		JPanel pbotones = new JPanel(new FlowLayout());
		JPanel pinferior = new JPanel(new FlowLayout());

		//panel de información central
		JPanel pinformacion = new JPanel(new BorderLayout());
		pcentral.add(pinformacion);

		//creamos los botones y los añadimos al panel
		bservidor = new JButton ("Seleccionar servidor");
		bservidor.addActionListener(this);


		busuario = new JButton ("Seleccionar usuario");
		busuario.addActionListener(this);

		bcarpetalocal = new JButton ("Seleccionar carpeta local");
		bcarpetalocal.addActionListener (this);

		bcarpetaremota = new JButton ("seleccionar carpeta remota");
		bcarpetaremota.addActionListener(this);

		bejecutar = new JButton ("Ejecutar");
		bejecutar.addActionListener(this);
		brestart = new JButton ("Restart");
		brestart.addActionListener(this);


		pbotones.add(bservidor);
		pbotones.add(busuario);
		pbotones.add(bcarpetalocal);
		pbotones.add(bcarpetaremota);
		pbotones.add(bejecutar);
		pbotones.add(brestart);





		//Se lo añadimos al borderlayout
		contenedor.add(pcentral, BorderLayout.CENTER);
		contenedor.add(pbotones, BorderLayout.NORTH);
		contenedor.add(pinferior, BorderLayout.SOUTH);

		//Esto siempre hay que ponerlo

		setTitle ("Ponte titulo");
		setSize (800,800);
		setVisible(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton origen = (JButton) e.getSource();



		if(origen == bservidor) {
			bservidor.add(pop_servidor);
			pop_servidor.add(String.valueOf(ip));
			JLabel ip = new JLabel ("Introduce la ip del servidor");
			ip.add(new JTextField(15));
			pop_servidor.add(String.valueOf(puerto));
			JLabel puerto = new JLabel ("Introduce el puerto del servidor");
			ip.add(new JTextField(15));
			//obtener IP y pasarla como parametro
			//sac.restart

		}
		else if(origen == bcarpetalocal) {
			bcarpetalocal.add(pop_carpeta_local);
			pop_carpeta_local.add(String.valueOf(carpetalocal));
			JLabel carpetalocal = new JLabel("Selecciona la carpeta local que quieres sincronizar");
			carpetalocal.add(new JTextField(200));
			//sac.reload

		}
		else if(origen == bcarpetaremota) {
			pop_carpeta_remota.add(String.valueOf(carpetaremota));
			JLabel carpetaremota = new JLabel ("Introduce la carpeta remota que quieres sincrinzar");
			ip.add(new JTextField(200));
			//sac.reload


		}
		else if(origen == busuario) {
			busuario.add(pop_usuario);
			pop_usuario.add(String.valueOf(usuario));
			JLabel usuario = new JLabel ("Introduce tu usuario");
			usuario.add(new JTextField(15));
			pop_usuario.add(String.valueOf(password));
			JLabel password = new JLabel ("Introduce tu contraseña");
			usuario.add(new JTextField(15));
			//sac.restart

		}
		else if(origen == brestart) {
			//sac.restart;

		}
		else if(origen == bejecutar) {
			//sa.ejecutar;
		}



	}





}