package com.mathengine.units;import java.util.Locale;
public final class UnitConverter {
public enum Category{LENGTH,MASS,TIME,AREA,VOLUME,DATA,SPEED,TEMPERATURE}
private static final String[][] NAMES={{"m","km","cm","mm","mi","ft","in"},{"kg","g","mg","lb","oz"},{"s","min","h","day"},{"m²","km²","cm²","ft²","acre"},{"m³","L","mL","ft³","gal"},{"B","KB","MB","GB","TB"},{"m/s","km/h","mph","ft/s"},{"°C","°F","K","°R"}};
private static final double[][] FACTORS={{1,1000,.01,.001,1609.344,.3048,.0254},{1,.001,1e-6,.45359237,.028349523125},{1,60,3600,86400},{1,1e6,1e-4,.09290304,4046.8564224},{1,.001,1e-6,.028316846592,.003785411784},{1,1e3,1e6,1e9,1e12},{1,1/3.6,.44704,.3048}};
public String[] units(Category c){return NAMES[c.ordinal()].clone();}
public double convert(double value,Category c,int from,int to){if(from<0||to<0||from>=NAMES[c.ordinal()].length||to>=NAMES[c.ordinal()].length)throw new IllegalArgumentException("Unknown unit");if(c==Category.TEMPERATURE)return temperature(value,from,to);double[] f=FACTORS[c.ordinal()];return value*f[from]/f[to];}
private double temperature(double v,int from,int to){double k;switch(from){case 0:k=v+273.15;break;case 1:k=(v-32)*5/9+273.15;break;case 2:k=v;break;case 3:k=v*5/9;break;default:throw new IllegalArgumentException("Unknown temperature unit");}switch(to){case 0:return k-273.15;case 1:return(k-273.15)*9/5+32;case 2:return k;case 3:return k*9/5;default:throw new IllegalArgumentException("Unknown temperature unit");}}
public String format(double v){return String.format(Locale.US,"%.8g",v);}
}
