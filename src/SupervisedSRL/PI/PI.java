package SupervisedSRL.PI;
import SentenceStruct.Sentence;
import SupervisedSRL.Features.FeatureExtractor;
import SupervisedSRL.Strcutures.IndexMap;
import SupervisedSRL.Strcutures.ModelInfo;
import SupervisedSRL.Strcutures.ProjectConstantPrefixes;
import ml.AveragedPerceptron;
import util.IO;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Maryam Aminian on 10/21/16.
 */
public class PI {

    public static void train(ArrayList<String> trainSentencesInCONLLFormat, ArrayList<String> devSentencesInCONLLFormat,
                               IndexMap indexMap, int maxNumberOfTrainingIterations, String PIModelPath,
                             int numOfPIFeatures) throws Exception {

        HashSet<String> labelSet = new HashSet<String>();
        labelSet.add("1");
        labelSet.add("0");
        AveragedPerceptron ap = new AveragedPerceptron(labelSet, numOfPIFeatures);
        double bestAcc = 0;
        int noImprovement = 0;

        for (int iter = 0; iter < maxNumberOfTrainingIterations; iter++) {
            System.out.print("iteration:" + iter + "...\n");

            for (int sIdx = 0; sIdx < trainSentencesInCONLLFormat.size(); sIdx++) {
                Sentence sentence = new Sentence(trainSentencesInCONLLFormat.get(sIdx), indexMap);
                ArrayList<Integer> predicateIndices = sentence.getPredicatesIndices();

                for (int wordIdx = 1; wordIdx < sentence.getLength(); wordIdx++) {
                    Object[] featureVector = FeatureExtractor.extractPIFeatures();
                    String label = (predicateIndices.contains(wordIdx)) ? "1" : "0";
                    ap.learnInstance(featureVector, label);
                }
            }
            //making prediction on dev data
            AveragedPerceptron decodeAp = ap.calculateAvgWeights();
            int correct = 0;
            int total = 0;

            for (int sIdx = 0; sIdx < devSentencesInCONLLFormat.size(); sIdx++) {
                Sentence sentence = new Sentence(devSentencesInCONLLFormat.get(sIdx), indexMap);
                ArrayList<Integer> predicateIndices = sentence.getPredicatesIndices();

                for (int wordIdx = 1; wordIdx < sentence.getLength(); wordIdx++) {
                    total++;
                    Object[] featureVector = FeatureExtractor.extractPIFeatures();
                    String goldLabel = (predicateIndices.contains(wordIdx)) ? "1" : "0";
                    String prediction = decodeAp.predict(featureVector);
                    if (goldLabel.equals(prediction))
                        correct++;
                }
            }
            double acc = (double) correct / total;
            if (acc > bestAcc) {
                noImprovement = 0;
                bestAcc = acc;
                System.out.print("\nSaving final model...");
                ModelInfo.saveModel(ap, PIModelPath);
                System.out.println("Done!");
            } else {
                noImprovement++;
                if (noImprovement > 5) {
                    System.out.print("\nEarly stopping...");
                    break;
                }
            }
        }
    }

    public static void predict (ArrayList<String> evalSentencesInCONLLFormat, IndexMap indexMap,
                                String PIModelPath, String path2SavePredictions) throws Exception {
        HashSet<Integer>[] PIPredictions = new HashSet[evalSentencesInCONLLFormat.size()];
        AveragedPerceptron classifier = IO.load(PIModelPath);

        for (int senIdx =0 ; senIdx < evalSentencesInCONLLFormat.size(); senIdx++){
            HashSet<Integer> prediction4ThisSentence = new HashSet<>();
            Sentence sentence = new Sentence(evalSentencesInCONLLFormat.get(senIdx), indexMap);

            for (int wordIdx =0; wordIdx< sentence.getLength(); wordIdx++){
                Object[] featureVector = FeatureExtractor.extractPIFeatures();
                String prediction = classifier.predict(featureVector);
                if (prediction.equals("1"))
                    prediction4ThisSentence.add(wordIdx);
            }
            PIPredictions[senIdx] = prediction4ThisSentence;
        }
        IO.write(PIPredictions, path2SavePredictions);
    }

}
