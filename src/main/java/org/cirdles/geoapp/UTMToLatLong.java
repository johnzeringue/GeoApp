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
    
    public static void convert(UTM utm, String datum) {
        
        Datum datumInformation = Datum.valueOf(datum);
        
        BigDecimal flattening3D = new BigDecimal(datumInformation.getFlattening3D());
        
        BigDecimal[] betaSeries = {
            
            KrugerSeries.beta1(flattening3D).setScale(PRECISION),
            KrugerSeries.beta2(flattening3D).setScale(PRECISION),
            KrugerSeries.beta3(flattening3D).setScale(PRECISION),
            KrugerSeries.beta4(flattening3D).setScale(PRECISION),
            KrugerSeries.beta5(flattening3D).setScale(PRECISION),
            KrugerSeries.beta6(flattening3D).setScale(PRECISION),
            KrugerSeries.beta7(flattening3D).setScale(PRECISION)
            
        };
        
        char hemisphere = utm.getHemisphere();
        
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
        
    }
    
    private static BigDecimal calcXiNorth(char hemisphere, BigDecimal 
            meridianRadius, BigDecimal northing) {
        
        BigDecimal xiNorth;
        
        
        if(hemisphere == 'N') {
            
            xiNorth = northing.divide(SCALE_FACTOR.multiply(meridianRadius)).
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
    
}
