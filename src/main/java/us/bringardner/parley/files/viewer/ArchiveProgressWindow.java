/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2026 <A href="http://bringardner.com/tony">Tony Bringardner</A>
 * 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *   
 *	@author Tony Bringardner   
 */
package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import us.bringardner.swing.gradient.GradientLook;

/**
 * The small window Compress, Expand and Delete show while they run (not modal: the viewer stays usable).
 * It says what is being done; below, a bar that fills as the work goes, and how far it has got:
 * "152 of 401 (38%): docs/f234.bin" when the number of items is known first (Compress counts them;
 * a zip's index lists them), else "37 items (45%): ..." with the percentage of the archive read (a
 * tar has no index). The bar is a busy bar only until then ("Getting ready...").
 * <p>
 * It is as wide as its message needs (a long name widens it, up to most of the screen), and no
 * narrower than {@link #WIDTH}; a long name below the bar is cut short rather than widening it.
 * <p>
 * The work reports from its own thread; the window is brought up to date on the event thread, at
 * most once per event-thread turn however fast the reports come.
 */
class ArchiveProgressWindow extends JDialog implements ArchiveActions.Progress {
	private static final long serialVersionUID = 1L;

	/** The narrowest the window is. */
	static final int WIDTH = 420;
	/** The most of the screen's width the window takes. */
	static final double MOST_OF_SCREEN = 0.8;
	/** The bar's steps when it shows bytes read. */
	static final int BYTE_STEPS = 1000;

	private final JProgressBar bar = new JProgressBar();
	private final JLabel detail = new JLabel("Getting ready\u2026");

	// the latest reports, from the work's thread
	private volatile int total = -1;
	private volatile int done;
	private volatile String name = "";
	private volatile long bytesDone, bytesTotal = -1;
	/** an update is waiting on the event thread */
	private final AtomicBoolean pending = new AtomicBoolean();

	ArchiveProgressWindow(Frame owner, String message) {
		this(owner, "Archive", message);
	}

	/** With its own title (Delete uses it too). */
	ArchiveProgressWindow(Frame owner, String title, String message) {
		super(owner, title, false);
		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
		content.add(new JLabel(message), BorderLayout.NORTH);
		bar.setIndeterminate(true);
		content.add(bar, BorderLayout.CENTER);
		content.add(detail, BorderLayout.SOUTH);
		setContentPane(content);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// on the theme's gradient, as the viewer's other windows
		GradientLook.apply(this);
		pack();
		int most = (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().width * MOST_OF_SCREEN);
		setSize(new Dimension(Math.min(Math.max(WIDTH, getWidth()), most), getHeight()));
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	@Override
	public void total(int count) {
		total = count;
		update();
	}

	@Override
	public void item(int done, String name) {
		this.done = done;
		this.name = name;
		update();
	}

	@Override
	public void bytes(long done, long total) {
		bytesDone = done;
		bytesTotal = total;
		update();
	}

	private void update() {
		if( pending.compareAndSet(false, true) ) {
			SwingUtilities.invokeLater(()->{
				pending.set(false);
				show(total, done, name, bytesDone, bytesTotal);
			});
		}
	}

	/** Event thread. */
	private void show(int total, int done, String name, long bytesDone, long bytesTotal) {
		int percent = -1;
		if( total > 0 ) {
			bar.setIndeterminate(false);
			bar.setMaximum(total);
			bar.setValue(Math.min(done, total));
			percent = (int) (100L*Math.min(done, total)/total);
		} else if( bytesTotal > 0 ) {
			bar.setIndeterminate(false);
			bar.setMaximum(BYTE_STEPS);
			bar.setValue((int) (BYTE_STEPS*Math.min(bytesDone, bytesTotal)/bytesTotal));
			percent = (int) (100*Math.min(bytesDone, bytesTotal)/bytesTotal);
		}
		detail.setText(text(total, done, name, percent));
	}

	/** What is said below the bar. */
	static String text(int total, int done, String name, int percent) {
		String pct = percent >= 0 ? " ("+percent+"%)" : "";
		if( total > 0 ) {
			return done+" of "+total+pct+(done > 0 ? ": "+name : "");
		}
		if( done > 0 ) {
			return done+(done == 1 ? " item" : " items")+pct+": "+name;
		}
		return percent >= 0 ? percent+"%" : "Getting ready\u2026";
	}

	/** What the window says below the bar (for tests). */
	String getDetail() {
		return detail.getText();
	}

	/** The bar (for tests). */
	JProgressBar getBar() {
		return bar;
	}
}
