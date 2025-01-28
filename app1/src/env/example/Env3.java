import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;


public class Env3 extends Environment {
    public static final int GRID_SIZE = 10; // Define grid size (10x10)
    static Logger logger = Logger.getLogger(Env3.class.getName());

    // Define constants for different garbage colors and agent
    public static final int GARBAGE = 3;

    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");

    private EnvModel model;
    private EnvView view;

    private int moveCount = 0;
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
        if (moveCount >= 92) {
            System.err.println("All Agents have completed 31 moves.");
            System.exit(0); // End simulation
            return false; 
            

        }

        

        try {
            Thread.sleep(500); // Simulate delay for the action
        } catch (Exception e) {}
        // updatePercepts();
        informAgsEnvironmentChanged();
        return true;
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
        // new: track each agent’s current assigned garbage
        private final Map<Integer, Garbage> assignedToAgent  = new HashMap<>();

        public void clearAllAssignments() {
            assignedGarbage.clear();
            assignedToAgent.clear();
        }

    
        public boolean isGarbageAssigned(Garbage garbage) {
            return assignedGarbage.containsKey(garbage);
        }
    
        public int getAssignedAgent(Garbage garbage) {
            return assignedGarbage.getOrDefault(garbage, -1);
        }
    
        public void clearGarbageAssignment(Garbage garbage) {
            // remove from garbage->agent map
            Integer ag = assignedGarbage.remove(garbage);
            if (ag != null) {
                // also remove from agent->garbage
                assignedToAgent.remove(ag);
            }
        }
    }
    
    


    void updatePercepts() {
        clearPercepts();
    
        // Update positions of agents
        Location bobLoc = model.getAgPos(0);
        Location aliceLoc = model.getAgPos(1);
        Location charlieLoc = model.getAgPos(2);

        // Update garbage percepts
        for (Garbage g : model.getGarbageList()) {
            addPercept("agent", Literal.parseLiteral("garbage(" + g.x + "," + g.y + "," + g.reward + ")"));
        }
    }
    
    
    

    class EnvModel extends GridWorldModel {
        private List<Garbage> garbageList = new ArrayList<>();          // Garbage in the Enviroment
        private List<Garbage> targetGarbageList = new ArrayList<>();    // Garbage already assigned to an agents

        List<Node> global_path1 = null;
        List<Node> global_path2 = null;
        List<Node> global_path3 = null;

        private double agent1Points = 0.0;
        private double agent2Points = 0.0;
        private double agent3Points = 0.0;
        private double totalPoints = 0.0;
        
        private ManagerAgent manager = new ManagerAgent();
    
        public EnvModel(int width, int height) {
            super(width, height, 3); // Initialize with 3 agent
            
            // Create and place garbage objects with different properties
            garbageList.add(new Garbage(3, 2, Color.GREEN, 0.4));
            garbageList.add(new Garbage(4, 1, Color.YELLOW, 0.2));
            garbageList.add(new Garbage(7, 6, Color.PINK, 0.8));
            garbageList.add(new Garbage(8, 6, Color.BLUE, 0.6));

            
            
    
            for (Garbage g : garbageList) {
                add(GARBAGE, g.x, g.y); // Use GARBAGE as the identifier for garbage items
            }


            
            addWall(0, 0, 9, 0); // Add walls to the top edge
            addWall(0, 0, 0, 9); // Add walls to the left edge
            addWall(0, 9, 9, 9); // Add walls to the bottom edge
            addWall(9, 0, 9, 9); // Add walls to the right edge

            //Add walls inside the grid as obstacles
            addWall(2, 8, 2, 8);
            addWall(5, 6, 5, 6);
            addWall(7, 4, 7, 4);
            addWall(7, 3, 7, 3);
            

            // setAgPos(0, 1, 6); //Presentation starting position
            placeAgentRandomly(0);
            placeAgentRandomly(1);
            placeAgentRandomly(2);
        }

        public void removeGarbage(Garbage garbage) {
            targetGarbageList.remove(garbage);
            remove(GARBAGE, garbage.x, garbage.y); // Remove from the grid
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
            view.repaint();
        }


        
        void assignGarbageWithContractNet() {
            List<Location> agentLocs = new ArrayList<>();
            agentLocs.add(getAgPos(0)); // agent 0
            agentLocs.add(getAgPos(1)); // agent 1
            agentLocs.add(getAgPos(2)); // agent 2
        
            AStar star = new AStar(this);
        
            // For each garbage that is not yet assigned
            for (Garbage g : new ArrayList<>(garbageList)) {
                if (!manager.isGarbageAssigned(g)) {
                    double bestUtility = -Double.MAX_VALUE;
                    int bestAgent = -1;
                    List<Node> bestPath = null;
        
                    for (int ag = 0; ag < agentLocs.size(); ag++) {
                        // Skip if this agent already has a garbage assigned
                        if (manager.assignedToAgent.containsKey(ag)) {
                            continue;
                        }
        
                        Location loc = agentLocs.get(ag);
                        List<Node> path = star.findPath(loc, new Location(g.x, g.y));
                        if (path.isEmpty()) {
                            System.err.println("Could not reach garbage continuing");
                            continue; 
                        }
        
                        double pathCost = path.size() * 0.01;
                        double utility = g.reward - pathCost;
        
                        if (utility > bestUtility && path.size() <= (93 - moveCount)) {
                            bestUtility = utility;
                            bestAgent = ag;
                            bestPath = path;
                        }
                    }
        
                    if (bestAgent != -1) {
                        // Assign the garbage to the best agent
                        targetGarbageList.add(g);
                        manager.assignedGarbage.put(g, bestAgent);
                        manager.assignedToAgent.put(bestAgent, g);
                        // System.out.println("Assigned garbage at (" + g.x + "," + g.y + ") to agent " + bestAgent);
                    }
                }
            }
        }
        
        

        public List<Node> findTargetGarbage(int ag, Location agentLoc) {
            // We'll just look for the single garbage that manager assigned to 'ag'.
        
            AStar star = new AStar(this);
            // In case the manager assigned multiple tasks, pick the *first* or the *best*.
            // For simplicity, let's pick the first one in targetGarbageList that is assigned to us:
            for (Garbage g : targetGarbageList) {
                int assignedAgent = manager.getAssignedAgent(g);
                if (assignedAgent == ag) {
                    // Found a garbage assigned to me
                    List<Node> path = star.findPath(agentLoc, new Location(g.x, g.y));
                    return path; // done
                }
            }
            return new ArrayList<>(); // none assigned or no path
        }
        
        
        
        
        
        
        boolean agentDo(int ag, List<Node> path, Location agentLoc) throws Exception {
            if (path == null || path.isEmpty()) {
                System.err.println("Agent " + ag + " has no valid path to follow.");
                return false; // No path to follow
            }
            
            
            Node nextNode = path.get(0);
        
            // --- If agent=0, check conflict with agent=1's path (global_path2)
            if ((ag == 0 && global_path2 != null && !global_path2.isEmpty()) && isFree(nextNode.x, nextNode.y)) {
                Node agent2NextNode = global_path2.get(0);
                if (nextNode.x == agent2NextNode.x && nextNode.y == agent2NextNode.y) {
                    System.err.println("Conflict detected: Agent 0 and Agent 1 want the same spot.");
                    // Give priority to the smaller ID (Agent 0)
                    if (ag < 1) {
                        System.err.println("Agent 0 has priority, moves ahead.");
                    } else {
                        return false;
                    }
                }
        
            // --- If agent=1, check conflict with agent=2's path (global_path3)
            } else if ((ag == 1 && global_path3 != null && !global_path3.isEmpty()) && isFree(nextNode.x, nextNode.y)) {
                Node agent3NextNode = global_path3.get(0);
                moveCount++;
                if (nextNode.x == agent3NextNode.x && nextNode.y == agent3NextNode.y) {
                    System.err.println("Conflict detected: Agent 1 and Agent 2 want the same spot.");
                    // Give priority to the smaller ID (Agent 1)
                    if (ag < 2) {
                        System.err.println("Agent 1 has priority, moves ahead.");
                    } else {
                        return false;
                    }
                }
        
            // --- Else check conflict with agent=2 UNLESS this *is* agent=2
            } else if (ag != 2 && global_path3 != null && !global_path3.isEmpty() && isFree(nextNode.x, nextNode.y)) {
                Node agent3NextNode = global_path3.get(0);
                moveCount++;
                if (nextNode.x == agent3NextNode.x && nextNode.y == agent3NextNode.y) {
                    System.err.println("Conflict detected: Agent " + ag + " and Agent 2 want the same spot.");
                    // Give priority to the smaller ID
                    if (ag < 2) {
                        System.err.println("Agent " + ag + " has priority, moves ahead.");
                    } else {
                        return false;
                    }
                }
            }
        
            // --- If we reach here, either no conflict or this agent won priority ---
            path.remove(0);
            view.repaint();
            // System.err.println("Current Position of Agent:"+getAgPos(ag));
            Location newagloc = new Location(nextNode.x, nextNode.y);
            if (getAgPos(ag) == newagloc){
                moveCount--;
            }
            setAgPos(ag, nextNode.x, nextNode.y);
            // System.err.println("New Position of Agent"+nextNode.x + " "+ nextNode.y);
            moveCount++;
            // System.err.println("MoveCount:" + moveCount);
            view.repaint();
            
            // Check if agent is on garbage
            Garbage target = garbageAt(nextNode);
            if (target != null && nextNode.x == target.x && nextNode.y == target.y) {
                // remove garbage from the grid, etc.
                model.removeGarbage(target);
                updatePercepts();
                model.garbageList.remove(target);
                model.targetGarbageList.remove(target);
                model.manager.clearGarbageAssignment(target);
            
                // Update points
                if (ag == 0) {
                    agent1Points += target.reward;
                } else if (ag == 1) {
                    agent2Points += target.reward;
                } else {
                    agent3Points += target.reward;
                }
                totalAgentPoints = agent1Points + agent2Points + agent3Points;
            
                // Respawn with same reward/color, random position
                model.respawnGarbage(target);
            
                //Clear *all* manager assignments & paths to force recalculation
                model.manager.clearAllAssignments();
                model.global_path1 = new ArrayList<>();
                model.global_path2 = new ArrayList<>();
                model.global_path3 = new ArrayList<>();
                System.err.println("Agent "+ag+" Collected the Garbage: "+target.reward +"\nRecalculating all paths based on new location");
            
                return true; // picked up garbage
            }
        
            return false; // moved, but no garbage
        }
        
        
        
    

       
        void nextSlot() throws Exception {

            assignGarbageWithContractNet();


            Location agentLoc1 = getAgPos(0);
            Location agentLoc2 = getAgPos(1);
            Location agentLoc3 = getAgPos(2);
        
            boolean agent1PickedUpGarbage = false;
            boolean agent2PickedUpGarbage = false;
            boolean agent3PickedUpGarbage = false;
        
            // Initialize or update paths for agents 
            if (global_path1 == null || global_path1.isEmpty()) {
                global_path1 = findTargetGarbage(0, agentLoc1);
                if (global_path1 == null) global_path1 = new ArrayList<>();
            }
            if (global_path2 == null || global_path2.isEmpty()) {
                global_path2 = findTargetGarbage(1, agentLoc2);
                if (global_path2 == null) global_path2 = new ArrayList<>();
            }
            if (global_path3 == null || global_path3.isEmpty()) {
                global_path3 = findTargetGarbage(2, agentLoc3);
                if (global_path3 == null) global_path3 = new ArrayList<>();
            }
    
            // Move Agent 1
            if (global_path1 == null || !global_path1.isEmpty()) {
                agent1PickedUpGarbage = agentDo(0, global_path1, agentLoc1);
            } else {
                System.err.println("Agent 1 has no path.");
            }
    
            // Update graphics for Agent 1
            view.repaint();
    
            // Move Agent 2
            if (global_path2 == null || global_path2.isEmpty()) {
                System.err.println("Agent 2 has no path.");
            } else {
                agent2PickedUpGarbage = agentDo(1, global_path2, agentLoc2);
            }
            // Update graphics for Agent 3
            view.repaint();

            // Move Agent 3
            if (global_path3 == null || !global_path3.isEmpty()) {
                agent3PickedUpGarbage = agentDo(2, global_path3, agentLoc3);
            } else{
                System.err.println("Agent 3 has no path.");
            }
    
            // Update graphics for Agent 3
            view.repaint();
            

        
            // Increment move count and end simulation after 31 moves
            // moveCount++;
            // System.err.println(moveCount);
            if (moveCount >= 93) {
                // Show results and exit
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Simulation Complete!\n" +
                    "Points Collected by Agent 1: " + agent1Points + "\n" +
                    "Points Collected by Agent 2: " + agent2Points + "\n" +
                    "Points Collected by Agent 3: " + agent3Points + "\n" + 
                    "Total Points: " + totalAgentPoints);
        
                writeTotalPointsToCSV();
                System.exit(0); // End simulation
            }
        
            // Ensure percepts and view are updated
            updatePercepts();
            informAgsEnvironmentChanged();
        }
        
        
        
        
        
        
        // Helper to find garbage at a specific location
        private Garbage garbageAt(Node node) {
            for (Garbage g : model.garbageList) {
                if (g.x == node.x && g.y == node.y) {
                    return g;
                }
            }
            return null;
        }
        


        void writeTotalPointsToCSV() {
            try (FileWriter fileWriter = new FileWriter("3agentsRecalculating.csv", true);
                 PrintWriter printWriter = new PrintWriter(fileWriter)) {
                 
                // Write a single row, columns separated by commas, e.g.: "10.0,8.5,9.2,27.7"
                printWriter.println(agent1Points + "," 
                                   + agent2Points + "," 
                                   + agent3Points + "," 
                                   + totalAgentPoints);
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
            if (id == 0) { // Agent 1
                c = Color.RED; 
                g.setColor(c); 
                super.drawAgent(g, x, y, c, id);
                drawString(g, x, y, defaultFont, "A");
            } else if (id == 1) { // Agent 2
                c = Color.CYAN;
                g.setColor(c);
                super.drawAgent(g, x, y, c, id);
                drawString(g, x, y, defaultFont, "B");
            } else if (id == 2) { // Agent 3
                c = Color.MAGENTA;
                g.setColor(c);
                super.drawAgent(g, x, y, c, id);
                drawString(g, x, y, defaultFont, "C");
            } else {
                // Draw garbage items
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