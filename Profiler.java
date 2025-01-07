import java.util.function.Function;

public class Profiler {
  
    // Analyse la fonction donnée avec un paramètre et retourne le résultat
    public static double analyse(Function<Double, Double> oneMethod, double p){
        double res = oneMethod.apply(p);
        return res;
    }

    /**
     * Si clock0 est >0, retourne une chaîne de caractères
     * représentant la différence de temps depuis clock0.
     * @param clock0 instant initial
     * @return expression du temps écoulé depuis clock0
     */
    public static String timestamp(long clock0) {
        String result = null;

        if (clock0 > 0) {
            double elapsed = (System.nanoTime() - clock0) / 1e9;
            String unit = "s";
            if (elapsed < 1.0) {
                elapsed *= 1000.0;
                unit = "ms";
            }
            result = String.format("%.4g%s elapsed", elapsed, unit);
        }
        return result;
    }

    /**
     * retourne l'heure courante en ns.
     * @return temps actuel en nanosecondes
     */
    public static long timestamp() {
        return System.nanoTime();
    }

    // Exemple d'utilisation
    public static void main(String[] args) {
        // Exemple de fonction 
        Function<Double, Double> square = x -> x * x;

        // Capture de l'instant initial
        long start = timestamp();

        // Analyse d'une fonction
        double result = analyse(square, 5.0);

        // Affichage du résultat et du temps écoulé
        System.out.println("Result: " + result);
        System.out.println(timestamp(start));
    }
}
