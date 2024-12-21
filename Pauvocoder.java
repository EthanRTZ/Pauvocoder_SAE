import static java.lang.System.exit;

public class Pauvocoder {

    // Processing SEQUENCE size (100 msec with 44100Hz samplerate)
    final static int SEQUENCE = StdAudio.SAMPLE_RATE / 10;

    // Overlapping size (20 msec)
    final static int OVERLAP = SEQUENCE / 5;
    // Best OVERLAP offset seeking window (15 msec)
    final static int SEEK_WINDOW = 3 * OVERLAP / 4;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: pauvocoder <input.wav> <freqScale>\n");
            exit(1);
        }


        String wavInFile = args[0];
        double freqScale = Double.valueOf(args[1]);
        String outPutFile = wavInFile.split("\\.")[0] + "_" + freqScale + "_";

        // Open input .wev file
        double[] inputWav = StdAudio.read(wavInFile);

        // Resample test
        double[] newPitchWav = resample(inputWav, freqScale);
        StdAudio.save(outPutFile + "Resampled.wav", newPitchWav);

        // Simple dilatation
        double[] outputWav = vocodeSimple(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "Simple.wav", outputWav);

        // Simple dilatation with overlaping
        outputWav = vocodeSimpleOver(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "SimpleOver.wav", outputWav);

        // Simple dilatation with overlaping and maximum cross correlation search
        outputWav = vocodeSimpleOverCross(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "SimpleOverCross.wav", outputWav);

        joue(outputWav);

        // Some echo above all
        outputWav = echo(outputWav, 100, 0.7);
        StdAudio.save(outPutFile + "SimpleOverCrossEcho.wav", outputWav);



    }

    /**
     * Resample inputWav with freqScale
     *
     * @param inputWav
     * @param freqScale
     * @return resampled wav
     */
    public static double[] resample(double[] inputWav, double freqScale) {
        if (freqScale <= 0) {
            throw new IllegalArgumentException("Le facteur de ré-échantillonnage doit être positif");
        }

        // Si freqScale est égal à 1, aucun ré-échantillonnage nécessaire
        if (freqScale == 1.0) {
            return inputWav.clone();
        }

        // Calcul de la taille du tableau de sortie
        int tailleOutput;
        if (freqScale > 1) {
            // Sous-échantillonnage : on garde 1/freqScale échantillons
            tailleOutput = (int) (inputWav.length / freqScale);
        } else {
            // Sur-échantillonnage : on ajoute des échantillons
            tailleOutput = (int) (inputWav.length / freqScale);
        }

        double[] output = new double[tailleOutput];

        if (freqScale > 1) {
            // Sous-échantillonnage : on prend un échantillon tous les freqScale
            for (int i = 0; i < tailleOutput; i++) {
                int inputIndex = (int) (i * freqScale);
                output[i] = inputWav[inputIndex];
            }
        } else {
            // Sur-échantillonnage : on interpole entre les échantillons
            for (int i = 0; i < tailleOutput; i++) {
                double inputIndex = i * freqScale;
                int index1 = (int) Math.floor(inputIndex);
                int index2 = Math.min(index1 + 1, inputWav.length - 1);
                double frac = inputIndex - index1;

                output[i] = inputWav[index1] * (1 - frac) + inputWav[index2] * frac;
            }
        }

        return output;
    }


    /**
     * Simple dilatation, without any overlapping
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimple(double[] inputWav, double dilatation) {
        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de dilatation doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += SEQUENCE) {
            // Calculer la position correspondante dans le signal d'entrée
            int positionInput = (int) (i * dilatation);

            // Copier la séquence
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput && positionInput + j < inputWav.length; j++) {
                output[i + j] = inputWav[positionInput + j];
            }
        }

        return output;
    }

    /**
     * Simple dilatation, with overlapping
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOver(double[] inputWav, double dilatation) {
        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de dilatation doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += (SEQUENCE - OVERLAP)) {
            // Calculer la position correspondante dans le signal d'entrée
            int positionInput = (int) (i * dilatation);

            // Copier la séquence avec fenêtrage
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput && positionInput + j < inputWav.length; j++) {
                // Calculer le coefficient de fenêtrage (fenêtre de Hanning)
                double window = 0.5 * (1 - Math.cos(2 * Math.PI * j / (SEQUENCE - 1)));

                // Appliquer le fenêtrage et ajouter au signal de sortie
                if (j < OVERLAP) {
                    // Zone de chevauchement : faire un fondu enchaîné
                    double fadeOut = (double) (OVERLAP - j) / OVERLAP;
                    double fadeIn = (double) j / OVERLAP;
                    output[i + j] = output[i + j] * fadeOut + inputWav[positionInput + j] * window * fadeIn;
                } else {
                    output[i + j] = inputWav[positionInput + j] * window;
                }
            }
        }

        return output;
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] inputWav, double dilatation) {
        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de dilatation doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += (SEQUENCE - OVERLAP)) {
            // Calculer la position correspondante dans le signal d'entrée
            int positionInput = (int) (i * dilatation);

            // Rechercher le meilleur offset dans la fenêtre de recherche
            int bestOffset = 0;
            double bestCorrelation = Double.NEGATIVE_INFINITY;

            for (int offset = -SEEK_WINDOW / 2; offset < SEEK_WINDOW / 2; offset++) {
                if (positionInput + offset >= 0 && positionInput + offset + SEQUENCE < inputWav.length) {
                    double correlation = 0;

                    // Calculer la corrélation pour cet offset
                    for (int k = 0; k < OVERLAP; k++) {
                        correlation += inputWav[positionInput + offset + k] * inputWav[positionInput + offset + SEQUENCE - OVERLAP + k];
                    }

                    if (correlation > bestCorrelation) {
                        bestCorrelation = correlation;
                        bestOffset = offset;
                    }
                }
            }

            // Utiliser le meilleur offset trouvé
            positionInput += bestOffset;

            // Copier la séquence avec fenêtrage
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput && positionInput + j < inputWav.length; j++) {
                // Calculer le coefficient de fenêtrage (fenêtre de Hanning)
                double window = 0.5 * (1 - Math.cos(2 * Math.PI * j / (SEQUENCE - 1)));

                // Appliquer le fenêtrage et ajouter au signal de sortie
                if (j < OVERLAP) {
                    // Zone de chevauchement : faire un fondu enchaîné
                    double fadeOut = (double) (OVERLAP - j) / OVERLAP;
                    double fadeIn = (double) j / OVERLAP;
                    output[i + j] = output[i + j] * fadeOut + inputWav[positionInput + j] * window * fadeIn;
                } else {
                    output[i + j] = inputWav[positionInput + j] * window;
                }
            }
        }

        return output;
    }

    /**
     * Play the wav
     *
     * @param wav
     */
    public static void joue(double[] wav) {
        // Jouer le son
        StdAudio.play(wav);
    }

    /**
     * Add an echo to the wav
     *
     * @param wav
     * @param delay in msec
     * @param gain
     * @return wav with echo
     */
    public static double[] echo(double[] wav, double delay, double gain) {
        if (gain < 0 || gain > 1) {
            throw new IllegalArgumentException("Le gain doit être compris entre 0 et 1");
        }

        // Convertir le délai de millisecondes en nombre d'échantillons
        int delaySamples = (int) (delay * StdAudio.SAMPLE_RATE / 1000.0);

        // Créer un nouveau tableau pour stocker le signal avec écho
        double[] result = new double[wav.length];

        // Copier le signal original
        for (int i = 0; i < wav.length; i++) {
            result[i] = wav[i];

            // Ajouter l'écho si possible
            if (i >= delaySamples) {
                result[i] += gain * wav[i - delaySamples];

                // S'assurer que l'amplitude reste dans [-1, 1]
                if (result[i] > 1.0) result[i] = 1.0;
                if (result[i] < -1.0) result[i] = -1.0;
            }
        }

        return result;
    }

}


