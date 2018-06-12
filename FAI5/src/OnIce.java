import java.util.*;

public class OnIce {

    public static final double GOLD_REWARD = 100.0;
    public static final double PIT_REWARD = -150.0;
    public static final double DISCOUNT_FACTOR = 0.5;
    public static final double EXPLORE_PROB = 0.2;  // for Q-learning
    public static final double LEARNING_RATE = 0.1;
    public static final int ITERATIONS = 10000;
    public static final int MAX_MOVES = 1000;

    // Using a fixed random seed so that the behavior is a little
    // more reproducible across runs & students
    public static Random rng = new Random(2018);

    public static void main(String[] args) {
        Scanner myScanner = new Scanner(System.in);
        Problem problem = new Problem(myScanner);
        Policy policy = problem.solve(ITERATIONS);
        if (policy == null) {
            System.err.println("No policy.  Invalid solution approach?");
        } else {
            System.out.println(policy);
        }
        if (args.length > 0 && args[0].equals("eval")) {
            System.out.println("Average utility per move: " 
                                + tryPolicy(policy, problem));
        }
    }

    public static class Problem {
        public String approach;
        public double[] moveProbs;
        public ArrayList<ArrayList<String>> map;

        // Format looks like
        // MDP    [approach to be used]
        // 0.7 0.2 0.1   [probability of going 1, 2, 3 spaces]
        // - - - - - - P - - - -   [space-delimited map rows]
        // - - G - - - - - P - -   [G is gold, P is pit]
        //
        // You can assume the maps are rectangular, although this isn't enforced
        // by this constructor.

