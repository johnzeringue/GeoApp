/*
 * DatumEnum.java
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
 */
package org.cirdles.geoapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static java.math.RoundingMode.UNNECESSARY;
import org.nd4j.linalg.util.BigDecimalMath;

/**
 *
 * @author Elaina Cole
 */




public enum DatumEnum {
    
    //datum name, equatorial radius, polar radius, flattening 3D, eccentricity, meridian radius
    
    WGS84("WGS84",  6378137.0, 6356752.314, 0.00167922, 0.081819191, 6367449.146),
    NAD83("NAD83", 6378137.0, 6356752.314, 0.00167922, 0.081819191, 6367449.146),
    GRS80("GRS80", 6378137.0, 6356752.3, 0.00167922, 0.081819191, 6367449.146),
    WGS72("WGS72", 6378135.0, 6356750.5, 0.001679206, 0.081818849, 6367447.239),
    AGD65("AUSTRALIAN 1965", 6378160.0, 6356774.7, 0.001679263, 0.081820217, 6367471.839),
    KRASOVSKY_1940("KRASOVSKY 1940", 6378245.0, 6356863.0, 0.001678981, 0.08181337, 6367558.487),
    NAD27("NAD27", 6378206.4, 6356583.8, 0.001697916, 0.082271854, 6367399.689),
    IN24("IN24", 6378388.0, 6356911.9, 0.001686344, 0.081991978, 6367654.477),
    HAYFORD_1909("HAYFORD 1909", 6378388.0, 6356911.9, 0.001686344, 0.081991978, 6367654.477),
    CLARKE_1880("CLARKE 1880", 6378249.1, 6356514.9, 0.001706683, 0.082483257, 6367386.637),
    CLARKE_1866("CLARKE 1866", 6378206.4, 6356583.8, 0.001697916, 0.082271854, 6367399.689),
    AIRY_1830("AIRY 1830", 6377563.4, 6356256.9, 0.001673221, 0.081673399, 6366914.606),
    BESSEL_1841("BESSEL 1941", 6377397.2, 6356079.0, 0.001674185, 0.081696846, 6366742.561),
    EVEREST_1830("EVEREST 1830", 6377276.3, 6356075.4, 0.00166499, 0.08147292, 6366680.262);
    
    
    private String datum;
    private double equatorialRadius;
    private double polarRadius;
    private double meridianRadius;
    private double flattening3D;
    private double eccentricity;
    
    private static final RoundingMode roundMode = UNNECESSARY;
    
    private DatumEnum(String datum, double equatorialRadius, double polarRadius,
            double flattening3D, double eccentricity, double meridianRadius){
        
        this.datum = datum;
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.meridianRadius = meridianRadius;
        this.flattening3D =  flattening3D;
        this.eccentricity = eccentricity;
        
    }
    
    public String getDatum(){
        return datum;
    }
    
    public double getEquatorialRadius(){
        return equatorialRadius;
    }
    
    public double getPolarRadius(){
        return polarRadius;
    }
    
    public double getMeridianRadius() {
        return meridianRadius;
    }
    
    public double getFlattening3D() {
        return flattening3D;
    }
    
    public double getEccentricity() {
        return eccentricity;
    }
    

    
}
