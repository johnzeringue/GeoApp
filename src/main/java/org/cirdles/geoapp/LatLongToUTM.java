
/*
 * LatLongToUTM.java
 *
 * Created Oct 25, 2015
 *
 * Copyright 2015 CIRDLES.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 
The arctanh capacity is provided by Michael Thomas Flanagan's Java Scientific Library:
   Dr Michael Thomas Flanagan
   www.ee.ucl.ac.uk/~mflanaga
   Department of Electronic and Electrical Engineering
   UCL (University College London)
   Torrington Place
   London
   WC1E 7JE

The capacity to use trig functions on BigDecimals is provided by deeplearning4j's
nd4j repository, which can be accessed here: https://github.com/deeplearning4j/nd4j

The formulas are based on Charles Karney's formulas for UTM conversion:
"Transverse Mercator with an accuracy of a few nanometers"
Charles F. F. Karney
SRI International, 201 Washington Rd, Princeton, NJ 08543-5300

Information that helped me understand the formulas:
"How to Use the Spreadsheet for Converting UTM to Latitude and Longitude (Or Vice Versa)" and
"Converting UTM to Latitude and Longitude (Or Vice Versa)"
Steven Dutch, Natural and Applied Sciences, University of Wisconsin - Green Bay

 */
package org.cirdles.geoapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import flanagan.math.*;
import org.nd4j.linalg.util.BigDecimalMath;

/**
 *
 * @author Elaina Cole
 */
public class LatLongToUTM {
    
    private static final BigDecimal scaleFactor = new BigDecimal(0.9996);
    private static final BigDecimal falseEasting = new BigDecimal(500000);
    private static final BigDecimal southHemisphereSubtraction = new BigDecimal(10000000);
    private static final BigDecimal one = new BigDecimal(1);
    private static final BigDecimal two = new BigDecimal(2);
    private static final int numOfDecimals = 10;
    
    public static UTM convert(double latitude, double longitude, String datumName) {
        
        return convert(new BigDecimal(latitude), new BigDecimal(longitude), datumName);
        
    }     
    public static UTM convert(BigDecimal latitude, BigDecimal longitude, String datumName){
        
        DatumEnum datumEnum = DatumEnum.valueOf(datumName);
        BigDecimal flattening3D = new BigDecimal(datumEnum.getFlattening3D());
        BigDecimal meridianRadius = new BigDecimal(datumEnum.getMeridianRadius());
        BigDecimal eccentricity = new BigDecimal(datumEnum.getEccentricity());
        
        BigDecimal latitudeRadians = latitude.abs().multiply(
                new BigDecimal(Math.PI)).divide(new BigDecimal(180.0), numOfDecimals,
                RoundingMode.HALF_UP);

        int zoneNumber = calcZoneNumber(longitude);
        
        BigDecimal zoneCentralMeridian = calcZoneCentralMeridian(zoneNumber);
        
        BigDecimal changeInLongitudeDegree = (longitude.subtract(zoneCentralMeridian)).abs();
        
        BigDecimal changeInLongitudeRadians = (changeInLongitudeDegree.multiply(
                new BigDecimal(Math.PI))).divide(new BigDecimal(180), numOfDecimals, 
                RoundingMode.HALF_UP);
        

        BigDecimal conformalLatitude = calcConformalLatitude(eccentricity, latitudeRadians);
        
        BigDecimal tauPrime = BigDecimalMath.tan(conformalLatitude);
        
        BigDecimal xiPrimeNorth = calcXiPrimeNorth(changeInLongitudeRadians, tauPrime);
        
        BigDecimal etaPrimeEast = calcEtaPrimeEast(changeInLongitudeRadians, tauPrime);
        
        BigDecimal[] alphaSeries = {
            KrugerSeries.alpha1(flattening3D),
            KrugerSeries.alpha2(flattening3D), 
            KrugerSeries.alpha3(flattening3D),
            KrugerSeries.alpha4(flattening3D), 
            KrugerSeries.alpha5(flattening3D),
            KrugerSeries.alpha6(flattening3D), 
            KrugerSeries.alpha7(flattening3D)};

        BigDecimal xiNorth = calcXiNorth(xiPrimeNorth, etaPrimeEast, alphaSeries);
        
        BigDecimal etaEast = calcEtaEast(xiPrimeNorth, etaPrimeEast, alphaSeries);
        
        BigDecimal easting = calcEasting(meridianRadius, etaEast, longitude);
        BigDecimal northing = calcNorthing(meridianRadius, xiNorth, latitude);
        
        char zoneLetter = calcZoneLetter(latitude);
        char hemisphere = calcHemisphere(latitude);
        
        return new UTM(easting, northing, hemisphere, zoneNumber, zoneLetter);
        
    }
    

    
    
