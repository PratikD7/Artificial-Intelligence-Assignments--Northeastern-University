import java.util.*;

// Semisupervised Tomatoes:
// EM some Naive Bayes and Markov Models to do sentiment analysis.
// Based on solution code for Assignment 3.
//
// Input from train.tsv.zip at 
// https://www.kaggle.com/c/sentiment-analysis-on-movie-reviews
//
// itself gathered from Rotten Tomatoes.
//
// Format is PhraseID[unused]   SentenceID  Sentence[tokenized]
//
// Just a few sentiment labels this time - this is semisupervised.
//
// We'll only use the first line for each SentenceID, since the others are
// micro-analyzed phrases that would just mess up our counts.
//
// After training, we'll identify the top words for each cluster by
// Pr(cluster | word) - the words that are much more likely in the cluster
// than in the general population - and categorize the new utterances.

public class SemisupervisedTomatoes { /////////////////////??????????????? PUNCTUATIONS,

    public static final int CLASSES = 2;
    // Assume sentence numbering starts with this number in the file
    public static final int FIRST_SENTENCE_NUM = 1;
    
    // Probability of either a unigram or bigram that hasn't been seen
    // Gotta make this real generous if we're not using logs
    public static final double OUT_OF_VOCAB_PROB = 0.000001;

    // Words to print per class
    public static final int TOP_N = 10;
    // Times (in expectation) that we need to see a word in a cluster
    // before we think it's meaningful enough to print in the summary
    public static final double MIN_TO_PRINT = 15.0;

    public static boolean USE_UNIFORM_PRIOR = false;
    public static boolean SEMISUPERVISED = true;
    public static boolean FIXED_SEED = true;

    public static final int ITERATIONS = 200;

    // We may play with this in the assignment, but it's good to have common
    // ground to talk about
    public static Random rng = (FIXED_SEED? new Random(2018) : new Random());

    public static NaiveBayesModel nbModel = new NaiveBayesModel();

    public static class NaiveBayesModel {
        public double[] classCounts;
        public double[] totalWords;
        public ArrayList<HashMap<String, Double>> wordCounts;

        public NaiveBayesModel() {
            classCounts = new double[CLASSES];
            totalWords = new double[CLASSES];
            wordCounts = new ArrayList<HashMap<String, Double>>();
            for (int i = 0; i < CLASSES; i++) {
                wordCounts.add(new HashMap<String, Double>());
            }
        }

        // Update the model given a sentence and its probability of
        // belonging to each class
        void update(String sentence, ArrayList<Double> probs) {
            // TODO

            // Split the sentence into words and apply case folding
            String[] tokens = sentence.split(" ");
            for (int i=0; i<tokens.length; i++){
                tokens[i] = tokens[i].toLowerCase();
            }

            // Update the values of classcounts, totalwords and wordcounts
            for (int j=0;j<probs.size();j++){

                HashMap<String, Double> hmap = new HashMap<>();
                hmap = wordCounts.get(j);

                classCounts[j] += probs.get(j);
                totalWords[j] += probs.get(j) * sentence.length();
                for (int k=0;k<tokens.length;k++){
                    boolean flag = false;

                    if (wordCounts.get(j).containsKey(tokens[k])){
                        flag = true;
                    }

                    if (flag){
                        hmap.put(tokens[k], wordCounts.get(j).get(tokens[k]) + probs.get(j)); ///////////?????????
                        wordCounts.set(j, hmap);
                    }
                    else{
                        hmap.put(tokens[k], probs.get(j));
                        wordCounts.set(j, hmap);
                    }

                }
            }
        }

