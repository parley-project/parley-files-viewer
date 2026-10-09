package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import us.bringardner.parley.files.viewer.FileSourceViewer.CopyProgressListner;

import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.event.ActionEvent;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class CopySingleDialog extends JDialog implements CopyProgressListner {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private boolean canceled=false;
	private boolean paused = false;
	private JButton pauseButton;
	private JLabel statusLabel;
	private JLabel contentLabel;
	private JProgressBar progressBar;
	AtomicLong startTime = new AtomicLong();
	AtomicLong expected = new AtomicLong();
	AtomicReference<Throwable> error = new AtomicReference<>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			CopySingleDialog dialog = new CopySingleDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public CopySingleDialog() {
		
		setBounds(100, 100, 459, 157);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel northPanel = new JPanel();
			FlowLayout flowLayout = (FlowLayout) northPanel.getLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			contentPanel.add(northPanel, BorderLayout.NORTH);
			{
				contentLabel = new JLabel("Downloading xxx.x");
				contentLabel.setHorizontalAlignment(SwingConstants.LEFT);
				northPanel.add(contentLabel);
			}
		}
		{
			JPanel southPanel = new JPanel();
			contentPanel.add(southPanel, BorderLayout.SOUTH);
			{
				statusLabel = new JLabel("Status");
				southPanel.add(statusLabel);
			}
		}
		{
			JPanel centerPanel = new JPanel();
			contentPanel.add(centerPanel, BorderLayout.CENTER);
			centerPanel.setLayout(new BorderLayout(0, 0));
			{
				progressBar = new JProgressBar();
				centerPanel.add(progressBar);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				pauseButton = new JButton("Pause");
				pauseButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						actionPause();
					}
				});
				pauseButton.setActionCommand("OK");
				buttonPane.add(pauseButton);
				getRootPane().setDefaultButton(pauseButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						actionCancel();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	protected void actionCancel() {
		canceled = true;
		dispose();
	}

	protected void actionPause() {
		if( paused) {
			paused = false;
			pauseButton.setText("Pause");
		} else {
			paused = true;
			pauseButton.setText("Continue");
		}
		
	}

	@Override
	public void setDescription(String description) {
		SwingUtilities.invokeLater(()->{
			contentLabel.setText(description);
			contentPanel.updateUI();
		});
		
		
	}

	@Override
	public void copyStarted(final long bytesExpected) {
		expected.set(bytesExpected);
		startTime.set(System.currentTimeMillis());
		SwingUtilities.invokeLater(()->{contentPanel.updateUI();});
		
	}

	public static final String msg = "%2.2f%% %d%s %2.2f%s/s";
	public static long K = 1024;
	public static long M = K*K;
	public static long G = M*M;
	
	@Override
	public void updateProgress(final long copied) {
		
		final long lastTime = System.currentTimeMillis();
		
		final double perc = ((double)copied/(double)expected.get())*100.0;
		String tag = "B";
		long val = copied;
		if( val >= G) {
			val /= G;
			tag = "GB";
		} else if( val >= M) {
			val /= M;
			tag = "MB";
		} else if( val >= K) {
			val /= K;
			tag = "KB";
		}
		
		final long ms = lastTime-startTime.get();
		final int sec = (int)(ms/1000L);
		
		final double speed = (val / (sec>0?sec:1));
	        
		final int mb = (int) val;
		final String tag2 = tag;
		
		
		SwingUtilities.invokeLater(()->{
			statusLabel.setText((String.format(msg, perc,mb,tag2,speed,tag2)));
			progressBar.setValue((int)perc);
			contentPanel.updateUI();
		});
		
		
	}

	@Override
	public void copyComplete() {
		dispose();		
	}

	@Override
	public void error(Exception e) {
		error.set(e);		
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public boolean isPaused() {
		return paused;
	}

}
