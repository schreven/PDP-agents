package solution;


import java.util.Random;

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

public class ReactiveDummy implements ReactiveBehavior {

	private Random random;
	private int numActions;
	private Agent myAgent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		this.random = new Random();
		this.numActions = 0;
		this.myAgent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();
		boolean areNeighbors = AreNeighbors(currentCity,availableTask);

		if (availableTask == null || !areNeighbors) {
			if (numActions >= 1) {
				System.out.println("TASK" + (availableTask==null)+"TEST"+AreNeighbors(vehicle.getCurrentCity(),availableTask));
			}
			
			action = new Move(currentCity.randomNeighbor(random));
		} 
		else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	public boolean AreNeighbors(City currentCity, Task currentTask) {
		
		if (currentTask == null) {
			return false;
		}
		for (City neighbor : currentCity.neighbors()) {
			if (neighbor.id == currentTask.deliveryCity.id) {
				return true;
			}
						
		}
		return false;
	}
}