        // Classify a new sentence using the data and a Naive Bayes model.
        // Assume every token in the sentence is space-delimited, as the input
        // was.  Return a list of class probabilities.
        public ArrayList<Double> classify(String sentence) {
            // TODO (the below is a placeholder to compile)


            double[] probs = new double[CLASSES];

            for (int i=0; i< CLASSES; i++){
	            double prior_prob;
	            double posterior_prob;

                prior_prob = classCounts[i] / sum(classCounts);
	            probs[i] = prior_prob;

                // Naive Bayes Rule
                String[] tokenized = sentence.split(" ");
                for (int j = 0; j < tokenized.length; j++){
                    try {
	                    double posterior = wordCounts.get(i).get(tokenized[j]) /(totalWords[i]);
                        posterior_prob = posterior;
                    }
                    // If the word doesnt exist in the training data
                    catch (NullPointerException e){
                        posterior_prob = OUT_OF_VOCAB_PROB;
                    }
                    probs[i] *= posterior_prob;
                }
            }




		// Also adjust for the prob getting 0 that results from multiplication : Double.MIN_NORMAL
	        for (int z=0; z<probs.length; z++){
            	if (probs[z] == 0.0){
            		probs[z] = Double.MIN_NORMAL;
	            }
	        }

	        double sumOfProbs = sum(probs);
	        for (int z=0; z<probs.length; z++){
		        probs[z] /= sumOfProbs;
	        }


	        for (int z=0;z<probs.length; z++){
	            if (probs[z] == Double.POSITIVE_INFINITY){
	                probs[z] = 0.5;
                }
            }

			ArrayList <Double> al = new ArrayList<>();
	        for (int q=0; q<probs.length; q++)
	        {
	        	al.add(probs[q]);
	        }



            return al;
        }

        // printTopWords: Print five words with the highest
        // Pr(thisClass | word) = scale Pr(word | thisClass)Pr(thisClass)
        // but skip those that have appeared (in expectation) less than 
        // MIN_TO_PRINT times for this class (to avoid random weird words
        // that only show up once in any sentence)
        void printTopWords(int n) {
            for (int c = 0; c < CLASSES; c++) {
                System.out.println("Cluster " + c + ":");
                ArrayList<WordProb> wordProbs = new ArrayList<WordProb>();
                for (String w : wordCounts.get(c).keySet()) {
                    if (wordCounts.get(c).get(w) >= MIN_TO_PRINT) {
                        // Treating a word as a one-word sentence lets us use
                        // our existing model
                        ArrayList<Double> probs = nbModel.classify(w);
                        wordProbs.add(new WordProb(w, probs.get(c)));
                    }
                }
                Collections.sort(wordProbs);
                for (int i = 0; i < n; i++) {
                    if (i >= wordProbs.size()) {
                        System.out.println("No more words...");
                        break;
                    }
                    System.out.println(wordProbs.get(i).word);
                }
            }
        }

	    public static float sum(double[] values) {
		    float result = 0;
		    for (double value:values)
			    result += value;
		    return result;
	    }
    }

    public static void main(String[] args) {
        Scanner myScanner = new Scanner(System.in);
        ArrayList<String> sentences = getTrainingData(myScanner);
        trainModels(sentences);
        nbModel.printTopWords(TOP_N);
        classifySentences(myScanner);
    }

