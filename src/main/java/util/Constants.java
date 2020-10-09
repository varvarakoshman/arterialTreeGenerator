package util;

public class Constants {

    public static final int N_TERM = 250; // # of terminal segments
    public static final int N_TOTAL = 2 * N_TERM - 1; // # of total segments
    public static final int N_TOSS = 10;
    public static final double R_PERF = 0.5; // radius of a perfusion area in dm
    public static final double A_PERF = Math.PI * Math.pow(R_PERF, 2); // area of a perfusion area in meters
    public static final double R_SUPP = Math.sqrt(A_PERF / (N_TERM * Math.PI)); // radius of a small supportive circle (initial perfusion area)

    // REAL WORLD PARAMETERS
    public static final double Q_PERF = 8.33 * 10E-6;
    public static final double Q_TERM = Q_PERF / N_TERM;
    public static final double P_PERF = 1.33 * 10E4;
    public static final double P_TERM = 8.38 * 10E3;
    public static final double DELTA_P_ = P_PERF - P_TERM;
    public static final double VISCOSITY = 3.6 * 10E-3;
    public static final double GAMMA = 3; // bifurcation exponent

    //  TARGET FUNCTION PARAMETERS
    public static final int MU = 1;
    public static final int LAMBDA = 2;
}
