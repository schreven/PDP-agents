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
	
	private TaskSet tasksCarried = null;
	
	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	String heuristic;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		heuristic = agent.readProperty("heuristic", String.class, "distance");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasksLeft) {
			
		Plan plan;
		Deque<DeliberativeAction> taskPlan;
		
		//first time plan a plan is computed, instantiate tasksCarried
		if (tasksCarried == null) {tasksCarried = TaskSet.noneOf(tasksLeft);}
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			//Create a new astarPlan instance with the appropriate heuristic
			DeliberativePlan astarPlan = new DeliberativePlan(vehicle, tasksLeft, tasksCarried,"ASTAR", heuristic);
			//Search through the tree for a suitable final state
			astarPlan.searchFinalState();
			//Trace back through the tree and generate a plan of tasks
			taskPlan = astarPlan.generateTaskPlan();
			
			break;
			
		case BFS:
			//Create a new bfsPlan instance
			DeliberativePlan bfsPlan = new DeliberativePlan(vehicle, tasksLeft, tasksCarried,"BFS");
			//Search through the tree for a suitable final state
			bfsPlan.searchFinalState();
			//Trace back through the tree and generate a plan of tasks
			taskPlan = bfsPlan.generateTaskPlan();
			
			break;
			
		default:
			throw new AssertionError("Should not happen.");
		}		
		
		plan = taskPlanToLogistPlan(taskPlan);
		return plan;
	}
	
	//translates taskPlan into logist plan
	public Plan taskPlanToLogistPlan(Deque<DeliberativeAction> taskPlan) {

		City currentCity = agent.vehicles().get(0).getCurrentCity();
		Plan plan = new Plan(currentCity);
		
		while(taskPlan.peek()!=null){
			DeliberativeAction action=taskPlan.pop();
			//picking up a task
			if (action.isPickup()) {
				// move: current city => pickup location
				for (City city : currentCity.pathTo(action.getTask().pickupCity))
					plan.appendMove(city);
				plan.appendPickup(action.getTask());
				// udate current city
				currentCity = action.getTask().pickupCity;
			}
			//delivering a task
			else{
				// move: current city => delivery location
				for (City city : currentCity.pathTo(action.getTask().deliveryCity))
					plan.appendMove(city);
				plan.appendDelivery(action.getTask());
				// update current city
				currentCity = action.getTask().deliveryCity;
			}
		
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			tasksCarried = carriedTasks;
		}
	}
	
	double getMaxDistance(Topology topology) {
		double maxDistance = 0;
		for (City city1: topology.cities()) {
			for (City city2: topology.cities()) {
				if (maxDistance < city1.distanceTo(city2)) maxDistance = city1.distanceTo(city2);
			}
		}
		return maxDistance;
	}
}

