package com.oddrat.android.anemoi;


public class KalmanSimple {

	// 1D low pass kalman filter //

	private double Q = 1;
	private double R = 1;
	private double P = 1;
	private double X = 0, K = 0;
	
	public KalmanSimple() {
		// TODO Auto-generated constructor stub
	}
	
	public KalmanSimple(double q, double r, double p) {
		this.Q = q;
		this.R = r;
		this.P = p;
	}
	
	private void measurementUpdate() {
		K = (P + Q) / (P + Q + R);
		P = R * K;
	}

	public double update(double measurement) {
		measurementUpdate();
		double result = X + (measurement - X) * K;
		X = result;

		return result;
	}
}