    private static int calcZoneNumber(BigDecimal longitude) {
        int zoneNumber;
        BigDecimal six = new BigDecimal(6);
        
        if (longitude.signum() < 0) {
            
            BigDecimal oneEighty = new BigDecimal(180);
            zoneNumber = ((oneEighty.add(longitude)).divide(six, numOfDecimals, 
                    RoundingMode.HALF_UP)).intValue()+ 1;
        }
            
        else {
            
            BigDecimal thirtyOne = new BigDecimal(31);
            zoneNumber = ((longitude.divide(six, numOfDecimals, RoundingMode.HALF_UP)).abs().add(
                    thirtyOne)).intValue();
        }
        
        return zoneNumber;
        
    }
    
    private static BigDecimal calcZoneCentralMeridian(int zoneNumber) {
        
        BigDecimal zoneCentralMeridian = new BigDecimal(zoneNumber * 6 - 183);
        return zoneCentralMeridian;
    
    }
    
    
    private static BigDecimal calcConformalLatitude(BigDecimal eccentricity, BigDecimal latitudeRadians) {
        
        BigDecimal conformalLatitude;
        
        BigDecimal tanOfLatRad = BigDecimalMath.tan(latitudeRadians);
        BigDecimal arcSinhOfTanLatRad = BigDecimalMath.asinh(tanOfLatRad);
        BigDecimal sinhOfArcSinh = BigDecimalMath.sinh(arcSinhOfTanLatRad);
        
        BigDecimal sinOfLatRad = BigDecimalMath.sin(latitudeRadians);
        BigDecimal eccTimesSin = eccentricity.multiply(sinOfLatRad);
        BigDecimal arcTanhOfMult = new BigDecimal (Fmath.atanh(
                eccTimesSin.doubleValue()));
        BigDecimal eccTimesArcTanh = eccentricity.multiply(arcTanhOfMult);
        
        BigDecimal subtraction = sinhOfArcSinh.subtract(eccTimesArcTanh);
        
        conformalLatitude = BigDecimalMath.atan(subtraction);
        
        return conformalLatitude;
        
    }
    

    
    //Need xi north and east to find easting and northing
    private static BigDecimal calcXiPrimeNorth(BigDecimal changeInLongitudeRadians,
            BigDecimal tauPrime) {
        
        BigDecimal cosOfLatRad = BigDecimalMath.cos(changeInLongitudeRadians);
        
        BigDecimal xiPrime = BigDecimalMath.atan(tauPrime.divide(cosOfLatRad, 
                numOfDecimals, RoundingMode.HALF_UP));
        
        return xiPrime;
    }
    
    private static BigDecimal calcEtaPrimeEast(BigDecimal changeInLongitudeRadians, 
            BigDecimal tauPrime) {
        
        BigDecimal etaPrime;
        
        BigDecimal sinOfLatRad = BigDecimalMath.sin(changeInLongitudeRadians);
        BigDecimal cosOfLatRad = BigDecimalMath.cos(changeInLongitudeRadians);
        BigDecimal cosOfLatRadSquared = cosOfLatRad.pow(2);
        BigDecimal tauPrimeSquared = tauPrime.pow(2);
        BigDecimal sqrt = BigDecimalMath.sqrt(tauPrimeSquared.add(cosOfLatRadSquared));
        BigDecimal sinOverSqrt = sinOfLatRad.divide(sqrt, numOfDecimals, RoundingMode.HALF_UP);
        etaPrime = BigDecimalMath.asinh(sinOverSqrt);
        
        return etaPrime;
        
    }
    

