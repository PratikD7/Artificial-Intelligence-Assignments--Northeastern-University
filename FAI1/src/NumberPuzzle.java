// FINAL SUBMISSION //


// NAME OF THE STUDENT - PRATIK DEVIKAR

// Time required for execution:
// 1. Number of misplaced Tiles Heuristic : 61,485,802 nanosec => 0.061 sec
// 2. Manhattan Distance heuristic : 44,533,710 nanosec => 0.044 sec

// Answer for Q.9
// Assuming we are now allowing diagonal moves,
// the Euclidean Distance heuristic is still admissible and consistent. (Because the shortest distance between any 2 tiles is given by Euclidean Distance
// and at each step we are getting close to our goal).
// Using Euclidean distance as the heuristic would definitely work.
// But there are some drawbacks:
// Since the Euclidean distance shows a straight line path from source to destination (not necessarily a diagonal path),
// and since Euclidean distance between any two points is smaller than or equal to Manhattan Distance, we will still
// get the shortest paths, but it will take longer time for execution because the square root function(in Euclidean distance) is quite expensive, computationally.

//---------------------------------------------------------------------------------------------------------------------
import java.util.*;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move(Manhattan Distance)

public class NumberPuzzle implements Comparable<NumberPuzzle>{
    public static final int PUZZLE_WIDTH = 4;       // A 4x4 puzzle box
    public static final int BLANK = 0;

    // BETTER:  false for tiles-displaced heuristic, true for Manhattan distance
    public static boolean BETTER = false;

    // Path from the starting point to the goal
    private LinkedList<NumberPuzzle> path = new LinkedList<>();

    // A linked hash map to store the parent-child relationship in the resulting tree
    private LinkedHashMap<NumberPuzzle, NumberPuzzle> parent = new LinkedHashMap<>();

    // You can change this representation if you prefer.
    // If you don't, be careful about keeping the tiles and the blank
    // row and column consistent.
    private int[][] tiles;  // [row][column]
    private int blank_r, blank_c;   // blank row and column

    // OpenList and ClosedList
    private Queue<NumberPuzzle> openList = new PriorityQueue<>();
    private ArrayList<NumberPuzzle> closedList = new ArrayList<>();

    // F = Total cost
    // G = Cost so far
    // H = Heuristics cost
    private int costF, costG, costH;

    public static void main(String[] args)  {
        // Take input for the puzzle
        NumberPuzzle myPuzzle = readPuzzle();

        long startTime = System.nanoTime();

        // Solve the puzzle
        BETTER = true; // True for Manhattan Distance heuristic, false for Misplaced Tiles Heuristic
        LinkedList<NumberPuzzle> solutionSteps = myPuzzle.solve(BETTER);

        // Print the solution steps
        printSteps(solutionSteps);

        long tt;
        tt = System.nanoTime() - startTime;
//        System.out.println("Time elapsed: "+tt+" nanosec");
    }

    private static NumberPuzzle goalNode() {
        NumberPuzzle newPuzzle = new NumberPuzzle();
        newPuzzle.blank_r = 3; newPuzzle.blank_c = 3;

        newPuzzle.tiles[0][0] = 1;
        newPuzzle.tiles[0][1] = 2;
        newPuzzle.tiles[0][2] = 3;
        newPuzzle.tiles[0][3] = 4;
        newPuzzle.tiles[1][0] = 5;
        newPuzzle.tiles[1][1] = 6;
        newPuzzle.tiles[1][2] = 7;
        newPuzzle.tiles[1][3] = 8;
        newPuzzle.tiles[2][0] = 9;
        newPuzzle.tiles[2][1] = 10;
        newPuzzle.tiles[2][2] = 11;
        newPuzzle.tiles[2][3] = 12;
        newPuzzle.tiles[3][0] = 13;
        newPuzzle.tiles[3][1] = 14;
        newPuzzle.tiles[3][2] = 15;

        return newPuzzle;
    }

