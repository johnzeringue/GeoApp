/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cirdles.geoapp;

import java.math.BigDecimal;

/**
 *
 * @author Elaina Cole
 */
public class LatLongToUTMDriver {
    
    public static void main(String[] args) {
        
        BigDecimal latitude = new BigDecimal(84);
        BigDecimal longitude = new BigDecimal(102);
        
        UTM utm = LatLongToUTM.convert(latitude, longitude, "WGS84");
        System.out.println("Northing: " + utm.getNorthing());
        System.out.println("Easting: " + utm.getEasting());
        System.out.println("Zone: " + utm.getZoneNumber() + " " + utm.getZoneLetter());
        System.out.println("Hemisphere: " + utm.getHemisphere());
        
        double lat = -1.0;
        double lng = -77.0;
        
        UTM utm2 = LatLongToUTM.convert(lat, lng, "NAD83");
        System.out.println("Northing: " + utm2.getNorthing());
        System.out.println("Easting: " + utm2.getEasting());
        System.out.println("Zone: " + utm2.getZoneNumber() + " " + utm2.getZoneLetter());
        System.out.println("Hemisphere: " + utm2.getHemisphere());
        
        
    }
    
}
