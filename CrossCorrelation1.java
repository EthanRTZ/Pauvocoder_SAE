import java.util.Arrays;

public class CrossCorrelation1 {

    // Méthode pour calculer la moyenne d'un signal avec boucle for-each
    public static double calculateMean(double[] signal) {
        double sum = 0;
        int n = signal.length;
        for (int i = 0; i < n; i++) {
            sum += signal[i];
        }
        return sum / n;
    }

    // Méthode pour calculer la corrélation croisée
    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        // Calcul de la moyenne des signaux
        double mean1 = calculateMean(sig1);
        double mean2 = calculateMean(sig2);

        // Décalage (lag)
        for (int lag = 0; lag < n; lag++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + lag) % n; // Index circulaire pour gérer le décalage
                sum += (sig1[i] - mean1) * (sig2[j] - mean2);
            }
            result[lag] = sum / n; // Normalisation par la taille du signal
        }

        return result;
    }

    public static void main(String[] args) {
        // Deux signaux d'entrée
        double[] sig1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        double[] sig2 = {20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

        // Capture le temps initial
        long start = Profiler.timestamp();

        // Calcul de la corrélation croisée
        double[] result = crosscorrelation(sig1, sig2);

        // Capture et affiche le temps écoulé
        long end = System.nanoTime();
        System.out.println(Profiler.timestamp(start));

        // Affichage du résultat
        System.out.println(Arrays.toString(result));
    }
}
