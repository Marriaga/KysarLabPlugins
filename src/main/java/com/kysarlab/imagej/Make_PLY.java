/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.kysarlab.imagej;

import java.awt.FileDialog;
import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Make_PLY implements PlugIn {

    // VARIABLES AND PARAMETERS
	// Original Stack
	private ImagePlus imp;

	// Overall Stack Properties
	private Calibration cal;
	private int num_pix_wide, num_pix_high;//, nSlices;
	private double pix_depth, pix_width;
	private double PIX_HEIGHT = 1.0;

	// Properties
	private String plyFileName;

	// FUNCTIONS

	// To set the final location of the ply file
	private String getFileLocation(String defaultDir, String defaultName) {
		FileDialog fd = new FileDialog(IJ.getInstance(), "Set ply file name and location ...", FileDialog.SAVE);
		fd.setFile(defaultName);
		fd.setDirectory(defaultDir);
		fd.show();

		return fd.getDirectory()+fd.getFile();
	}

	// Collect relevant properties of the stack
	private void getInfo(ImagePlus implus) {
		cal = imp.getCalibration();
		pix_depth=cal.pixelDepth;
		pix_width=cal.pixelWidth;

		// W,H,NCh,nSlices,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		num_pix_wide = imp_dim[0];
		num_pix_high = imp_dim[1];
		//nSlices = imp_dim[3];
	}

	private void makeNodes() {
		double[] scale = {pix_width, pix_depth, PIX_HEIGHT};
		double[] x_nodes = new double[num_pix_wide*num_pix_high];
		double[] y_nodes = new double[num_pix_wide*num_pix_high];
		for(int i = 0; i < num_pix_wide; i++) {
			for(int j = 0; j < num_pix_high; j++){
				x_nodes[j*num_pix_wide + i] = i;
				y_nodes[i*num_pix_high + j] = j;
			}
		}
	}


	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage(); // IJ.runPlugIn("Average_Height",""); ImagePlus avg_height = WindowManager.getImage("Average Height");
		// Get img info
		getInfo(imp);

		// Get Image Pixels
		ImageProcessor img_p = imp.getProcessor();
		float[] img_pix = (float[]) img_p.getPixels();
		
		// Select Save Location
		FileInfo fiOriginal = imp.getOriginalFileInfo();
		plyFileName = getFileLocation(fiOriginal.directory, imp.getShortTitle()+".ply");

	}

	public void showAbout() {
		IJ.showMessage("Make PLY",
			"Makes a ply file based on average height image."
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
