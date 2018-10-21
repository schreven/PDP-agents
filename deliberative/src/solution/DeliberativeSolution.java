package solution;

import java.util.List;
import java.util.Map;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeSolution implements DeliberativeBehavior {	
	
	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = BFSPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFSPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	
	private Plan BFSPlan(Vehicle vehicle, TaskSet tasks) {
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		
		//Define initial state
		DeliberativeState initialState = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),1.0);
		
		/*
		DeliberativeState initialState1 = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),4.0);
		DeliberativeState initialState2 = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),5.0);
		DeliberativeState initialState3 = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),10.0);
		DeliberativeState initialState4 = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),2.0);
		DeliberativeState initialState5 = new DeliberativeState(currentCity, tasks, TaskSet.noneOf(tasks),0.0);
		*/
		
		//Set up the queue for search algorithm
		Comparator<DeliberativeState> comparator = new DistanceTravelledComparator();
		Queue<DeliberativeState> statesQueued = new PriorityQueue<DeliberativeState>(11, comparator);
		//Queue<DeliberativeState> statesQueued = new LinkedList<DeliberativeState>();
	
		
		statesQueued.add(initialState);
		
		/*
		statesQueued.add(initialState1);
		statesQueued.add(initialState2);
		statesQueued.add(initialState3);
		statesQueued.add(initialState4);
		statesQueued.add(initialState5);
		
		while (true) {
			if (statesQueued.peek() == null) {
				System.out.println("Failure: No final state reached, and no more states to visit");
				break;
			}
			
			DeliberativeState currentState = statesQueued.poll();
			System.out.println(currentState.getCost());
		}
		*/
		//Set up the list to keep track of visited stated
		ArrayList<DeliberativeState> statesSeen = new ArrayList<DeliberativeState>();
		statesSeen.add(initialState);
		
		//Set up mapping of parent states to retrieve path
		Map<DeliberativeState, DeliberativeState> parent = new HashMap<DeliberativeState, DeliberativeState>();
		
		//To save the final state
		DeliberativeState finalState = null;
		
		//Temporary states
		Task tempTask;
		TaskSet tempTasksLeft;
		TaskSet tempTasksCarried;
		DeliberativeState exploredState;
		
		
		Double tempCost = 0.0;
		
		System.out.println("Searching for final state");
		while (true) {
			if (statesQueued.peek() == null) {
				System.out.println("Failure: No final state reached, and no more states to visit");
				break;
			}
			
			DeliberativeState currentState = statesQueued.remove();
			
			System.out.println(currentState.getCost());
			
			//System.out.println(currentState.getTasksCarried().weightSum());
			
			if(currentState.getTasksLeft().isEmpty() && currentState.getTasksCarried().isEmpty()){
				System.out.println("Succes: Reached a final state");
				finalState = currentState;
				break;
			}
			
			
			//explore states picking up packages
			for(Iterator<Task> task_it = currentState.getTasksLeft().iterator(); task_it.hasNext(); ) {
				tempTask = task_it.next();
				tempTasksLeft = TaskSet.copyOf(currentState.getTasksLeft());
				tempTasksLeft.remove(tempTask);
				tempTasksCarried = TaskSet.copyOf(currentState.getTasksCarried());
				tempTasksCarried.add(tempTask);
				
				//Verify that vehicle is capable of carrying that extra task
				if (tempTasksCarried.weightSum() > capacity){continue;}
				//Create the new state
				exploredState = new DeliberativeState(tempTask.pickupCity, tempTasksLeft, tempTasksCarried, 0.0);
				
				//Verify that the new state has not already been seen
				if (!(statesSeen.contains(exploredState))) {
					//compute and set cost of the state
					tempCost = currentState.getCost() + currentState.getCity().distanceTo(exploredState.getCity());
					exploredState.setCost(tempCost);
					//add to list of seen
					statesSeen.add(exploredState);
					//Add explored state to the queue
					statesQueued.add(exploredState);
					//save trace of parent of the newly explored state
					parent.put(exploredState, currentState);
				}
			}
			
			//explore states delivering tasks
			for(Iterator<Task> task_it = currentState.getTasksCarried().iterator(); task_it.hasNext(); ) {
				
				tempTask = task_it.next();
				tempTasksCarried = TaskSet.copyOf(currentState.getTasksCarried());
				tempTasksCarried.remove(tempTask);
				//Create the new state
				exploredState = new DeliberativeState(tempTask.deliveryCity, currentState.getTasksLeft(), tempTasksCarried, 0.0);
				
				//Verify that the new state has not already been seen
				if (!(statesSeen.contains(exploredState))) {
					//compute and set cost of the state
					tempCost = currentState.getCost() + currentState.getCity().distanceTo(exploredState.getCity());
					exploredState.setCost(tempCost);
					//add to list of seen
					statesSeen.add(exploredState);
					//Add explored state to the queue
					statesQueued.add(exploredState);
					//save trace to parent of the newly explored state
					parent.put(exploredState, currentState);

				}
			}
			
			

		}
		
		System.out.println("Retracing itinerary to get to final state");
		
		//trace back set of necessary actions
		
		DeliberativeState tracedState = finalState;
		DeliberativeState tracedStateParent = finalState;
		TaskSet taskSetTemp;
		Task taskTemp;
		Iterator<Task> task_it;
		DeliberativeAction actionTemp;
		Deque<DeliberativeAction> actionPlan = new ArrayDeque<DeliberativeAction>();
		
		
		while(tracedState!=initialState) {
			System.out.println(tracedState.getCity());
			tracedStateParent = parent.get(tracedState);
			//A Task was picked up
			if (tracedStateParent.getTasksLeft()!=tracedState.getTasksLeft()) {
				taskSetTemp = TaskSet.intersectComplement(tracedStateParent.getTasksLeft(), tracedState.getTasksLeft());
				task_it = taskSetTemp.iterator();
				taskTemp = task_it.next();
				actionTemp = new DeliberativeAction(true,taskTemp);
				actionPlan.push(actionTemp);
				//Health check, there should only be one task change per action
				if (task_it.hasNext()) {
					System.out.println("Error: Two tasks picked up in one action");
				}
			}
			//A Task was dropped
			else if (tracedStateParent.getTasksCarried()!=tracedState.getTasksCarried()){
				taskSetTemp = TaskSet.intersectComplement(tracedStateParent.getTasksCarried(), tracedState.getTasksCarried());
				task_it = taskSetTemp.iterator();
				taskTemp = task_it.next();
				actionTemp = new DeliberativeAction(false, taskTemp);
				actionPlan.push(actionTemp);
				//Health check, there should only be one task change per action
				if (task_it.hasNext()) {
					System.out.println("Error: Two tasks dropped up in one action");
				}
			}
			else{
				//should never come here
			}
			tracedState = tracedStateParent;
		}
		
		
		
		
		
		//for (DeliberativeAction action : actionPlan)
		while(actionPlan.peek()!=null){
			DeliberativeAction action=actionPlan.pop();
			//picking up a task
			if (action.getAction()==true) {
				// move: current city => pickup location
				for (City city : currentCity.pathTo(action.getTask().pickupCity))
					plan.appendMove(city);
				plan.appendPickup(action.getTask());
				// set current city
				currentCity = action.getTask().pickupCity;
			}
			//delivering a task
			else if (action.getAction()==false) {
				// move: current city => delivery location
				for (City city : currentCity.pathTo(action.getTask().deliveryCity))
					plan.appendMove(city);
				plan.appendDelivery(action.getTask());
				// set current city
				currentCity = action.getTask().deliveryCity;
			}
		
		}
		
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}

