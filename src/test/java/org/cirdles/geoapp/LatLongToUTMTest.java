/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cirdles.geoapp;

import java.math.BigDecimal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author evc1996
 */
public class LatLongToUTMTest {
    
    public LatLongToUTMTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of convert method, of class LatLongToUTM.
     */
    @org.junit.Test
    public void testConvert_3args_1() {
        System.out.println("convert");
        double latitude = 70.57927709;
        double longitude = 45.59941973;
        String datumName = "WGS84";
        UTM expResult = null;
        UTM result = LatLongToUTM.convert(latitude, longitude, datumName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of convert method, of class LatLongToUTM.
     */
    @org.junit.Test
    public void testConvert_3args_2() {
        System.out.println("convert");
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        String datumName = "";
        UTM expResult = null;
        UTM result = LatLongToUTM.convert(latitude, longitude, datumName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    
}
