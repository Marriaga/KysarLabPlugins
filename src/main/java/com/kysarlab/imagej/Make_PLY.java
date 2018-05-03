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
	private int num_pix_wide, num_pix_high;//, nSlices;
	private double pix_depth, pix_width;
	private double PIX_HEIGHT = 1.0;

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
		pix_depth=cal.pixelDepth;
		pix_width=cal.pixelWidth;

		// W,H,NCh,nSlices,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		num_pix_wide = imp_dim[0];
		num_pix_high = imp_dim[1];
		//nSlices = imp_dim[3];
	}

	private float[] listNodesByCoordinates(double[] z_values) {
		double[] scale = {pix_width, pix_depth, PIX_HEIGHT};
		float[] nodes = new float[num_pix_wide*num_pix_high*z_values.length];

		int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
				num_node = i + j * num_pix_wide;
				nodes[num_node*3] = (float) (i * scale[0]);
				nodes[num_node*3 + 1] = (float) (j * scale[1]);
				nodes[num_node*3 + 2] = (float) (z_values[num_node] * scale[2]);
			}				
		}
		
		return nodes;
	}
	
	private int[] listFacesByNodeVertices() {
		//Each "square" face has two triangular faces (4 distinct vertices shared among 2 triangles)
		int num_square_faces = (num_pix_wide*num_pix_high - (num_pix_wide + num_pix_high - 1));
		int num_vertices = num_square_faces*2*3; //6 vertices for 2 triangles
		int[] vertices = new int[num_vertices];
		int npw = num_pix_wide;
		//Triangles listed counter-clockwise
		// Example: For a 15x15 image, the first two triangles would be:
		//			 1, 0, 15
		//			16, 1, 15
		for(int i = 0; i < num_square_faces; i++){
				vertices[i*npw] = i+1;
				vertices[i*npw + 1] = i;
				vertices[i*npw + 2] = i+npw;
				vertices[i*npw + 3] = i+npw+1;
				vertices[i*npw + 4] = i+1;
				vertices[i*npw + 5] = i+npw;
		}
		return vertices;
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
