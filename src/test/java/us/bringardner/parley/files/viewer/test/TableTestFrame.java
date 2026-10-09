package us.bringardner.parley.files.viewer.test;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JTable;
import javax.swing.UIDefaults;
import javax.swing.JScrollPane;

public class TableTestFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private JScrollPane scrollPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TableTestFrame frame = new TableTestFrame();
					frame.setVisible(true);
					frame.scrollPane.getColumnHeader().setBackground(Color.yellow);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public TableTestFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		
		String[] columnNames = {"Column1","Column1",};
		Object[][] data = {{"one","two"},{"one","two"}};
		
		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		UIDefaults defaults= javax.swing.UIManager.getDefaults();
		defaults.put("TableHeader.background", new ColorUIResource(Color.blue));
		defaults.put("TableHeader.foreground", new ColorUIResource(Color.red));
		defaults.put("TableHeader.opaque", false);
		table = new JTable();
		table.setRowHeight(60);
		table.setFillsViewportHeight(true);
		table.setModel(new DefaultTableModel(data, columnNames));
		table.setAutoCreateRowSorter(true);
		scrollPane.setViewportView(table);
		
	}

}
