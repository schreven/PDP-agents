package solution4;

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
import solution2.AuctionAction;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionSolution4 implements AuctionBehavior {

	private Random random;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	
	private List<Vehicle> myVehicles;
	private Map<Vehicle, List<AuctionAction>> myVehicleActions;
	private Map<Vehicle, City> myCurrentCities;
	private Vehicle myChosenVehicle;
	private int newPickupPosition;
	private int newDeliveryPosition;
	
	private int opponentVehicleId;
	private List<City> opponentCurrentCities;
	
	private double myProfit;
	private double opponentProfit;
	
	private final double inf = Double.POSITIVE_INFINITY;
	private final int opponentVehicleAmount = 3;
	
	private List<Long> myBids;
	private List<Long> opponentBids;
	private int opponentId;
	
	//private List<Task> myTasks;
	//private List<Task> opponentTasks;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		//long seed = -9019554669489983951L * myVehicles.get(0).homeCity().hashCode() * agent.id();
		long seed = 1;
		this.random = new Random(seed);
		
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.myVehicles = agent.vehicles();
		this.myVehicleActions = new HashMap<Vehicle, List<AuctionAction>>();
		this.myCurrentCities = new HashMap<Vehicle, City>();
		
		//set myCurrentCities
		for (Vehicle vehicle: myVehicles) {
			this.myVehicleActions.put(vehicle, new ArrayList<AuctionAction>());
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
			
			System.out.println("won!");
			
			List<AuctionAction> l = myVehicleActions.get(myChosenVehicle);
			
			//Set the new pickup in the vehicles list
			
			AuctionAction newPickup = new AuctionAction(previous,true);
			if (newPickupPosition == l.size()) {
				l.add(newPickup);
			}
			else {
				l.add(l.get(l.size()-1));
				for (int i = l.size()-2; i>newPickupPosition ; i--) {
					l.set(i, l.get(i-1));
				}
				l.set(newPickupPosition, newPickup);
			}
			
			//Set the new Delivery in the vehicles list
			AuctionAction newDelivery = new AuctionAction(previous,false);
			if (newDeliveryPosition == l.size()) {
				l.add(newDelivery);
			}
			else {
				l.add(l.get(l.size()-1));
				for (int i = l.size()-2; i>newDeliveryPosition ; i--) {
					l.set(i, l.get(i-1));
				}
				l.set(newDeliveryPosition, newDelivery);
			}
			
			
			myVehicleActions.put(myChosenVehicle, l);
			//myCurrentCities.put(myChosenVehicle, previous.deliveryCity);
			myProfit += bids[winner];
			
		}
		else {
			
			System.out.println("lost, opponent bid "+bids[winner]);
			
			//opponentTasks.add(previous);
			//opponentCurrentCities.set(opponentVehicleId, previous.deliveryCity);
			opponentProfit += bids[winner];
		}
		
		myBids.add(bids[agent.id()]);		
		opponentBids.add(bids[opponentId]);
	}
	
	@Override
	public Long askPrice(Task task) {
	
		/*
		//Verify that at least one vehicle has the capacity to carry the task
		for (Vehicle vehicle: myVehicles){
			if (vehicle.capacity() > task.weight) {
			break;}
			return null;
		}
		*/
		double myMarginalCost = inf;
		City cityPrePickup;
		City cityPostPickup;
		City cityPreDelivery;
		City cityPostDelivery;
		
		/* Find my vehicle performing task for the lowest cost and corresponding cost*/
		for (Vehicle vehicle: myVehicles) {
			//Loop over possible pickup placements
			for(int i = 0; i<=myVehicleActions.get(vehicle).size(); i++) {
				//Loop over possible delivery placements
				for (int j = i; j <= myVehicleActions.get(vehicle).size(); j++) {
					/* Handle special cases for the edges */
					int pickupNotLast = 1;
					int deliveryNotLast = 1;
					if(!verifyCapacity(vehicle,task,i,j)) continue;
					if (i==0) cityPrePickup = vehicle.homeCity();
					else cityPrePickup = myVehicleActions.get(vehicle).get(i-1).getCity();
					if (i == myVehicleActions.get(vehicle).size()) {
						cityPostPickup = task.pickupCity;
						pickupNotLast = 0;
					}
					else cityPostPickup = myVehicleActions.get(vehicle).get(i).getCity();
					if (j==i) cityPreDelivery = task.pickupCity;
					else cityPreDelivery = myVehicleActions.get(vehicle).get(j-1).getCity();
					if (j==myVehicleActions.get(vehicle).size()) {
						cityPostDelivery = task.deliveryCity;
						deliveryNotLast = 0;
					}
					else cityPostDelivery = myVehicleActions.get(vehicle).get(j).getCity();
					
					/* calculate the difference in distance for placing the actions
					  on this vehicle at these positions*/
					long distanceSum = -cityPrePickup.distanceUnitsTo(cityPostPickup)*pickupNotLast
							+cityPrePickup.distanceUnitsTo(task.pickupCity)
							+task.pickupCity.distanceUnitsTo(cityPostPickup)*pickupNotLast
							-cityPreDelivery.distanceUnitsTo(cityPostDelivery)*deliveryNotLast
							+cityPreDelivery.distanceUnitsTo(task.deliveryCity)
							+task.deliveryCity.distanceUnitsTo(cityPostDelivery)*deliveryNotLast;
						
					double tempMarginalCost = Measures.unitsToKM(distanceSum
							* vehicle.costPerKm());
					if (tempMarginalCost<myMarginalCost) {
						myMarginalCost = tempMarginalCost;
						myChosenVehicle = vehicle;
						newPickupPosition = i;
						newDeliveryPosition = j+1;
					}
				}
			}
			
			
		}
		
		
		//find opponent vehicle performing task for the lowest cost, and corresponding cost
		
		double opponentMarginalCost = inf;
		for (int i = 0; i<opponentVehicleAmount; i++) {
			long distanceSum = task.pickupCity.distanceUnitsTo(task.deliveryCity)
					+ opponentCurrentCities.get(i).distanceUnitsTo(task.pickupCity);
			
			double tempMarginalCost = Measures.unitsToKM(distanceSum
							* myVehicles.get(0).costPerKm());
			if (tempMarginalCost<opponentMarginalCost) {
				opponentMarginalCost = tempMarginalCost;
				opponentVehicleId = i;
			}
		}
		
		
		double bid;
		/*
		//Limit opponent winnings if his marginal cost is lower
		if (myMarginalCost>=opponentMarginalCost) {
			bid = myMarginalCost;
		}
		//Aim for some winnings if my marginal cost is lower
		else {
			bid = (1./4)*myMarginalCost + (3./4)*opponentMarginalCost;
			//bid = (1./4)*myMarginalCost + (3./4)*calculateAverage(opponentBids);
		}
		*/
		
		//double myMarginalCost_tmp = myMarginalCost;
		myMarginalCost = useTaskDistrubution(myMarginalCost, task.pickupCity, myVehicleActions.get(myChosenVehicle));
		
		long movingAverage = calculateAverage(opponentBids.subList(Math.max(opponentBids.size() - 3, 0), opponentBids.size()));		
		
		bid = (1./4)*myMarginalCost + (3./4)*movingAverage;
		
		//bid = useTaskDistrubution(bid, task.pickupCity, myVehicleActions.get(myChosenVehicle));
		
		//System.out.println("cost: " + (int)myMarginalCost + " myMarginalCost: " + (int)bid);
		
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

		for (AuctionAction action : myVehicleActions.get(vehicle)) {
			// move: current city => location
			for (City city : current.pathTo(action.getCity()))
				plan.appendMove(city);
			
			if (action.isPickup()) plan.appendPickup(action.getTask());
			else plan.appendDelivery(action.getTask());

			// set current city
			current = action.getCity();
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
	
	private Boolean verifyCapacity(Vehicle vehicle, Task addedTask, int indexPickup, int indexDelivery) {
		int capacity = 0;
		for (int i=0; i<indexPickup; i++) {
			if(myVehicleActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehicleActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehicleActions.get(vehicle).get(i).getTask().weight;
		}
		capacity+=addedTask.weight;
		if(capacity>vehicle.capacity()) return false;
		for (int i=indexPickup; i<indexDelivery; i++) {
			if(myVehicleActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehicleActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehicleActions.get(vehicle).get(i).getTask().weight;
			if(capacity>vehicle.capacity()) return false;
		}
		capacity-=addedTask.weight;
		for (int i=indexDelivery; i<myVehicleActions.get(vehicle).size(); i++) {
			if(myVehicleActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehicleActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehicleActions.get(vehicle).get(i).getTask().weight;
			if(capacity>vehicle.capacity()) return false;
		}
		return true;

	}
	
	private double useTaskDistrubution(double initialBid, City taskPickUpCity, List<AuctionAction> currentCities) {
		// reduces bid if there is a high chance that a task to the taskPickUpCity happens. (or if we already have a task to go to this city?)
		
		double prob = 0;
		int cityCount = 0;
		
		// the more task there are, the less chances there is to have a desired task in the future
		// double prob_threshold = 1/(1+myBids.size());		
		double prob_threshold = 0.1;
				
		// compute the joint probability that a task from a destination city to the desired pickUpCity will be asked.
		for (AuctionAction action : currentCities) {
			if (action.isPickup()) continue;
			
			// however, if we already have a task to go there, aggressively bet to be sure to have the task
			// if (action.getCity().id == taskPickUpCity.id) return initialBid*0.75;
			
			City deliveryCity = action.getCity();
			
			prob += distribution.probability(deliveryCity, taskPickUpCity);		
			cityCount++;
		}
		
		if (cityCount != 0) prob /= cityCount;
		
		System.out.println("joint prob: "+prob);		
		
		// Modify the bid according to the probability
		// if (prob > prob_threshold) return initialBid *= (1-prob_threshold);
		if (prob > prob_threshold) return initialBid *= 0.8;
				
		return initialBid;
	}
	
}
