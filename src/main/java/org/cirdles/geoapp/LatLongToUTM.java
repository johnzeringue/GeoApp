
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
import org.apache.commons.math3.analysis.function.Asinh;
import org.apache.commons.math3.analysis.function.Atanh;


/**
 *
 * @author Elaina Cole
 */
public class LatLongToUTM {
    
    private static final BigDecimal scaleFactor = new BigDecimal(0.9996);
    private static final BigDecimal falseEasting = new BigDecimal(500000);
    private static final BigDecimal southHemisphereSubtraction = new BigDecimal(10000000);
    private static final BigDecimal one = new BigDecimal(1);
    private static final int precision = 10;
    
    /**
     * 
     * @param latitude
     * @param longitude
     * @param datumName
     * @return UTM
     * 
     * Converts double latitude longitude to BigDecimal and converts it to UTM
     */
    
    public static UTM convert(double latitude, double longitude, String datumName) {
        
        return convert(new BigDecimal(latitude), new BigDecimal(longitude), datumName);
        
    }     
    
    /**
     * 
     * @param latitude
     * @param longitude
     * @param datumName
     * @return UTM
     * 
     * Converts BigDecimal latitude longitude to UTM 
     */
    public static UTM convert(BigDecimal latitude, BigDecimal longitude, String datumName){
        
        DatumEnum datumEnum = DatumEnum.valueOf(datumName);
        BigDecimal flattening3D = new BigDecimal(datumEnum.getFlattening3D());
        BigDecimal meridianRadius = new BigDecimal(datumEnum.getMeridianRadius());
        BigDecimal eccentricity = new BigDecimal(datumEnum.getEccentricity());
        
        BigDecimal latitudeRadians = latitude.abs().multiply(
                new BigDecimal(Math.PI)).divide(new BigDecimal(180.0), precision,
                RoundingMode.HALF_UP);
        
        int zoneNumber = calcZoneNumber(longitude);
        
        BigDecimal zoneCentralMeridian = calcZoneCentralMeridian(zoneNumber);
        
        BigDecimal changeInLongitudeDegree = (longitude.subtract(zoneCentralMeridian)
                ).abs().setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal changeInLongitudeRadians = (changeInLongitudeDegree.multiply(
                new BigDecimal(Math.PI))).divide(new BigDecimal(180), precision, 
                RoundingMode.HALF_UP);
        

        BigDecimal conformalLatitude = calcConformalLatitude(eccentricity, 
                latitudeRadians).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal tauPrime = (new BigDecimal(Math.tan(conformalLatitude.
                doubleValue()))).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal xiPrimeNorth = calcXiPrimeNorth(changeInLongitudeRadians, 
                tauPrime).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal etaPrimeEast = calcEtaPrimeEast(changeInLongitudeRadians, 
                tauPrime).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal[] alphaSeries = {
            KrugerSeries.alpha1(flattening3D).setScale(precision, RoundingMode.HALF_UP),
            KrugerSeries.alpha2(flattening3D.setScale(precision, RoundingMode.HALF_UP)), 
            KrugerSeries.alpha3(flattening3D).setScale(precision, RoundingMode.HALF_UP),
            KrugerSeries.alpha4(flattening3D).setScale(precision, RoundingMode.HALF_UP), 
            KrugerSeries.alpha5(flattening3D).setScale(precision, RoundingMode.HALF_UP),
            KrugerSeries.alpha6(flattening3D).setScale(precision, RoundingMode.HALF_UP), 
            KrugerSeries.alpha7(flattening3D).setScale(precision, RoundingMode.HALF_UP)};
        

        BigDecimal xiNorth = calcXiNorth(xiPrimeNorth, etaPrimeEast, 
                alphaSeries).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal etaEast = calcEtaEast(xiPrimeNorth, etaPrimeEast, 
                alphaSeries).setScale(precision, RoundingMode.HALF_UP);
        
        BigDecimal easting = calcEasting(meridianRadius, etaEast, longitude, 
                zoneCentralMeridian).setScale(precision, RoundingMode.HALF_UP);
        BigDecimal northing = calcNorthing(meridianRadius, xiNorth, 
                latitude).setScale(precision, RoundingMode.HALF_UP);
        
        char zoneLetter = calcZoneLetter(latitude);
        char hemisphere = calcHemisphere(latitude);
        
        return new UTM(easting, northing, hemisphere, zoneNumber, zoneLetter);
        
    }
    

    
    /**
     * 
     * @param longitude
     * @return int zone number
     * 
     * Returns the zone number that the longitude corresponds to on the UTM map
     */
    private static int calcZoneNumber(BigDecimal longitude) {
        int zoneNumber;
        BigDecimal six = new BigDecimal(6);
        
        if (longitude.signum() < 0) {
            
            BigDecimal oneEighty = new BigDecimal(180);
            zoneNumber = ((oneEighty.add(longitude)).divide(six, precision, 
                    RoundingMode.HALF_UP)).intValue()+ 1;
        }
            
        else {
            
            BigDecimal thirtyOne = new BigDecimal(31);
            zoneNumber = ((longitude.divide(six, precision, RoundingMode.HALF_UP)).abs().add(
                    thirtyOne)).intValue();
        }
        
        return zoneNumber;
        
    }
    