    public static ArrayList<String> getTrainingData(Scanner sc) {
        int nextFresh = FIRST_SENTENCE_NUM;
        ArrayList<String> sentences = new ArrayList<String>();
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("---")) {
                return sentences;
            }
            // Data should be filtered now, so just add it
            sentences.add(line);
        }
        return sentences;
    }

    static void trainModels(ArrayList<String> sentences) {
        // We'll start by assigning the sentences to random classes.
        // 1.0 for the random class, 0.0 for everything else
        System.err.println("Initializing models....");
        HashMap<String,ArrayList<Double>> naiveClasses = randomInit(sentences);
        // Initialize the parameters by training as if init were
        // ground truth (essentially starting with M step)
        // TODO : Initialize the model paramters using the supervised labeled sentences
        ArrayList<Double> probs;
        initializeModelParamters(sentences);

        for (int i = 0; i < ITERATIONS; i++) {
            System.err.println("EM round " + i);
            // TODO:  E STEP
            // Calling classify on all sentences to get probability arraylists for each
            for (String sentence: sentences){
                probs = nbModel.classify(sentence);
                naiveClasses.put(sentence, probs);
            }

            // Reset the model parameters
            for (int d=0;d<nbModel.classCounts.length; d++){
                nbModel.classCounts[d] = 0;
            }
            for (int d=0;d<nbModel.totalWords.length; d++){
                nbModel.totalWords[d] = 0;
            }
            nbModel.wordCounts.clear();
            for (int h = 0; h < CLASSES; h++) {
                nbModel.wordCounts.add(new HashMap<String, Double>());
            }

            // TODO:  M STEP
            // throwing out the old counts of everything and using update() to get new counts
            for (Map.Entry<String, ArrayList<Double>> entry: naiveClasses.entrySet()){
                nbModel.update(entry.getKey(), entry.getValue());
            }

        }
    }

    private static void initializeModelParamters(ArrayList<String> sentences) {
        ArrayList<Double> probs = new ArrayList<>();
        Double pos=0.0, neg=0.0;
        for (int k=0;k<sentences.size(); k++){
            if (sentences.get(k).startsWith(":)")){
                pos+=1;
            }
            else if (sentences.get(k).startsWith(":(")){
                neg+=1;
            }
        }
        probs.add(neg/(pos+neg));
        probs.add(pos/(pos+neg));
        for (int k=0;k<sentences.size(); k++) {
            if (sentences.get(k).startsWith(":)") || sentences.get(k).startsWith(":(")) {
                nbModel.update(sentences.get(k), probs);
            }
        }
    }

    static HashMap<String,ArrayList<Double>> randomInit(ArrayList<String> sents) {
        HashMap<String,ArrayList<Double>> counts = new HashMap<String,ArrayList<Double>>();
        for (String sent : sents) {
            ArrayList<Double> probs = new ArrayList<Double>();
            if (SEMISUPERVISED && sent.startsWith(":)")) {
                // Class 1 = positive
                probs.add(0.0);
                probs.add(1.0);
                for (int i = 2; i < CLASSES; i++) {
                    probs.add(0.0);
                }
                // Shave off emoticon
                sent = sent.substring(3);
            } else if (SEMISUPERVISED && sent.startsWith(":(")) {
                // Class 0 = negative
                probs.add(1.0);
                probs.add(0.0);
                for (int i = 2; i < CLASSES; i++) {
                    probs.add(0.0);
                }
                // Shave off emoticon
                sent = sent.substring(3);
            } else {
                double baseline = 1.0/CLASSES;
                // Slight deviation to break symmetry
                int randomBumpedClass = rng.nextInt(CLASSES);
                double bump = (1.0/CLASSES * 0.25);
                if (SEMISUPERVISED) {
                    // Symmetry breaking not necessary, already got it
                    // from labeled examples
                    bump = 0.0;
                }
                for (int i = 0; i < CLASSES; i++) {
                    if (i == randomBumpedClass) {
                        probs.add(baseline + bump);
                    } else {
                        probs.add(baseline - bump/(CLASSES-1));
                    }
                }
            }
            counts.put(sent, probs);
        }
        return counts;
    }

    public static class WordProb implements Comparable<WordProb> {
        public String word;
        public Double prob;

        public WordProb(String w, Double p) {
            word = w;
            prob = p;
        }

        public int compareTo(WordProb wp) {
            // Reverse order
            if (this.prob > wp.prob) {
                return -1;
            } else if (this.prob < wp.prob) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static void classifySentences(Scanner scan) {
        while(scan.hasNextLine()) {
            String line = scan.nextLine();
            System.out.print(line + ":");
            ArrayList<Double> probs = nbModel.classify(line);
            for (int c = 0; c < CLASSES; c++) {
                System.out.print(probs.get(c) + " ");
            }
            System.out.println();
        }
    }

}
