/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.kysarlab.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageConverter;

public class Average_Height implements PlugIn {
	private ImagePlus imp;

	// Overall Stack Properties
	private int w, h;
	private double pD;

	// When you click the button
    public void run(String arg) {
		
		// Get the current image
		imp = WindowManager.getCurrentImage();

		// Convert to Gray32 and get pixel scale
		ImageConverter ic = new ImageConverter(imp);
		ic.convertToGray32();
		Calibration cal = imp.getCalibration();
		pD=cal.pixelDepth;
		// pW=cal.pixelWidth;

		// W,H,NCh,NSl,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		w = imp_dim[0];
		h = imp_dim[1];
		int NSl = imp_dim[3];
	
		// Initialize Figures
		float[] si_pixels = new float[w * h];
		float[] sixh_pixels = new float[w * h];
		float[] h_pixels = new float[w * h];

		// Process the average height algorithm
		int idx = 0;
		for (int s=1; s < NSl+1; s++) {
			IJ.showProgress(s, NSl);
			// FloatProcessor slice_p = imp.getStack().getProcessor(s).convertToFloatProcessor();
			ImageProcessor slice_p = imp.getStack().getProcessor(s);
			float[] slice_pixels = (float[]) slice_p.getPixels();

			// Add pixels of slice to the sum and sum x height
			for (int j=0; j < h; j++) {
				for (int i=0; i < w; i++) {
					idx = i + j * w;
					si_pixels[idx] += slice_pixels[idx];
					sixh_pixels[idx] += slice_pixels[idx]*pD*(s-1);
				}
			}
		}

		// Divide pixels of sum x height by sum
		for (int j=0; j < h; j++) {
			for (int i=0; i < w; i++) {
				idx = i + j * w;
				if (si_pixels[idx] == 0.0) {
					h_pixels[idx] = (float) 0.0;
				} else {
					h_pixels[idx] = sixh_pixels[idx]/si_pixels[idx];
				}
			}
		}
		ImageProcessor avg_h_ip = new FloatProcessor(w,h,h_pixels);
		ImagePlus avg_height = new ImagePlus("Average Height",avg_h_ip);
		avg_height.setCalibration(cal);
		avg_height.show();
	}

	public void showAbout() {
		IJ.showMessage("Average Height",
			"Computes an image from a stack that corresponds to the weighted average of the height at each pixel"
		);
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Average_Height.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/flybrain.zip");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
