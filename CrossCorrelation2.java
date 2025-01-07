public class CrossCorrelation2 {

    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        // Perform FFT on both signals
        double[] fftSig1 = FFT.fft(sig1);
        double[] fftSig2 = FFT.fft(sig2);

        // Compute conjugate of fftSig2 and element-wise multiplication
        double[] conjugateProduct = new double[n];
        for (int i = 0; i < n; i++) {
            conjugateProduct[i] = fftSig1[i] * Math.conj(fftSig2[i]);
        }

        // Perform inverse FFT
        double[] ifftResult = FFT.ifft(conjugateProduct);

        // Normalize the result
        for (int i = 0; i < n; i++) {
            result[i] = ifftResult[i] / n;
        }

        return result;
    }

    public static void main(String[] args) {
        double[] sig1 = {1, 2, 3, 4};
        double[] sig2 = {4, 3, 2, 1};
        double[] result = crosscorrelation(sig1, sig2);
        System.out.println(Arrays.toString(result));
    }
}

// FFT utility class (to be implemented or imported from library)
class FFT {
    public static double[] fft(double[] input) {
        // FFT implementation goes here
        return input; // Placeholder
    }

    public static double[] ifft(double[] input) {
        // Inverse FFT implementation goes here
        return input; // Placeholder
    }
}