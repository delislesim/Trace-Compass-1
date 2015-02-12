/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * Contributors:
 *     Marc Dumais - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import java.util.Hashtable;

import org.eclipse.cdt.visualizer.ui.util.Colors;
import org.eclipse.swt.graphics.Color;


/** Manages which color is assigned to a given id */
public class ColorChoser {
    /** Maps id to display color */
    private Hashtable<String, Color> m_idColorMap = null;
    private int m_allocatedIndex = 0;

    // TODO: here are a few options for colors palettes. Will need to test
    // to really compare them and make a choice...

    // pool of easily distinguished colors
//  final int colorPool[][] = new int[][] {
//          {240,163,255},{0,117,220},{153,63,0},{76,0,92},{25,25,25},
//          {0,92,49},{43,206,72},{255,204,153},{128,128,128},
//          {148,255,181},{143,124,0},{157,204,0},{194,0,136},{0,51,128},
//          {255,164,5},{255,168,187},{66,102,0},{255,0,16},{94,241,242},
//          {0,153,143},{224,255,102},{116,10,255},{153,0,0},{255,255,128},
//          {255,255,0},{255,80,5}};
//  final int colorPool[][] = new int[][] {
//          {255,0,0}, {228,228,0}, {0,255,0}, {0,255,255}, {176,176,255},
//          {255,0,255}, {228,228,228}, {176,0,0}, {186,186,0}, {0,176,0},
//          {0,176,176}, {132,132,255}, {176,0,176}, {186,186,186}, {135,0,0},
//          {135,135,0}, {0,135,0}, {0,135,135}, {73,73,255}, {135,0,135},
//          {135,135,135}, {85,0,0}, {84,84,0}, {0,85,0}, {0,85,85}, {0,0,255},
//          {85,0,85}, {84,84,84}
//  };
    final int colorPool[][] = new int[][] {
            {255, 179, 0},{128, 62, 117},{255, 104, 0},{166, 189, 215},
            {193, 0, 32},{206, 162, 98},{129, 112, 102},{0, 125, 52},
            {246, 118, 142},{0, 83, 138},{255, 122, 92},{83, 55, 122},
            {255, 142, 0},{179, 40, 81},{244, 200, 0},{127, 24, 13},
            {147, 170, 0},{89, 51, 21},{241, 58, 19},{35, 44, 22}
    };

    public ColorChoser() {
        m_idColorMap = new  Hashtable<>();
    }

    public void dispose() {
        if (m_idColorMap != null) {
            m_idColorMap.clear();
            m_idColorMap = null;
        }
    }

    /** Returns a color for a string. Colors are allocated in a "first come, first served"
     * fashion. Once a color is attributes for a string, it will stick for the life
     * time of the object */
    public synchronized Color getColor(String str) {

        Color color;
        if (m_idColorMap.containsKey(str)) {
            color = m_idColorMap.get(str);
        }
        else {
            // allocate next available color
            color = Colors.getColor(
                    colorPool[m_allocatedIndex][0],
                    colorPool[m_allocatedIndex][1],
                    colorPool[m_allocatedIndex][2]
            );
            m_idColorMap.put(str, color);
            m_allocatedIndex++;

            // if color pool exhausted, start re-using colors
            if (m_allocatedIndex >=  colorPool.length) {
                m_allocatedIndex = 0;
            }
        }

        return color;
    }
}
