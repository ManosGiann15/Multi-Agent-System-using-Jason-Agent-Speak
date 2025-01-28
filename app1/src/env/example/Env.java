import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;


public class Env extends Environment {
    public static final int GRID_SIZE = 10; // Define grid size (10x10)
    static Logger logger = Logger.getLogger(Env.class.getName());

    // Define constants for different garbage colors and agent
    public static final int GARBAGE = 3;

    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");

    private EnvModel model;
    private EnvView view;

    private int moveCount = 0;
    private double totalCost = 0.0;
    private double agent1Points = 0.0;
    private double agent2Points = 0.0;  
    private double totalAgentPoints = 0.0;


    public void init(String[] args) {
        model = new EnvModel(GRID_SIZE, GRID_SIZE);
        view = new EnvView(model);
        try {
            model.setView(view);
        } catch (Exception e) {}
        updatePercepts();
    }


    @Override
    public boolean executeAction(String ag, Structure action) {
        try {
            if (action.equals(ns)) {
                // Move to next slot
                model.nextSlot();
                informAgsEnvironmentChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Stop after 31 moves
        if (moveCount >= 31) {
            logger.info("Agent has completed 31 moves.");
            return false;  // End the simulation
        }

        

        try {
            Thread.sleep(500); // Simulate delay for the action
        } catch (Exception e) {}
        // updatePercepts();
        informAgsEnvironmentChanged();
        return true;
    }


    // Helper to map slave agent name to an internal ID
    private int getAgentId(String agentName) {
        if (agentName.equals("bob")) {
            // Logic to distinguish between bob and alice under the slave agent
            // Example: Assign IDs based on position or specific characteristics
            return 0; // Placeholder logic for bob
        }
        return -1; // Unknown agent
    }



    

    // Define the Garbage class
    class Garbage {
        int x, y;
        Color color;
        double reward;

        public Garbage(int x, int y, Color color, double reward) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.reward = reward;
        }

        private Garbage(double reward) {
            this.reward = reward;
        }
    }

    class Node {
        int x, y;
        double gCost, hCost, fCost;
        Node parent;
    
        public Node(int x, int y) {
            this.x = x;
            this.y = y;
            this.gCost = Double.MAX_VALUE; // Initially, the gCost is infinite
            this.hCost = 0;
            this.fCost = Double.MAX_VALUE; // fCost = gCost + hCost
            this.parent = null;
        }
    
        // Calculate heuristic cost (Manhattan distance)
        public void setHCost(Node goal) {
            this.hCost = Math.abs(this.x - goal.x) + Math.abs(this.y - goal.y);
        }
    
        // Update fCost
        public void calculateFCost() {
            this.fCost = this.gCost + this.hCost;
        }
    }
    

    class AStar {
        private static final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}}; // Right, Down, Left, Up
        private EnvModel model;

        public AStar(EnvModel model) {
            this.model = model;
        }

        public List<Node> findPath(Location start, Location goal) {
            PriorityQueue<Node> openList = new PriorityQueue<>((a, b) -> Double.compare(a.fCost, b.fCost));
            List<Node> closedList = new ArrayList<>();
            
            Node startNode = new Node(start.x, start.y);
            Node goalNode = new Node(goal.x, goal.y);
            startNode.gCost = 0;
            startNode.setHCost(goalNode);
            startNode.calculateFCost();
            openList.add(startNode);

            while (!openList.isEmpty()) {
                Node currentNode = openList.poll();

                if (currentNode.x == goalNode.x && currentNode.y == goalNode.y) {
                    return reconstructPath(currentNode); // Return the path if goal is reached
                }

                closedList.add(currentNode);

                for (int[] direction : DIRECTIONS) {
                    int newX = currentNode.x + direction[0];
                    int newY = currentNode.y + direction[1];
                    if (model.isWall(newX, newY)) continue; // Skip walls

                    Node neighbor = new Node(newX, newY);
                    if (closedList.contains(neighbor)) continue;

                    double tentativeGCost = currentNode.gCost + 0.01; // Cost for each move

                    if (!openList.contains(neighbor)) {
                        neighbor.gCost = tentativeGCost;
                        neighbor.setHCost(goalNode); // Set heuristic distance to the goal
                        neighbor.calculateFCost();
                        neighbor.parent = currentNode;
                        openList.add(neighbor);
                    }
                }
            }

            return new ArrayList<>(); // Return an empty list if no path found
        }