    /**
     * 
     * @param zoneNumber
     * @return BigDecimal central meridian
     * 
     * Central meridian is the center of the zone on the UTM map
     */
    private static BigDecimal calcZoneCentralMeridian(int zoneNumber) {
        
        BigDecimal zoneCentralMeridian = new BigDecimal(zoneNumber * 6 - 183);
        return zoneCentralMeridian;
    
    }
    
    /**
     * 
     * @param eccentricity
     * @param latitudeRadians
     * @return BigDecimal conformal latitude
     * 
     * Eccentricity helps define the shape of the ellipsoidal representation of
     * the earth
     * 
     * Conformal latitude gives an angle-preserving (conformal) transformation 
     * to the sphere. It defines a transformation from the ellipsoid to a sphere 
     * of arbitrary radius such that the angle of intersection between any two 
     * lines on the ellipsoid is the same as the corresponding angle on the sphere.
     */
    private static BigDecimal calcConformalLatitude(BigDecimal eccentricity, BigDecimal latitudeRadians) {
        
        BigDecimal conformalLatitude;
        
        double latRadDouble = latitudeRadians.doubleValue();
        double eccDouble = eccentricity.doubleValue();
        double confLatDouble;
        
        Atanh atanh = new Atanh();
        Asinh asinh = new Asinh();
        
        confLatDouble = Math.atan(Math.sinh(asinh.value( Math.tan(latRadDouble)) -
            eccDouble * atanh.value(eccDouble * Math.sin(latRadDouble))));
        
        conformalLatitude = new BigDecimal(confLatDouble);
        
        return conformalLatitude;
        
    }
    

    /**
     * 
     * @param changeInLongitudeRadians
     * @param tauPrime
     * @return BigDecimal xi prime
     * 
     * tau refers to the torsion of a curve
     * 
     * xi refers to the north-south direction
     */
    private static BigDecimal calcXiPrimeNorth(BigDecimal changeInLongitudeRadians,
            BigDecimal tauPrime) {
        
        double cosOfLatRad = Math.cos(changeInLongitudeRadians.doubleValue());
        
        BigDecimal xiPrime = new BigDecimal(Math.atan(tauPrime.doubleValue() / cosOfLatRad));
        
        return xiPrime;
    }
    
    /**
     * 
     * @param changeInLongitudeRadians
     * @param tauPrime
     * @return BigDecimal eta prime
     * 
     * eta refers to the east-west direction
     */
    private static BigDecimal calcEtaPrimeEast(BigDecimal changeInLongitudeRadians, 
            BigDecimal tauPrime) {
        
        BigDecimal etaPrime;
        
        double sinOfLatRad = Math.sin(changeInLongitudeRadians.doubleValue());
        double cosOfLatRad = Math.cos(changeInLongitudeRadians.doubleValue());
        double cosOfLatRadSquared = Math.pow(cosOfLatRad, 2);
        
        BigDecimal tauPrimeSquared = tauPrime.pow(2);
        
        double sqrt = Math.sqrt(tauPrimeSquared.doubleValue() + cosOfLatRadSquared);
        double sinOverSqrt = sinOfLatRad / sqrt;
        
        Asinh asinhOfSin = new Asinh();
        etaPrime = new BigDecimal(asinhOfSin.value(sinOverSqrt));
        
        return etaPrime;
        
    }
    
    
    /**
     * 
     * @param xiPrimeNorth
     * @param etaPrimeEast
     * @param alphaSeries
     * @return BigDecimal xi
     * 
     * alpha series based on Krüger series, which entails mapping the ellipsoid 
     * to the conformal sphere.
     * 
     */
    private static BigDecimal calcXiNorth(BigDecimal xiPrimeNorth,
            BigDecimal etaPrimeEast, BigDecimal[] alphaSeries) {

        double multiplicand = 2;
        double xiNorthDouble = xiPrimeNorth.doubleValue();
        double xiPrimeNortDouble = xiPrimeNorth.doubleValue();
        double etaPrimeEastDouble = etaPrimeEast.doubleValue();
        
        for (BigDecimal alpha : alphaSeries) {
            
            double sinOfXiPrimeNorth = Math.sin(
                    xiPrimeNortDouble * multiplicand);
            
            double coshOfEtaPrimeEast = Math.cosh(
                    etaPrimeEastDouble * multiplicand);
            
            double augend = (alpha.doubleValue() * sinOfXiPrimeNorth) * 
                    coshOfEtaPrimeEast;
            
            xiNorthDouble = xiNorthDouble + augend;
            
            multiplicand = multiplicand + 2;
        }
        
        BigDecimal xiNorth = new BigDecimal(xiNorthDouble);
        
        return xiNorth;
       
        
    }
    
