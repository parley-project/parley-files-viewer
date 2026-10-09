/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ActivityLayer extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	private boolean running;
	private boolean fadingOut;
	private Timer timer;

	private int angle;
	private int fadeCount;
	private int fadeLimit = 15;
	private boolean useGreyOut = false;
	private int maxSize = 50;
	//private static Color clear = new Color(1, 1, 1, 0);

	@Override
	public void paint (Graphics g) {
		int w = getWidth();
		int h = getHeight();

		// Paint the view.
		super.paint (g);
		
		if (!running) {
			return;
		}

		Graphics2D g2 = (Graphics2D)g.create();
		//g2.setColor(clear);
		//g2.fillRect(0, 0, w, h);
		
		float fade = (float)((double)fadeCount / (double)fadeLimit);
		// Gray it out.
		if(useGreyOut) {
			Composite urComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f * fade));
			g2.fillRect(0, 0, w, h);
			g2.setComposite(urComposite);
		}

		// Paint the wait indicator.
		int s = Math.min(Math.min(w, h) / 5,maxSize);

		int cx = w / 2;
		int cy = h / 2;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(s / 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.setPaint(Color.darkGray);
		g2.rotate(Math.PI * angle / 180, cx, cy);

		for (int i = 0; i < 12; i++) {
			double scale = (11.0f - (double)i) / 11.0f;
			g2.drawLine(cx + s, cy, cx + s * 2, cy);
			g2.rotate(-Math.PI / 6, cx, cy);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(scale * fade)));
		}

		g2.dispose();
	}

	public void actionPerformed(ActionEvent e) {
		//System.out.println("action");
		if (running) {
			firePropertyChange("tick", 0, 1);
			angle += 3;
			if (angle >= 360) {
				angle = 0;
			}
			if (fadingOut) {
				if (--fadeCount == 0) {
					running = false;
					timer.stop();
				}
			}
			else if (fadeCount < fadeLimit) {
				fadeCount++;
			}
			repaint();
		}
	}

	public void start() {
		if (running) {
			return;
		}

		// Run a thread for animation.
		running = true;
		fadingOut = false;
		fadeCount = 0;
		int fps = 24;
		int tick = 1000 / fps;
		timer = new Timer(tick, this);
		timer.start();
		SwingUtilities.invokeLater(new Runnable() {

			
			public void run() {
				setOpaque(false);
				setVisible(true);;

			}
		});


	}

	public void stop() {
		fadingOut = true;
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				setOpaque(false);
				setVisible(false);;

			}
		});
	}

	public boolean isRunning() {
		return running;
	}


	public boolean isUseGreyOut() {
		return useGreyOut;
	}

	public void setUseGreyOut(boolean useGreyOut) {
		this.useGreyOut = useGreyOut;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

}