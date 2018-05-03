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
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class Curvature implements PlugIn {
	private ImagePlus imp;

	// Overall Image Properties
	private int w, h;
	private double pW;

	// Dialog Parameters
	private String method;
	private int pR;

	private int indx(int i, int j){return i + j * w;}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Compute Curvature");

		String [] methods = new String [] {"Satelite Points","Gradient Based (not implemented)"};
		gd.addRadioButtonGroup("Method:", methods, 1, 2, "Satelite Points");
		gd.addNumericField("Pixel Range", Math.max(w/10,1), 0,6,"pixels");

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		method = gd.getNextRadioButton();
		pR = (int) gd.getNextNumber();

		return true;
	}	


	private double TPcurv(double ax,double ay,double bx,double by,double cx,double cy) {
		double d=(ax-bx)*(by-cy)-(bx-cx)*(ay-by);
		if (d==0.0) {return Double.NaN;}
		else {
			double u = (ax*ax-bx*bx+ay*ay-by*by)/2;
			double v = (bx*bx-cx*cx+by*by-cy*cy)/2;
			double x = (u*(by-cy)-v*(ay-by))/d;
			double y = (v*(ax-bx)-u*(bx-cx))/d;
			double r = Math.sqrt((ax-x)*(ax-x)+(ay-y)*(ay-y));
			return r;
		}
	}


	private void SateliteCurvature(float[] ipf, float[] rpf) {

		// Add pixels of slice to the sum and sum x height
		for (int y=0; y < h; y++) {
			int ymin=Math.max(y-pR,0);
			int ymax=Math.min(y+pR,h-1);
			int ymid=ymin+(ymax-ymin)/2;
			for (int x=0; x < w; x++) {
				int xmin=Math.max(x-pR,0);
				int xmax=Math.min(x+pR,w-1);
				int xmid=xmin+(xmax-xmin)/2;

				double rx = TPcurv(
					xmin*pW,ipf[indx(xmin, y)],
					xmid*pW,ipf[indx(xmid, y)],
					xmax*pW,ipf[indx(xmax, y)]);
				double ry = TPcurv(
					ymin*pW,ipf[indx(x, ymin)],
					ymid*pW,ipf[indx(x, ymid)],
					ymax*pW,ipf[indx(x, ymax)]);


				double threshold = 1.0E10;
				double rp = threshold;

				if (rx == Double.NaN && ry == Double.NaN){rp=threshold;}
				else if (rx == Double.NaN) {rp=ry;}
				else if (ry == Double.NaN) {rp=rx;}
				else {rp=(rx+ry)/2;}

				if (rp>threshold){rp=threshold;}
					
				rpf[indx(x,y)] = (float) rp;
			}

		}
	}


	// When you click the button
    public void run(String arg) {
		
		// Get the current image
		imp = WindowManager.getCurrentImage();

		// Convert to Gray32 and get pixel scale
		ImageConverter ic = new ImageConverter(imp);
		ic.convertToGray32();
		Calibration cal = imp.getCalibration();
		pW=cal.pixelWidth;

		// W,H,NCh,NSl,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		w = imp_dim[0];
		h = imp_dim[1];
	
		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}

		// Initialize Curvature Figure
		float[] rpf = new float[w * h];

		// Get Image Pixels
		ImageProcessor img_p = imp.getProcessor();
		float[] ipf = (float[]) img_p.getPixels();

		if (method=="Satelite Points"){
			SateliteCurvature(ipf,rpf);
		}

		ImageProcessor r_ip = new FloatProcessor(w,h,rpf);
		ImagePlus radius = new ImagePlus("Radius of Curvature",r_ip);
		radius.setCalibration(cal);
		radius.show();
	}

	public void showAbout() {
		IJ.showMessage("Curvature",
			"Computes the curvature of an image, assming that the image represents a surface"
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
		Class<?> clazz = Curvature.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
