import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

// An assignment on decision trees, using the "Adult" dataset from
// the UCI Machine Learning Repository.  The dataset predicts
// whether someone makes over $50K a year from their census data.
//
// Input data is a comma-separated values (CSV) file where the
// target classification is given a label of "Target."
// The other headers in the file are the feature names.
//
// Features are assumed to be strings, with comparison for equality
// against one of the values as a decision, unless the value can
// be parsed as a double, in which case the decisions are < comparisons
// against the values seen in the data.

public class DecisionTree {

	public Feature feature;   // if true, follow the yes branch
	public boolean decision;  // for leaves
	public DecisionTree yesBranch;
	public DecisionTree noBranch;

	public static double CHI_THRESH = 3.84;  // chi-square test critical value
	public static double EPSILON = 0.00000001; // for determining whether vals roughly equal
	public static boolean PRUNE = true;  // prune with chi-square test or not
	public static void main(String[] args) {

		Results r;

		Scanner scanner = new Scanner(System.in);
		// Keep header line around for interpreting decision trees
		String header = scanner.nextLine();
		Feature.featureNames = header.split(",");
		System.err.println("Reading training examples...");
		ArrayList<Example> trainExamples = readExamples(scanner, true);
		// We'll assume a delimiter of "---" separates train and test as before
		DecisionTree tree = new DecisionTree(trainExamples);
//        System.out.println(tree);
		System.out.println("Training data results: ");
		System.out.println(tree.classify(trainExamples));
//	    System.out.println(tree);
		System.err.println("Reading test examples...");
		Scanner scanner2 = new Scanner(System.in);
		ArrayList<Example> testExamples = readExamples(scanner2, false);
		Results results = tree.classifyTest(testExamples);
		System.out.println("Test data results: ");
		System.out.print(results);
	}

