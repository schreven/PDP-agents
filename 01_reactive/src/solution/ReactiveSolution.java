package solution;

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

	private double discountFactor;
	private int numActions;
	private Agent myAgent;
	private double stoppingCriterion;
	
	private ArrayList<ReactiveState> stateList;											// List of all the states
	private Map<ReactiveState, ArrayList<ReactiveAction>> possibleActions;				// Each state has a list of possible actions from this particular state
	private Map<City, ArrayList<ReactiveState>> statesInCity;							// List of different states in each city
	private Map<ReactiveState, ReactiveAction> strategy;								// Best action to take in state S
	private int costKm;																	// cost per km of the vehicle
	
	// class to return result of findMaxQ function
	public class BestResult {
		private final double v;
		private final ReactiveAction action;
		
		public BestResult(double v, ReactiveAction action) {
			this.v = v;
			this.action = action;
		}
		
		double getV() {
			return v;
		}
		
		ReactiveAction getAction() {
			return action;
		}
	}
		
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.discountFactor = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		this.costKm = agent.vehicles().get(0).costPerKm();								// find the cost per km of the agent
		this.stateList = new ArrayList<ReactiveState>();
		this.possibleActions = new HashMap<ReactiveState, ArrayList<ReactiveAction>>();
		this.statesInCity = new HashMap<City, ArrayList<ReactiveState>>();	
		this.strategy = new HashMap<ReactiveState, ReactiveAction>();	

		this.stoppingCriterion = 0.001;
		
		LearnStrategy(topology, td);	
		
		/*
		for (Map.Entry<ReactiveState, ReactiveAction> entry : strategy.entrySet()) {
			System.out.println(entry.getKey().getCurrentCity());
			System.out.println(entry.getKey().taskAvailable());
		}*/
		
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;		
		City currentCity = vehicle.getCurrentCity();
		
		/* Identify current state (comparison with the list) */
		ReactiveState currentState = FindState(currentCity, availableTask);
				
		/* Find out what to do */
		ReactiveAction bestAction = strategy.get(currentState);
		
		if (bestAction.getAction()) {				// Pickup
			action = new Pickup(availableTask);
		}
		else {										// Move
			action = new Move(bestAction.getDestinationCity());
		}
		
		/* Output */
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;		
		
		return action;
	}	
			
	public void LearnStrategy(Topology topology, TaskDistribution td) {		
		
		/* Populate States */
		for (City city : topology) {
			ArrayList<ReactiveState> cityStates = new ArrayList<ReactiveState>();
			cityStates.add(new ReactiveState(city));							// States where there is no task available in the city
			
			for (City dest : topology) {
				if (city.id == dest.id) continue;								// Origin city and destination city cannot be the same
				cityStates.add(new ReactiveState(city, dest));					// States where there is a task to city dest
			}
			stateList.addAll(cityStates);
			statesInCity.put(city, cityStates);
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
			
			if (state.taskAvailable()) {										// When there is a task
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
		
		/* Initialize V values arbitrarily*/
		Map<ReactiveState, Double> Vvalues = new HashMap<ReactiveState, Double>();
		Map<ReactiveState, Double> NewVvalues = new HashMap<ReactiveState, Double>();
		for (ReactiveState state : stateList) {
			Vvalues.put(state, 1.0);
		}
		
		/* Compute V(s) and Best(s) */
		boolean done = false;
		while (!done) {
			for (ReactiveState state : possibleActions.keySet()) {					// Iterate over states
				Map<ReactiveAction, Double> Qvalues = new HashMap<ReactiveAction, Double>();
							
				for (ReactiveAction action : possibleActions.get(state)) {			// Iterate over possible actions of the state
					double sum = 0.0;
					City dest = action.getDestinationCity();		
					
				    // action A is to go from the city of state S to the destination city in S'. T(s,a,s') = 0 for all the other cities.
					// In the destination city, the possible states are either taskAvailible = 0, or a task to a next city.
					// Thus, T(s,a,s') is linked to the probability to have a task available in S':
					
					/* Compute Q(s,a) */
					for (ReactiveState state_ : statesInCity.get(dest)) {			// Iterate over all states in destination city
						
						sum += td.probability(state_.getCurrentCity(), state_.getDestinationCity()) * Vvalues.get(state_);
					}				
					double q = action.getReward() + discountFactor * sum;			
					Qvalues.put(action, q);							
				}	
				
				/* Compute Best(s) and V(s) */
				BestResult result = findMaxQ(Qvalues);			
				NewVvalues.put(state, result.getV());
				strategy.put(state, result.getAction());	
				
			}
			
			/* Stopping criterion */
			double diff = findMaxDiff(Vvalues, NewVvalues);
			if (diff < stoppingCriterion) done = true;
			Vvalues = new HashMap<ReactiveState, Double>(NewVvalues);				// Update V values		
		}			
	}
	
	public BestResult findMaxQ(Map<ReactiveAction, Double> Qvalues) {
		
		double bestV = 0.0;
		ReactiveAction bestAction = new ReactiveAction(false, null);
		
		for (Map.Entry<ReactiveAction, Double> entry : Qvalues.entrySet()) {
			if (entry.getValue() > bestV) {
				bestV = entry.getValue();
				bestAction = entry.getKey();
			}
		}		
		
		return new BestResult(bestV, bestAction);
	}
	
	public double findMaxDiff(Map<ReactiveState, Double> oldV, Map<ReactiveState, Double> newV) {
		double maxDiff = -1.0;

		for (ReactiveState key : oldV.keySet()) {
			double diff = Math.abs(oldV.get(key) - newV.get(key));
			if (diff > maxDiff) maxDiff = diff;
		}
		return maxDiff;
	}
	
	public ReactiveState FindState(City currentCity, Task currentTask) {
		
		for (ReactiveState state : stateList) {
			
			if (state.getCurrentCity().id == currentCity.id) {											// find same city
				if (currentTask == null) {
					if (state.taskAvailable() == false) return state;									// no task
				}
				else if (state.getDestinationCity() != null) {
					if (state.getDestinationCity().id == currentTask.deliveryCity.id) return state;		// found same task
				}
			}			
		}		

		return null;																					// should never go here
	}
	
}