        Problem (Scanner sc) {
            approach = sc.nextLine();
            String probsString = sc.nextLine();
            String[] probsStrings = probsString.split(" ");
            moveProbs = new double[probsStrings.length];
            for (int i = 0; i < probsStrings.length; i++) {
                try {
                    moveProbs[i] = Double.parseDouble(probsStrings[i]);
                } catch (NumberFormatException e) {
                    break;
                }
            }
            map = new ArrayList<ArrayList<String>>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] squares = line.split(" ");
                ArrayList<String> row = new ArrayList<String>(Arrays.asList(squares));
                map.add(row);
            }
        }

        Policy solve(int iterations) {
            if (approach.equals("MDP")) {
                MDPSolver mdp = new MDPSolver(this);
                return mdp.solve(this, iterations);
            } else if (approach.equals("Q")) {
                QLearner q = new QLearner(this);
                return q.solve(this, iterations);
            }
            return null;
        }

    }

    public static class Policy {
        public String[][] bestActions;

        public Policy(Problem prob) {
            bestActions = new String[prob.map.size()][prob.map.get(0).size()];
        }

        public String toString() {
            String out = "";
            for (int r = 0; r < bestActions.length; r++) {
                for (int c = 0; c < bestActions[0].length; c++) {
                    if (c != 0) {
                        out += " ";
                    }
                    out += bestActions[r][c];
                }
                out += "\n";
            }
            return out;
        }
    }

    public static class MDPSolver {
    
        // We'll want easy access to the real rewards while iterating, so
        // we'll keep both of these around
        public double[][] utilities;
        public double[][] rewards;

        public MDPSolver(Problem prob) {
            utilities = new double[prob.map.size()][prob.map.get(0).size()];
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            // Initialize utilities to the rewards in their spaces,
            // else 0
            for (int r = 0; r < utilities.length; r++) {
                for (int c = 0; c < utilities[0].length; c++) {
                    String spaceContents = prob.map.get(r).get(c);
                    if (spaceContents.equals("G")) {
                        utilities[r][c] = GOLD_REWARD;
                        rewards[r][c] = GOLD_REWARD;
                    } else if (spaceContents.equals("P")) {
                        utilities[r][c] = PIT_REWARD;
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        utilities[r][c] = 0.0;
                        rewards[r][c] = 0.0;
                    }
                }
            }
        }

        Policy solve(Problem prob, int iterations) {
            Policy policy = new Policy(prob);
            // TODO your code here & you'll probably want at least one helper function




			for (int z=0; z<iterations; z++){

				// Calculating utilities for every state
				for (int r = 0; r < utilities.length; r++) {
					for (int c = 0; c < utilities[0].length; c++) {
						if (!prob.map.get(r).get(c).equals("G") && !prob.map.get(r).get(c).equals("P")){

							// Calculating the direction with maximum benefit
							String[] direction = {"R","L","D","U"};
							String dir;
							HashMap<String, Double> scoreslist = new HashMap<>();

							int row=r, col = c;
							for (int d=0;d<direction.length;d++){
								double score=0;
								switch(direction[d]){
									case "R":
										for (int k=0; k<prob.moveProbs.length; k++){
											if (col != utilities[0].length-1) {
												col = c+ (k+1);
												if (col >= utilities[0].length) {
													col = utilities[0].length - (col - utilities[0].length) - 1;
													score += utilities[row][col] * prob.moveProbs[k];
												} else {
													score += utilities[row][col] * prob.moveProbs[k];
												}
											}
										}
										break;

									case "L":
										for (int k=0; k<prob.moveProbs.length; k++){
											if(col!=0) {
												col = c - (k + 1);

												if (col < 0) {
													col = Math.abs(col) - 1;
													score += utilities[row][col] * prob.moveProbs[k];
												} else {
													score += utilities[row][col] * prob.moveProbs[k];
												}
											}
										}
										break;

									case "D":
										for (int k=0; k<prob.moveProbs.length; k++){
											if(row != utilities.length) {
												row = r + (k + 1);

												if (row >= utilities.length) {
													row = utilities.length - (row - utilities.length) - 1;
													score += utilities[row][col] * prob.moveProbs[k];
												} else {
													score += utilities[row][col] * prob.moveProbs[k];
												}
											}
										}
										break;

									case "U":
										for (int k=0; k<prob.moveProbs.length; k++){
											if(row!=0) {
												row = r - (k + 1);

												if (row < 0) {
													row = Math.abs(row) - 1;
													score += utilities[row][col] * prob.moveProbs[k];
												} else {
													score += utilities[row][col] * prob.moveProbs[k];
												}
											}
										}
										break;
								}
								scoreslist.put(direction[d], score);
								row=r; col=c;
							}

							Map.Entry<String, Double> maxEntry = null;

							for (Map.Entry<String, Double> entry : scoreslist.entrySet())
							{
								if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
								{
									maxEntry = entry;
								}
							}

							utilities[r][c] = rewards[r][c] + DISCOUNT_FACTOR * maxEntry.getValue();
							policy.bestActions[r][c] = maxEntry.getKey();

						}
						else{
							policy.bestActions[r][c] = prob.map.get(r).get(c);
						}
					}
				}

			}
            return policy;
        }
    }

    // QLearner:  Same problem as MDP, but the agent doesn't know what the
    // world looks like, or what its actions do.  It can learn the utilities of
    // taking actions in particular states through experimentation, but it
    // has no way of realizing what the general action model is 
    // (like "Right" increasing the column number in general).
    public static class QLearner {

        // Use these to index into the first index of utilities[][][]
        public static final int UP = 0;
        public static final int RIGHT = 1;
        public static final int DOWN = 2;
        public static final int LEFT = 3;
        public static final int ACTIONS = 4;

        public double utilities[][][];  // utilities of actions
        public double rewards[][];
    
        public QLearner(Problem prob) {
            utilities = new double[ACTIONS][prob.map.size()][prob.map.get(0).size()];
            // Rewards are for convenience of lookup; the learner doesn't
            // actually "know" they're there until encountering them
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            for (int r = 0; r < rewards.length; r++) {
                for (int c = 0; c < rewards[0].length; c++) {
                    String locType = prob.map.get(r).get(c);
                    if (locType.equals("G")) {
                        rewards[r][c] = GOLD_REWARD;
                    } else if (locType.equals("P")) {
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        rewards[r][c] = 0.0; // not strictly necessary to init
                    }
                }
            }
            // Java: default init utilities to 0
        }

        public Policy solve(Problem prob, int iterations) {
            Policy policy = new Policy(prob);
            // TODO: your code here; probably wants at least one helper too
            return policy;
        }
    }



    // Returns the average utility per move of the policy,
	// as measured from ITERATIONS random drops of an agent onto
	// empty spaces
	public static double tryPolicy(Policy policy, Problem prob) {
		int totalUtility = 0;
		int totalMoves = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			// Random empty starting loc
			int row, col;
			do {
				row = rng.nextInt(prob.map.size());
				col = rng.nextInt(prob.map.get(0).size());
			} while (!prob.map.get(row).get(col).equals("-"));
			// Run until pit, gold, or MAX_MOVES timeout
			// (in case policy recommends driving into wall repeatedly,
			// for example)
			for (int moves = 0; moves < MAX_MOVES; moves++) {
				totalMoves++;
				String policyRec = policy.bestActions[row][col];
				// Determine how far we go in that direction
				int displacement = 1;
				double totalProb = 0;
				double moveSample = rng.nextDouble();
				for (int p = 0; p <= prob.moveProbs.length; p++) {
					totalProb += prob.moveProbs[p];
					if (moveSample <= totalProb) {
						displacement = p+1;
						break;
					}
				}
				int new_row = row;
				int new_col = col;
				if (policyRec.equals("U")) {
					new_row -= displacement;
					if (new_row < 0) {
						new_row = 0;
					}
				} else if (policyRec.equals("R")) {
					new_col += displacement;
					if (new_col >= prob.map.get(0).size()) {
						new_col = prob.map.get(0).size()-1;
					}
				} else if (policyRec.equals("D")) {
					new_row += displacement;
					if (new_row >= prob.map.size()) {
						new_row = prob.map.size()-1;
					}
				} else if (policyRec.equals("L")) {
					new_col -= displacement;
					if (new_col < 0) {
						new_col = 0;
					}
				}
				row = new_row;
				col = new_col;
				if (prob.map.get(row).get(col).equals("G")) {
					totalUtility += GOLD_REWARD;
					// End the current trial
					break;
				} else if (prob.map.get(row).get(col).equals("P")) {
					totalUtility += PIT_REWARD;
					break;
				}
			}
		}

		return totalUtility/(double)totalMoves;
	}



}