    NumberPuzzle() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
    }

    static NumberPuzzle readPuzzle() {
        NumberPuzzle newPuzzle = new NumberPuzzle();

        Scanner myScanner = new Scanner(System.in);
        int row = 0;
        while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
            String line = myScanner.nextLine();
            String[] numStrings = line.split(" ");
            for (int i = 0; i < PUZZLE_WIDTH; i++) {
                if (numStrings[i].equals("-")) {
                    newPuzzle.tiles[row][i] = BLANK;
                    newPuzzle.blank_r = row;
                    newPuzzle.blank_c = i;
                } else {
                    newPuzzle.tiles[row][i] = new Integer(numStrings[i]);
                }
            }
            row++;
        }
        return newPuzzle;
    }

    private static NumberPuzzle goal (){
        NumberPuzzle goalPuzzle = new NumberPuzzle();
        goalPuzzle.blank_r = PUZZLE_WIDTH-1;
        goalPuzzle.blank_c = PUZZLE_WIDTH-1;

        goalPuzzle.tiles[0][0] = 1;
        goalPuzzle.tiles[0][1] = 2;
        goalPuzzle.tiles[0][2] = 3;
        goalPuzzle.tiles[0][3] = 4;
        goalPuzzle.tiles[1][0] = 5;
        goalPuzzle.tiles[1][1] = 6;
        goalPuzzle.tiles[1][2] = 7;
        goalPuzzle.tiles[1][3] = 8;
        goalPuzzle.tiles[2][0] = 9;
        goalPuzzle.tiles[2][1] = 10;
        goalPuzzle.tiles[2][2] = 11;
        goalPuzzle.tiles[2][3] = 12;
        goalPuzzle.tiles[3][0] = 13;
        goalPuzzle.tiles[3][1] = 14;
        goalPuzzle.tiles[3][2] = 15;
        goalPuzzle.tiles[3][3] = 0;

        return goalPuzzle;
    }

    public String toString() {
        String out = "";
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (j > 0) {
                    out += " ";
                }
                if (tiles[i][j] == BLANK) {
                    out += "-";
                } else {
                    out += tiles[i][j];
                }
            }
            out += "\n";
        }
        return out;
    }

    public NumberPuzzle copy() {
        NumberPuzzle clone = new NumberPuzzle();
        clone.blank_r = blank_r;
        clone.blank_c = blank_c;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                clone.tiles[i][j] = this.tiles[i][j];
            }
        }
        return clone;
    }

    // betterH:  if false, use tiles-out-of-place heuristic
    //           if true, use total-manhattan-distance heuristic
    LinkedList<NumberPuzzle> solve(boolean betterH)  {
        // Solve the puzzle using A* search algorithm and return the path
        this.aStarAlgorithm(betterH);
        return this.path;
    }

    public boolean solved() {
        int shouldBe = 1;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (tiles[i][j] != shouldBe) {
                    return false;
                } else {
                    // Take advantage of BLANK == 0
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
        }
        return true;
    }

    static void printSteps(LinkedList<NumberPuzzle> steps) {
        NumberPuzzle s;
        while(steps.size()!=0){
            s = steps.getLast();
            System.out.println(s);
            steps.remove(s);
        }
    }

    // Calculate the number of misplaced tiles of a particular state of the puzzle
    private int misplacedTilesHeuristic(){
        int numberOfMisplacedTiles = 0;
        int correctTileNumber = 1;

        for(int i=0;i<PUZZLE_WIDTH;i++){
            for(int j=0;j<PUZZLE_WIDTH;j++){
                if (this.tiles[i][j] != correctTileNumber && this.tiles[i][j]!=0) {
                    numberOfMisplacedTiles++;
                }
                correctTileNumber++;
            }
        }
        return numberOfMisplacedTiles;
    }

    // Calculate the Manhattan Distance of a particular state of a puzzle
    private int manhattanDistanceHeuristic(){
        int sumOfManhattanDistance = 0;

        NumberPuzzle goalNode = goal();
        // Dictionary to store the positions of true values
        LinkedHashMap<Integer, int[]> tileMap = new LinkedHashMap<>();
        for(int i=0;i<PUZZLE_WIDTH;i++){
            for(int j=0;j<PUZZLE_WIDTH;j++){
                tileMap.put(goalNode.tiles[i][j], new int[]{i, j});
                }
            }

        // Calculating the Manhattan Distance between the tile's actual vs goal positions
        for(int i=0;i<PUZZLE_WIDTH;i++){
            for(int j=0;j<PUZZLE_WIDTH;j++){
                if (this.tiles[i][j]!=0 && this.tiles[i][j] != goalNode.tiles[i][j]) {
                    int row, col;
                    // Row and column of the actual tiles
                    row = tileMap.get(this.tiles[i][j])[0];
                    col = tileMap.get(this.tiles[i][j])[1];
                    sumOfManhattanDistance = sumOfManhattanDistance + Math.abs(i-row) + Math.abs(j-col);
                }
            }
        }
        return sumOfManhattanDistance;
    }

    // Locates the coordinates of all the possible neighbours for a particular state of the puzzle
    private ArrayList<int[]> locateAllNeighbours (){
        ArrayList<int[]> listOfNeighboursCoords = new ArrayList<>();
        int row, column;


        // List of all the neighbours of the blank cell if they exist
        //1.
        row = blank_r - 1;
        column = blank_c;
        if (0<=row && row<4){
            listOfNeighboursCoords.add(new int[]{row, column});
        }

        //2.
        row = blank_r + 1;
        column = blank_c;
        if (0<=row && row<4){
            listOfNeighboursCoords.add(new int[]{row, column});
        }

        //3.
        row = blank_r;
        column = blank_c - 1;
        if (0<=column && column<4){
            listOfNeighboursCoords.add(new int[]{row, column});
        }

        //4.
        row = blank_r;
        column = blank_c + 1;
        if (0<=column && column<4){
            listOfNeighboursCoords.add(new int[]{row, column});
        }

    return listOfNeighboursCoords;
    }

    private void initializePriorityQueue(){
        this.openList.add(this);
    }

    private void  aStarAlgorithm(boolean betterH)  {

        // INITIALIZE THE COSTS
        // Manhattan Distance
        if (betterH){
            this.costH = this.manhattanDistanceHeuristic();
            this.costG = 0;
            this.costF = this.costG + this.costH;
        }

        // Number of misplaced tiles
        else {
            // Defining and initializing the cost variables
            this.costH = this.misplacedTilesHeuristic();
            this.costG = 0;
            this.costF = this.costG + this.costH;
        }

        // Initialize the priority queue(openlist) at the beginning
        this.initializePriorityQueue();

        if (this.openList.size() != 0) {
            while (this.openList.size() != 0) {
                NumberPuzzle currentNode;
                // Extract the "best" node from the openlist (priority queue)
                currentNode = this.openList.poll();
                // Add it to the closed list
                this.closedList.add(currentNode);
                // If we find the goal state, terminate and return the path
                if (currentNode.solved()) {
                    this.pathFromStartToGoal();
                    return;
                } else {
                    //Generate all the neighbours of the current node
                    generateAllTheNeighbours(currentNode, betterH);
                }
            }
        }
        else
            System.out.println("There's no solution to this input!");
    }

    private void pathFromStartToGoal() {
        // It stores the path from goal node to the start point

        NumberPuzzle goalNode = goal();
        NumberPuzzle nextNode;
        nextNode = goalNode;
        // Initialize the path
        this.path.add(goalNode);

        // While the start node is not reached
        while(!equals(nextNode, this)) {
            // Use hashmap to find the parent of every node from goal state to the start
            for (Map.Entry<NumberPuzzle, NumberPuzzle> entry : this.parent.entrySet()) {
                if (equals(entry.getKey(), nextNode)) {
                    nextNode = entry.getValue();
                    this.path.add(nextNode);
                    break;
                }
            }
        }
    }

    private void generateAllTheNeighbours(NumberPuzzle currentNode, boolean betterH) {
        ArrayList<int[]> ListOfNeighbours;
        ListOfNeighbours = currentNode.locateAllNeighbours();
        int row, column;
        int temp_r, temp_c;
        int temp;

        for (int[] neighbours : ListOfNeighbours){
            // Generate a new neighbor puzzle
            NumberPuzzle neighborPuzzle;
            neighborPuzzle = currentNode.copy();

            // Swap the blank places with appropriate numbers
            row = neighbours[0]; column = neighbours[1];
            temp_r = neighborPuzzle.blank_r;
            temp_c = neighborPuzzle.blank_c;
            temp = neighborPuzzle.tiles[row][column];
            neighborPuzzle.blank_r = row;
            neighborPuzzle.blank_c = column;
            neighborPuzzle.tiles[neighborPuzzle.blank_r][neighborPuzzle.blank_c] = 0;
            neighborPuzzle.tiles[temp_r][temp_c] = temp;

            //Cost value of the neighbors
            if (betterH){
                neighborPuzzle.costG = currentNode.costG + 1;
                neighborPuzzle.costH = neighborPuzzle.manhattanDistanceHeuristic();
                neighborPuzzle.costF = neighborPuzzle.costG + neighborPuzzle.costH;
            }
            else{
                neighborPuzzle.costG = currentNode.costG + 1;
                neighborPuzzle.costH = neighborPuzzle.misplacedTilesHeuristic();
                neighborPuzzle.costF = neighborPuzzle.costG + neighborPuzzle.costH;
            }


            // If the neighbor is in the openlist
            if (isAlreadyInOpenList(neighborPuzzle)){
                for (NumberPuzzle ol: this.openList){
                    if (equals(neighborPuzzle, ol)){
                        // Update the neighbor's parent if the cost required is less than its previous cost
                        if (neighborPuzzle.costG < ol.costG){
                            parent.put(neighborPuzzle, currentNode);
                            ol.costG = neighborPuzzle.costG;
                        }
                        break;
                    }
                }
            }
            // If the neighbor is in the closedlist
            else if (isAlreadyInClosedList(neighborPuzzle)){
                for (NumberPuzzle ol: this.closedList){
                    if (equals(neighborPuzzle, ol)){
                        // Update the neighbor's parent if the cost required is less than its previous cost
                        // Also put the neighbor from closed list to open list
                        if (neighborPuzzle.costG < ol.costG){
                            this.closedList.remove(neighborPuzzle);
                            this.openList.add(neighborPuzzle);
                            parent.put(neighborPuzzle, currentNode);
                            ol.costG = neighborPuzzle.costG;
                        }
                        break;
                    }
                }
            }
            else {
                this.openList.add(neighborPuzzle);
                parent.put(neighborPuzzle, currentNode);
            }
        }
    }

    private boolean isAlreadyInClosedList(NumberPuzzle neighborPuzzle) {
        for (NumberPuzzle ol: this.closedList){
            if (equals(neighborPuzzle, ol)){
                return true;
            }
        }
        return false;
    }

    private boolean isAlreadyInOpenList(NumberPuzzle neighborPuzzle) {
        for (NumberPuzzle ol: this.openList){
            if (equals(neighborPuzzle, ol)){
                   return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(NumberPuzzle np) {
        if (this.costF > np.costF)
            return 1;
        else if (this.costF < np.costF)
            return -1;
        else return 0;
    }

    private boolean equals(NumberPuzzle np1, NumberPuzzle np2){
        if (!(np1 instanceof NumberPuzzle) && !(np2 instanceof NumberPuzzle))
            return false;
        boolean flag = true;

        for(int i=0;i<PUZZLE_WIDTH;i++){
            for(int j=0;j<PUZZLE_WIDTH;j++){
                if (np1.tiles[i][j] != np2.tiles[i][j])
                    flag = false;
            }
        }
        return flag && np1.blank_c == (np2.blank_c) && np1.blank_r == (np2.blank_r);
    }

}
