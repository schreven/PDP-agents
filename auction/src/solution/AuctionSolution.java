package solution;

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
public class AuctionSolution implements AuctionBehavior {

	private final int aggressiveRounds = 3;
	private Random random;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	
	private List<Vehicle> myVehicles;
	private Map<Vehicle, List<AuctionAction>> myVehiclesActions;
	private Map<Vehicle, City> myCurrentCities;
	private Vehicle myChosenVehicle;
	private int newPickupPosition;
	private int newDeliveryPosition;
	private List<Long> myBids;
	private double myProfit;
	
	private int opponentId;
	private City oppHomeCity;
	private int oppCostPerKm;
	private List<AuctionAction> opponentVehicleActions;
	private int newOppPickupPosition;
	private int newOppDeliveryPosition;
	private List<Long> opponentBids;
	private double opponentProfit;
	private double opponentMarginalCost;
	private long movingAverage;
	
	private double errOpponentCost;
	private double errMovingAverage;
	
	private List<City> opponentCurrentCities;
	private int roundsWon;
	
	
	
	private final double inf = Double.POSITIVE_INFINITY;
	
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		
		
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.myVehicles = agent.vehicles();
		this.myVehiclesActions = new HashMap<Vehicle, List<AuctionAction>>();
		this.myCurrentCities = new HashMap<Vehicle, City>();
		this.myBids = new ArrayList<Long>();
		this.myProfit = 0;
		this.roundsWon = 0;
		
		long seed = -9019554669489983951L * myVehicles.get(0).homeCity().hashCode() * agent.id();
		this.random = new Random(seed);
		
		//set myCurrentCities
		for (Vehicle vehicle: myVehicles) {
			this.myVehiclesActions.put(vehicle, new ArrayList<AuctionAction>());
			this.myCurrentCities.put(vehicle, vehicle.homeCity());
		}
		
		this.opponentId = -agent.id() +1; 
		this.oppHomeCity = topology.cities().get(random.nextInt(myVehicles.size()));
		int costPerKmSum = 0;
	    for (int i = 0; i<myVehicles.size();i++) {
	        costPerKmSum += myVehicles.get(i).costPerKm();
	    }
		this.oppCostPerKm = costPerKmSum/myVehicles.size();
		this.opponentVehicleActions = new ArrayList<AuctionAction>();
		this.opponentBids = new ArrayList<Long>();
		this.opponentProfit = 0;
		
		this.opponentCurrentCities = new ArrayList<City>();
		this.errOpponentCost = 1;
		this.errMovingAverage = 1;

	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println(this.oppCostPerKm);
		if (winner == agent.id()) {
			
			System.out.println("won!");
			
			List<AuctionAction> l = myVehiclesActions.get(myChosenVehicle);
			
			//Set the new Pickup in the vehicles list
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
			
			
			myVehiclesActions.put(myChosenVehicle, l);
			myProfit += bids[winner];
			roundsWon +=1;
			
		}
		else {
			System.out.println("lost, opponent bid "+bids[winner]);
			
			List<AuctionAction> l = opponentVehicleActions;
			
			//Set the new Pickup in the vehicles list
			AuctionAction newPickup = new AuctionAction(previous,true);
			if (newOppPickupPosition == l.size()) {
				l.add(newPickup);
			}
			else {
				l.add(l.get(l.size()-1));
				for (int i = l.size()-2; i>newOppPickupPosition ; i--) {
					l.set(i, l.get(i-1));
				}
				l.set(newOppPickupPosition, newPickup);
			}
			
			//Set the new Delivery in the vehicles list
			AuctionAction newDelivery = new AuctionAction(previous,false);
			if (newOppDeliveryPosition == l.size()) {
				l.add(newDelivery);
			}
			else {
				l.add(l.get(l.size()-1));
				for (int i = l.size()-2; i>newOppDeliveryPosition ; i--) {
					l.set(i, l.get(i-1));
				}
				l.set(newOppDeliveryPosition, newDelivery);
			}
			
			
			opponentVehicleActions= l;
			
			opponentProfit += bids[winner];
		}
		