	public static ArrayList<Example> readExamples(Scanner scanner, boolean trainData) {
		ArrayList<Example> examples = new ArrayList<Example>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("---")) {
				break;
			}
			// Skip missing data lines
			if (!line.contains("?")) {
				Example newExample = new Example(line);
				examples.add(newExample);
			}
		}
		return examples;
	}

	public static class Example {
		// Not all features will use both arrays.  The Feature.isNumerical static
		// array will determine whether the numericals array can be used.  If not,
		// the strings array will be used.  The indices correspond to the columns
		// of the input, and thus the different features.  "target" is special
		// as it gives the desired classification of the example.
		public String[] strings;     // Use only if isNumerical[i] is false
		public double[] numericals;  // Use only if isNumerical[i] is true
		boolean target;

		// Construct an example from a CSV input line
		public Example(String dataline) {
			// Assume a basic CSV with no double-quotes to handle real commas
			strings = dataline.split(",");
			// We'll maintain a separate array with everything that we can
			// put into numerical form, in numerical form.
			// No real need to distinguish doubles from ints.
			numericals = new double[strings.length];
			if (Feature.isNumerical == null) {
				// First data line; we're determining types
				Feature.isNumerical = new boolean[strings.length];
				for (int i = 0; i < strings.length; i++) {
					if (Feature.featureNames[i].equals("Target")) {
						target = strings[i].equals("1");
					} else {
						try {
							numericals[i] = Double.parseDouble(strings[i]);
							Feature.isNumerical[i] = true;
						} catch (NumberFormatException e) {
							Feature.isNumerical[i] = false;
							// string stays where it is, in strings
						}
					}
				}
			} else {
				for (int i = 0; i < strings.length; i++) {
					if (i >= Feature.isNumerical.length) {
						System.err.println("Too long line: " + dataline);
					} else if (Feature.featureNames[i].equals("Target")) {
						target = strings[i].equals("1");
					} else if (Feature.isNumerical[i]) {
						try {
							numericals[i] = Double.parseDouble(strings[i]);
						} catch (NumberFormatException e) {
							Feature.isNumerical[i] = false;
							// string stays where it is
						}
					}
				}
			}
		}

		// Possibly of help in debugging:  a way to print examples
		public String toString() {
			String out = "";
			for (int i = 0; i < Feature.featureNames.length; i++) {
				out += Feature.featureNames[i] + "=" + strings[i] + ";";
			}
			return out;
		}
	}

	public static class Results {
		public int true_positive;  // correctly classified "yes"
		public int true_negative;  // correctly classified "no"
		public int false_positive; // incorrectly classified "yes," should be "no"
		public int false_negative; // incorrectly classified "no", should be "yes"

		public Results() {
			true_positive = 0;
			true_negative = 0;
			false_positive = 0;
			false_negative = 0;
		}

		public String toString() {
			String out = "Precision: ";
			out += String.format("%.4f", true_positive/(double)(true_positive + false_positive));
			out += "\nRecall: " + String.format("%.4f",true_positive/(double)(true_positive + false_negative));
			out += "\n";
			out += "Accuracy: ";
			out += String.format("%.4f", (true_positive + true_negative)/(double)(true_positive + true_negative + false_positive + false_negative));
			out += "\n";
			return out;
		}
	}

	public static class Feature {
		// Which feature are we talking about?  Can index into Feature.featureNames
		// to get name of the feature, or into strings and numericals arrays of example
		// to get feature value
		public int featureNum;
		// WLOG assume numerical features are "less than"
		// and String features are "equal to"
		public String svalue;  // the string value to compare a string feature against
		public double dvalue;  // the numerical threshold to compare a numerical feature against
		public static String[] featureNames;  // extracted from the header
		public static boolean[] isNumerical = null;  // need to read a line to see the size

		public Feature(int featureNum, String value) {
			this.featureNum = featureNum;
			this.svalue = value;
		}

		public Feature(int featureNum, double value) {
			this.featureNum = featureNum;
			this.dvalue = value;
		}

		// Ask whether the answer is "yes" or "no" to the question implied by this feature
		// when applied to a particular example
		public boolean apply(Example e) {
			if (Feature.isNumerical[featureNum]) {
				return (e.numericals[featureNum] < dvalue);
			} else {
				return (e.strings[featureNum].equals(svalue));
			}
		}

		// It's suggested that when you generate a collection of potential features, you
		// use a HashSet to avoid duplication of features.  The equality and hashCode operators
		// that follow can help you with this.
		public boolean equals(Object o) {
			if (!(o instanceof Feature)) {
				return false;
			}
			Feature otherFeature = (Feature) o;
			if (featureNum != otherFeature.featureNum) {
				return false;
			} else if (Feature.isNumerical[featureNum]) {
				if (Math.abs(dvalue - otherFeature.dvalue) < EPSILON) {
					return true;
				}
				return false;
			} else {
				if (svalue.equals(otherFeature.svalue)) {
					return true;
				}
				return false;
			}
		}

		public int hashCode() {
			return (featureNum + (svalue == null ? 0 : svalue.hashCode()) + (int) (dvalue * 10000));
		}

		// Print feature's check; called when printing decision trees
		public String toString() {
			if (Feature.isNumerical[featureNum]) {
				return Feature.featureNames[featureNum] + " < " + dvalue;
			} else {
				return Feature.featureNames[featureNum] + " = " + svalue;
			}
		}


	}

	// This constructor should create the whole decision tree recursively.
	DecisionTree(ArrayList<Example> examples) {
		// TODO your code here
		yesBranch = null;
		noBranch = null;
		feature = null;

		int yesex=0, noex=0;
		for (int i=0;i<examples.size();i++){
			if(examples.get(i).target == false) noex+=1;
			else yesex+=1;
		}
		if (yesex > noex) decision = true;
		else decision = false;

	}


	public double entropy(ArrayList<Integer> yesList, ArrayList<Integer> noList){
		double p1, p2;
		double E1=0, E2=0;

		int targetYes=0, targetNo=0;
		for (int i=0;i<yesList.size();i++){
			if (yesList.get(i)==0) targetNo+=1;
			else targetYes+=1;
			p1 = (double)targetYes/(targetYes+targetNo);
			p2 = (double)targetNo/(targetYes+targetNo);
			if (p1==0) E1 =  (- (p2*Math.log(p2)/Math.log(2)));
			else if (p2==0) E1 =  (- (p1*Math.log(p1)/Math.log(2)));
			else E1 =  (-(p1*Math.log(p1)/Math.log(2)) - (p2*Math.log(p2)/Math.log(2)));
		}

		targetYes=0; targetNo=0;
		for (int i=0;i<noList.size();i++){
			if (noList.get(i)==0) targetNo+=1;
			else targetYes+=1;
			p1 = (double)targetYes/(targetYes+targetNo);
			p2 = (double)targetNo/(targetYes+targetNo);
			if (p1==0) E2 =  (- (p2*Math.log(p2)/Math.log(2)));
			else if (p2==0) E2 =  (- (p1*Math.log(p1)/Math.log(2)));
			else E2 =  (-(p1*Math.log(p1)/Math.log(2)) - (p2*Math.log(p2)/Math.log(2)));
		}

		return ((yesList.size()*E1 + noList.size()*E2) / (yesList.size()+noList.size()));
	}

	public String toString() {
		return toString(0);
	}

	// Print the decision tree as a set of nested if/else statements.
	// This is a little easier than trying to print with the root at the top.
	public String toString(int depth) {
		String out = "";
		for (int i = 0; i < depth; i++) {
			out += "    ";
		}
		if (feature == null) {
			out += (decision ? "YES" : "NO");
			out += "\n";
			return out;
		}
		out += "if " + feature + "\n";
		out += yesBranch.toString(depth+1);
		for (int i = 0; i < depth; i++) {
			out += "    ";
		}
		out += "else\n";
		out += noBranch.toString(depth+1);
		return out;
	}

	public boolean isTwoArrayListsSame(ArrayList<Example> list1, ArrayList<Example> list2)
	{
		//null checking
		if(list1==null && list2==null)
			return true;
		if((list1 == null && list2 != null) || (list1 != null && list2 == null))
			return false;

		if(list1.size()!=list2.size())
			return false;
		for(Object itemList1: list1)
		{
			if(!list2.contains(itemList1))
				return false;
		}

		return true;
	}



	public Results classify(ArrayList<Example> examples) {
		Results results = new Results();
		Results finalResults = new Results();

		if (examples.size()!=0) {

			boolean allAgree = false;
			for (int i = 1; i < examples.size(); i++) {
				if (examples.get(i - 1).target == examples.get(i).target) {
					allAgree = true;
				} else {
					allAgree = false;
					break;
				}
			}


			if (allAgree) {


				this.decision = examples.get(0).target;


				for (int i=0;i<examples.size();i++){
					if (this.decision == false){
						if (examples.get(i).target == false){
							results.true_negative += 1;
						}
						else{
							results.false_positive += 1;
						}
					}
					else{
						if (examples.get(i).target == true){
							results.true_positive += 1;
						}
						else{
							results.false_negative += 1;
						}
					}
				}
				return results;




			} else {

				int targetIndex=0;
				for (int i=0;i<Feature.featureNames.length;i++){
					if (Feature.featureNames[i].equals("Target")) {
						targetIndex = i;
						break;
					}
				}


				ArrayList<ArrayList<String>> idealFeature = new ArrayList();
				// Select the feature and value with the least entropy.
				// That would be the node of the tree. Split the examples into two different branches
				for (int i = 0; i < Feature.featureNames.length; i++) {
					if (i!= targetIndex) {
						ArrayList<Double> entropyValues = new ArrayList();
						for (int j = 0; j < examples.size(); j++) {
							ArrayList<Integer> yesList = new ArrayList();
							ArrayList<Integer> noList = new ArrayList();
							ArrayList<Boolean> targetValues = new ArrayList();
							for (int k = 0; k < examples.size(); k++) {
								// Calculate entropy with feature i and value j (< or = depends) and select value with least entropy (APPLY)

								Feature f;
								if (Feature.isNumerical[i]) {
									f = new Feature(i, examples.get(j).numericals[i]);
								} else {
									f = new Feature(i, examples.get(j).strings[i]);
								}

								targetValues.add(f.apply(examples.get(k)));


								if (f.apply(examples.get(k)))
									yesList.add(Integer.parseInt(examples.get(k).strings[targetIndex]));
								else noList.add(Integer.parseInt(examples.get(k).strings[targetIndex]));
							}
							entropyValues.add(entropy(yesList, noList));
						}
						double min = entropyValues.get(0);
						int index = 0;
						for (int a = 1; a < entropyValues.size(); a++) {
							if (entropyValues.get(a) < min) {
								min = entropyValues.get(a);
								index = a;
							}
						}

						//Order : Feature index, Entropy value, Value for the feature at which E is minimum
						ArrayList<String> temp = new ArrayList();
						temp.add(String.valueOf(i));
						temp.add(String.valueOf(min));
						temp.add(examples.get(index).strings[i]);
						idealFeature.add(temp);

					}
				}

				Double minEnt = Double.parseDouble(idealFeature.get(0).get(1));
				Double featureIndex = Double.parseDouble(idealFeature.get(0).get(0));
				Double thresholdValueNumeric=0.0;
				String thresholdValueString="";
				if (Feature.isNumerical[featureIndex.intValue()]){
					thresholdValueNumeric = Double.parseDouble(idealFeature.get(0).get(2));
				}
				else{
					thresholdValueString = idealFeature.get(0).get(2);
				}

					boolean flagNumeric = true;
					for (int z = 1; z < idealFeature.size(); z++) {
						if (Feature.isNumerical[(int) Double.parseDouble(idealFeature.get(z).get(0))]) {
							flagNumeric = true;
							if (Double.parseDouble(idealFeature.get(z).get(1)) < minEnt) {
								featureIndex = Double.parseDouble(idealFeature.get(z).get(0));
								thresholdValueNumeric = Double.parseDouble(idealFeature.get(z).get(2));
							}
						}
						else{
							flagNumeric = false;
							if (Double.parseDouble(idealFeature.get(z).get(1)) < minEnt) {
								featureIndex = Double.parseDouble(idealFeature.get(z).get(0));
								thresholdValueString = (idealFeature.get(z).get(2));
							}
						}
					}

					if (flagNumeric) {
						Feature f;
						f = new Feature(featureIndex.intValue(), thresholdValueNumeric);
						this.feature = f;
					}
					else{
						Feature f;
						f = new Feature(featureIndex.intValue(), thresholdValueString);
						this.feature = f;
					}


//				else{
//					for (int z = 1; z < idealFeature.size(); z++) {
//						if (Double.parseDouble(idealFeature.get(z).get(1)) < minEnt) {
//							thresholdValueString = (idealFeature.get(z).get(2));
//						}
//					}
//					Feature f;
//					f = new Feature(featureIndex.intValue(), thresholdValueNumeric);
//					this.feature = f;
//				}


				ArrayList<Example> yesExamples = new ArrayList();
				ArrayList<Example> noExamples = new ArrayList();
				for (int i = 0; i < examples.size(); i++) {
					try {
						Double.parseDouble(idealFeature.get(0).get(2));
						if (examples.get(i).numericals[featureIndex.intValue()] < thresholdValueNumeric) {
							yesExamples.add(examples.get(i));
						} else{
							noExamples.add(examples.get(i));
						}
					}
					catch(NumberFormatException e) {
						if (examples.get(i).strings[featureIndex.intValue()].equals(thresholdValueString)) {
							yesExamples.add(examples.get(i));
						} else {
							noExamples.add(examples.get(i));
						}
					}
				}

				if (isTwoArrayListsSame(examples, yesExamples) || isTwoArrayListsSame(examples, noExamples)){
					// Return the majority
					int yes=0, no=0;
					for (int i=0;i<examples.size();i++){
						if (examples.get(i).target == true) yes+=1;
						else no+=1;
					}
					if (yes>no) this.decision = true;
					else this.decision = false;
					return results;
				}


				Results r1;
				Results r2;

				DecisionTree dt1 = new DecisionTree(yesExamples);
				DecisionTree dt2 = new DecisionTree(noExamples);

				this.yesBranch = dt1;
				this.noBranch = dt2;

				r1 = dt1.classify(yesExamples);
				r2 = dt2.classify(noExamples);
//				}

				finalResults.true_positive = r1.true_positive + r2.true_positive;
				finalResults.true_negative = r1.true_negative + r2.true_negative;
				finalResults.false_positive = r1.false_positive + r2.false_positive;
				finalResults.false_negative = r1.false_negative + r2.false_negative;

			}

			// the truth to populate the results structure
			return finalResults;
		}
		else return results;
	}

	public Results classifyTest(ArrayList<Example> examples) {
		Results results = new Results();
		Results r;
		System.out.println();

		for (int i=0;i<examples.size();i++){
			r = test(this, examples.get(i));
			results.true_positive += r.true_positive;
			results.true_negative += r.true_negative;
			results.false_positive += r.false_positive;
			results.false_negative += r.false_negative;
		}

		return results;
	}

	private Results test(DecisionTree decisionTree, Example example) {
		Results results = new Results();
		if (decisionTree.yesBranch==null && decisionTree.noBranch==null){
			if (decisionTree.decision == false){
				if (example.target == false){
					results.true_negative += 1;
				}
				else{
					results.false_positive += 1;
				}
			}
			else{
				if (example.target == true){
					results.true_positive += 1;
				}
				else{
					results.false_negative += 1;
				}
			}
		}
		else{
			if (Feature.isNumerical[decisionTree.feature.featureNum]) {
				if (example.numericals[decisionTree.feature.featureNum] < decisionTree.feature.dvalue) {
					results = test(decisionTree.yesBranch, example);
				} else {
					results = test(decisionTree.noBranch, example);
				}
			}
			else{
				if (example.strings[decisionTree.feature.featureNum].equals(decisionTree.feature.svalue)) {
					results = test(decisionTree.yesBranch, example);
				} else {
					results = test(decisionTree.noBranch, example);
				}
			}
			return results;
		}
		return results;
	}


}
