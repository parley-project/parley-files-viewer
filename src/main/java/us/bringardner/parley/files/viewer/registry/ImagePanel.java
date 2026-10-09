package us.bringardner.parley.files.viewer.registry;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ImagePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	
	/**
	 * Create the panel.
	 */
	public ImagePanel(BufferedImage image) {
		setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel(""+image.getWidth()+" X "+image.getHeight());
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblNewLabel, BorderLayout.NORTH);
		
		JPanel imagePanel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {				
				g.setColor(Color.white);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.drawImage(image, 0, 0, null);				
			}
		};
		
		imagePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		
		add(imagePanel, BorderLayout.CENTER);

	}

}
