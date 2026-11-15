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
 * The small window Compress and Expand show while they run (not modal: the viewer stays usable).
 * It says what is being done, and below a bar, how far it has got: "37 of 120: docs/sub/b.txt"
 * with the bar filling when the number of items is known first, else "37 items: ..." with a busy
 * bar (a tar has no index).
 * <p>
 * The work reports from its own thread; the window is brought up to date on the event thread, at
 * most once per event-thread turn however fast the items come.
 */
class ArchiveProgressWindow extends JDialog implements ArchiveActions.Progress {
	private static final long serialVersionUID = 1L;

	/** The width the window keeps, so a long name is cut short rather than widening it. */
	static final int WIDTH = 420;

	private final JProgressBar bar = new JProgressBar();
	private final JLabel detail = new JLabel("Getting ready…");

	// the latest report, from the work's thread
	private volatile int total = -1;
	private volatile int done;
	private volatile String name = "";
	/** an update is waiting on the event thread */
	private final AtomicBoolean pending = new AtomicBoolean();

	ArchiveProgressWindow(Frame owner, String message) {
		super(owner, "Archive", false);
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
		setSize(new Dimension(WIDTH, getHeight()));
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

	private void update() {
		if( pending.compareAndSet(false, true) ) {
			SwingUtilities.invokeLater(()->{
				pending.set(false);
				show(total, done, name);
			});
		}
	}

	/** Event thread. */
	private void show(int total, int done, String name) {
		if( total > 0 ) {
			bar.setIndeterminate(false);
			bar.setMaximum(total);
			bar.setValue(Math.min(done, total));
		}
		if( done > 0 ) {
			detail.setText(total > 0 ? done+" of "+total+": "+name : done+(done == 1 ? " item: " : " items: ")+name);
		} else if( total > 0 ) {
			detail.setText(total+(total == 1 ? " item" : " items"));
		}
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