		myBids.add(bids[agent.id()]);		
		opponentBids.add(bids[opponentId]);
		errOpponentCost = Math.abs(bids[opponentId]-opponentMarginalCost);
		errMovingAverage = Math.abs(bids[opponentId]-movingAverage);
	}
	
	@Override
	public Long askPrice(Task task) {

		City cityPrePickup;
		City cityPostPickup;
		City cityPreDelivery;
		City cityPostDelivery;
		
		/* Find my vehicle performing task for the lowest cost and corresponding cost*/
		double myMarginalCost = inf;
		for (Vehicle vehicle: myVehicles) {
			//Loop over possible pickup placements
			for(int i = 0; i<=myVehiclesActions.get(vehicle).size(); i++) {
				//Loop over possible delivery placements
				for (int j = i; j <= myVehiclesActions.get(vehicle).size(); j++) {
					/* Handle special cases for the edges */
					int pickupNotLast = 1;
					int deliveryNotLast = 1;
					if(!verifyCapacity(vehicle,task,i,j)) continue;
					if (i==0) cityPrePickup = vehicle.homeCity();
					else cityPrePickup = myVehiclesActions.get(vehicle).get(i-1).getCity();
					if (i == myVehiclesActions.get(vehicle).size()) {
						cityPostPickup = task.pickupCity;
						pickupNotLast = 0;
					}
					else cityPostPickup = myVehiclesActions.get(vehicle).get(i).getCity();
					if (j==i) cityPreDelivery = task.pickupCity;
					else cityPreDelivery = myVehiclesActions.get(vehicle).get(j-1).getCity();
					if (j==myVehiclesActions.get(vehicle).size()) {
						cityPostDelivery = task.deliveryCity;
						deliveryNotLast = 0;
					}
					else cityPostDelivery = myVehiclesActions.get(vehicle).get(j).getCity();
					
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
		
		
		/*Estimate opponent marginal cost
		 *Assuming opponent has a single vehicle with unlimited capacity
		 *and cost per km the average of ours */
		opponentMarginalCost = inf;
		//Loop over possible pickup placements
		for(int i = 0; i<=opponentVehicleActions.size(); i++) {
			//Loop over possible delivery placements
			for (int j = i; j <= opponentVehicleActions.size(); j++) {
				/* Handle special cases for the edges */
				int pickupNotLast = 1;
				int deliveryNotLast = 1;
				if (i==0) cityPrePickup = oppHomeCity;
				else cityPrePickup = opponentVehicleActions.get(i-1).getCity();
				if (i == opponentVehicleActions.size()) {
					cityPostPickup = task.pickupCity;
					pickupNotLast = 0;
				}
				else cityPostPickup = opponentVehicleActions.get(i).getCity();
				if (j==i) cityPreDelivery = task.pickupCity;
				else cityPreDelivery = opponentVehicleActions.get(j-1).getCity();
				if (j==opponentVehicleActions.size()) {
					cityPostDelivery = task.deliveryCity;
					deliveryNotLast = 0;
				}
				else cityPostDelivery = opponentVehicleActions.get(j).getCity();
				
				/* calculate the difference in distance for placing the actions
				  on this vehicle at these positions*/
				long distanceSum = -cityPrePickup.distanceUnitsTo(cityPostPickup)*pickupNotLast
						+cityPrePickup.distanceUnitsTo(task.pickupCity)
						+task.pickupCity.distanceUnitsTo(cityPostPickup)*pickupNotLast
						-cityPreDelivery.distanceUnitsTo(cityPostDelivery)*deliveryNotLast
						+cityPreDelivery.distanceUnitsTo(task.deliveryCity)
						+task.deliveryCity.distanceUnitsTo(cityPostDelivery)*deliveryNotLast;
					
				double tempMarginalCost = Measures.unitsToKM(distanceSum
						* oppCostPerKm);
				if (tempMarginalCost<opponentMarginalCost) {
					opponentMarginalCost = tempMarginalCost;
					newOppPickupPosition = i;
					newOppDeliveryPosition = j+1;
				}
			}
		}
		
		
		
		double bid;
		
		//use task distribution to refine my marginal cost
		myMarginalCost = useTaskDistrubution(myMarginalCost, task.pickupCity, myVehiclesActions.get(myChosenVehicle));
		
		//moving average over bids of opponent
		movingAverage = calculateAverage(opponentBids.subList(Math.max(opponentBids.size() - 3, 0), opponentBids.size()));		
		
		double opponentEstimate = (errMovingAverage*opponentMarginalCost 
				+ errOpponentCost*errMovingAverage)
				/(errMovingAverage + errOpponentCost);
		

		if(roundsWon<aggressiveRounds) {
			bid = (1./3)*myMarginalCost+ (2./3)*(roundsWon/(aggressiveRounds-1))*myMarginalCost;
		}
		else {
		bid = (1./4)*myMarginalCost + (3./4)*opponentEstimate;
		//if we estimate opponent has a lower marginal cost
		//if(bid<myMarginalCost) {
		//	bid = myMarginalCost;
		//}
		}

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

		for (AuctionAction action : myVehiclesActions.get(vehicle)) {
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
			if(myVehiclesActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehiclesActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehiclesActions.get(vehicle).get(i).getTask().weight;
		}
		capacity+=addedTask.weight;
		if(capacity>vehicle.capacity()) return false;
		for (int i=indexPickup; i<indexDelivery; i++) {
			if(myVehiclesActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehiclesActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehiclesActions.get(vehicle).get(i).getTask().weight;
			if(capacity>vehicle.capacity()) return false;
		}
		capacity-=addedTask.weight;
		for (int i=indexDelivery; i<myVehiclesActions.get(vehicle).size(); i++) {
			if(myVehiclesActions.get(vehicle).get(i).isPickup()) 
				capacity+=myVehiclesActions.get(vehicle).get(i).getTask().weight;
			else
				capacity-=myVehiclesActions.get(vehicle).get(i).getTask().weight;
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
		// double prob_threshold = 0.1;
				
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
		
		//System.out.println("joint prob: "+prob);		
		
		// Modify the bid according to the probability
		// if (prob > prob_threshold) return initialBid *= (1-prob_threshold);
		// if (prob > prob_threshold) return initialBid *= 0.8;
				
		return initialBid * (1-prob);
	}
	
}
