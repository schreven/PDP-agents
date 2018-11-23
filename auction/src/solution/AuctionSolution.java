package solution;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionSolution implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	
	private List<Long[]> previousBids;
	private List<Integer> occupiedCities;
	private List<Integer> winnerList;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		
		this.previousBids = new ArrayList<Long[]>();
		this.occupiedCities = new ArrayList<Integer>();
		this.winnerList = new ArrayList<Integer>();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
		
		// save info about last auction
		this.winnerList.add(winner);
		this.previousBids.add(bids.clone());
		this.occupiedCities.add((previous.deliveryCity).id);
	}
	
	
	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight) return null;

		// Compute marginal costs
		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		//From template: ask for marginal cost + random profit 		
		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		/* ANALYSIS OF REWARD PROBABILITIES */
		// Compute the expected profit of the destination city. If it is high enough, we might reduce the bid to
		// maximize the chance to have the next task.
		double expectedReward = 0;
		for (City city : topology.cities()) {
			// consider only tasks that can be carried
			if (distribution.weight(task.deliveryCity, city) <= vehicle.capacity()-task.weight) {
				
				
				double futureCityProb = distribution.probability(task.deliveryCity, city);
				double futureDistance = (task.deliveryCity).distanceTo(city);
				
				// is it better to look at the sum of the expected rewards or at the max ? (two possible agents)
				// here, the expected reward is the minimum that we could ask
				expectedReward += futureCityProb * vehicle.costPerKm()*futureDistance;
			}
		}
		// If the expected profit is higher than the reward of the auctioned task: 
		// (i.e. the reward might increase if we win!)
		if (expectedReward >= bid) {
			// we reduce the bid by the same ratio between the expected reward and the reward
			double diff = bid/expectedReward;
			bid -= bid*diff;
		}
		
		/* ANALYSIS OF COMPETITORS */
		// look at competitors bid and always bid in the same range.
		// figure where the winners are and go where the expected rewards are high AND where there are less agents.
		// we assume that a winner is at the destination city of the task he won.
				
		// TODO, using values saved in global
		
		// security to avoid deficit
		if (bid < marginalCost) bid = marginalCost;
		
		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		System.out.println("Agent " + agent.name() + " has "+agent.getTotalTasks()+"/"+tasks.size()+" tasks.");
		
		// TODO replace the naive plan by an optimized one (centralized tp?)

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
