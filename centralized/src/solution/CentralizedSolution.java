package solution;

//the list of imports
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


@SuppressWarnings("unused")
public class CentralizedSolution implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    

    // max iteration allowed to find local optimal.
    private final int maxIterations = 10000;
    
    /* CSP Variables */  
    // nextTask is divided in two arrays because we have two key types (Task and Vehicle)
    private Map<CentralizedAction, CentralizedAction> nextActionA;
    private Map<Vehicle, CentralizedAction> nextActionV;
    private Map<CentralizedAction, Integer> time;
    private Map<CentralizedAction, Vehicle> vehicle;
    private CentralizedPlan currentPlan;
    
    private Map<Vehicle, Integer> availableCapacity; 
    
    //Map a pickup action with its delivery action
    private Map<CentralizedAction,CentralizedAction> correspondingDelivery;

    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config\\settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;	
        this.nextActionA = new HashMap<CentralizedAction, CentralizedAction>();
        this.nextActionV = new HashMap<Vehicle, CentralizedAction>();
        this.time = new HashMap<CentralizedAction, Integer>();
        this.vehicle = new HashMap<CentralizedAction, Vehicle>();
        this.currentPlan = new CentralizedPlan(nextActionA, nextActionV, time, vehicle);
        this.correspondingDelivery = new HashMap<CentralizedAction,CentralizedAction>();
    }

    // List of the plan for each vehicle
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis(); 	// save current time to measure computation duration
        double totalCost = 0;							// total cost of a solution
        double oldTotalCost = 0;						// total cost of a previous solution
        Set<CentralizedAction> actions = toActionSet(tasks);
        
        
        /* Use Constraint Satisfaction Problem Optimization to find list of plans*/
        List<Plan> plans = new ArrayList<Plan>();
        List<Plan> oldPlans = new ArrayList<Plan>();
        
        // Initialize variables with non optimized plan.
        // Fails if a task has a weight bigger than any vehicle capacity.
        try {
			SelectInitialSolution(vehicles, actions);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
        
        // create Array of Logist plans and compute initial costs
    	plans = ComputePlans();
    	oldTotalCost = ComputeCost(plans, vehicles);
        
        // loop until criterion reached
        for (int i = 0; i < maxIterations; i++) {
        	
        	// Save current solution
        	oldPlans = plans;
        
        	// find neighbor solution
        	do {
        		ChooseNeighbours();
        	} 
        	while (!VerifyConstraints()); // verify if solution meet constraints. if not, start again.
        	        	
            // create Array of Logist plans and compute cost of the solution (i.e. objective function)
        	plans = ComputePlans();
        	totalCost = ComputeCost(plans, vehicles);        
	        
	        // if cost is better (i.e. lower), keep it. otherwise start again from previous solution.
	        if (oldTotalCost < totalCost) {
	        	plans = oldPlans;
	        }
	        else {
	        	// if we keep the new plan, replace the oldTotalCost by the new one
	        	oldTotalCost = totalCost;
	        }	        
	                
        }
        /* end */
        
        // compute plan computation duration
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        return plans;
    }
    
    private void SelectInitialSolution(List<Vehicle> vehicles, Set<CentralizedAction> actions) throws Exception {
    	/* Give all the tasks to the biggest vehicle. Only updates global variables*/
    	/* The vehicle only carries one task at a time with this plan*/
    	
    	int order = 1;			// order in which the task are handled by a vehicle
    	CentralizedAction prevAction = null;	// previous action carried by a vehicle
    	    	
    	// find biggest vehicle, i.e. vehicle with largest capacity
    	Vehicle bigVehicle = vehicles.get(0);
    	for (Vehicle vehicle : vehicles) {
    		if (vehicle.capacity() > bigVehicle.capacity()) {
    			bigVehicle = vehicle;
    		}    		
    	}
    	
    	// iterate over task set
    	for (CentralizedAction action : actions) { 
    		//select only pickups in the loop
    		if (!action.isPickup()) continue;
    		
    		// if the biggest vehicle cannot carry a task, the problem is unsolvable.
    		if (action.getTask().weight > bigVehicle.capacity()) {
    			throw new Exception("The task (id: "+action.getTask().id+") has a weight too big ! Problem unsolvable.");  	
    		}
 
    		// fill arrays for pickup
    		if (prevAction != null) this.nextActionA.put(prevAction, action);  
    		this.nextActionV.put(bigVehicle, action);
    		this.time.put(action, order);
    		this.vehicle.put(action, bigVehicle);
    		order++;
    		// fill arrays for delivery
    		this.nextActionA.put(action, correspondingDelivery.get(action));
    		this.nextActionA.put(correspondingDelivery.get(action), null);
    		this.nextActionV.put(bigVehicle, correspondingDelivery.get(action));
    		this.time.put(correspondingDelivery.get(action), order);
    		this.vehicle.put(correspondingDelivery.get(action), bigVehicle);
    		order++;
    		prevAction = correspondingDelivery.get(action);

    	}   
    	//set the current plan
    	this.currentPlan = new CentralizedPlan(this.nextActionA,this.nextActionV, this.time, this.vehicle);
    	
    	if (!currentPlan.verifyConstraints(vehicles, actions)) {
    		throw new Exception("Problem solved, but did not meet the constraints");  	// shouldn't go here
    	}
    	
    }
    
    
    private List<CentralizedPlan> ChooseNeighbours(List<Vehicle> vehicles, List<CentralizedPlan> plans) {
    	/* Find neighbours to the current solution */
    	List<CentralizedPlan> neighbourPlans = new ArrayList<CentralizedPlan>();
    	
    	
    	//select one random vehicle "transmitting" his tasks
    	Random randomizer = new Random();
    	Vehicle randomVehicle = vehicles.get(randomizer.nextInt(vehicles.size()));
    	
    	for (Vehicle vehicleTemp : vehicles) {
    		if (vehicleTemp == randomVehicle) continue;
    		newPlan = changingVehicle(nextActionV.get(randomVehicle),vehicleTemp))
    		
    	}
    	
    	
    }
    
    
    
    
    private Set<CentralizedAction> toActionSet(TaskSet tasks){
    	Set<CentralizedAction> actionSet = new HashSet<CentralizedAction>();
    	for (Task taskTemp : tasks) {
    		//add the corresponding pickup and delivery
    		CentralizedAction pickupAction = new CentralizedAction(taskTemp,true);
    		actionSet.add(pickupAction);
    		CentralizedAction deliveryAction = new CentralizedAction(taskTemp,false);
    		actionSet.add(deliveryAction);
    		//updating the mapping
    		this.correspondingDelivery.put(pickupAction, deliveryAction);
    	}
    	return actionSet;
    }
    
    private CentralizedPlan changingVehicle(Vehicle vehicleGive, Vehicle vehicleReceive) {
    	CentralizedPlan newPlan;// = new CentralizedPlan();
    	CentralizedAction transmittedPickup = nextActionV.get(vehicleGive);
    	CentralizedAction tranmittedDelivery = this.correspondingDelivery.get(transmittedPickup);
    	
    	nextActionV.put(vehicleGive, nextActionA.get(transmittedPickup));
    	nextActionA.put(transmittedPickup, nextActionV.get(vehicleReceive));
    	nextActionV.put(vehicleReceive, transmittedPickup);
    	vehicle.put(transmittedPickup, vehicleReceive);
    	
    	
    	
    	
    	return newPlan;
    	
    }
}
