/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.kysarlab.imagej;

import java.util.Arrays;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageConverter;
import ij.gui.GenericDialog;

public class Make_PLY implements PlugIn {

    // VARIABLES AND PARAMETERS
	// Original Stack
	private ImagePlus imp;

	// Overall Stack Properties
	private Calibration cal;
	private int w, h, nSlices;
	private double maxdim;

	// Properties
	private double min, max; // Threshhold min/max
	private double sigma; // Std. dev. for Gaussian Smoothing
	private boolean show_height; // Show heigh figure
	private String [] interp_methods =  {"Linear","Monotone Cubic"};
	private String theInterpMethod;


	// FUNCTIONS

	// Shows dialog
	private boolean showDialog() {
		// specify fields in Dialog
		GenericDialog gd = new GenericDialog("Make PLY properties");
		gd.addRadioButtonGroup("Interpolation Method:", interp_methods, 4, 1, interp_methods[0]);
		gd.addNumericField("Minimum pixel threshold", 0.0,   2);
		gd.addNumericField("Maximum pixel threshold", maxdim, 2);
		gd.addNumericField("Standard Deviation (Smoothing)", 10.0, 2);
		gd.addCheckbox("Show Average Height map", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		theInterpMethod = gd.getNextRadioButton();
		min = (double) gd.getNextNumber();
		max = (double) gd.getNextNumber();
		sigma = (double) gd.getNextNumber();
		show_height = gd.getNextBoolean();

		return true;
	}	

	// Collect relevant properties of the stack
	private void getInfo(ImagePlus implus) {
		cal = imp.getCalibration();
		// pD=cal.pixelDepth;
		// pW=cal.pixelWidth;

		// W,H,NCh,nSlices,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		w = imp_dim[0];
		h = imp_dim[1];
		nSlices = imp_dim[3];

		// get Automax
		switch(imp.getType()) {
			case ImagePlus.COLOR_256: maxdim=256.0; break;
			case ImagePlus.COLOR_RGB: maxdim=256.0; break;
			case ImagePlus.GRAY8: maxdim=255.0; break;
			case ImagePlus.GRAY16: maxdim=65535.0; break;
			case ImagePlus.GRAY32: maxdim=1.0; break;
		}
	}


	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage(); // IJ.runPlugIn("Average_Height",""); ImagePlus avg_height = WindowManager.getImage("Average Height");
		// Get img info
		getInfo(imp);

		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}


	}

	public void showAbout() {
		IJ.showMessage("Make PLY",
			"Makes a ply file based on average heigh image."
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
		Class<?> clazz = Make_PLY.class;
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
