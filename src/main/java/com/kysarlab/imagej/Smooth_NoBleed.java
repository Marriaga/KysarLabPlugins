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
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

public class Smooth_NoBleed implements PlugIn {

    // VARIABLES AND PARAMETERS
	// Original Stack
	private ImagePlus imp;

	// Overall Stack Properties
	private Calibration cal;
	private int num_pix_wide, num_pix_high;//, nSlices;

    // Properties
    private static double RATIO = 0.5;

    private float back_threshold;
    private int n_steps;
    private double sigma;
    private double a_fact; 

	// FUNCTIONS

    // Shows dialog
	private boolean showDialog() {
		// specify fields in Dialog
		GenericDialog gd = new GenericDialog("Smooth Without Bleeding properties");
		gd.addNumericField("Background threshold", 0.0,   2);
		gd.addNumericField("Number of Iterations", 4, 0);
		gd.addNumericField("Standard Deviation (Smoothing)", 10.0, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		back_threshold = (float) gd.getNextNumber();
		n_steps = (int) gd.getNextNumber();
		sigma = (double) gd.getNextNumber();
        a_fact = sigma*Math.sqrt((1-RATIO)/(1-Math.pow(RATIO,n_steps))); //sqrt((1-R)/(1-R^N))
		return true;
	}	

	// Collect relevant properties of the stack
	private void getInfo(ImagePlus implus) {
		cal = imp.getCalibration();
		// W,H,NCh,nSlices,NFr = imp.getDimensions()
		int[] imp_dim = imp.getDimensions();
		num_pix_wide = imp_dim[0];
		num_pix_high = imp_dim[1];
		//nSlices = imp_dim[3];
	}

    // Makes the mask image by setting to 1 everything below threshold and zero elsewhere
    private void makeMask(float[] mask_pixels, float threshold){
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                if (mask_pixels[num_node]>threshold) {
                    mask_pixels[num_node] = 0.0f;
                } else {
                    mask_pixels[num_node] = 1.0f;
                }
			}				
		}
    }


    private void clearMask(float[] input_pixels, float[] mask_pixels) {
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                if (mask_pixels[num_node]==1.0f) input_pixels[num_node]=0.0f;
			}				
		}
    }

    private void multiplypixels(float[] inputA, float[] inputB, float[] output) {
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                output[num_node]=inputA[num_node]*inputB[num_node];
			}				
		}
    }

    private void fixresult(float[] result_pixels, float[] temp_pixels, float[] mask_pixels) {
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                if (mask_pixels[num_node]==1.0f) {
                    result_pixels[num_node]=0.0f;
                } else {
                    result_pixels[num_node]+=temp_pixels[num_node];
                }
			}				
		}
    }

    private void resetmask(float[] result_pixels, float[] mask_pixels, float[] backup_pixels) {
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                if (mask_pixels[num_node]==1.0f) {
                    result_pixels[num_node]=backup_pixels[num_node];
                }
			}				
		}
    }

    private void deepCopy(float[] source, float[] dest) {
        int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
                num_node = i + j * num_pix_wide;
                dest[num_node]= source[num_node];
			}				
		}
    }


	// When you click the button
    public void run(String arg) {

		// Get the current image
		imp = WindowManager.getCurrentImage(); // IJ.runPlugIn("Average_Height",""); ImagePlus avg_height = WindowManager.getImage("Average Height");
		// Get Image info
		getInfo(imp);
		// Get Image Processor
        ImageProcessor img_p = imp.getProcessor();

		// Run the dialog to get parameters and exits if cancelled
		if (!showDialog()) {return;}

        // Mask (1 outside, 0 inside)
        ImageProcessor mask = img_p.convertToFloatProcessor();
        float[] mask_pixels = (float[]) mask.getPixels();
        makeMask(mask_pixels,back_threshold);

        // Allocate Corrector
        ImageProcessor corrector = mask.convertToFloatProcessor();
        float[] corrector_pixels = (float[]) corrector.getPixels();

        // Final Result
        ImageProcessor result = img_p.convertToFloatProcessor();
        float[] result_pixels = (float[]) result.getPixels();
        float[] temp_pixels = result_pixels.clone();
        float[] backup_pixels = result_pixels.clone();
        clearMask(result_pixels,mask_pixels);

        double sigma = 1.0;
        for(int n = 0; n < n_steps; n++){
            sigma = Math.sqrt(Math.pow(RATIO, n))*a_fact;
            deepCopy(mask_pixels,corrector_pixels);
            corrector.blurGaussian(sigma);
            multiplypixels(result_pixels,corrector_pixels,temp_pixels);
            result.blurGaussian(sigma);
            fixresult(result_pixels,temp_pixels,mask_pixels);
        }

        resetmask(result_pixels,mask_pixels,backup_pixels);
        
		ImagePlus blurred_image = new ImagePlus("Selective Blur",result);
		blurred_image.setCalibration(cal);
        blurred_image.show();
        
        // showPixels(result_pixels,"Result");
        // showPixels(mask_pixels,"Mask");
        // showPixels(backup_pixels,"Backup");
        // showPixels(temp_pixels,"Temp");
        // showPixels(corrector_pixels,"Corrector");

	}

    void showPixels (float[] pixels, String Title ) {
		ImageProcessor ip = new FloatProcessor(num_pix_wide,num_pix_high,pixels);
		ImagePlus iplus = new ImagePlus(Title,ip);
		iplus.setCalibration(cal);
		iplus.show();
    }

	public void showAbout() {
		IJ.showMessage("Smooth Without Bleed",
			"Makes a gaussian blur that prevents the outside from affecting the inside."
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
		Class<?> clazz = Smooth_NoBleed.class; 
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open sample
		File file = new File(clazz.getResource("/Circles_smudge.tif").getFile());
		ImagePlus image = IJ.openImage(file.getAbsolutePath());

		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
