package SupervisedSRL;

import SupervisedSRL.Strcutures.IndexMap;
import SupervisedSRL.Strcutures.Properties;
import SupervisedSRL.Strcutures.RerankerFeatureMap;
import ml.AveragedPerceptron;
import ml.RerankerAveragedPerceptron;
import util.IO;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monadiab on 9/12/16.
 */
public class Step8 {

    public static void decode(Properties properties)
            throws Exception {
        if (!properties.getSteps().contains(8))
            return;
        System.out.println("\n>>>>>>>>>>>>>\nStep 8 -- Decoding\n>>>>>>>>>>>>>\n");
        AveragedPerceptron aiClassifier = AveragedPerceptron.loadModel(properties.getAiModelPath());
        AveragedPerceptron acClassifier = AveragedPerceptron.loadModel(properties.getAcModelPath());
        IndexMap indexMap = IO.load(properties.getIndexMapFilePath());
        String pdModelDir = properties.getPdModelDir();
        ArrayList<String> devSentences = IO.readCoNLLFile(properties.getDevFile());
        int numOfPDFeatures = properties.getNumOfPDFeatures();
        int numOfAIFeatures = properties.getNumOfAIFeatures();
        int numOfACFeatures = properties.getNumOfACFeatures();
        int numOfGlobalFeatures= properties.getNumOfGlobalFeatures();
        int aiMaxBeamSize = properties.getNumOfAIBeamSize();
        int acMaxBeamSize = properties.getNumOfACBeamSize();
        String outputFile = properties.getOutputFilePath();
        double aiCoefficient = properties.getAiCoefficient();
        String BJOutput = properties.getBJOutput();
        if (properties.useReranker()) {
            HashMap<Object, Integer>[] rerankerFeatureMap = IO.load(properties.getRerankerFeatureMapPath());
            RerankerAveragedPerceptron reranker = RerankerAveragedPerceptron.loadModel(properties.getRerankerModelPath());
            SupervisedSRL.Reranker.Decoder decoder = new SupervisedSRL.Reranker.Decoder(aiClassifier, acClassifier, reranker, indexMap, rerankerFeatureMap, pdModelDir);
            decoder.decode(devSentences, numOfPDFeatures, numOfAIFeatures, numOfACFeatures, numOfGlobalFeatures, aiMaxBeamSize, acMaxBeamSize, pdModelDir, outputFile, aiCoefficient);
        } else {
            SupervisedSRL.Decoder decoder = new SupervisedSRL.Decoder(aiClassifier, acClassifier);
            decoder.decodeUsingDisambiguatedPredicates(indexMap, devSentences, aiMaxBeamSize,
                    acMaxBeamSize, numOfAIFeatures, numOfACFeatures, numOfPDFeatures, pdModelDir,
                    outputFile, aiCoefficient, BJOutput);
        }
    }
}
