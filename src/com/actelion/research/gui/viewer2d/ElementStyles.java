package com.actelion.research.gui.viewer2d;

import java.awt.Color;

/**
 * Constants used for the 3D visualization
 * 
 * @author freyssj
 */
public interface ElementStyles {
	
	/** Arrays of colors(r,g,b) indexed by atom number */  
	public static final Color[] ELEMENT_COLORS = new Color[]{
		new Color(0.40f, 0.60f, 0.40f), 	//None
		new Color(1.00f, 1.00f, 1.00f),  // 1. Hydrogen   = white		
		new Color(0.87f, 1.00f, 1.00f),  // 2. Helium     = lt blue
		new Color(0.81f, 0.48f, 1.00f),  // 3. Lithium    = lt purple
		new Color(0.78f, 1.00f, 0.00f),  // 4. Beryllium  = yel grn
		new Color(1.00f, 0.71f, 0.71f),  // 5. Boron      = pink
//		{0.00f, 1.00f, 0.00f),  // 6. Carbon     = green
		new Color(0.70f, 0.70f, 0.70f),  // 6. Carbon     = grey
		new Color(0.00f, 0.00f, 1.00f),  // 7. Nitrogen   = blue
		new Color(1.00f, 0.00f, 0.00f),  // 8. Oxygen     = red
		new Color(0.71f, 1.00f, 1.00f),  // 9. Flourine   = cyan
		new Color(0.67f, 0.91f, 0.97f),  // 10. Neon       = aqua
		new Color(0.67f, 0.35f, 0.97f),  // 11. Sodium     = purple
		new Color(0.55f, 1.00f, 0.00f),  // 12. Magnesium  = aqua grn
		new Color(0.84f, 0.65f, 0.65f),  // 13. Aluminum   = med pink
		new Color(0.52f, 0.61f, 0.61f),  // 14. Silicon    = gray grn
		new Color(1.00f, 0.52f, 0.00f),  // 15. Phosphorus = orange
		new Color(1.00f, 0.81f, 0.19f),  // 16. Sulfur     = drk yellow
		new Color(0.13f, 0.97f, 0.13f),  // 17. Chlorine   = lt green
		new Color(0.51f, 0.84f, 0.91f),  // 18. Argon      = sky blue
		new Color(0.55f, 0.26f, 0.84f),  // 19. Potassium  = drk purple
		new Color(0.22f, 1.00f, 0.00f),  // 20. Calcium    = med green
		new Color(0.70f, 0.70f, 0.70f), 	// 21.
		new Color(0.70f, 0.70f, 0.70f), 	// 22.
		new Color(0.70f, 0.70f, 0.70f), 	// 23.
		new Color(0.70f, 0.70f, 0.70f), 	// 24.
		new Color(0.70f, 0.70f, 0.70f), 	// 25.
		new Color(0.52f, 0.48f, 0.78f),  // 26. Iron       = royal blue
		new Color(0.70f, 0.70f, 0.70f), 	// 27.
		new Color(0.70f, 0.70f, 0.70f), 	// 28.
		new Color(0.70f, 0.70f, 0.70f), 	// 29.
		new Color(0.70f, 0.70f, 0.70f), 	// 30.
		new Color(0.70f, 0.70f, 0.70f), 	// 31.
		new Color(0.70f, 0.70f, 0.70f), 	// 32.
		new Color(0.70f, 0.70f, 0.70f), 	// 33.
		new Color(0.70f, 0.70f, 0.70f), 	// 34.
		new Color(0.13f, 0.70f, 0.13f), 	// 35. Bromide
		new Color(0.70f, 0.70f, 0.70f), 	// 36.
		new Color(0.70f, 0.70f, 0.70f), 	// 37.
		new Color(0.70f, 0.70f, 0.70f), 	// 38.
		new Color(0.70f, 0.70f, 0.70f), 	// 39.
		new Color(0.70f, 0.70f, 0.70f), 	// 40.
		new Color(0.70f, 0.70f, 0.70f), 	// 41.
		new Color(0.70f, 0.70f, 0.70f), 	// 42.
		new Color(0.70f, 0.70f, 0.70f), 	// 43.
		new Color(0.70f, 0.70f, 0.70f), 	// 44.
		new Color(0.70f, 0.70f, 0.70f), 	// 45.
		new Color(0.70f, 0.70f, 0.70f), 	// 46. Palladium
		new Color(0.70f, 0.70f, 0.70f), 	// 47.
		new Color(0.70f, 0.70f, 0.70f), 	// 48.
		new Color(0.70f, 0.70f, 0.70f), 	// 49.
		new Color(0.70f, 0.70f, 0.70f), 	// 50.
		new Color(0.70f, 0.70f, 0.70f), 	// 51.
		new Color(0.70f, 0.70f, 0.70f), 	// 52.
		new Color(0.58f, 0.00f, 0.58f),  // 53. Iodine     = deep prpl
		new Color(0.70f, 0.70f, 0.70f), 	// 54.
		new Color(0.70f, 0.70f, 0.70f), 	// 55.
		new Color(0.70f, 0.70f, 0.70f), 	// 56.
		new Color(0.70f, 0.70f, 0.70f), 	// 57.
		new Color(0.70f, 0.70f, 0.70f), 	// 58.
		new Color(0.70f, 0.70f, 0.70f), 	// 59.
		new Color(0.70f, 0.70f, 0.70f), 	// 60.
		new Color(0.70f, 0.70f, 0.70f), 	// 61.
		new Color(0.70f, 0.70f, 0.70f), 	// 62.
		new Color(0.70f, 0.70f, 0.70f), 	// 63.
		new Color(0.70f, 0.70f, 0.70f), 	// 64.
		new Color(0.70f, 0.70f, 0.70f), 	// 65.
		new Color(0.70f, 0.70f, 0.70f), 	// 66.
		new Color(0.70f, 0.70f, 0.70f), 	// 67.
		new Color(0.70f, 0.70f, 0.70f), 	// 68.
		new Color(0.70f, 0.70f, 0.70f), 	// 69.
		new Color(0.70f, 0.70f, 0.70f), 	// 70.
		new Color(0.70f, 0.70f, 0.70f), 	// 71.
		new Color(0.70f, 0.70f, 0.70f), 	// 72.
		new Color(0.70f, 0.70f, 0.70f), 	// 73.
		new Color(0.70f, 0.70f, 0.70f), 	// 74.
		new Color(0.70f, 0.70f, 0.70f), 	// 75.
		new Color(0.70f, 0.70f, 0.70f), 	// 76.
		new Color(0.70f, 0.70f, 0.70f), 	// 77.
		new Color(0.70f, 0.70f, 0.70f), 	// 78.
		new Color(0.70f, 0.70f, 0.70f), 	// 79.
		new Color(0.70f, 0.70f, 0.70f), 	// 80.
		new Color(0.70f, 0.70f, 0.70f), 	// 81.
		new Color(0.70f, 0.70f, 0.70f), 	// 82.
		new Color(0.70f, 0.70f, 0.70f), 	// 83.
		new Color(0.70f, 0.70f, 0.70f), 	// 84.
		new Color(0.70f, 0.70f, 0.70f), 	// 85.
		new Color(0.70f, 0.70f, 0.70f), 	// 86.
		new Color(0.70f, 0.70f, 0.70f), 	// 87.
		new Color(0.70f, 0.70f, 0.70f), 	// 88.
		new Color(0.70f, 0.70f, 0.70f), 	// 89.
		new Color(0.70f, 0.70f, 0.70f), 	// 90.
		new Color(0.70f, 0.70f, 0.70f), 	// 91.
		new Color(0.70f, 0.70f, 0.70f), 	// 92.
		new Color(0.70f, 0.70f, 0.70f), 	// 93.
		new Color(0.70f, 0.70f, 0.70f), 	// 94.
		new Color(0.70f, 0.70f, 0.70f), 	// 95.
		new Color(0.70f, 0.70f, 0.70f), 	// 96.
		new Color(0.70f, 0.70f, 0.70f), 	// 97.
		new Color(0.70f, 0.70f, 0.70f), 	// 98.
		new Color(0.70f, 0.70f, 0.70f) 		// 99. 	
		};
		
}
