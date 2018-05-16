// NAME OF THE STUDENT - PRATIK DEVIKAR

// Time required for executio:
// 1. Number of misplaced Tiles Heuristic : 46663487 nanosec
// 2. Manhattan Distance heuristic : 29655692 nanosec

// Answer for Q.9
//




//---------------------------------------------------------------------------------------------------------------------
import java.util.*;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move(Manhattan Distance)

public class NumberPuzzle2 implements Comparable<NumberPuzzle2>{
    public static final int PUZZLE_WIDTH = 4;       // A 4x4 puzzle box
    public static final int BLANK = 0;

    // BETTER:  false for tiles-displaced heuristic, true for Manhattan distance
    public static boolean BETTER = false;

    // Path from the starting point to the goal
    private LinkedList<NumberPuzzle2> path = new LinkedList<>();

    // A linked hash map to store the parent-child relationship in the resulting tree
    private LinkedHashMap<NumberPuzzle2, NumberPuzzle2> parent = new LinkedHashMap<>();

    // You can change this representation if you prefer.
    // If you don't, be careful about keeping the tiles and the blank
    // row and column consistent.
    private int[][] tiles;  // [row][column]
    private int blank_r, blank_c;   // blank row and column

    // OpenList and ClosedList
    private Queue<NumberPuzzle2> openList = new PriorityQueue<>();
    private ArrayList<NumberPuzzle2> closedList = new ArrayList<>();

    // F = Total cost
    // G = Cost so far
    // H = Heuristics cost
    private int costF, costG, costH;

    public static void main(String[] args)  {
        // Take input for the puzzle
        NumberPuzzle2 myPuzzle = readPuzzle();

        long startTime = System.nanoTime();

        // Solve the puzzle
        BETTER = true; // For Manhattan Distance heuristic
        LinkedList<NumberPuzzle2> solutionSteps = myPuzzle.solve(BETTER);

        // Print the solution steps
        printSteps(solutionSteps);

        long tt;
        tt = System.nanoTime() - startTime;
//        System.out.println("Time elapsed: "+tt+" nanosec");
    }

    private static NumberPuzzle2 goalNode() {
        NumberPuzzle2 newPuzzle = new NumberPuzzle2();
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

    NumberPuzzle2() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
    }

    static NumberPuzzle2 readPuzzle() {
        NumberPuzzle2 newPuzzle = new NumberPuzzle2();

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

    private static NumberPuzzle2 goal (){
        NumberPuzzle2 goalPuzzle = new NumberPuzzle2();
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

    public NumberPuzzle2 copy() {
        NumberPuzzle2 clone = new NumberPuzzle2();
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
    LinkedList<NumberPuzzle2> solve(boolean betterH)  {
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

    static void printSteps(LinkedList<NumberPuzzle2> steps) {
        NumberPuzzle2 s;
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

        NumberPuzzle2 goalNode = goal();
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
                    double temp = Math.sqrt(Math.pow((double)i-row, 2.0) + Math.pow((double)j-col, 2.0));
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
                NumberPuzzle2 currentNode;
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

        NumberPuzzle2 goalNode = goal();
        NumberPuzzle2 nextNode;
        nextNode = goalNode;
        // Initialize the path
        this.path.add(goalNode);

        // While the start node is not reached
        while(!equals(nextNode, this)) {
            // Use hashmap to find the parent of every node from goal state to the start
            for (Map.Entry<NumberPuzzle2, NumberPuzzle2> entry : this.parent.entrySet()) {
                if (equals(entry.getKey(), nextNode)) {
                    nextNode = entry.getValue();
                    this.path.add(nextNode);
                    break;
                }
            }
        }
    }

    private void generateAllTheNeighbours(NumberPuzzle2 currentNode, boolean betterH) {
        ArrayList<int[]> ListOfNeighbours;
        ListOfNeighbours = currentNode.locateAllNeighbours();
        int row, column;
        int temp_r, temp_c;
        int temp;

        for (int[] neighbours : ListOfNeighbours){
            // Generate a new neighbor puzzle
            NumberPuzzle2 neighborPuzzle;
            neighborPuzzle = currentNode.copy();
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
                for (NumberPuzzle2 ol: this.openList){
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
                for (NumberPuzzle2 ol: this.closedList){
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

    private boolean isAlreadyInClosedList(NumberPuzzle2 neighborPuzzle) {
        for (NumberPuzzle2 ol: this.closedList){
            if (equals(neighborPuzzle, ol)){
                return true;
            }
        }
        return false;
    }

    private boolean isAlreadyInOpenList(NumberPuzzle2 neighborPuzzle) {
        for (NumberPuzzle2 ol: this.openList){
            if (equals(neighborPuzzle, ol)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(NumberPuzzle2 np) {
        if (this.costF > np.costF)
            return 1;
        else if (this.costF < np.costF)
            return -1;
        else return 0;
    }

    private boolean equals(NumberPuzzle2 np1, NumberPuzzle2 np2){
        if (!(np1 instanceof NumberPuzzle2) && !(np2 instanceof NumberPuzzle2))
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
