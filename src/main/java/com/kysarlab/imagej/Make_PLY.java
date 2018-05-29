/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.kysarlab.imagej;

import java.awt.FileDialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
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
	// private double PIX_HEIGHT = 1.0;

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

	private float[] listNodesByCoordinates(float[] z_values) {
		double[] scale = {pix_width, pix_width, pix_depth}; //TODO: Fix for pixels that are not square
		float[] nodes = new float[num_pix_wide*num_pix_high*3];

		int num_node = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++) {
				num_node = i + j * num_pix_wide;
				nodes[num_node*3] = (float) (i * scale[0]);
				nodes[num_node*3 + 1] = (float) ((num_pix_high-j-1) * scale[1]);
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
		int idx = 0;
		for(int j = 0; j < num_pix_high; j++){
			for(int i = 0; i < num_pix_wide; i++){
				if(i != num_pix_wide-1 && j != num_pix_high-1) {
					idx = j*(npw-1)+i;
					vertices[idx*6] = idx+1+j;
					vertices[idx*6 + 1] = idx+j;
					vertices[idx*6 + 2] = idx+npw+j;
					vertices[idx*6 + 3] = idx+npw+1+j;
					vertices[idx*6 + 4] = idx+1+j;
					vertices[idx*6 + 5] = idx+npw+j;
				}
			}
		}
		
		return vertices;
	} 

	public void writePLYToFile(String fileName, float[]nodes, int[]faces) throws IOException {
		String str = "ply\nformat ascii 1.0\ncomment VCGLIB generated\n";
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		writer.write(str);
		str = "element vertex "+Integer.toString(num_pix_high*num_pix_wide)+ "\n";
		writer.write(str);
		str = "property float x\nproperty float y\nproperty float z\n";
		writer.write(str);
		str = "element face "+Integer.toString((num_pix_high*num_pix_wide-(num_pix_high+num_pix_wide-1))*2)+ "\n";
		writer.write(str);
		str = "property list uchar int vertex_indices\nend_header\n";
		writer.write(str);

		//write nodes
		for(int n = 0; n < nodes.length/3; n++){
			str = Float.toString(nodes[n*3]) + " " + Float.toString(nodes[n*3+1]) + " " + Float.toString(nodes[n*3+2]) + "\n";
			writer.write(str);
		}

		//write faces
		for(int f = 0; f < faces.length/3; f++){
			str = "3 "+Integer.toString(faces[f*3]) + " " + Integer.toString(faces[f*3+1]) + " " + Integer.toString(faces[f*3+2]) + "\n";
			writer.write(str);
		}

		writer.close();
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

		//Get Nodes
		float[] nodes;
		nodes = listNodesByCoordinates(img_pix);

		//Get faces
		int[] faces;
		faces = listFacesByNodeVertices();

		//Write PLY 
		System.out.println(plyFileName);
		try {
			writePLYToFile(plyFileName, nodes, faces);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Caught IOException: " + e.getMessage());
		}

		// //Write nodes and vertices into PLY file
		// try {
		// 	writeNodesAndVerticesToFile(plyFileName);
		// } catch (IOException e) {
		// 	// TODO Auto-generated catch block
		// 	e.printStackTrace();
		// }

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
