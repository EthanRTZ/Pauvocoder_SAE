import java.util.Arrays;

public class CrossCorrelation1 {

    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        // Iterate through each index of the first array
        for (int i = 0; i < n; i++) {
            // Multiply each corresponding element without summation
            int j = n - 1 - i; // Reverse index for the second array
            result[i] = sig1[i] * sig2[j];
        }

        return result;
    }

    public static void main(String[] args) {
        double[] sig1 = {1, 2, 3, 4};
        double[] sig2 = {4, 3, 2, 1};

        // Capture le temps initial
        long start = Profiler.timestamp();

        double[] result = crosscorrelation(sig1, sig2);

        // Capture et affiche le temps écoulé
        System.out.println(Profiler.timestamp(start));

        System.out.println(Arrays.toString(result));
    }
}
