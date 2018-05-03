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
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class Gradual_Brightness implements PlugIn {
	private ImagePlus imp;

	// Overall Stack Properties
	private int stackSize;

	// Fit Parameters
	private String [] methods =  {"Linear: A + Bx","Hyperbolic: A + B/x","Exponential: Ae^(Bx)","Power: Ax^B"};
	private String theMethod;
	private double pA,pB,pD;


	// From Plugin.ContrastEnhancer
    void normalize(ImageProcessor ip, double min, double max) {
        int min2 = 0;
        int max2 = 255;
        int range = 256;
        if (ip instanceof ShortProcessor)
            {max2 = 65535; range=65536;}
        else if (ip instanceof FloatProcessor)
            normalizeFloat(ip, min, max);
        int[] lut = new int[range];
        for (int i=0; i<range; i++) {
            if (i<=min)
                lut[i] = min2;
            else if (i>=max)
                lut[i] = max2;
            else
                lut[i] = (int)(((double)(i-min)/(max-min))*max2);
        }
        ip.applyTable(lut);
	}
    void normalizeFloat(ImageProcessor ip, double min, double max) {
        double scale = max>min?1.0/(max-min):1.0;
        int size = ip.getWidth()*ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        double v;
        for (int i=0; i<size; i++) {
            v = pixels[i] - min;
            if (v<0.0) v = 0.0;
            v *= scale;
            if (v>1.0) v = 1.0;
            pixels[i] = (float)v;
        }
    }

	// Dialog for input
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Set Interpolator Properties");
		gd.addRadioButtonGroup("Fitting Type:", methods, 4, 1, methods[0]);
		gd.addNumericField("A", 1.0, 4);
		gd.addNumericField("B", 1.0, 4);
		gd.addNumericField("Max Delta (-1: Use max)", -1.0, 2);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		theMethod = gd.getNextRadioButton();
		pA = gd.getNextNumber();
		pB = gd.getNextNumber();
		pD = gd.getNextNumber();
		return true;
	}

	private double getMin(int slice) {
		if (theMethod==methods[0]) { // Linear
			return (double) (pA+pB*slice);
		} else if (theMethod==methods[1]) { // Hyperbolic
			return (double) (pA+pB/slice);
		} else if (theMethod==methods[2]) { // Exponential
			return (double) (pA*Math.exp(pB*slice));
		} else if (theMethod==methods[3]) { // Power
			return (double) (pA*Math.pow(slice,pB));
		}
		return -1.0;
	}

	private double getMax(ImageProcessor ip, double min) {
		double maxmax = 255;
		if (pD != -1) 
			maxmax=min+pD;
		else if (ip instanceof ShortProcessor)
			maxmax=65535;
        else if (ip instanceof FloatProcessor)
			maxmax=ip.getMax();

		return maxmax;
	}


	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage();
		stackSize = imp.getStackSize();

		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}

		// Process the average height algorithm
		ImageStack stack = imp.getStack();
		for (int s=1; s <= stackSize; s++) {
			IJ.showProgress(s, stackSize);
			ImageProcessor slice_p = stack.getProcessor(s);
			double min = getMin(s);
			double max = getMax(slice_p,min);
			normalize(slice_p,min,max);
		}

		imp.show();
	}

	public void showAbout() {
		IJ.showMessage("Gradual Brightness",
			"Changes the brigthness of each slice by considering a gradual growth of the min and max values based on some points and a fitting expression between them."
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
		Class<?> clazz = Gradual_Brightness.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the fly brain sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/flybrain.zip");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
