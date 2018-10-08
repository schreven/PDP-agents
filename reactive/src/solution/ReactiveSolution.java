package solution;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveSolution implements ReactiveBehavior {

	private Random random;
	private double discountFactor;
	private int numActions;
	private Agent myAgent;
	private double stoppingCriterion;
	
	private ArrayList<ReactiveState> stateList;											// List of all the states
	private Map<ReactiveState, ArrayList<ReactiveAction>> possibleActions;				// Each state has a list of possible actions from this particular state
	private int costKm;																	// cost per km of the vehicle
	
	private Map<Integer, Double> Vlist;
	private Map<Integer, Integer> Best;
	
	private class MaxV {
		double v;
		int a;
	}
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.discountFactor = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		this.costKm = agent.vehicles().get(0).costPerKm();								// find the cost per km of the agent
		this.stateList = new ArrayList<ReactiveState>();
		this.possibleActions = new HashMap<ReactiveState, ArrayList<ReactiveAction>>();
		
		this.Vlist = new HashMap<Integer, Double>();
		this.Best = new HashMap<Integer, Integer>();
		this.stoppingCriterion = 0.01;
		
		LearnStrategy(topology, td);		
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();	
		int bestTask =  stateList.get(currentCity.id);
		
		if (availableTask != null && availableTask.deliveryCity.id == bestTask) {
			action = new Pickup(availableTask);
		}
		else {
			action = new Move(currentCity.randomNeighbor(random));
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;		
		
		return action;
	}	
			
	public void LearnStrategy(Topology topology, TaskDistribution td) {		
		
		/* Populate States */
		for (City city : topology) {
			stateList.add(new ReactiveState(city));								// States where there is no task available in the city
			
			for (City dest : topology) {
				if (city.id == dest.id) continue;								// Origin city and destination city cannot be the same
				stateList.add(new ReactiveState(city, dest));					// States where there is a task to city dest
			}
		}
		
		/* Find all different state-action couples and compute R(s,a)*/
		double cost = 0.0;
		for (ReactiveState state : stateList) {
			City city = state.getCurrentCity();
			ArrayList<ReactiveAction> actionList = new ArrayList<ReactiveAction>();
			
			for (City neighbor : city.neighbors()) {							// When there is no task, move to every neighbors
				ReactiveAction action = new ReactiveAction(false, neighbor);
				cost = -(double)costKm*city.distanceTo(neighbor);
				action.setReward(cost);											// save R(s,a)
				actionList.add(action);
			}	
			
			if (state.taskAvailible()) {										// When there is a task
				for (City dest : topology) {
					if (city.id == dest.id) continue;
					ReactiveAction action = new ReactiveAction(true, dest);
					cost = -(double)costKm*city.distanceTo(dest);
					action.setReward((double)td.reward(city, dest) + cost);		// save R(s,a)
					actionList.add(action);
				}				
			}
			
			possibleActions.put(state, actionList);								// add all possible actions for each state
		}
		
		
		// TODO: IMPLEMENT V(S) AND BEST(S) COMPUTATION
		
		
		/* OLD SOLUTION
		Map<Integer, Double> previousV = new HashMap<Integer, Double>();
		// Initialize V values
		for (City city : topology) {
			Vlist.put(city.id, 1.0);
			previousV.put(city.id, 0.0);
		}		
		
		// Compute V values
			
		while (findMaxDiff(Vlist, previousV) > stoppingCriterion) {
			previousV = new HashMap<Integer, Double>(Vlist);
			computeV(topology, td);		
		}
		
		// Iterate over all cities
		// Agent accepts only tasks going to Best city
		for (City city : topology) {
			stateList.put(city.id, Best.get(city.id));			
		}	*/
		
	}
	
	/* OLD SOLUTION
	public void computeV(Topology topology, TaskDistribution td) {
		
		// Iterate over all cities
		for (City cityA : topology) {			
			Map<Integer, Double> Qlist = new HashMap<Integer, Double>();
			
			for (City cityB : topology) {
				if (cityA.id == cityB.id) continue;				
				double reward = td.reward(cityA, cityB);	
				// the sum is simplified because in state S, action A gives reward only if we go to S' ??? not sure
				Qlist.put(cityB.id, reward+discountFactor*td.probability(cityA, cityB)*Vlist.get(cityB.id));	
			}
			
			MaxV maxv = findMax(Qlist);
						
			Vlist.put(cityA.id, maxv.v);
			Best.put(cityA.id, maxv.a);	
		}
	}
	
	public MaxV findMax(Map<Integer, Double> map) {
		MaxV result = new MaxV();
		result.v = 0.0;
		
		for(int key : map.keySet())
		    if (map.get(key) > result.v) {
		    	result.v = map.get(key);
		    	result.a = key;
		    }
		
		return result;
	}
	
	public double findMaxDiff(Map<Integer, Double> mapA, Map<Integer, Double> mapB) {
		double maxDiff = 0.0;

		for (int key : mapA.keySet()) {
			double diff = Math.abs(mapA.get(key) - mapB.get(key));
			if (diff > maxDiff) maxDiff = diff;
		}		
		
		return maxDiff;
	}*/
	
	
}
