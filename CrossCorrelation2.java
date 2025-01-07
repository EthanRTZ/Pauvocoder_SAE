import java.util.Arrays;

public class CrossCorrelation2 {

    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        // Start recursive calculation
        calculateRecursive(sig1, sig2, result, 0, n);
        
        return result;
    }

    private static void calculateRecursive(double[] sig1, double[] sig2, double[] result, int index, int n) {
        // Base case: if index reaches the length, stop recursion
        if (index >= n) {
            return;
        }

        // Recursive calculation
        int j = n - 1 - index; // Reverse index for the second array
        result[index] = sig1[index] * sig2[j];

        // Recursive call for the next index
        calculateRecursive(sig1, sig2, result, index + 1, n);
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
