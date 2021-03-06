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
import org.apache.commons.math3.analysis.function.Atanh;

/**
 *
 * @author Elaina Cole
 */
public class UTMToLatLong {
    
    private static final BigDecimal SCALE_FACTOR = new BigDecimal(0.9996);
    private static final BigDecimal FALSE_EASTING = new BigDecimal(500000);
    private static final BigDecimal SOUTH_HEMISPHERE_SUBTRACTION = new BigDecimal(10000000);
    private static final BigDecimal ONE = new BigDecimal(1);
    private static final int PRECISION = 10;
    
    
    public static String convert(UTM utm, String datum) {
        
        Datum datumInformation = Datum.valueOf(datum);
        
        BigDecimal flattening3D = new BigDecimal(datumInformation.getFlattening3D());
        
        BigDecimal[] betaSeries = {
            
            KrugerSeries.beta1(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta2(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta3(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta4(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta5(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta6(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP),
            KrugerSeries.beta7(flattening3D).setScale(PRECISION, RoundingMode.HALF_UP)
            
        };
        
        char hemisphere = utm.getHemisphere();
        
        double zoneCentralMeridian = utm.getZoneNumber() * 6 - 183;
        
        BigDecimal meridianRadius = new BigDecimal(datumInformation.getMeridianRadius());
        
        BigDecimal northing = utm.getNorthing();
        
        BigDecimal easting = utm.getEasting();
        
        BigDecimal xiNorth = calcXiNorth(hemisphere, meridianRadius, northing);
        
        BigDecimal etaEast = calcEtaEast(easting, meridianRadius);
        
        BigDecimal xiPrime = calcXiPrime(xiNorth, etaEast, betaSeries);
        
        BigDecimal etaPrime = calcEtaPrime(xiNorth, etaEast, betaSeries);
        
        BigDecimal tauPrime = calcTauPrime(xiPrime, etaPrime);
        
        BigDecimal eccentricity = new BigDecimal(datumInformation.getEccentricity());
        
        BigDecimal sigma = calcSigma(eccentricity, tauPrime);
        
        BigDecimal latitude = calcLatitude(eccentricity, sigma, tauPrime, 5, tauPrime);
        
        BigDecimal longitude = calcLongitude(zoneCentralMeridian, etaPrime, xiPrime);
        
        String latAndLong = "Latitude: " + latitude + "\nLongitude: " + longitude;
        
        return latAndLong;
        
    }
    
    private static BigDecimal calcXiNorth(char hemisphere, BigDecimal 
            meridianRadius, BigDecimal northing) {
        
        BigDecimal xiNorth;
        
        
        if(hemisphere == 'N') {
            
            xiNorth = northing.divide(SCALE_FACTOR.multiply(meridianRadius), RoundingMode.HALF_UP).
                    setScale(PRECISION, RoundingMode.HALF_UP);
            
        }
        
        else {
            
            BigDecimal minuend = new BigDecimal(10000000);
            
            xiNorth = (minuend.subtract(northing)).divide(
                    SCALE_FACTOR.multiply(meridianRadius), PRECISION, 
                    RoundingMode.HALF_UP);
            
        }
            
        
        
        return xiNorth;
        
    }
    
    
    private static BigDecimal calcEtaEast(BigDecimal easting, BigDecimal meridianRadius) {
        
        BigDecimal etaEast = (easting.subtract(FALSE_EASTING)).divide(
            SCALE_FACTOR.multiply(meridianRadius), PRECISION, RoundingMode.HALF_UP);
        
        return etaEast;
    }
    
    
    private static BigDecimal calcXiPrime(BigDecimal xiNorth, BigDecimal etaEast, 
            BigDecimal[] betaSeries) {
        
        double xiNorthDouble = xiNorth.doubleValue();
        double etaEastDouble = etaEast.doubleValue();
        
        BigDecimal sinOfXiNorth;
        BigDecimal coshOfEtaEast;
        
        BigDecimal subtrahend = new BigDecimal(0.0);
        int multiplicand = 2;
        
        for(BigDecimal beta : betaSeries) {
            
            sinOfXiNorth = new BigDecimal(Math.sin(multiplicand * xiNorthDouble));
            coshOfEtaEast = new BigDecimal(Math.cosh(multiplicand * etaEastDouble));
            
            subtrahend.add(beta.multiply(sinOfXiNorth).multiply(coshOfEtaEast));
            
            multiplicand += 2;
            
        }
        
        BigDecimal xiPrime = xiNorth.subtract(subtrahend);
        
        return xiPrime;
        
        
    }
    
    
    private static BigDecimal calcEtaPrime(BigDecimal xiNorth, BigDecimal etaEast,
            BigDecimal[] betaSeries) {
        
        double xiNorthDouble = xiNorth.doubleValue();
        double etaEastDouble = etaEast.doubleValue();
        
        BigDecimal cosOfXiNorth;
        BigDecimal sinhOfEtaEast;
        
        BigDecimal subtrahend = new BigDecimal(0.0);
        int multiplicand = 2;
        
        for(BigDecimal beta : betaSeries) {
            
            cosOfXiNorth = new BigDecimal(Math.cos(multiplicand * xiNorthDouble));
            sinhOfEtaEast = new BigDecimal(Math.sinh(multiplicand * etaEastDouble));
            
            subtrahend.add(beta.multiply(cosOfXiNorth).multiply(sinhOfEtaEast));
            
            multiplicand += 2;
            
        }
        
        BigDecimal etaPrime = etaEast.subtract(subtrahend);
        
        return etaPrime;
        
        
    }
    
    private static BigDecimal calcTauPrime(BigDecimal xiPrime, BigDecimal etaPrime) {
        
        double xiPrimeDouble = xiPrime.doubleValue();
        double etaPrimeDouble = etaPrime.doubleValue();
        
        BigDecimal sinOfXiPrime = new BigDecimal(Math.sin(xiPrimeDouble));
        BigDecimal cosOfXiPrime = new BigDecimal(Math.cos(xiPrimeDouble));
        BigDecimal sinhOfEtaPrime = new BigDecimal(Math.sinh(etaPrimeDouble));
        
        BigDecimal squareRoot = new BigDecimal(Math.sqrt(sinhOfEtaPrime.pow(2).
                add(cosOfXiPrime.pow(2)).doubleValue()));
        
        BigDecimal tauPrime = sinOfXiPrime.divide(squareRoot, PRECISION, RoundingMode.HALF_UP);
        
        return tauPrime;
    }
    
    private static BigDecimal calcSigma(BigDecimal eccentricity, BigDecimal tau) {
        
        double eccentricityDouble = eccentricity.doubleValue();
        double tauDouble = tau.doubleValue();
        
        Atanh atanh = new Atanh();
        double sigmaDouble = Math.sinh(eccentricityDouble *
            (atanh.value( eccentricityDouble * tauDouble / Math.sqrt(
            1 + Math.pow(tauDouble, 2)))));
        
        BigDecimal sigma = new BigDecimal (sigmaDouble);
        
        return sigma;
        
    }
    
    
    private static BigDecimal functionOfTau(BigDecimal currentTau, BigDecimal
        currentSigma, BigDecimal originalTau) {
        
        BigDecimal funcOfTau = originalTau.multiply(new BigDecimal(Math.sqrt(1 + 
            currentSigma.pow(2).doubleValue()))).subtract(currentSigma.multiply(
            new BigDecimal(Math.sqrt(1 + currentTau.pow(2).doubleValue())))).subtract(originalTau);
        
        return funcOfTau;
        
    }
    

    
    private static BigDecimal changeInTau(BigDecimal eccentricity, BigDecimal 
        currentTau, BigDecimal currentSigma) {
        
        BigDecimal changeInTau = ((new BigDecimal(Math.sqrt((1 + currentSigma.pow(2).doubleValue()) * 
            (1 + currentTau.pow(2).doubleValue())))).subtract(
            currentSigma.multiply(currentTau))).multiply(new BigDecimal(1 - 
            eccentricity.pow(2).doubleValue())).multiply(new BigDecimal(Math.sqrt(
            1 + currentTau.pow(2).doubleValue()))).divide(ONE.add(
            ONE.subtract(eccentricity.pow(2))).multiply(currentTau.pow(2)), PRECISION, RoundingMode.HALF_UP);
        
        
        return changeInTau;
        
    }
    
    
    private static BigDecimal approximateTau(BigDecimal currentTau, BigDecimal
        funcOfTau, BigDecimal changeInTau) {
        
        BigDecimal newTau = currentTau.subtract(funcOfTau.divide(changeInTau, PRECISION, RoundingMode.HALF_UP));
        
        return newTau;
        
    }
    
    private static BigDecimal calcLatitude(BigDecimal eccentricity, 
        BigDecimal sigma, BigDecimal currentTau, int numOfApproximations, 
        BigDecimal originalTau) {
        
        BigDecimal funcOfTau = functionOfTau(currentTau, sigma, originalTau);
        BigDecimal changeInTau = changeInTau(eccentricity, currentTau, sigma);
        BigDecimal nextTau = approximateTau(currentTau, funcOfTau, changeInTau);
        BigDecimal nextSigma = calcSigma(eccentricity, nextTau);
        
        if (numOfApproximations != 0) {
            
            numOfApproximations -= 1;
            
            return calcLatitude(eccentricity, nextSigma, nextTau,
                numOfApproximations, originalTau);
            
        }
        
        BigDecimal latitude = (new BigDecimal(Math.atan(currentTau.doubleValue())))
           .multiply(new BigDecimal(180.0 / Math.PI));
        
        return latitude;
        
    }
    
    private static BigDecimal calcLongitude(double zoneCentralMeridian, 
        BigDecimal etaPrime, BigDecimal xiPrime) {
        
        double longitudeRadians = Math.atan(Math.sinh(etaPrime.doubleValue())/
            Math.cos(xiPrime.doubleValue()));
        
        BigDecimal changeInLongitude = new BigDecimal(longitudeRadians*180.0/Math.PI);
        
        BigDecimal longitude = new BigDecimal(zoneCentralMeridian).add(changeInLongitude);
        
        return longitude;
    }

    
}
