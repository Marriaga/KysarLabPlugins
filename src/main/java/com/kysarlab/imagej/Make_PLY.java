/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.kysarlab.imagej;

import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Make_PLY implements PlugIn {

    // VARIABLES AND PARAMETERS
	// Original Stack
	private ImagePlus imp;

	// Overall Stack Properties
	private Calibration cal;
	private int w, h;//, nSlices;
	private double pD,pW;

	// Properties
	private String plyFileName;


	// FUNCTIONS




	// Shows dialog
	private boolean showDialog() {
		// specify fields in Dialog
		GenericDialog gd = new GenericDialog("Make PLY properties");
		gd.addStringField("Choose ply location:", "");

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		plyFileName = gd.getNextString();

		return true;
	}	

	// Collect relevant properties of the stack
	private void getInfo(ImagePlus implus) {
		cal = imp.getCalibration();
		pD=cal.pixelDepth;
		pW=cal.pixelWidth;

		// W,H,NCh,nSlices,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		w = imp_dim[0];
		h = imp_dim[1];
		//nSlices = imp_dim[3];
	}


	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage(); // IJ.runPlugIn("Average_Height",""); ImagePlus avg_height = WindowManager.getImage("Average Height");
		// Get img info
		getInfo(imp);

		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}

		// Get Image Pixels
		ImageProcessor img_p = imp.getProcessor();
		float[] img_pix = (float[]) img_p.getPixels();
		


		


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

		// open sample
		File file = new File(clazz.getResource("/avg-height-example.tif").getFile());
		ImagePlus image = IJ.openImage(file.getAbsolutePath());

		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
