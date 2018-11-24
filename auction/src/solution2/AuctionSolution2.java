package solution2;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
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
public class AuctionSolution2 implements AuctionBehavior {

	private Random random;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	
	private List<Vehicle> myVehicles;
	private Map<Vehicle, List<Task>> myVehicleTasks;
	private Map<Vehicle, City> myCurrentCities;
	private Vehicle myChosenVehicle;
	
	private int opponentVehicleId;
	private List<City> opponentCurrentCities;
	
	private double myProfit;
	private double opponentProfit;
	
	private final double inf = Double.POSITIVE_INFINITY;
	private final int opponentVehicleAmount = 3;
	
	private List<Long> myBids;
	private List<Long> opponentBids;
	private long averageOpponentBid;
	private int opponentId;
	
	
	//private List<Task> myTasks;
	//private List<Task> opponentTasks;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		//long seed = -9019554669489983951L * myVehicles.get(0).homeCity().hashCode() * agent.id();
		long seed = 1;
		this.random = new Random(seed);
		
		System.out.println("test0");

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.myVehicles = agent.vehicles();
		this.myVehicleTasks = new HashMap<Vehicle, List<Task>>();
		this.myCurrentCities = new HashMap<Vehicle, City>();
		
		//set myCurrentCities
		for (Vehicle vehicle: myVehicles) {
			this.myVehicleTasks.put(vehicle, new ArrayList<Task>());
			this.myCurrentCities.put(vehicle, vehicle.homeCity());
		}
		this.opponentCurrentCities = new ArrayList<City>();
		
		//Randomly setting opponentCurrentCities
		for (int i = 0; i<opponentVehicleAmount; i++) {
			this.opponentCurrentCities.add(topology.cities().get(random.nextInt(myVehicles.size())));
		}
		
		this.myProfit = 0;
		this.opponentProfit = 0;
		
		this.myBids = new ArrayList<Long>();
		this.opponentBids = new ArrayList<Long>();
		this.opponentId = -agent.id() +1; 
			
		
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
	
		if (winner == agent.id()) {
			//myTasks.add(previous);
			List<Task> l = myVehicleTasks.get(myChosenVehicle);
			l.add(previous);
			System.out.println("test1");
			myVehicleTasks.put(myChosenVehicle, l);
			myCurrentCities.put(myChosenVehicle, previous.deliveryCity);
			myProfit += bids[winner];
			
		}
		else {
			//opponentTasks.add(previous);
			opponentCurrentCities.set(opponentVehicleId, previous.deliveryCity);
			opponentProfit += bids[winner];
		}
		
		myBids.add(bids[agent.id()]);
		opponentBids.add(bids[opponentId]);
		averageOpponentBid = calculateAverage(opponentBids);
		System.out.println(averageOpponentBid);
	}
	
	@Override
	public Long askPrice(Task task) {
	
		//Verify that at least one vehicle has the capacity to carry the task
		for (Vehicle vehicle: myVehicles){
			if (vehicle.capacity() > task.weight) {
			break;}
			return null;
		}

		System.out.println("test2");
		
		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		
		
		//find my vehicle performing task for the lowest cost and corresponding cost
		double myMarginalCost = inf;
		for (int i = 0; i<myVehicles.size(); i++) {
			long distanceSum = distanceTask
					+ myCurrentCities.get(myVehicles.get(i)).distanceUnitsTo(task.pickupCity);
			
			double tempMarginalCost = Measures.unitsToKM(distanceSum
							* myVehicles.get(i).costPerKm());
			if (tempMarginalCost<myMarginalCost) {
				myMarginalCost = tempMarginalCost;
				myChosenVehicle = myVehicles.get(i);
			}
		}
		
		
		//find opponent vehicle performing task for the lowest cost and corresponding cost
		double opponentMarginalCost = inf;
		for (int i = 0; i<opponentVehicleAmount; i++) {
			long distanceSum = distanceTask
					+ opponentCurrentCities.get(i).distanceUnitsTo(task.pickupCity);
			
			double tempMarginalCost = Measures.unitsToKM(distanceSum
							* myVehicles.get(0).costPerKm());
			if (tempMarginalCost<opponentMarginalCost) {
				opponentMarginalCost = tempMarginalCost;
				opponentVehicleId = i;
			}
		}
		
		double bid;
		//Limit opponent winnings if his marginal cost is lower
		//or if my agent is winning
		if (myMarginalCost>=opponentMarginalCost) {
			bid = myMarginalCost;
		}
		//Aim for some winnings if my marginal cost is lower
		else {
			bid = (1./4)*myMarginalCost + (3./4)*opponentMarginalCost;
			//bid = (1./4)*myMarginalCost + (3./4)*averageOpponentBid;
		}
		//bid = myMarginalCost +800;
		return (long) Math.round(bid);
	}
	

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		List<Plan> plans = new ArrayList<Plan>();
		for (Vehicle vehicle: myVehicles){
			Plan planVehicle = auctionPlan(vehicle, tasks);
			plans.add(planVehicle);
		}
		return plans;
	}

	private Plan auctionPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : myVehicleTasks.get(vehicle)) {
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
	
	private long calculateAverage(List <Long> bids) {
		  long sum = 0;
		  if(!bids.isEmpty()) {
		    for (Long bid : bids) {
		        sum += bid;
		    }
		    return sum / bids.size();
		  }
		  return sum;
	}
}
