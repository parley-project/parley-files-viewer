package us.bringardner.parley.files.viewer.registry;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceChooserDialog;

public class RegistryImageFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField;
	private JPanel imageListPanel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RegistryImageFrame frame = new RegistryImageFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public RegistryImageFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1045, 481);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel controlPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) controlPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(controlPanel, BorderLayout.NORTH);

		textField = new JTextField();
		controlPanel.add(textField);
		textField.setColumns(60);

		JButton btnNewButton = new JButton("Browse");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionBrowse();
			}
		});
		controlPanel.add(btnNewButton);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		imageListPanel = new JPanel();
		scrollPane.setViewportView(imageListPanel);
		imageListPanel.setLayout(new BoxLayout(imageListPanel, BoxLayout.Y_AXIS));
		loadImages("/Applications/GitHub Desktop.app/Contents/Resources/electron.icns");
		//loadImages("/Volumes/Data/Applications/ExecutableJars/OpenSCAD.app/Contents/Resources/OpenSCAD.icns");
	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}  

	private void loadImages(String path) {
		try {
			/*
			List<Icon> list = MacRegistry.getIcons(path);
			imageListPanel.removeAll();
			BufferedImage small = null;
			for(BufferedImage image : list) {
				imageListPanel.add(new ImagePanel(image));
				if( small == null || small.getHeight()>image.getHeight()) {
					small = image;
				}
			}
			if( small != null ) {
				imageListPanel.add(new ImagePanel(resize(small, 16, 16)));
			}
			contentPane.updateUI();
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void actionBrowse() {
		FileSourceChooserDialog fc = new FileSourceChooserDialog();
		if( fc.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
			FileSource file = fc.getSelectedFile();
			loadImages(file.getAbsolutePath());
		}

	}

}