    /**
     * 
     * @param xiPrimeNorth
     * @param etaPrimeEast
     * @param alphaSeries
     * @return BigDecimal eta
     */
    private static BigDecimal calcEtaEast(BigDecimal xiPrimeNorth,BigDecimal 
            etaPrimeEast, BigDecimal[] alphaSeries) {
        
        
        double multiplicand = 2;
        double etaEastDouble = etaPrimeEast.doubleValue();
        double etaPrimeEastDouble = etaPrimeEast.doubleValue();
        double xiPrimeNorthDouble = xiPrimeNorth.doubleValue();
        
        for(int i=0; i < alphaSeries.length - 2; i++) {
            
            double cosOfXiPrimeNorth = Math.cos(
                    xiPrimeNorthDouble * multiplicand);
            
            double sinhOfEtaPrimeEast = Math.sinh(
                    etaPrimeEastDouble * multiplicand);
            
            double augend = (alphaSeries[i].doubleValue() * cosOfXiPrimeNorth)*
                    sinhOfEtaPrimeEast;
            
            etaEastDouble = etaEastDouble + augend;
            multiplicand = multiplicand + 2;
            
        }
        
        double cosOfXiPrimeNorth = Math.cos(
                    xiPrimeNorthDouble * multiplicand);
            
        double sinhOfEtaPrimeEast = Math.sinh(
                    etaPrimeEastDouble * multiplicand);
            
        double augend = (alphaSeries[5].doubleValue() * cosOfXiPrimeNorth)*
                    sinhOfEtaPrimeEast;
            
        etaEastDouble = etaEastDouble + augend;
        
        BigDecimal etaEast = new BigDecimal(etaEastDouble);
        
        return etaEast;

        
    }
    
    /**
     * 
     * @param latitude
     * @return char zone letter
     * 
     * Latitude corresponds to a zone letter on the UTM map. Match up zone letter
     * and zone number on UTM map to find the specific zone.
     * 
     */
    private static char calcZoneLetter(BigDecimal latitude) {
        String letters = "CDEFGHJKLMNPQRSTUVWXX";
        double lat = latitude.doubleValue();
        
        if(lat >= -80 && lat <= 84)
            return letters.charAt(new Double(Math.floor((lat+80.0)/8.0)).intValue());
        
        else
            return 'Z';
        
    } 

    /**
     * 
     * @param meridianRadius
     * @param etaEast
     * @param longitude
     * @param centralMeridian
     * @return BigDecimal easting
     * 
     * The meridian radius is based on the lines that run north-south on a map.
     * 
     * UTM easting coordinates are referenced to the center line of the zone 
     * known as the central meridian. The central meridian is assigned an 
     * easting value of 500,000 meters East.
     */
    private static BigDecimal calcEasting(BigDecimal meridianRadius, BigDecimal
            etaEast, BigDecimal longitude, BigDecimal centralMeridian) { 
        
        BigDecimal easting = (scaleFactor.multiply(meridianRadius)).multiply(etaEast);
        BigDecimal eastOfCM = one;
        
        if (longitude.compareTo(centralMeridian) < 0)
            eastOfCM = eastOfCM.multiply(new BigDecimal(-1));
        
        easting = falseEasting.add(eastOfCM.multiply(easting));
        
        
        return easting;
    }
    
    /**
     * 
     * @param meridianRadius
     * @param xiNorth
     * @param latitude
     * @return BigDecimal northing
     * 
     * UTM northing coordinates are measured relative to the equator. For 
     * locations north of the equator the equator is assigned the northing 
     * value of 0 meters North. To avoid negative numbers, locations south of 
     * the equator are made with the equator assigned a value of 10,000,000 
     * meters North.
     */
    private static BigDecimal calcNorthing(BigDecimal meridianRadius, BigDecimal 
            xiNorth, BigDecimal latitude) {
        
        BigDecimal northing = (scaleFactor.multiply(meridianRadius)).multiply(xiNorth);
        
        if(latitude.signum() < 0)
            northing = southHemisphereSubtraction.subtract(northing);
        

        return northing;
        
    }
    
    /**
     * 
     * @param latitude
     * @return char Hemisphere
     * 
     * Returns whether the latitude indicates being in the southern or northern
     * hemisphere
     */
    private static char calcHemisphere(BigDecimal latitude) {
        
        char hemisphere = 'N';
        
        if (latitude.signum() == -1)
            hemisphere = 'S';
        
        return hemisphere;
    }
    
}