        private List<Node> reconstructPath(Node goalNode) {
            List<Node> path = new ArrayList<>();
            Node current = goalNode;
            while (current != null) {
                path.add(0, current); // Add to the front of the list
                current = current.parent;
            }
            return path;
        }
    }

    class ManagerAgent {
        private final Map<Garbage, Integer> assignedGarbage = new HashMap<>();
    
        public void receiveTarget(String agentName, Garbage garbage, List<Node> path, Location location) {
            System.out.println(agentName + " sent target: " + garbage.reward + ", Location = (" + garbage.x + ", " + garbage.y + ")");
            // You can extend this to include more logic if needed
        }
    
        public void resolveConflict(Garbage target, Location loc1, Location loc2) {
            int dist1 = Math.abs(target.x - loc1.x) + Math.abs(target.y - loc1.y); // Manhattan distance for Agent 1
            int dist2 = Math.abs(target.x - loc2.x) + Math.abs(target.y - loc2.y); // Manhattan distance for Agent 2
    
            if (dist1 <= dist2) {
                assignedGarbage.put(target, 0); // Assign to Agent 1
                System.out.println("Manager assigned garbage (" + target.x + "," + target.y + ") to Agent 1");
            } else {
                assignedGarbage.put(target, 1); // Assign to Agent 2
                System.out.println("Manager assigned garbage (" + target.x + "," + target.y + ") to Agent 2");
            }
        }
    
        public boolean isGarbageAssigned(Garbage garbage) {
            return assignedGarbage.containsKey(garbage);
        }
    
        public int getAssignedAgent(Garbage garbage) {
            return assignedGarbage.getOrDefault(garbage, -1);
        }
    
        public void clearGarbageAssignment(Garbage garbage) {
            assignedGarbage.remove(garbage);
        }
    }
    
    


    void updatePercepts() {
        clearPercepts();
    
        // Update positions of agents under slave
        Location bobLoc = model.getAgPos(0);
        Location aliceLoc = model.getAgPos(1);
        addPercept("slave", Literal.parseLiteral("pos(bob," + bobLoc.x + "," + bobLoc.y + ")"));
        addPercept("slave", Literal.parseLiteral("pos(alice," + aliceLoc.x + "," + aliceLoc.y + ")"));
    
        // Update garbage percepts
        for (Garbage g : model.getGarbageList()) {
            addPercept("slave", Literal.parseLiteral("garbage(" + g.x + "," + g.y + "," + g.reward + ")"));
        }
    }
    
    
    

    class EnvModel extends GridWorldModel {
        private List<Garbage> garbageList = new ArrayList<>();
        private List<Garbage> targetGarbageList = new ArrayList<>();    // Garbage already taken by agents
        Set<Location> reservedPositions = new HashSet<>();
        List<Node> global_path1 = null;
        List<Node> global_path2 = null;
        private double agent1Points = 0.0;
        private double agent2Points = 0.0;
        private double totalPoints = 0.0;

        
        private ManagerAgent manager = new ManagerAgent();

        
        private boolean isWaiting = false; // Flag to indicate waiting state

        
    
        public EnvModel(int width, int height) {
            super(width, height, 3); // Initialize with 1 agent
            
            // Create and place garbage objects with different properties
            garbageList.add(new Garbage(3, 2, Color.GREEN, 0.4));
            garbageList.add(new Garbage(4, 1, Color.YELLOW, 0.2));
            garbageList.add(new Garbage(7, 6, Color.PINK, 0.8));
            garbageList.add(new Garbage(8, 6, Color.BLUE, 0.6));

            
            
    
            for (Garbage g : garbageList) {
                add(GARBAGE, g.x, g.y); // Use GARBAGE as the identifier for garbage items
            }


            // Add walls to the top edge
            addWall(0, 0, 9, 0); 
            // Add walls to the left edge
            addWall(0, 0, 0, 9); 
            // Add walls to the bottom edge
            addWall(0, 9, 9, 9);
            // Add walls to the right edge
            addWall(9, 0, 9, 9);
            addWall(2, 8, 2, 8);
            addWall(5, 6, 5, 6);
            addWall(7, 4, 7, 4);
            addWall(7, 3, 7, 3);
            

            // setAgPos(0, 1, 6); //Presentation starting position
            placeAgentRandomly(0);
            placeAgentRandomly(1);
        }

        public void removeGarbage(Garbage garbage) {
            targetGarbageList.remove(garbage);
            remove(GARBAGE, garbage.x, garbage.y); // Remove from the grid
            view.repaint();
        }
        
        public void addGarbage(Garbage garbage) {
            garbageList.add(garbage);
            add(GARBAGE, garbage.x, garbage.y); // Add to the grid
            view.repaint();
        
        }

        public void placeAgentRandomly(int ag) {
            Random rand = new Random();
            int agentX, agentY;
            do {
                agentX = rand.nextInt(10); // Random X between 0 and 9
                agentY = rand.nextInt(10); // Random Y between 0 and 9
            } while (!isFree(agentX, agentY));
            
            setAgPos(ag, agentX, agentY); // Add the agent to the grid
        }


        private void respawnGarbage(Garbage garbage) {
            boolean validPosition = false;
            int newX = 0, newY = 0;
        
            while (!validPosition) {
                newX = (int) (Math.random() * GRID_SIZE);
                newY = (int) (Math.random() * GRID_SIZE);
        
                if (!model.isWall(newX, newY) && !model.hasObject(GARBAGE, newX, newY)) {
                    validPosition = true;
                }
            }
        
            Garbage newGarbage = new Garbage(newX, newY, garbage.color, garbage.reward);
            garbageList.add(newGarbage); // Add to list
            model.add(GARBAGE, newGarbage.x, newGarbage.y); // Add to grid
        }
        

        
        

        public List<Node> findSecondBestGarbage(int ag, Location agentLoc) {
            // A* initialization
            AStar star = new AStar(this);
            List<Node> bestPath = null;
            List<Node> secondBestPath = null;
        
            // Variables to track the best and second-best garbage
            Garbage bestGarbage = null;
            Garbage secondBestGarbage = null;
            double maxUtility = Double.MIN_VALUE;
            double secondMaxUtility = Double.MIN_VALUE;
        
            for (Garbage g : model.getGarbageList()) {
                // Find the path to the current garbage
                List<Node> path = star.findPath(agentLoc, new Location(g.x, g.y));
                double pathCost = path.size() * 0.01;
        
                // Calculate utility as a combination of reward and proximity
                double utility = g.reward - pathCost;
        
                if (utility > maxUtility) {
                    // Update second-best garbage to the previous best
                    secondMaxUtility = maxUtility;
                    secondBestGarbage = bestGarbage;
                    secondBestPath = bestPath;
        
                    // Update best garbage
                    maxUtility = utility;
                    bestGarbage = g;
                    bestPath = path;
                } else if (utility > secondMaxUtility) {
                    // Update second-best garbage if it's better than the current second-best
                    secondMaxUtility = utility;
                    secondBestGarbage = g;
                    secondBestPath = path;
                }
            }
        
            // Return the path to the second-best garbage
            return secondBestPath;
        }
        
        

        public List<Node> findTargetGarbage(int ag, Location agentLoc, int remainingMoves) {
            AStar star = new AStar(this);
            List<Node> bestPath = null;
            Garbage targetGarbage = null;
            double maxUtility = Double.MIN_VALUE;
        
            synchronized (garbageList) { // Synchronize access to garbageList
                for (Garbage g : garbageList) {
                    if (targetGarbageList.contains(g)) {
                        continue; // Skip garbage that is already targeted
                    }
        
                    // Find the path to the current garbage
                    List<Node> path = star.findPath(agentLoc, new Location(g.x, g.y));
                    if (path.isEmpty()) {
                        continue; // Skip if no valid path found
                    }
        
                    // Calculate the number of moves needed to reach the target
                    int movesRequired = path.size(); // One move per step in the path
        
                    // Check if the agent can reach the target within the remaining moves
                    if (movesRequired > remainingMoves-1) {
                        System.err.println("The targte is too far recalculating");
                        // garbageList.remove(g);
                        continue; // Skip this garbage as it is too far
                    }
        
                    // Calculate utility as a combination of reward and proximity
                    double pathCost = movesRequired * 0.01; // Example cost per step
                    double utility = g.reward - pathCost;
        
                    if (utility > maxUtility) {
                        maxUtility = utility;
                        targetGarbage = g;
                        bestPath = path;
                    }
                }
        
                // Mark the selected garbage as targeted
                if (targetGarbage != null) {
                    targetGarbageList.add(targetGarbage);
                }
            }
        
            return bestPath;
        }
        
        
        
        
        
        
        
        boolean agentDo(int ag, List<Node> path, Location agentLoc) throws Exception {
            if (path.isEmpty()) {
                System.err.println("Agent " + ag + " has no valid path to follow.");
                return false; // No path to follow
            }
        
            Node nextNode = path.get(0); // Get the next step in the path
            // Check for conflict with other agents' planned paths
            if ((ag == 0 && global_path2 != null && !global_path2.isEmpty()) && isFree(nextNode.x, nextNode.y)) {
                Node agent2NextNode = global_path2.get(0); // Get Agent 2's planned next move
                if (nextNode.x == agent2NextNode.x && nextNode.y == agent2NextNode.y) {
                    System.err.println("Conflict detected: Agent " + ag + " and Agent 2 are trying to move to the same spot.");
                    return false; // Stay in the current position to avoid conflict
                }
            }
        
        
            // Move to the next step in the path
            path.remove(0);
            setAgPos(ag, nextNode.x, nextNode.y);
        
            // Check if agent is at garbage location
            Garbage target = garbageAt(nextNode);
            if (target != null && nextNode.x == target.x && nextNode.y == target.y) {
                model.removeGarbage(target); // Remove garbage from grid
                garbageList.remove(target); // Remove from list
                targetGarbageList.remove(target); // Remove from targeted list
                manager.clearGarbageAssignment(target); // Clear assignment from ManagerAgent
        
                // Update points for the agent
                if (ag == 0) {
                    agent1Points += target.reward;
                } else if (ag == 1) {
                    agent2Points += target.reward;
                }
        
                // Update total points
                totalPoints = agent1Points + agent2Points;
        
                // Respawn garbage
                respawnGarbage(target);
                return true; // Garbage was collected
            }
        
            return false; // Garbage not collected
        }

        
        
        

    

       
        void nextSlot() throws Exception {
            Location agentLoc1 = getAgPos(0);
            Location agentLoc2 = getAgPos(1);
        
            boolean agent1PickedUpGarbage = false;
            boolean agent2PickedUpGarbage = false;
        
            int remainingMoves = 31 - moveCount; // Calculate remaining moves
        
            synchronized (garbageList) {
                // Initialize or update paths for agents
                if (global_path1 == null || global_path1.isEmpty()) {
                    global_path1 = findTargetGarbage(0, agentLoc1, remainingMoves);
                    if (global_path1 == null) global_path1 = new ArrayList<>();
                }
                if (global_path2 == null || global_path2.isEmpty()) {
                    global_path2 = findTargetGarbage(1, agentLoc2, remainingMoves);
                    if (global_path2 == null) global_path2 = new ArrayList<>();
                }
        
                // Move Agent 1
                if (!global_path1.isEmpty()) {
                    agent1PickedUpGarbage = agentDo(0, global_path1, agentLoc1);
                } else {
                    System.err.println("Agent 1 has no path.");
                }
        
                // Update graphics for Agent 1
                view.repaint();
        
                // Move Agent 2
                if (!global_path2.isEmpty()) {
                    agent2PickedUpGarbage = agentDo(1, global_path2, agentLoc2);
                } else {
                    System.err.println("Agent 2 has no path.");
                }
        
                // Update graphics for Agent 2
                view.repaint();
        
                // Recalculate paths only if garbage was picked up
                if (agent1PickedUpGarbage) {
                    global_path1 = findTargetGarbage(0, getAgPos(0), remainingMoves);
                    if (global_path1 == null) global_path1 = new ArrayList<>();
                }
                if (agent2PickedUpGarbage) {
                    global_path2 = findTargetGarbage(1, getAgPos(1), remainingMoves);
                    if (global_path2 == null) global_path2 = new ArrayList<>();
                }
            }
        
            // Increment move count and end simulation after 31 moves
            moveCount++;
            if (moveCount >= 31) {
                // Show results and exit
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Simulation Complete!\n" +
                    "Points Collected by Agent 1: " + agent1Points + "\n" +
                    "Points Collected by Agent 2: " + agent2Points + "\n" +
                    "Total Points: " + totalPoints);
        
                writeTotalPointsToCSV();
                System.exit(0); // End simulation
            }
        
            // Ensure percepts and view are updated
            updatePercepts();
            informAgsEnvironmentChanged();
        }
        
        
        
        
        
        
        
        // Helper to find garbage at a specific location
        private Garbage garbageAt(Node node) {
            for (Garbage g : garbageList) {
                if (g.x == node.x && g.y == node.y) {
                    return g;
                }
            }
            return null;
        }
        
        
        
        
        
        
        

        void writeTotalPointsToCSV() {
            try (FileWriter fileWriter = new FileWriter("totalPoints.csv", true);
                PrintWriter printWriter = new PrintWriter(fileWriter)) {
                printWriter.println("Agent 1 Points," + agent1Points);
                printWriter.println("Agent 2 Points," + agent2Points);
                printWriter.println("Total Points," + totalPoints);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        


        public boolean isWall(int x, int y) {
            // Ensure coordinates are within bounds
            if (x < 0 || y < 0 || x >= GRID_SIZE || y >= GRID_SIZE) return true;
        
            // Check if the cell is not free and does not contain garbage
            GridWorldModel model = view.getModel();
            if (!model.isFree(x, y) && !model.hasObject(GARBAGE, x, y)) {
                return true; // The cell is occupied by something other than garbage
            }
            
            // The cell is either free or contains garbage
            return false; 
        }
        
        
    
        public List<Garbage> getGarbageList() {
            return garbageList;
        }

    }
    

    class EnvView extends GridWorldView {
        public EnvView(EnvModel model) {
            super(model, "My World", 1000);
            defaultFont = new Font("Arial", Font.BOLD, 18);
            setVisible(true);
            repaint();
        }
    
        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            if (id == 0) { // Check if this is the agent
                c = Color.RED; // Set the color to red for the agent
                g.setColor(c); // Apply color to graphics context
                super.drawAgent(g, x, y, c, id);
                drawString(g, x, y, defaultFont, "A"); // Draw label "A"
            } else if (id == 1) {
                c = Color.CYAN; // Set the color for the agent
                g.setColor(c); // Apply color to graphics context
                super.drawAgent(g, x, y, c, id);
                drawString(g, x, y, defaultFont, "B"); // Draw label "B"
            } else {
                // Cast model to EnvModel to access getGarbageList
                EnvModel envModel = (EnvModel) model;
                List<Garbage> garbageCopy = new ArrayList<>(envModel.getGarbageList());
                for (Garbage garbage : garbageCopy) {
                    super.drawAgent(g, garbage.x, garbage.y, garbage.color, id);
                }
            }
            repaint(); // Ensure the view is updated
            try {
                Thread.sleep(0); // Simulate delay if necessary
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        
    }
    
    
}
