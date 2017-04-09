package util;

import SentenceStruct.Sentence;
import SentenceStruct.simplePA;
import SupervisedSRL.Strcutures.SRLOutput;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Maryam Aminian on 4/12/16.
 */
public class IO {

    public static ArrayList<String> readCoNLLFile(String coNLLFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(coNLLFile), "UTF-8"));
        ArrayList<String> sentences = new ArrayList<String>();
        String line2read = "";
        int counter = 0;
        StringBuilder sentence = new StringBuilder();
        while ((line2read = reader.readLine()) != null) {
            if (line2read.equals("")) //sentence break
            {
                counter++;

                if (counter % 100000 == 0)
                    System.out.print(counter);
                else if (counter % 10000 == 0)
                    System.out.print(".");

                String senText = sentence.toString().trim();
                if (senText.length() > 0)
                    sentences.add(senText);
                sentence = new StringBuilder();
            } else {
                sentence.append(line2read);
                sentence.append("\n");
            }
        }
        return sentences;
    }

    public static ArrayList<String> readCoNLLFile_into_words(String coNLLFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(coNLLFile)));
        ArrayList<String> sentences = new ArrayList<String>();
        String line2read = "";
        String sentence = "";

        while ((line2read = reader.readLine()) != null) {
            if (line2read.equals("")) //sentence break
            {
                sentences.add(sentence.trim());
                sentence = "";
            } else {
                sentence += line2read.split("\t")[1] + " ";
            }
        }
        return sentences;
    }

    public static <T> void write(T o, String filePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(filePath);
        GZIPOutputStream gz = new GZIPOutputStream(fos);
        ObjectOutput writer = new ObjectOutputStream(gz);
        writer.writeObject(o);
        writer.close();
    }

    public static <T> T load(String filePath) throws Exception {
        FileInputStream fis = new FileInputStream(filePath);
        GZIPInputStream gz = new GZIPInputStream(fis);
        ObjectInput reader = new ObjectInputStream(gz);
        return (T) reader.readObject();
    }

    public static ArrayList<String> readPlainFile(String plainFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plainFile)));
        ArrayList<String> sentences = new ArrayList<String>();
        String line2read = "";

        while ((line2read = reader.readLine()) != null) {

            sentences.add(line2read.trim());
        }
        return sentences;
    }

    public static Object[] readCoNLLFile_into_words_and_conll_data(String coNLLFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(coNLLFile)));
        ArrayList<String> sentences_conll = new ArrayList<String>();
        ArrayList<String> sentences_words = new ArrayList<String>();

        String line2read = "";
        String sentence_conll = "";
        String sentence_words = "";

        int sentenceCounter = 0;
        while ((line2read = reader.readLine()) != null) {
            if (line2read.equals("")) //sentence break
            {
                sentenceCounter++;

                if (sentenceCounter % 100000 == 0)
                    System.out.print(sentenceCounter);
                else if (sentenceCounter % 10000 == 0)
                    System.out.print(".");

                sentences_conll.add(sentence_conll);
                sentences_words.add(sentence_words.trim());
                sentence_conll = "";
                sentence_words = "";
            } else {
                sentence_conll += line2read + "\n";
                sentence_words += line2read.split("\t")[1] + " ";
            }
        }

        return new Object[]{sentences_conll, sentences_words};
    }

    public static SRLOutput generateCompleteOutputSentenceInCoNLLFormat(Sentence inputSentence, String inputSentenceStr,
                                                                        TreeMap<Integer, simplePA> prediction, boolean supplement) {
        String finalSentence = "";
        ArrayList<String> sentenceForOutput = IO.getSentenceFixedFields(inputSentenceStr);
        String[] inputSentenceFillPreds = inputSentence.getFillPredicate();
        HashMap<Integer, HashSet<Integer>> inputSentenceUndecidedArgs = inputSentence.getUndecidedArgs();
        Object[] o =  createFinalLabeledOutput(inputSentence, prediction,
                inputSentenceFillPreds, inputSentenceUndecidedArgs, supplement);

        TreeMap<Integer, simplePA>  finalLabels = (TreeMap<Integer, simplePA>) o[0];
        double overlap = (double) o[1];

        for (int wordIdx = 0; wordIdx < sentenceForOutput.size(); wordIdx++) {
            //for each word in the sentence
            finalSentence += sentenceForOutput.get(wordIdx) + "\t";  //filling fields 0-11
            int realWordIdx = wordIdx +1 ;

            if (finalLabels.containsKey(realWordIdx)) {
                simplePA simplePA = finalLabels.get(realWordIdx);
                finalSentence += "Y\t"; //filed 12
                finalSentence += simplePA.getPredicateLabel(); //field 13
            }else {
                finalSentence += "_\t"; //filed 12
                finalSentence += "_"; //field 13
            }

            //checking if this word has been an argument for other predicates or not (fields 14-end)
            for (int pIdx : finalLabels.keySet()) {
                if (finalLabels.get(pIdx).getArgumentLabels().containsKey(realWordIdx))
                    // either argument label or "?"
                    finalSentence += "\t" + finalLabels.get(pIdx).getArgumentLabels().get(realWordIdx);
                else {
                    finalSentence += "\t_";
                }
            }
            finalSentence += "\n";
        }
        finalSentence += "\n";
        return new SRLOutput(finalSentence, overlap);
    }

    public static String formatString2Conll(String input) {
        String ConllFormatString = "";
        int wordIdx = 0;
        for (String word : input.split(" ")) {
            wordIdx++;
            ConllFormatString += wordIdx + "\t" + word + "\n";
        }

        return ConllFormatString;
    }

    public static ArrayList<String> getSentenceFixedFields(String sentenceInCoNLLFormat) {
        String[] lines = sentenceInCoNLLFormat.split("\n");
        ArrayList<String> sentenceForOutput = new ArrayList<String>();
        for (String line : lines) {
            String[] fields = line.split("\t");
            String filedsForOutput = "";
            //we just need the first 12 fields. The rest of filed must be filled based on what system predicted
            for (int k = 0; k < 12; k++)
                filedsForOutput += fields[k] + "\t";
            sentenceForOutput.add(filedsForOutput.trim());
        }
        return sentenceForOutput;
    }

    /**
     * creates final labeling of the sentence
     * @param inputSentence sentence with original labeling, either gold or projected
     * @param prediction predictions made by SRL
     * @param supplement a boolean argument specifying if we need to supplement predicted labels to the original ones or not!
     */
    public static Object[] createFinalLabeledOutput (Sentence inputSentence,
                                                                       TreeMap<Integer, simplePA> prediction,
                                                                       String[] fillPredicates,
                                                                       HashMap<Integer, HashSet<Integer>> undecidedArgs,
                                                                       boolean supplement){
        TreeMap<Integer, simplePA> output = new TreeMap<>(inputSentence.getPAMap());  //contains neither "_", nor "?"
        int overlap =0;
        int totalNumOfPredictedDependencies = 0;

        if (supplement) {
            int pSeq = -1;
            for (int ppIdx : prediction.keySet()) {
                pSeq++;
                totalNumOfPredictedDependencies += prediction.get(ppIdx).getArgumentLabels().size();
                //for each predicted predicate
                if (!output.containsKey(ppIdx)) {
                    if (fillPredicates[ppIdx].equals("?")) {
                        simplePA p = new simplePA(prediction.get(ppIdx).getPredicateLabel(),
                                prediction.get(ppIdx).getArgumentLabels());
                        output.put(ppIdx, p);
                    }
                } else {
                    String pPLabel = prediction.get(ppIdx).getPredicateLabel();
                    String previousPLabel = output.get(ppIdx).getPredicateLabel();

                    for (int pArgIdx : prediction.get(ppIdx).getArgumentLabels().keySet()) {
                        if (!output.get(ppIdx).getArgumentLabels().containsKey(pArgIdx)) {
                            if (undecidedArgs.containsKey(pSeq) && undecidedArgs.get(pSeq).contains(pArgIdx)) {
                                String pArgLabel = prediction.get(ppIdx).getArgumentLabels().get(pArgIdx);
                                output.get(ppIdx).getArgumentLabels().put(pArgIdx, pArgLabel);
                            }
                        }else{
                           String pALabel = prediction.get(ppIdx).getArgumentLabels().get(pArgIdx);
                            String previousALabel = output.get(ppIdx).getArgumentLabels().get(pArgIdx);

                            if (pALabel.equals(previousALabel))
                                overlap++;
                        }
                    }
                }
            }
        }else
        {
            for (int ppIdx : prediction.keySet()) {
                totalNumOfPredictedDependencies += prediction.get(ppIdx).getArgumentLabels().size();
                //for each predicted predicate
                if (output.containsKey(ppIdx)){
                    String pPLabel = prediction.get(ppIdx).getPredicateLabel();
                    String previousPLabel = output.get(ppIdx).getPredicateLabel();

                    for (int pArgIdx : prediction.get(ppIdx).getArgumentLabels().keySet()) {
                        if (output.get(ppIdx).getArgumentLabels().containsKey(pArgIdx)) {
                            String pALabel = prediction.get(ppIdx).getArgumentLabels().get(pArgIdx);
                            String previousALabel = output.get(ppIdx).getArgumentLabels().get(pArgIdx);
                            if (pALabel.equals(previousALabel))
                                overlap++;
                        }
                    }
                }
            }
            output = prediction;
        }
        double overlapScore = 0;
        if (totalNumOfPredictedDependencies !=0)
            overlapScore =((double) overlap)/totalNumOfPredictedDependencies;
        return new Object[]{output, overlapScore};
    }

    public static HashSet<String> obtainLabels(List<String> sentences) {
        System.out.println("Getting set of labels...");
        HashSet<String> labels = new HashSet<String>();

        int counter = 0;
        for (String sentence : sentences) {
            counter++;
            if (counter % 1000 == 0)
                System.out.println(counter + "/" + sentences.size());

            String[] tokens = sentence.trim().split("\n");
            for (String token : tokens) {
                String[] fields = token.split("\t");
                for (int k = 14; k < fields.length; k++) {
                    if (!fields[k].equals("_") && !fields[k].equals("?"))
                        labels.add(fields[k]);
                }
            }
        }
        return labels;
    }

    public static boolean makeDirectory(String path) {
        File dir = new File(path);
        if (dir.exists())
            dir.delete();
        dir.mkdir();
        return true;
    }

    public static HashMap<Integer, String>[] getDisambiguatedPredicatesFromOutput (String output, int numOfSentences) throws IOException {

        HashMap<Integer, String>[] disambiguatedPredicates = new HashMap[numOfSentences];
        ArrayList<String> sentences = readCoNLLFile(output);
        for (int senID =0 ; senID < sentences.size(); senID++){
            HashMap<Integer, String> disambiguatedPredicates4ThisSentence = new HashMap<>();
            String sentence = sentences.get(senID);
            String[] tokens = sentence.trim().split("\n");

            for (int tokenIdx = 0; tokenIdx < tokens.length; tokenIdx++) {
                String token = tokens[tokenIdx];
                String[] fields = token.split("\t");
                int index = Integer.parseInt(fields[0]);
                String predicateLabel = fields[13];
                if (!predicateLabel.equals("_"))
                    disambiguatedPredicates4ThisSentence.put(index, predicateLabel);
            }
            disambiguatedPredicates[senID] = disambiguatedPredicates4ThisSentence;
        }
        return disambiguatedPredicates;
    }
}