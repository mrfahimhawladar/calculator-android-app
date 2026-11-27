package com.mathengine.finance;import java.time.LocalDate;import java.time.temporal.ChronoUnit;
public final class QuickTools{private QuickTools(){}
public static double bmi(double w,double h){if(!Double.isFinite(w)||w<=0||!Double.isFinite(h)||h<=0)throw new IllegalArgumentException("Weight and height must be positive");double m=h/100d;return w/(m*m);}
public static double discountPrice(double p,double d){if(!Double.isFinite(p)||p<0||!Double.isFinite(d)||d<0||d>100)throw new IllegalArgumentException("Enter a price and discount from 0 to 100%");return p*(1-d/100d);}
public static double gstAmount(double a,double r){if(!Double.isFinite(a)||a<0||!Double.isFinite(r)||r<0||r>100)throw new IllegalArgumentException("Enter a valid amount and tax rate");return a*r/100d;}
public static String convertRadix(String s,int f,int t){if(f<2||f>36||t<2||t>36)throw new IllegalArgumentException("Base must be 2–36");try{return Long.toString(Long.parseLong(s.trim(),f),t).toUpperCase(java.util.Locale.ROOT);}catch(NumberFormatException e){throw new IllegalArgumentException("Number is invalid for base "+f);}}
public static long daysBetween(String a,String b){try{return ChronoUnit.DAYS.between(LocalDate.parse(a),LocalDate.parse(b));}catch(RuntimeException e){throw new IllegalArgumentException("Dates must use YYYY-MM-DD");}}
}
