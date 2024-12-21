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

        System.out.println("Début du traitement du fichier: " + wavInFile);
        System.out.println("Facteur de fréquence: " + freqScale);
        System.out.println("Taille du fichier d'entrée: " + inputWav.length + " échantillons");
        System.out.println("Durée du fichier original: " + String.format("%.2f", inputWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");

        // Resample test
        System.out.println("\nApplication du resampling...");
        double[] newPitchWav = resample(inputWav, freqScale);
        System.out.println("Nouvelle taille après resampling: " + newPitchWav.length + " échantillons");
        System.out.println("Durée après resampling: " + String.format("%.2f", newPitchWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "Resampled.wav", newPitchWav);

        // Simple dilatation
        System.out.println("\nApplication de la dilatation simple:");
        double[] outputWav = vocodeSimple(newPitchWav, 1.0 / freqScale);
        System.out.println("Durée après dilatation simple: " + String.format("%.2f", outputWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "Simple.wav", outputWav);

        // Simple dilatation with overlaping
        outputWav = vocodeSimpleOver(newPitchWav, 1.0 / freqScale);
        System.out.println("Durée après dilatation avec fenêtrage: " + String.format("%.2f", outputWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "SimpleOver.wav", outputWav);

        // Simple dilatation with overlaping and maximum cross correlation search
        outputWav = vocodeSimpleOverCross(newPitchWav, 1.0 / freqScale);
        System.out.println("Durée après dilatation avec fenêtrage et cross-correlation: " + String.format("%.2f", outputWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "SimpleOverCross.wav", outputWav);

        joue(outputWav);

        // Some echo above all
        outputWav = echo(outputWav, 100, 0.7);
        System.out.println("Durée finale avec écho: " + String.format("%.2f", outputWav.length / (double)StdAudio.SAMPLE_RATE) + " secondes");
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
        System.out.println("Début du resampling:");
        System.out.println("- Taille originale: " + inputWav.length);
        System.out.println("- Facteur d'échelle: " + freqScale);

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

        System.out.println("- Taille après resampling: " + tailleOutput);
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
        System.out.println("\nDébut de la dilatation simple:");
        System.out.println("- Taille d'entrée: " + inputWav.length);
        System.out.println("- Facteur de dilatation: " + dilatation);
        System.out.println("- Taille de séquence: " + SEQUENCE);

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

        System.out.println("- Taille de sortie: " + tailleOutput);
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
        System.out.println("\nDébut de la dilatation avec fenêtrage:");
        System.out.println("- Taille d'entrée: " + inputWav.length);
        System.out.println("- Facteur de dilatation: " + dilatation);
        System.out.println("- Taille de séquence: " + SEQUENCE);

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

            // Copier la séquence avec fenêtrage simple
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput && positionInput + j < inputWav.length; j++) {
                // Fenêtre triangulaire simple
                double fenetre = (j < SEQUENCE/2) ? (j / (double)(SEQUENCE/2))
                                               : (2 - j / (double)(SEQUENCE/2));
                
                output[i + j] = inputWav[positionInput + j] * fenetre;
            }
        }

        System.out.println("- Taille de sortie: " + tailleOutput);
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
        System.out.println("\nDébut de la dilatation avec fenêtrage et cross-correlation:");
        System.out.println("- Taille d'entrée: " + inputWav.length);
        System.out.println("- Facteur de dilatation: " + dilatation);
        System.out.println("- Taille de séquence: " + SEQUENCE);

        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de dilatation doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += SEQUENCE) {
            // Position dans le signal d'entrée
            int positionInput = (int) (i * dilatation);
            
            // Vérifier qu'on ne dépasse pas les limites
            if (positionInput + SEQUENCE >= inputWav.length) {
                break;
            }

            // Copie directe de la séquence avec fenêtrage simple
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput; j++) {
                // Fenêtre triangulaire simple
                double fenetre = (j < SEQUENCE/2) ? (j / (double)(SEQUENCE/2))
                                               : (2 - j / (double)(SEQUENCE/2));
                
                output[i + j] = inputWav[positionInput + j] * fenetre;
            }
        }

        System.out.println("- Taille de sortie: " + tailleOutput);
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
        System.out.println("\nApplication de l'écho:");
        System.out.println("- Délai: " + delay + " ms");
        System.out.println("- Gain: " + gain);
        System.out.println("- Nombre d'échantillons de délai: " + (int)(delay * StdAudio.SAMPLE_RATE / 1000.0));

        if (gain < 0 || gain > 1) {
            throw new IllegalArgumentException("Le gain doit être compris entre 0 et 1");
        }

        // Convertir le délai de millisecondes en nombre d'échantillons
        int delaySamples = (int) (delay * StdAudio.SAMPLE_RATE / 1000.0);

        // Créer un nouveau tableau pour stocker le signal avec écho
        double[] resultat = new double[wav.length];

        // Copier le signal original
        for (int i = 0; i < wav.length; i++) {
            resultat[i] = wav[i];

            // Ajouter l'écho si possible
            if (i >= delaySamples) {
                resultat[i] += gain * wav[i - delaySamples];

                // S'assurer que l'amplitude reste dans [-1, 1]
                if (resultat[i] > 1.0) resultat[i] = 1.0;
                if (resultat[i] < -1.0) resultat[i] = -1.0;
            }
        }

        return resultat;
    }

    /**
     * Display the waveform
     * @param wav
     */
    public static void displayWaveform(double[] wav) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


}




