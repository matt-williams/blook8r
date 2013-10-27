package com.github.matt.williams.blook8r;

public class CoordinateMapper {
    // Global reference points.
    public static final double GP1X = -0.01975621696528207;
    public static final double GP1Y = 51.50525850486366;
    public static final double GP2X = -0.01902470873661133;
    public static final double GP2Y = 51.5051846939183;
    public static final double GP3X = -0.01987457772519519;
    public static final double GP3Y = 51.50479160522183;
    public static final double GP4X = -0.0191509753757757;
    public static final double GP4Y = 51.50471551467772;

    // Local reference points.
    public static final double LP1X = -8.5904;
    public static final double LP1Y = 8.4190;
    public static final double LP2X = 8.6079;
    public static final double LP2Y = 8.8072;
    public static final double LP3X = -8.2918;
    public static final double LP3Y = -8.9987;
    public static final double LP4X = 8.6676;
    public static final double LP4Y = -8.6897;

    public static final double GP12 = Math.sqrt(Math.pow(GP1X - GP2X, 2) + Math.pow(GP1Y - GP2Y, 2));
    public static final double GP34 = Math.sqrt(Math.pow(GP3X - GP4X, 2) + Math.pow(GP3Y - GP4Y, 2));
    public static final double GP13 = Math.sqrt(Math.pow(GP1X - GP3X, 2) + Math.pow(GP1Y - GP3Y, 2));
    public static final double GP24 = Math.sqrt(Math.pow(GP2X - GP4X, 2) + Math.pow(GP2Y - GP4Y, 2));

    public static final double LP12 = Math.sqrt(Math.pow(LP1X - LP2X, 2) + Math.pow(LP1Y - LP2Y, 2));
    public static final double LP34 = Math.sqrt(Math.pow(LP3X - LP4X, 2) + Math.pow(LP3Y - LP4Y, 2));
    public static final double LP13 = Math.sqrt(Math.pow(LP1X - LP3X, 2) + Math.pow(LP1Y - LP3Y, 2));
    public static final double LP24 = Math.sqrt(Math.pow(LP2X - LP4X, 2) + Math.pow(LP2Y - LP4Y, 2));

    public static double[] globalToLocal(double gx, double gy) {
        double u1 = ((gx - GP1X) * (GP2X - GP1X) + (gy - GP1Y) * (GP2Y - GP1Y)) / Math.pow(GP12, 2);
        double u2 = ((gx - GP3X) * (GP4X - GP3X) + (gy - GP3Y) * (GP4Y - GP3Y)) / Math.pow(GP34, 2);
        double v1 = ((gx - GP1X) * (GP3X - GP1X) + (gy - GP1Y) * (GP3Y - GP1Y)) / Math.pow(GP13, 2);
        double v2 = ((gx - GP2X) * (GP4X - GP2X) + (gy - GP2Y) * (GP4Y - GP2Y)) / Math.pow(GP24, 2);
        double u = u1 * (1 - (v1 + v2) / 2) + u2 * ((v1 + v2) / 2);
        double v = v1 * (1 - (u1 + u2) / 2) + v2 * ((u1 + u2) / 2);
        android.util.Log.e("CoordinateMapper", "(u, v) = (" + u + ", " + v + ")");
        double lx = LP1X + u * (LP2X - LP1X) + v * (LP3X - LP1X);
        double ly = LP1Y + u * (LP2Y - LP1Y) + v * (LP3Y - LP1Y);
        return new double[] {lx, ly};
    }


}
