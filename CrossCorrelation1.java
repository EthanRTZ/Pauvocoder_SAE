public class CrossCorrelation1 {

    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        for (int lag = 0; lag < n; lag++) {
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                int j = (i + lag) % n; // Circular shift
                sum += sig1[i] * sig2[j];
            }
            result[lag] = sum;
        }

        return result;
    }

    public static void main(String[] args) {
        double[] sig1 = {1, 2, 3, 4};
        double[] sig2 = {4, 3, 2, 1};
        double[] result = crosscorrelation(sig1, sig2);
        for (double value : result) {
            System.out.print(value + " ");
        }
    }
}