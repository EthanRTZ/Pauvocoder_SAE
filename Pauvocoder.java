//==============================================================================
// Chef de projet : Ethan Rietz
// Bînome : Gabin Morel
// Groupe 31
// Pour les traces d'execution, le programme creer un fichier a part tel que :
// <nom_audio.wav>_info_<freqScale>.txt
//==============================================================================

import static java.lang.System.exit;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class Pauvocoder {

    // Processing SEQUENCE size (100 msec with 44100Hz samplerate)
    final static int SEQUENCE = StdAudio.SAMPLE_RATE / 10;

    // Overlapping size (20 msec)
    final static int OVERLAP = SEQUENCE / 5;
    // Best OVERLAP offset seeking window (15 msec)
    final static int SEEK_WINDOW = 3 * OVERLAP / 4;

    private static PrintWriter logFile;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: pauvocoder <input.wav> <freqScale>\n");
            exit(1);
        }

        String wavInFile = args[0];
        double freqScale = Double.valueOf(args[1]);
        String outPutFile = wavInFile.split("\\.")[0] + "_"
                + freqScale + "_";

        // Création du fichier de log
        try {
            logFile = new PrintWriter(new FileWriter(
                    wavInFile.split("\\.")[0] + "_info_"
                            + freqScale + ".txt"
            ));
        } catch (IOException e) {
            System.err.println(
                    "Erreur lors de la création du fichier de log: "
                            + e.getMessage()
            );
            exit(1);
        }

        // Modifier tous les System.out.println pour écrire aussi dans le fichier
        log("Début du traitement du fichier: " + wavInFile);
        log("Facteur de fréquence: " + freqScale);

        // Open input .wev file
        double[] inputWav = StdAudio.read(wavInFile);

        log("Durée du fichier original: " +
                String.format("%.2f", inputWav.length
                        / (double) StdAudio.SAMPLE_RATE) +
                " secondes");

        // Resample test
        log("\nApplication du resampling");
        double[] nvTaill = resample(inputWav, freqScale);

        log("Durée après methode : " +
                String.format("%.2f", nvTaill.length
                        / (double) StdAudio.SAMPLE_RATE) +
                " secondes");
        StdAudio.save(outPutFile + "Resampled.wav", nvTaill);

        // Vocode simple
        log("\nApplication de Vocode simple :");
        double[] outputWav = vocodeSimple(nvTaill, 1.0 / freqScale);
        log("Durée après methode : " +
                String.format("%.2f", outputWav.length
                        / (double) StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "Simple.wav", outputWav);

        // Vocode simple Over
        outputWav = vocodeSimpleOver(nvTaill, 1.0 / freqScale);
        log("Durée après methode : " +
                String.format("%.2f", outputWav.length
                        / (double) StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "SimpleOver.wav", outputWav);

        // Vocode simple Over Cross
        outputWav = vocodeSimpleOverCross(nvTaill, 1.0 / freqScale);
        log("Durée après methode : " +
                String.format("%.2f", outputWav.length
                        / (double) StdAudio.SAMPLE_RATE) +
                " secondes");
        StdAudio.save(outPutFile + "SimpleOverCross.wav", outputWav);

        joue(outputWav);

        // Echo
        outputWav = echo(outputWav, 100, 0.7);
        log("Durée finale avec écho: " +
                String.format("%.2f", outputWav.length
                        / (double) StdAudio.SAMPLE_RATE) + " secondes");
        StdAudio.save(outPutFile + "SimpleOverCrossEcho.wav", outputWav);

        // À la fin du main, fermer le fichier
        logFile.close();
    }

    /**
     * Resample inputWav with freqScale
     *
     * @param inputWav
     * @param freqScale
     * @return resampled wav
     */
    public static double[] resample(double[] inputWav, double freqScale) {
        log("- Taille originale: " + inputWav.length);
        log("- Facteur d'échelle: " + freqScale);

        if (freqScale <= 0) {
            throw new IllegalArgumentException("Le facteur de " +
                    "ré-échantillonnage doit être positif");
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

                output[i] = inputWav[index1] * (1 - frac) + inputWav[index2]
                        * frac;
            }
        }

        log("- Taille après methode : " + tailleOutput);
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
        log("\nDébut de vocode simple :");
        log("- Taille d'entrée: " + inputWav.length);
        log("- Facteur de dilatation: " + dilatation);
        log("- Taille de séquence: " + SEQUENCE);

        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de dilatation " +
                    "doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += SEQUENCE) {
            // Calculer la position correspondante dans le signal d'entrée
            int positionInput = (int) (i * dilatation);

            // Copier la séquence
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput
                    && positionInput + j < inputWav.length; j++) {
                output[i + j] = inputWav[positionInput + j];
            }
        }

        log("- Taille de sortie: " + tailleOutput);
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
        log("\nDébut de vocode simple over:");
        log("- Taille d'entrée: " + inputWav.length);
        log("- Facteur de dilatation: " + dilatation);
        log("- Taille de séquence: " + SEQUENCE);

        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur de " +
                    "dilatation doit être positif");
        }

        // Calculer la taille du signal de sortie
        int tailleOutput = (int) (inputWav.length / dilatation);
        double[] output = new double[tailleOutput];

        // Pour chaque séquence de SEQUENCE échantillons
        for (int i = 0; i < tailleOutput; i += SEQUENCE) {
            // Calculer la position correspondante dans le signal d'entrée
            int positionInput = (int) (i * dilatation);

            // Copier la séquence avec fenêtrage simple
            for (int j = 0; j < SEQUENCE && i + j < tailleOutput
                    && positionInput + j < inputWav.length; j++) {
                // Fenêtre triangulaire simple
                double window = (j < SEQUENCE / 2)
                        ? (j / (double) (SEQUENCE / 2))
                        : (2 - j / (double) (SEQUENCE / 2));

                output[i + j] = inputWav[positionInput + j] * window;
            }
        }

        log("- Taille de sortie: " + tailleOutput);
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
        log("\nDébut de vocode simple over cross :");
        log("- Taille d'entrée: " + inputWav.length);
        log("- Facteur de dilatation: " + dilatation);
        log("- Taille de séquence: " + SEQUENCE);

        if (dilatation <= 0) {
            throw new IllegalArgumentException("Le facteur " +
                    "de dilatation doit être positif");
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
                double window = (j < SEQUENCE / 2)
                        ? (j / (double) (SEQUENCE / 2))
                        : (2 - j / (double) (SEQUENCE / 2));

                output[i + j] = inputWav[positionInput + j] * window;
            }
        }

        log("- Taille de sortie: " + tailleOutput);
        return output;
    }

    /**
     * Play the wav
     *
     * @param wav
     */
    public static void joue(double[] wav) {
        // Créer un thread pour la lecture audio
        Thread audioThread = new Thread(() -> {
            StdAudio.play(wav);
        });
        audioThread.start();

        // Afficher la forme d'onde pendant la lecture
        displayWaveform(wav);
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
        log("\nApplication de l'écho:");
        log("- Délai: " + delay + " ms");
        log("- Gain: " + gain);
        log("- Nombre d'échantillons de délai: " +
                (int) (delay * StdAudio.SAMPLE_RATE / 1000.0));

        if (gain < 0 || gain > 1) {
            throw new IllegalArgumentException("Le gain doit être " +
                    "compris entre 0 et 1");
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
     *
     * @param wav
     */
    public static void displayWaveform(double[] wav) {
        // Configuration de la fenêtre d'affichage
        int tailleFenetre = 44100; // 1 seconde d'échantillons à 44.1kHz
        int largeurFenetre = 1000;
        int hauteurFenetre = 400;

        StdDraw.setCanvasSize(largeurFenetre, hauteurFenetre);
        StdDraw.setXscale(0, largeurFenetre);
        StdDraw.setYscale(-1.2, 1.2);
        StdDraw.enableDoubleBuffering();

        // Paramètres d'affichage
        int echantillonsParPixel = tailleFenetre / largeurFenetre;
        // Réduit encore la fréquence de mise à jour
        int pasDefilement = tailleFenetre / 25;

        // Pré-calcul des formes d'onde pour réduire les calculs
        // pendant l'affichage
        int nombreFrames = wav.length / pasDefilement + 1;
        // [frame][pixel][min/max]
        double[][][] frames = new double[nombreFrames][largeurFenetre][2];

        // Pré-calcul de toutes les frames
        for (int frame = 0; frame < nombreFrames; frame++) {
            int basePosition = frame * pasDefilement;
            for (int i = 0; i < largeurFenetre; i++) {
                int debutEchantillon = basePosition
                        + (i * echantillonsParPixel);
                if (debutEchantillon + echantillonsParPixel < wav.length) {
                    double min = wav[debutEchantillon];
                    double max = min;

                    for (int j = 0; j < echantillonsParPixel
                            && debutEchantillon + j < wav.length; j++) {
                        double val = wav[debutEchantillon + j];
                        min = Math.min(min, val);
                        max = Math.max(max, val);
                    }

                    frames[frame][i][0] = min;
                    frames[frame][i][1] = max;
                }
            }
        }

        // Boucle d'affichage principale
        int frameActuelle = 0;
        while (frameActuelle < nombreFrames) {
            StdDraw.clear(StdDraw.WHITE);
            StdDraw.setPenColor(0, 0, 200);

            // Dessiner la frame actuelle
            for (int i = 0; i < largeurFenetre; i++) {
                double min = frames[frameActuelle][i][0];
                double max = frames[frameActuelle][i][1];
                if (max != 0 || min != 0) {
                    StdDraw.line(i, min, i, max);
                }
            }

            // Afficher le temps courant
            StdDraw.setPenColor(StdDraw.BLACK);
            double tempsCourant = (frameActuelle * pasDefilement)
                    / (double) StdAudio.SAMPLE_RATE;
            StdDraw.text(largeurFenetre / 2, -1.1,
                    String.format("Temps: %.2f s", tempsCourant));

            StdDraw.show();

            // Contrôle de la vitesse d'affichage
            try {
                Thread.sleep(40); // ~25 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            frameActuelle++;
        }
    }

    private static void log(String message) {
        System.out.println(message);
        if (logFile != null) {
            logFile.println(message);
            logFile.flush(); // Pour s'assurer que les données sont écrites immédiatement
        }
    }

}




