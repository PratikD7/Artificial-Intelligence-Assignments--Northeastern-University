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
											if(row != utilities.length - 1) {
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

	        for (int r = 0; r < rewards.length; r++) {
		        for (int c = 0; c < rewards[0].length; c++) {
					if (prob.map.get(r).get(c).equals("G") || prob.map.get(r).get(c).equals("P")){
						policy.bestActions[r][c] = prob.map.get(r).get(c);
					}
		        }
	        }



	        for (int z=0; z<ITERATIONS; z++) {
		        System.out.println(policy.toString());
		        int prev_row=-1, prev_col=-1;
		        System.out.println(z);
		        int rnd_row, rnd_col;
		        rnd_row = rng.nextInt(prob.map.size() - 1);
		        rnd_col = rng.nextInt(prob.map.get(0).size()-1);
		        int currentrow, currentcol;
		        currentrow = rnd_row;
		        currentcol = rnd_col;
				boolean wall=false;
		        boolean gameOver = false;

		        while (!gameOver) {

			        int indexOfNextStateSlipping;
			        double[] array = new double[prob.moveProbs.length+1];
			        array[0] = 0.0;
			        for (int y=0;y<prob.moveProbs.length;y++){
			        	array[y+1] = prob.moveProbs[y];
			        }
			        double[] mP = Sum(prob.moveProbs);
			        double rd = rng.nextDouble();
			        indexOfNextStateSlipping = nearestNumber(mP, rd);


		        	if (prev_col == currentcol && prev_row == currentrow){
//				        z -= 1;
		        		break;
			        }

			        if (prob.map.get(rnd_row).get(rnd_col).equals("G") || prob.map.get(rnd_row).get(rnd_col).equals("P")) {
				        if (prob.map.get(rnd_row).get(rnd_col).equals("G")) {
					        for (int k = 0; k < ACTIONS; k++) {
						        utilities[k][rnd_row][rnd_col] = GOLD_REWARD;
					        }
//					        z -= 1;
				        }
				        else{
					        for (int k = 0; k < ACTIONS; k++) {
						        utilities[k][rnd_row][rnd_col] = GOLD_REWARD;
					        }
//					        z -= 1;
				        }
				        break;
			        }

			        // If the starting position is NOT a Pit or Gold
			        if (!prob.map.get(rnd_row).get(rnd_col).equals("G") && !prob.map.get(rnd_row).get(rnd_col).equals("P")) {

				        // Random direction
				        if (rng.nextDouble() < EXPLORE_PROB) {
					        int rnd_dir, row, col;
					        rnd_dir = rng.nextInt(4);
					        String[] directions = {"R", "D", "L", "U"};
//					        String[] directions = {"U", "R", "D", "L"};
					        row = rnd_row;
					        col = rnd_col;
					        double scoreQ = 0.0;
					        switch (directions[rnd_dir]) {

						        case "U":
//							        if (row != 0){
								        currentrow = rnd_row - (indexOfNextStateSlipping+1);  ////????/
								        if (currentrow < 0){
								        	wall = true;
									        currentrow = Math.abs(currentrow) - 1;
								        }
//							        }
//							        for (int k = 0; k < prob.moveProbs.length; k++) {///???????????????

//								        if (row != 0) {
							        row = row - (indexOfNextStateSlipping + 1);
							        if (row < 0) {
								        row = Math.abs(row) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (row < 0) {
//											        row = Math.abs(row) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
										            max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;

						        case "R":
//						        	if (col != rewards[0].length - 1){
						        		currentcol = rnd_col + (indexOfNextStateSlipping+1);
						        		if (currentcol >= rewards[0].length){
									        wall = true;
									        currentcol = rewards[0].length - (currentcol - rewards[0].length) - 1;
								        }
//							        }

//							        for (int k = 0; k < prob.moveProbs.length; k++) {
//								        if (col != utilities[0].length - 1) {
							        col = col + (indexOfNextStateSlipping + 1);
							        if (col >= rewards[0].length) {
								        col = rewards[0].length - (col - rewards[0].length) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
										scoreQ += GOLD_REWARD;
										gameOver = true;
							        }
							        else if (prob.map.get(row).get(col).equals("P")){
							            scoreQ += PIT_REWARD;
								        gameOver = true;
							        }
							        else {
//									        if (col >= rewards[0].length) {
//										        col = rewards[0].length - (col - rewards[0].length) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;


						        case "D":
//						        	if (row != rewards.length - 1){
						        		currentrow = rnd_row + (indexOfNextStateSlipping+1);
						        		if (currentrow >= rewards.length){
									        wall = true;
									        currentrow = rewards.length - (currentrow - rewards.length) - 1;
								        }
//							        }


//							        for (int k = 0; k < prob.moveProbs.length; k++) {
//								        if (row != rewards.length - 1) {
							        row = row + (indexOfNextStateSlipping + 1);
							        if (row >= rewards.length) {
								        row = rewards.length - (row - rewards.length) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//										        if (row >= rewards.length) {
//											        row = rewards.length - (row - rewards.length) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//										        } else {
//											        int d = 0;
//											        double max = Double.NEGATIVE_INFINITY;
//											        for (int i = 0; i < ACTIONS; i++) {
//												        if (utilities[i][row][col] > max) {
//													        max = utilities[i][row][col];
//													        d = i;
//												        }
//											        }
//											        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//										        }
							        }
//								        }
//							        }
							        break;

						        case "L":
//							        if (col!=0){
								        currentcol = rnd_col - (indexOfNextStateSlipping+1);
								        if (currentcol < 0){
									        wall = true;
									        currentcol = Math.abs(currentcol) - 1;
								        }
//							        }

//							        for (int k = 0;/ k < prob.moveProbs.length; k++) {
//								        if (col != 0) {

							        col = col - (indexOfNextStateSlipping + 1);
							        if (col < 0) {
								        col = Math.abs(col) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (col < 0) {
//										        col = Math.abs(col) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;

					        }
					        if(!wall) {

							        utilities[rnd_dir][rnd_row][rnd_col] += LEARNING_RATE * (rewards[rnd_row][rnd_col] + DISCOUNT_FACTOR * (scoreQ) - utilities[rnd_dir][rnd_row][rnd_col]);
						        for (int h=1;h<ACTIONS;h++){
							        if (utilities[h][rnd_row][rnd_col] > utilities[h-1][rnd_row][rnd_col]){
								        policy.bestActions[rnd_row][rnd_col] = directions[h];
							        }
							        else{
								        policy.bestActions[rnd_row][rnd_col] = directions[0];
							        }
						        }
//							        policy.bestActions[rnd_row][rnd_col] = directions[d]; //????????? Select best actions based on maximum valued direction
							        prev_row = rnd_row;
							        prev_col = rnd_col;
							        rnd_col = currentcol;
							        rnd_row = currentrow;

					        }
					        else{
						        prev_row = -1;
						        prev_col = -1;
						        rnd_row = rng.nextInt(prob.map.size() - 1);
						        rnd_col = rng.nextInt(prob.map.get(0).size()-1);
//
					        }
				        }

				        // OR move according to the best Q value of its current position
				        else {
					        int row, col;
					        row = rnd_row;
					        col = rnd_col;
					        int direction = 0;
							double maxi = Double.NEGATIVE_INFINITY;

					        for (int i = 0; i < ACTIONS; i++) {
						        if (utilities[i][row][col] > maxi) {
						        	maxi = utilities[i][row][col];
							        direction = i;
						        }
					        }
					        double scoreQ = 0.0;
					        String[] directions = {"R", "D", "L", "U"};
//					        String[] directions = {"U", "R", "D", "L"};

					        switch (directions[direction]) {

						        case "U":
//							        if (row != 0) {
							        currentrow = rnd_row - (indexOfNextStateSlipping+1);
							        if (currentrow < 0){
								        wall = true;
								        currentrow = Math.abs(currentrow) - 1;
							        }
//							        }
//							        for (int k = 0; k < prob.moveProbs.length; k++) {

//								        if (row != 0) {
							        row = row - (indexOfNextStateSlipping + 1);
							        if (row < 0) {
								        row = Math.abs(row) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (row < 0) {
//										        row = Math.abs(row) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col] ;
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;

						        case "R":
//							        if (col != rewards[0].length - 1) {
							        currentcol = rnd_col + (indexOfNextStateSlipping+1);
							        if (currentcol >= rewards[0].length){
								        wall = true;
								        currentcol = rewards[0].length - (currentcol - rewards[0].length) - 1;
							        }
//							        }
//							        for (int k = 0; k < prob.moveProbs.length; k++) {
//								        if (col != rewards[0].length - 1) {

							        col = col + (indexOfNextStateSlipping + 1);
							        if (col >= rewards[0].length) {
								        col = rewards[0].length - (col - rewards[0].length) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (col >= rewards[0].length) {
//										        col = rewards[0].length - (col - rewards[0].length) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }

//							        }
							        break;

						        case "D":
//							        if (row != rewards.length - 1){
							        currentrow = rnd_row + (indexOfNextStateSlipping+1);
							        if (currentrow >= rewards.length){
								        wall = true;
								        currentrow = rewards.length - (currentrow - rewards.length) - 1;
							        }
//							        }
//							        for (int k = 0; k < prob.moveProbs.length; k++) {
//								        if (row != rewards.length - 1) {

							        row = row + (indexOfNextStateSlipping + 1);
							        if (row >= rewards.length) {
								        row = rewards.length - (row - rewards.length) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (row >= rewards.length) {
//										        row = rewards.length - (row - rewards.length) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col] ;
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;


						        case "L":
//							        if (col != 0) {
							        currentcol = rnd_col - (indexOfNextStateSlipping+1);
							        if (currentcol < 0){
								        wall = true;
								        currentcol = Math.abs(currentcol) - 1;
							        }
//							        }
//							        for (int k = 0; k < prob.moveProbs.length; k++) {
//								        if (col != 0) {

							        col = col - (indexOfNextStateSlipping + 1);
							        if (col < 0) {
								        col = Math.abs(col) - 1;
							        }
							        if (prob.map.get(row).get(col).equals("G")) {
								        scoreQ += GOLD_REWARD;
								        gameOver = true;
							        } else if (prob.map.get(row).get(col).equals("P")) {
								        scoreQ += PIT_REWARD;
								        gameOver = true;
							        } else {
//									        if (col < 0) {
//										        col = Math.abs(col) - 1;
									        int d = 0;
									        double max = Double.NEGATIVE_INFINITY;
									        for (int i = 0; i < ACTIONS; i++) {
										        if (utilities[i][row][col] > max) {
											        max = utilities[i][row][col];
											        d = i;
										        }
									        }
									        scoreQ += utilities[d][row][col];
//									        } else {
//										        int d = 0;
//										        double max = Double.NEGATIVE_INFINITY;
//										        for (int i = 0; i < ACTIONS; i++) {
//											        if (utilities[i][row][col] > max) {
//												        max = utilities[i][row][col];
//												        d = i;
//											        }
//										        }
//										        scoreQ += utilities[d][row][col] * prob.moveProbs[k];
//									        }
							        }
//								        }
//							        }
							        break;

					        }
//					        if (prev_row == currentrow && prev_col == currentcol){
//						        break;
//					        }
					        if (!wall) {
						        utilities[direction][rnd_row][rnd_col] += LEARNING_RATE * (rewards[rnd_row][rnd_col] + DISCOUNT_FACTOR * (scoreQ) - utilities[direction][rnd_row][rnd_col]);
						        for (int h=1;h<ACTIONS;h++){
						        	if (utilities[h][rnd_row][rnd_col] > utilities[h-1][rnd_row][rnd_col]){
								        policy.bestActions[rnd_row][rnd_col] = directions[h];
							        }
						        }
						         //Select best actions based on maximum valued direction
						        prev_row = rnd_row;
						        prev_col = rnd_col;
						        rnd_col = currentcol;
						        rnd_row = currentrow;
					        }
					        else{
						        prev_row = -1;
						        prev_col = -1;
						        rnd_row = rng.nextInt(prob.map.size() - 1);
						        rnd_col = rng.nextInt(prob.map.get(0).size()-1);
					        }
				        }
			        }
			        // If the starting position is a Pit or Gold
			        else {
//				        z -= 1;
				        gameOver = true;
			        }
		        }
	        }







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

	public static double[] Sum(double[] in) {
		double[] out = new double[in.length];
		double total = 0;
		for (int i = 1; i < in.length; i++) {
			total += in[i];
			out[i] = total;
		}
		return out;
	}

	public static int nearestNumber(double[] mP, double myNumber){
//		double distance = Math.abs(mP[0] - myNumber);
		int idx = 0;
		for(int c = 0; c < mP.length-1; c++){
			if (myNumber<mP[c+1] && myNumber>=mP[c]){
				idx = c;
				return idx;

			}
		}
		return idx;
	}

}
