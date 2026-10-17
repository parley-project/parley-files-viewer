package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import us.bringardner.parley.brand.ParleyBrand;

/**
 * Help &gt; About: Margot on the Parley tower beside the viewer's name and version, in the Parley
 * palette (ink background, gold text).
 */
public class AboutFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** The Parley web site, opened by the link at the bottom. */
	public static final String PARLEY_SITE = "https://parley.bringardner.us";

	/** The hero is drawn this tall; it is 960 x 1680, so about 183 px wide. */
	static final int HERO_HEIGHT = 320;

	/** Text that is not gold: a warm off-white that reads on ink. */
	static final Color TEXT = new Color(0xE8E2D0);

	/**
	 * Launch the window on its own.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AboutFrame frame = new AboutFrame();
					frame.setLocationRelativeTo(null);
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
	public AboutFrame() {
		setTitle("About parley-files-viewer");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImages(ParleyBrand.appIcons("files"));
		setResizable(false);

		JPanel contentPane = new JPanel(new BorderLayout(24, 0));
		contentPane.setBackground(ParleyBrand.INK);
		contentPane.setBorder(new EmptyBorder(20, 20, 20, 28));
		setContentPane(contentPane);

		Image hero = ParleyBrand.hero();
		int width = hero.getWidth(null) * HERO_HEIGHT / hero.getHeight(null);
		contentPane.add(new JLabel(new ImageIcon(hero.getScaledInstance(width, HERO_HEIGHT, Image.SCALE_SMOOTH))), BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		contentPane.add(text, BorderLayout.CENTER);

		text.add(Box.createVerticalGlue());
		text.add(label("parley-files-viewer", Font.BOLD, 22f, ParleyBrand.GOLD_BRIGHT));
		String version = AboutFrame.class.getPackage().getImplementationVersion();
		if( version != null ) {
			text.add(label("Version " + version, Font.PLAIN, 13f, ParleyBrand.GOLD));
		}
		text.add(Box.createVerticalStrut(16));
		text.add(label("<html>Browse, copy, search and edit files on any<br>"
				+ "parley-files file system: local, memory,<br>FTP, SFTP and JDBC.</html>", Font.PLAIN, 13f, TEXT));
		text.add(Box.createVerticalStrut(12));
		text.add(label("<html>An example of what Parley can help you build.</html>", Font.ITALIC, 13f, TEXT));
		text.add(Box.createVerticalStrut(20));
		text.add(link(PARLEY_SITE));
		text.add(Box.createVerticalStrut(6));
		text.add(label("Apache License 2.0", Font.PLAIN, 11f, ParleyBrand.GOLD_DARK));
		text.add(Box.createVerticalGlue());

		pack();
	}

	private static JLabel label(String text, int style, float size, Color color) {
		JLabel ret = new JLabel(text);
		ret.setFont(ret.getFont().deriveFont(style, size));
		ret.setForeground(color);
		ret.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		return ret;
	}

	/** The site's address, opened in the browser when clicked (where the desktop can). */
	private static JLabel link(String url) {
		JLabel ret = label(url.replaceFirst("^https://", ""), Font.PLAIN, 13f, ParleyBrand.GOLD_BRIGHT);
		if( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ) {
			ret.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			ret.setToolTipText(url);
			ret.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						Desktop.getDesktop().browse(new URI(url));
					} catch (Exception ex) {
						// nothing to do: the address is on screen to type in
					}
				}
			});
		}
		return ret;
	}
}
