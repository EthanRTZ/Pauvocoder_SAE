import java.util.Arrays;

public class CrossCorrelation2{

    // Méthode récursive pour calculer la moyenne d'un signal
    public static double calculateMeanRecursive(double[] signal, int index, double sum) {
        if (index == signal.length) {
            return sum / signal.length;
        }
        return calculateMeanRecursive(signal, index + 1, sum + signal[index]);
    }

    // Méthode pour calculer la corrélation croisée
    public static double[] crosscorrelation(double[] sig1, double[] sig2) {
        int n = sig1.length;
        double[] result = new double[n];

        // Calcul de la moyenne des signaux avec la méthode récursive
        double mean1 = calculateMeanRecursive(sig1, 0, 0);
        double mean2 = calculateMeanRecursive(sig2, 0, 0);

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
        System.out.println(Profiler.timestamp());

        // Affichage du résultat
        System.out.println(Arrays.toString(result));
    }
}