    private static BigDecimal calcXiNorth(BigDecimal xiPrimeNorth,
            BigDecimal etaPrimeEast, BigDecimal[] alphaSeries) {

        BigDecimal multiplicand = two;
        BigDecimal xiNorth = xiPrimeNorth;
        
        for (BigDecimal alpha : alphaSeries) {
            
            BigDecimal sinOfXiPrimeNorth = BigDecimalMath.sin(
                    xiPrimeNorth.multiply(multiplicand));
            
            BigDecimal coshOfEtaPrimeEast = BigDecimalMath.cosh(
                    etaPrimeEast.multiply(multiplicand));
            
            BigDecimal augend = (alpha.multiply(sinOfXiPrimeNorth)).multiply(coshOfEtaPrimeEast);
            
            xiNorth = xiNorth.add(augend);
            
            multiplicand = multiplicand.add(two);
        }
        

        return xiNorth;
       
        
    }
    
    
    private static BigDecimal calcEtaEast(BigDecimal xiPrimeNorth,BigDecimal 
            etaPrimeEast, BigDecimal[] alphaSeries) {
        
        
        BigDecimal multiplicand = two;
        BigDecimal etaEast = etaPrimeEast;
        
        
        for(int i=0; i < alphaSeries.length - 2; i++) {
            
            BigDecimal cosOfXiPrimeNorth = BigDecimalMath.cos(
                    xiPrimeNorth.multiply(multiplicand));
            
            BigDecimal sinhOfEtaPrimeEast = BigDecimalMath.sinh(
                    etaPrimeEast.multiply(multiplicand));
            
            BigDecimal augend = (alphaSeries[i].multiply(cosOfXiPrimeNorth)).multiply(
                    sinhOfEtaPrimeEast);
            
            etaEast.add(augend);
            multiplicand.add(two);
            
        }
        
        BigDecimal cosOfXiPrimeNorth = BigDecimalMath.cos(
                    xiPrimeNorth.multiply(multiplicand));
            
        BigDecimal sinhOfEtaPrimeEast = BigDecimalMath.sinh(
                    etaPrimeEast.multiply(multiplicand));
            
        BigDecimal augend = (alphaSeries[5].multiply(cosOfXiPrimeNorth)).multiply(
                    sinhOfEtaPrimeEast);
            
        etaEast.add(augend);
        

        return etaEast;

        
    }
    
    private static char calcZoneLetter(BigDecimal latitude) {
        String letters = "CDEFGHJKLMNPQRSTUVWXX";
        double lat = latitude.doubleValue();
        
        if(lat >= -80 && lat <= 84)
            return letters.charAt(new Double(Math.floor((lat+80.0)/8.0)).intValue());
        
        else
            return 'Z';
        
    } 

    
    private static BigDecimal calcEasting(BigDecimal meridianRadius, BigDecimal
            etaEast, BigDecimal longitude) { 
        
        BigDecimal easting = (scaleFactor.multiply(meridianRadius)).multiply(etaEast);
        BigDecimal eastOfCM = one;
        
        if (longitude.signum() < 0)
            eastOfCM = eastOfCM.multiply(new BigDecimal(-1));
        
        easting = falseEasting.add(eastOfCM.multiply(easting));
        
        
        return easting;
    }
    
    private static BigDecimal calcNorthing(BigDecimal meridianRadius, BigDecimal 
            xiNorth, BigDecimal latitude) {
        
        BigDecimal northing = (scaleFactor.multiply(meridianRadius)).multiply(xiNorth);
        
        if(latitude.signum() < 0)
            northing = southHemisphereSubtraction.subtract(northing);
        

        return northing;
        
    }
    
    private static char calcHemisphere(BigDecimal latitude) {
        
        char hemisphere = 'N';
        
        if (latitude.signum() == -1)
            hemisphere = 'S';
        
        return hemisphere;
    }
    public static void main(String[] args) {
        
        BigDecimal latitude = new BigDecimal(77);
        BigDecimal longitude = new BigDecimal(53.4);
        
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
