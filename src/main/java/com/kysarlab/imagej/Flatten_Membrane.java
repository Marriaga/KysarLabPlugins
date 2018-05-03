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

public class Flatten_Membrane implements PlugIn {

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
		GenericDialog gd = new GenericDialog("Flatten membrane properties");
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

	// Normalize value between minimum and maximum
	private double normval(double val) {
		if (val<min) return 0.0;
		if (val>max) return 1.0;
		return (val-min)/(max-min);
	}

	// Interpolate value between yi-1 and yi, for percentage alpha
	private double interpolatePixel(double y1, double y2, double alpha, double m1, double m2) {
		if (theInterpMethod=="Linear") {
			return y1*(1-alpha)+y2*alpha;
		} else if (theInterpMethod=="Monotone Cubic") {
			return y1*(1+2*alpha)*(1-alpha)*(1-alpha)+
				   m1*alpha*(1-alpha)*(1-alpha)+
				   y2*alpha*alpha*(3-2*alpha)+
				   m2*alpha*alpha*(alpha-1);
		}
		return 0.0;
	}

	private boolean isZero(double val) {
		return (Math.abs(val) < 2 * Double.MIN_VALUE);
	}

	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage(); // IJ.runPlugIn("Average_Height",""); ImagePlus avg_height = WindowManager.getImage("Average Height");
		// Get img info
		getInfo(imp);

		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}

		// Convert to Gray32
		IJ.showStatus("Converting Stack to 32-bit float ...");
		ImageConverter ic = new ImageConverter(imp);
		ic.convertToGray32();

		// Initialize Figures
		double[] si_pixels = new double[w * h];
		double[] sixh_pixels = new double[w * h];
		double[] h_min_pixels = new double[w * h];
		double[] h_max_pixels = new double[w * h];

		// Process the average height algorithm
		IJ.showStatus("Compute Average Height figure ...");
		int idx = 0;
		int stage = 0;
		ImageStack stack = imp.getStack();
		for (int s=1; s <= nSlices; s++) {
			IJ.showProgress(s, nSlices);
			// FloatProcessor slice_p = imp.getStack().getProcessor(s).convertToFloatProcessor();
			ImageProcessor slice_p = stack.getProcessor(s);
			float[] slice_pixels = (float[]) slice_p.getPixels();

			// Add pixels of slice to the sum and sum x height
			for (int j=0; j < h; j++) {
				for (int i=0; i < w; i++) {
					idx = i + j * w;

					// Get min-max slice
					double val= slice_pixels[idx];
					if (val>=min) {
						if (stage==0) h_min_pixels[idx] = s;
						else if (stage==1) h_max_pixels[idx] =s;
					}

					val=normval(val);

					si_pixels[idx] += val;
					sixh_pixels[idx] += val*s;
				}
			}
		}

		// Divide pixels of sum x height by sum
		float[] h_pixels = new float[w * h];
		for (int j=0; j < h; j++) {
			for (int i=0; i < w; i++) {
				idx = i + j * w;
				if (si_pixels[idx] == 0.0) {
					h_pixels[idx] = (float) 0.0;
				} else {
					h_pixels[idx] = (float) (sixh_pixels[idx]/si_pixels[idx]); 
				}
			}
		}

		// Smooth height
		IJ.showStatus("Smooth Average Height figure ...");
		ImageProcessor avg_h_ip = new FloatProcessor(w,h,h_pixels);
		avg_h_ip.blurGaussian(sigma);
		h_pixels = (float[]) avg_h_ip.getPixels();

		if (show_height) {
			ImagePlus avg_height = new ImagePlus("Average Height",avg_h_ip);
			avg_height.setCalibration(cal);
			avg_height.show();
		}


		// Compute diferential for shifting
		IJ.showStatus("Compute shifting ...");
		double h_target = Math.floor(nSlices/2.0);
		double[] d = new double[w * h];
		int[] f = new int[w * h];
		double[] a = new double[w * h];
		for (int j=0; j < h; j++) {
			for (int i=0; i < w; i++) {
				idx = i + j * w;
				d[idx] = h_pixels[idx]-h_target;
				f[idx] = (int) Math.floor(d[idx]);
				a[idx] = d[idx]-f[idx];
			}
		}

		// Make Integer Shifted Stack
		IJ.showStatus("Make Integer-shifted Stack ...");
		double[] fullstack = new double[w * h * nSlices];
		Arrays.fill(fullstack, 0.0);

		int targetSlice = 0;
		for (int s=1; s <= nSlices; s++) {
			IJ.showProgress(s, nSlices);
			float[] slice_pixels = (float[]) stack.getProcessor(s).getPixels();
			for (int j=0; j < h; j++) {
				for (int i=0; i < w; i++) {
					idx = i + j * w ;
					targetSlice = s-f[idx];
					if (targetSlice >= 1 && targetSlice <= nSlices) {
						fullstack[idx + (targetSlice-1)*w*h] = slice_pixels[idx];
					}
				}
			}
		}

		// Compute interpolation parameter for non-linear interpolation methods
		double[] deltaStack = new double[w * h * nSlices];
		double[] slopeStack = new double[w * h * nSlices];
		int ph,pi,pj;
		if (theInterpMethod=="Monotone Cubic") {
			IJ.showStatus("Computing cubic interpolation parameters ...");
			for (int s=1; s <= nSlices-1; s++) {
				IJ.showProgress(s, nSlices);
				for (int j=0; j < h; j++) {
					for (int i=0; i < w; i++) {
						ph = i + j*w + (s-1-1)*w*h; //k-1
						pi = i + j*w + (s  -1)*w*h; //k
						pj = i + j*w + (s+1-1)*w*h; //k+1

						deltaStack[pi] = fullstack[pj] - fullstack[pi]; //dk = yk+1 - yk

						if (isZero(deltaStack[pi])) { // if dk==0
							slopeStack[pi]=0.0; // mk=0
							if (s==nSlices-1) { // if k==n-1
								slopeStack[pj]=0.0;
							}

						} else if (s==1) { // if k==1
							slopeStack[pi]=deltaStack[pi]; //mk=dk

						} else if (s==nSlices-1) { // if k==n-1
							slopeStack[pj]=deltaStack[pi]; //mn=dn-1

						} else if (isZero(deltaStack[ph])) { // if dk-1==0
							slopeStack[pi]=0.0; // mk=0

						} else if (deltaStack[ph]*deltaStack[pi]<0.0) {
							slopeStack[pi]=0.0; // mk=0

						} else {
							slopeStack[pi]=0.5*(deltaStack[ph]+deltaStack[pi]);
						}
						
						if (!isZero(deltaStack[pi])) {
							double aa=slopeStack[pi]/deltaStack[pi];
							if (aa<0) {
								slopeStack[pi] = 0.0;
							} else if (aa>3) {
								slopeStack[pi] = 3.0*deltaStack[pi];
							}

						} else if (s!=1 && !isZero(deltaStack[ph])) {
							double bb=slopeStack[pi]/deltaStack[ph];
							if (bb<0) {
								slopeStack[pi] = 0.0;
							} else if (bb>3) {
								slopeStack[ph] = 3.0*deltaStack[ph];
							}
						}

						if (s==nSlices-1 && !isZero(deltaStack[pi])) {
							double bb=slopeStack[pj]/deltaStack[pi];
							if (bb>3) {
								slopeStack[pi] = 3.0*deltaStack[pi];
							}
						}
					}
				}
			}
		}

		// Interpolate for final Adjusted Stack
		IJ.showStatus("Interpolating ...");
		ImageStack shifted_stack = new ImageStack(w,h);
		double interpolatedValue;
		for (int s=1; s <= nSlices-1; s++) {
			IJ.showProgress(s, nSlices);
			float[] new_slice = new float[w * h];
			for (int j=0; j < h; j++) {
				for (int i=0; i < w; i++) {
					idx = i + j * w;
					pi = i + j*w + (s-1)*w*h;
					pj = i + j*w + (s+1-1)*w*h;
					interpolatedValue = interpolatePixel(fullstack[pi], fullstack[pj], a[idx],slopeStack[pi],slopeStack[pj]);
					// new_slice[idx] = (float) (fullstack[pi]*(1-a[idx])+fullstack[pj]*a[idx]);
					new_slice[idx] = (float) interpolatedValue;
				}
			}
			shifted_stack.addSlice("",new_slice);
		}

		ImagePlus new_stack = new ImagePlus("Adjusted Stack",shifted_stack);
		new_stack.setCalibration(cal);
		new_stack.setDisplayRange(0.0, maxdim);
		new_stack.show();

	}

	public void showAbout() {
		IJ.showMessage("Flatten Membrane",
			"Shifts pixels in the Z-direction, such that the average heigh becomes a plane at the center of the stack."
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
		Class<?> clazz = Flatten_Membrane.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		// ImagePlus image = IJ.openImage("http://imagej.net/images/flybrain.zip");
		ImagePlus image = IJ.openImage("C:\\Users\\Miguel\\Desktop\\20180308L_10X.tif");

		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
