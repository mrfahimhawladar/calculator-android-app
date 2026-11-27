package com.mathengine.finance;
public final class MortgageCalculator{public static final class Result{public final double monthlyPayment,totalPayment,totalInterest;private Result(double m,double t,double i){monthlyPayment=m;totalPayment=t;totalInterest=i;}}
public Result calculate(double p,double rate,int months){if(!Double.isFinite(p)||p<=0)throw new IllegalArgumentException("Principal must be positive");if(!Double.isFinite(rate)||rate<0)throw new IllegalArgumentException("Rate cannot be negative");if(months<=0||months>1200)throw new IllegalArgumentException("Term must be 1–1200 months");double r=rate/1200d,pay;if(r==0)pay=p/months;else{double f=Math.pow(1+r,months);pay=p*r*f/(f-1);}double total=pay*months;return new Result(pay,total,total-p);}
}
