package solution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class DeliberativePlan {
	
	private final String algorithm;
	private String heuristic;
	private final int capacity;
	private final int costKm;
	private final double maxDistance;
	private DeliberativeState initialState;
	private DeliberativeState finalState;
	private DeliberativeState currentState;
	//the queue for search algorithm
	Queue<DeliberativeState> statesQueued;
	//the list to keep track of visited state
	ArrayList<DeliberativeState> statesSeen;
	
	public DeliberativePlan(Vehicle vehicle, TaskSet tasksLeft, TaskSet tasksCarried, String algorithm) {
		this.algorithm = algorithm;
		this.heuristic = "None";
		this.capacity = vehicle.capacity();
		this.costKm = vehicle.costPerKm();
		this.maxDistance = 0;
		
		this.initialState = new DeliberativeState(vehicle.getCurrentCity(), tasksLeft, tasksCarried, null);
		this.currentState = this.initialState;
		this.statesQueued = new PriorityQueue<DeliberativeState>(11, new DistanceTravelledComparator());
		this.statesSeen = new ArrayList<DeliberativeState>();
	}
	
	public DeliberativePlan(Vehicle vehicle, TaskSet tasksLeft, TaskSet tasksCarried, String algorithm, String heuristic) {
		this.algorithm = algorithm;
		this.heuristic = heuristic;
		this.capacity = vehicle.capacity();
		this.costKm = vehicle.costPerKm();
		this.maxDistance = 0;
		
		this.initialState = new DeliberativeState(vehicle.getCurrentCity(), tasksLeft, tasksCarried, null);
		this.currentState = this.initialState;
		this.statesQueued = new PriorityQueue<DeliberativeState>(11, new DistanceTravelledComparator());
		this.statesSeen = new ArrayList<DeliberativeState>();
	}
	
	
  /*Search through the tree for a suitable final state*/
	public void searchFinalState() {
		
		//add initial state to queue and to list of states seen
		statesQueued.add(initialState);
		statesSeen.add(initialState);
		
		//Temporary variables
		Task tempTask;
		TaskSet tempTasksLeft;
		TaskSet tempTasksCarried;
		DeliberativeState exploredState;
		
		
		System.out.println("Searching for final state");
		double statesExplored = 0;
		queueloop:
		while (true) {
			if (statesQueued.peek() == null) {
				System.out.println("Failure: No final state reached, and no more states to visit");
				break;
			}
			statesExplored +=1;

			
			this.currentState = statesQueued.remove();
			
			//System.out.println(currentState.getCost());
			
			//See if final state has been reached
			if(this.currentState.getTasksLeft().isEmpty() && this.currentState.getTasksCarried().isEmpty()){
				System.out.println("Succes: Reached a final state");
				finalState = this.currentState;
				break;
			}
			
			
			//explore states picking up packages
			for(Iterator<Task> task_it = this.currentState.getTasksLeft().iterator(); task_it.hasNext(); ) {
				//create new tasksLeft and tasksCarried with the task passed from one to the other
				tempTask = task_it.next();
				tempTasksLeft = TaskSet.copyOf(this.currentState.getTasksLeft());
				tempTasksLeft.remove(tempTask);
				tempTasksCarried = TaskSet.copyOf(this.currentState.getTasksCarried());
				tempTasksCarried.add(tempTask);
				
				//Verify that vehicle is capable of carrying that extra task
				if (tempTasksCarried.weightSum() > capacity){continue;}
				
				//Create the new state
				exploredState = new DeliberativeState(tempTask.pickupCity, tempTasksLeft, tempTasksCarried, this.currentState);
				
				//computes cost and adds state if not already seen
				addExploredState(exploredState);
			}
			
			//explore states delivering tasks
			for(Iterator<Task> task_it = this.currentState.getTasksCarried().iterator(); task_it.hasNext(); ) {
				//create new tasksCarried with the task removed
				tempTask = task_it.next();
				tempTasksCarried = TaskSet.copyOf(this.currentState.getTasksCarried());
				tempTasksCarried.remove(tempTask);
				
				//Create the new state
				exploredState = new DeliberativeState(tempTask.deliveryCity, this.currentState.getTasksLeft(), tempTasksCarried, this.currentState);
			
				//computes cost and adds state if not already seen
				addExploredState(exploredState);

			}
		}
		System.out.println("Number of states explored: " + statesExplored);
		System.out.println("Kilometers to travel for the plan: " + finalState.getTravelCost());
	}
	
	/*Trace back through the tree and generate a plan of tasks*/
	public Deque<DeliberativeAction> generateTaskPlan() {
		System.out.println("Retracing itinerary to get to final state");
		
		//trace back set of necessary actions
		
		DeliberativeState tracedState = finalState;
		TaskSet taskSetTemp;
		Task taskTemp;
		Iterator<Task> task_it;
		DeliberativeAction actionTemp;
		Deque<DeliberativeAction> taskPlan = new ArrayDeque<DeliberativeAction>();
		
		
		while(tracedState!=initialState) {
			System.out.println(tracedState.getCity());
			//A Task was picked up
			if (tracedState.getParentState().getTasksLeft()!=tracedState.getTasksLeft()) {
				taskSetTemp = TaskSet.intersectComplement(tracedState.getParentState().getTasksLeft(), tracedState.getTasksLeft());
				task_it = taskSetTemp.iterator();
				taskTemp = task_it.next();
				actionTemp = new DeliberativeAction(true,taskTemp);
				taskPlan.push(actionTemp);
				//Health check, there should only be one task change per action
				if (task_it.hasNext()) {
					System.out.println("Error: Two tasks picked up in one action");
				}
			}
			//A Task was dropped
			else if (tracedState.getParentState().getTasksCarried()!=tracedState.getTasksCarried()){
				taskSetTemp = TaskSet.intersectComplement(tracedState.getParentState().getTasksCarried(), tracedState.getTasksCarried());
				task_it = taskSetTemp.iterator();
				taskTemp = task_it.next();
				actionTemp = new DeliberativeAction(false, taskTemp);
				taskPlan.push(actionTemp);
				//Health check, there should only be one task change per action
				if (task_it.hasNext()) {
					System.out.println("Error: Two tasks dropped up in one action");
				}
			}
			else{
				//should never come here
			}
			tracedState = tracedState.getParentState();
		}
		
		return taskPlan;
	}
	
	/*If the state has not been seen before, compute its cost and add it to the queue*/
	void addExploredState(DeliberativeState exploredState) {
		//set the cost
		exploredState.setCost(computeCost(exploredState));
		
		//Verify that the new state has not already been seen, or it it has, that the cost is now lower
		if ((statesSeen.contains(exploredState))) {
			while (true) {
				int i = statesSeen.indexOf(exploredState);
				//if no more identical states in list, break
				if (i==-1) break;
				//if state in list is worse, remove it
				else if(statesSeen.get(i).getCost()>exploredState.getCost()) {
					statesSeen.remove(statesSeen.get(i));
				}
				//if state in list is equal or better, break and newly explored state will not be added
				else {
					break;
				}
				
			}
		}
		
		if (!(statesSeen.contains(exploredState))) {// || (better == true)) {
			//add to list of seen
			statesSeen.add(exploredState);
			//Add explored state to the queue
			statesQueued.add(exploredState);
			
		}
		
	}
	
	double computeCost(DeliberativeState exploredState) {
		double cost = 0.0;
		
		//set the travel costs
		exploredState.setTravelCost(this.currentState.getTravelCost() + costKm * this.currentState.getCity().distanceTo(exploredState.getCity()));
		if (this.algorithm.equals("BFS")) {
			cost = exploredState.getTravelCost();
		}
		else if (this.algorithm.equals("ASTAR")) {
			exploredState.computeHeuristic(this.heuristic);
			cost = exploredState.getTravelCost() + exploredState.getHeuristicCost();
		}
		else {
			System.out.println("Error: Algorithm name does not exist");
		}
		return cost;
	}
	
	void setHeuristic(String heuristic) {
		this.heuristic = heuristic;
	}
	
	double computeHeuristicCost(DeliberativeState state) {
		
		return 1;
	}
	

}
