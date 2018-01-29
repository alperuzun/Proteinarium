package org.armanious.network.visualization;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.visualization.ForceDirectedLayout.GraphLayoutData;

public class GUIRenderer<K> extends Renderer<K> {
	
	private final Map<String, RendererPanel> frameMap = new HashMap<>();
	private final int skipIterations;
	
	private int iteration;

	public GUIRenderer(RendererConfig rc, File outputDirectory) {
		this(rc, outputDirectory, 20);
	}
	
	public GUIRenderer(RendererConfig rc, File outputDirectory, int skipIterations){
		super(rc, outputDirectory);
		this.skipIterations = skipIterations;
		iteration = skipIterations;
	}
	
	private RendererPanel getRendererPanel(String name){
		RendererPanel panel = frameMap.get(name);
		if(panel == null){
			frameMap.put(name, panel = new RendererPanel());
			final JFrame frame = new JFrame(name);
			frame.setSize(500, 500);
			frame.setContentPane(panel);
			frame.setVisible(true);
		}
		return panel;
	}
	
	@Override
	public void handleLayoutIteration(GraphLayoutData<K> data, String name) throws IOException {
		if(iteration == skipIterations){
			getRendererPanel(name).renderImage(super.generateBufferedImage(data, name));
			iteration = 0;
		}else{
			iteration++;
		}
	}
	
	private static class RendererPanel extends JPanel {
		private static final long serialVersionUID = 3508629473649941546L;
		
		private Image image;
		
		public void renderImage(BufferedImage image){
			this.image = image;
			repaint();
			addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					repaint();
				}
			});
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(image == null) return;
			
			final int imageWidth = image.getWidth(null);
			final int imageHeight = image.getHeight(null);
			final int panelWidth = getWidth();
			final int panelHeight = getHeight();
			
			final double scale = Math.max(1, Math.max((double) imageWidth / panelWidth, (double) imageHeight / panelHeight));
			final int imageRenderWidth = (int) (imageWidth / scale);
			final int imageRenderHeight = (int) (imageHeight / scale);
			
			//g.clearRect(0, 0, getWidth(), getHeight());
			g.drawImage(image, (panelWidth - imageRenderWidth) / 2, (panelHeight - imageRenderHeight) / 2, imageRenderWidth, imageRenderHeight, getBackground(), null);
		}
		
	}

}
