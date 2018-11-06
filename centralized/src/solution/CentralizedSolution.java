package solution;

import java.io.File;
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
    private final int maxIterations = 1000;
    
    //vehicles and actions for the planning
    private List<Vehicle> vehicles;
    private Set<CentralizedAction> actions;
    
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

    //test
    
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
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
        //this.currentPlan; = new CentralizedPlan(this.nextActionA, this.nextActionV, 
        //		this.time, this.vehicle, this.vehicles);
        this.correspondingDelivery = new HashMap<CentralizedAction,CentralizedAction>();
    }

    // List of the plan for each vehicle
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis(); 	// save current time to measure computation duration
       
        //instantiate
        this.vehicles = vehicles;
        this.actions = toActionSet(tasks);
        
        
        /* Use Constraint Satisfaction Problem Optimization to find list of plans*/
        List<Plan> plans = new ArrayList<Plan>();
        
        // Initialize variables with non optimized plan.
        // Fails if a task has a weight bigger than any vehicle capacity.
        try {
			SelectInitialSolution();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}     

        List<CentralizedPlan> neighbourPlans;
        CentralizedPlan bestNewPlan = new CentralizedPlan(new HashMap<CentralizedAction, CentralizedAction>(),
        		new HashMap<Vehicle, CentralizedAction>(),
        		new HashMap<CentralizedAction, Integer>(),
        		new HashMap<CentralizedAction, Vehicle>(),
        		new ArrayList<Vehicle>());
        
        // loop until criterion reached
        for (int i = 0; i < maxIterations; i++) {
     
        	/* Find neighbours */
        	neighbourPlans = ChooseNeighbours();
        	System.out.println("Found " + neighbourPlans.size()+ " valid neighbours");

        	
        	/* Select the most promissing one */
        	bestNewPlan = LocalChoice(neighbourPlans);
        	System.out.println("The cost of the the new plan is: " + bestNewPlan.getCost());
        	
        	//experimenting
        	//this.currentPlan = bestNewPlan;
        	setCurrentPlan(bestNewPlan);
        		        
	                
        }
        /* end */
        
        // compute plan computation duration
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        plans = this.currentPlan.getPlan();
        
        return plans;
    }
    
    private void SelectInitialSolution() throws Exception {
    	/* Give all the tasks to the biggest vehicle. Only updates global variables*/
    	/* The vehicle only carries one task at a time with this plan*/
    	
    	Integer order = 1;			// order in which the task are handled by a vehicle
    	CentralizedAction prevAction = null;	// previous action carried by a vehicle
    	    	
    	// find biggest vehicle, i.e. vehicle with largest capacity
    	Vehicle bigVehicle = vehicles.get(0);
    	for (Vehicle vehicle : vehicles) {
    		if (vehicle.capacity() > bigVehicle.capacity()) {
    			bigVehicle = vehicle;
    		}    		
    		//also fill nextActionV
    		nextActionV.put(vehicle, null);
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
    		if (order == 1) this.nextActionV.put(bigVehicle, action);
    		this.time.put(action, order);
    		this.vehicle.put(action, bigVehicle);
    		order++;
    		
    		// fill arrays for delivery
    		this.nextActionA.put(action, correspondingDelivery.get(action));
    		this.nextActionA.put(correspondingDelivery.get(action), null);
    		this.time.put(correspondingDelivery.get(action), order);
    		this.vehicle.put(correspondingDelivery.get(action), bigVehicle);
    		order++;
    		prevAction = correspondingDelivery.get(action);
    	}   
    	//set the current plan
    	this.currentPlan = new CentralizedPlan(this.nextActionA,this.nextActionV, 
    			this.time, this.vehicle, this.vehicles);
    	
    	
    	if (!verifyConstraints()) {
    		// verify if solution meet constraints
    		throw new Exception("Problem solved, but did not meet the constraints");  	// shouldn't go here
    	}
    	
    }
    
    
    private List<CentralizedPlan> ChooseNeighbours() {
    	resetCSPVariables();
    	
    	/* Find neighbours to the current solution */
    	List<CentralizedPlan> neighbourPlans = new ArrayList<CentralizedPlan>();
    	
    	//select one random vehicle "transmitting" his tasks and "shuffling" them
    	Vehicle chosenVehicle;
    	do {
    	Random randomizer = new Random();
    	chosenVehicle = vehicles.get(randomizer.nextInt(vehicles.size()));
    	} while(this.nextActionV.get(chosenVehicle)==null);
    	
    	//not problem here, is not null.
    	if (this.nextActionV.get(chosenVehicle)==null) {
    		System.out.println("is chosen vehicle null?");
    	}
    	
    	/*creating new plans with a task transmitted to another vehicle*/
    	for (Vehicle vehicleTemp : vehicles) {
    		if (vehicleTemp == chosenVehicle) continue;
    		CentralizedPlan newPlan = changingVehicle(chosenVehicle,vehicleTemp);
    		if (newPlan != null) {
    			neighbourPlans.add(newPlan);
    		}

	
    	}
    	

    	
    	/*creating new plans with the tasks for the vehicle shuffled*/
    	CentralizedAction actionTemp = nextActionV.get(chosenVehicle);
    	Integer lengthChosenVehicle = 0;
    	//compute number of actions for the vehicle
    	while(actionTemp != null) {
    		lengthChosenVehicle +=1;
    		actionTemp = nextActionA.get(actionTemp);
    	}
    	//if the vehicle has at least two tasks
    	if (lengthChosenVehicle>=2) {
    		for (int i = 0; i<=lengthChosenVehicle-1; i++) {
    			for (int j = i+1; j< lengthChosenVehicle; j++) {
    				CentralizedPlan newPlan = changingActionOrder(chosenVehicle,i,j);
    				if (newPlan != null) {
    	    			neighbourPlans.add(newPlan);
    	    		}
    			}
    		}
    	}	
    	return neighbourPlans;
    }
    
    /* create new plans by changing a task from a vehicle to another */
    private CentralizedPlan changingVehicle(Vehicle vehicleGive, Vehicle vehicleReceive) {
    	CentralizedPlan newPlan;
    	resetCSPVariables();
    	
    	
    	// PROBLEM HERE transmitted pickup is null
    	CentralizedAction transmittedPickup = this.nextActionV.get(vehicleGive);
    	CentralizedAction transmittedDelivery = this.correspondingDelivery.get(transmittedPickup);
    	
    	if (this.nextActionV.get(vehicleGive) == null) {
    		System.out.println("is null entering changingVehicle");
    	}
    	
    	if (transmittedPickup == null) {
    		System.out.println("test");
    	}
    	
    	
    	
    	//erasing from vehicleGive chain for the pickup action
    	nextActionV.put(vehicleGive, nextActionA.get(transmittedPickup));
    	//erasing from vehicleGive chain  for the delivery action
    	if (nextActionV.get(vehicleGive)==transmittedDelivery) {
    		nextActionV.put(vehicleGive, nextActionA.get(transmittedDelivery));}
    	else {
    		CentralizedAction actionTemp = nextActionV.get(vehicleGive);
			while (nextActionA.get(actionTemp) != null) {
				if (nextActionA.get(actionTemp) == transmittedDelivery) {
					nextActionA.put(actionTemp,nextActionA.get(transmittedDelivery));
					break;
				}
				actionTemp = nextActionA.get(actionTemp);
			}
    	}
    	//update vehicleReceive chain
    	nextActionA.put(transmittedPickup, transmittedDelivery);
    	nextActionA.put(transmittedDelivery, nextActionV.get(vehicleReceive));
    	nextActionV.put(vehicleReceive, transmittedPickup);
   	
    	updateTime(vehicleGive);
    	updateTime(vehicleReceive);
    	
    	
    	vehicle.put(transmittedPickup, vehicleReceive);
    	vehicle.put(transmittedDelivery, vehicleReceive);

    	
    	if (verifyConstraints()) {
    		// verify if solution meet constraints
    		newPlan = new CentralizedPlan(nextActionA, nextActionV, time, vehicle, this.vehicles);
    	}
    	else newPlan = null;
    	return newPlan;
    	
    }
    
    
    private CentralizedPlan changingActionOrder(Vehicle vehicle, Integer id1, Integer id2) {
    	CentralizedPlan newPlan;
    	resetCSPVariables();
    	Integer count;
    	
    	CentralizedAction a1;
    	CentralizedAction aPre2;
    	CentralizedAction a2;
    	if (id1==1) {
    		count = 1;
    		Vehicle aPre1 = vehicle; 	//previous "action" of action 1
    		a1 = nextActionV.get(vehicle); //action 1
    		
        	aPre2 = a1;  //previous action of action 2
        	a2 = nextActionA.get(aPre2);
        	count +=1;
        	while(count<id2) {
        		aPre2 = a2;
        		a2 = nextActionA.get(a2);
        		count +=1;
        	}
        	CentralizedAction aPost2 = nextActionA.get(a2); //post action of action 2	
        	nextActionV.put(aPre1, a2);
    	}
    	else {
    		CentralizedAction aPre1 = nextActionV.get(vehicle); //previous action of action 1
    		
    		a1 = nextActionA.get(aPre1);	//action 1
	    	count = 2;
	    	while (count<id1) {
	    		aPre1 =a1;
	    		a1 = nextActionA.get(a1);
	    		count +=1;
	    	}
	    	
	    	
        	aPre2 = a1;  //previous action of action 2
        	a2 = nextActionA.get(aPre2);
        	count +=1;
        	while(count<id2) {
        		aPre2 = a2;
        		a2 = nextActionA.get(a2);
        		count +=1;
        	}
        	nextActionA.put(aPre1, a2);	
    	}
    	
    	CentralizedAction aPost1 = nextActionA.get(a1); //post action of action 1
    	CentralizedAction aPost2 = nextActionA.get(a2); //post action of action 2
    	
    	//exchanging the tasks
    	if (aPost1==a2) {
    		nextActionA.put(a2,a1);
    		nextActionA.put(a1, aPost2);
    	}
    	else {
    		nextActionA.put(aPre2,a1);
    		nextActionA.put(a2,aPost1);
    		nextActionA.put(a1, aPost2);
    	}
    	updateTime(vehicle);
    	
    	if (verifyConstraints()) {
    		// verify if solution meet constraints
    		
    		newPlan = new CentralizedPlan(nextActionA, nextActionV, time, this.vehicle, this.vehicles);
    	}
    	else newPlan = null;
    	
    	return newPlan;
    }
    
    private void updateTime(Vehicle vehicle) {
    	CentralizedAction actionTemp = nextActionV.get(vehicle);
    	Integer timeTemp = 1;
    	while(actionTemp != null) {
    		//update time
    		time.put(actionTemp, timeTemp);
    		//iterate action and time
    		actionTemp = nextActionA.get(actionTemp);
    		timeTemp +=1;
    		
    	}
    }
    
    private CentralizedPlan LocalChoice(List<CentralizedPlan> neighbourPlans) {
    	CentralizedPlan bestPlan = neighbourPlans.get(0);
    	
    	// add current plan to list of compared with 40% chance
  	
    	Random random = new Random();
    	Double probPickSecond = 0.;
    	Double probAddCurrent = 0.4;
    	
    	if (random.nextFloat() < probAddCurrent) {
    		neighbourPlans.add(this.currentPlan);
    	}
    	
    	for (CentralizedPlan plan : neighbourPlans) {
    		
    		//find the best plan
    		if (plan.getCost() < bestPlan.getCost()) {
    			bestPlan = plan;
    		}   
    		else if (plan.getCost() == bestPlan.getCost()) {
    			// if equal cost, choose randomly
    	        if (random.nextInt()%2 == 0) bestPlan = plan;
    		}
    	}
    	
    	if (random.nextFloat() < probPickSecond) {
    		CentralizedPlan secondBestPlan = neighbourPlans.get(0);
    		//find the second next best plan
    		for (CentralizedPlan plan : neighbourPlans) {
    			if (plan == bestPlan) continue;
	    		if (plan.getCost() < secondBestPlan.getCost()) {
	    			secondBestPlan = plan;
	    		}  
    		}
    		bestPlan = secondBestPlan;
    	}
    	
    	return bestPlan;
    }
    
	private boolean verifyConstraints(){
		/* Based on the CSP Variables, verify if all the constraints are met */
		Integer capacityTemp;
		CentralizedAction action;
		
    	for (CentralizedAction actionTemp : nextActionA.keySet()) {
    		// Constraint 1
    		if (actionTemp == nextActionA.get(actionTemp)) {
    			System.out.println("Error: Constraint 1 reached, should not happen");
    			return false;
    		}
    		else if (nextActionA.get(actionTemp)!=null) {
    			// Constraint 3
    			if (time.get(nextActionA.get(actionTemp)) != (time.get(actionTemp) +1 )) {
    				System.out.println("Error: Constraint 3 reached, should not happen");
        			return false;} 
    			//Constraint 5 
    			if (vehicle.get(nextActionA.get(actionTemp))!= vehicle.get(actionTemp)) {
    				System.out.println("Error: Constraint 5 reached, should not happen");
    				return false;}
    		}
    		//Constraint 8
    		if(actionTemp.isPickup()) {
    			if (time.get(actionTemp) > time.get(correspondingDelivery.get(actionTemp))) {
    //				System.out.println("Warning: Constraint 5 reached");
    				return false;}
    			}
    		}
    		
    	
    	for (Vehicle vehicleTemp : nextActionV.keySet()) {
    		if (nextActionV.get(vehicleTemp) != null) {
	    		//Constraint 2
	    		if (time.get(nextActionV.get(vehicleTemp))!=1) {
	    			System.out.println("Error: Constraint 2 reached, should not happen");
	    			return false;
	    		}
	    		//Constraint 4
	    		else if (vehicle.get(nextActionV.get(vehicleTemp))!=vehicleTemp) {
	    			System.out.println("Error: Constraint 4 reached, should not happen");
	    			return false;
	    		}
	    		//Constraint 7
	    		action = nextActionV.get(vehicleTemp);
	    		capacityTemp = vehicleTemp.capacity();
	    		while (action!=null) {
	    			//substract to available capacity if pickup
	    			if (action.isPickup()) capacityTemp -= action.getTask().weight;
	    			//add to available capacity if delivery
	    			else capacityTemp += action.getTask().weight;
	    			if (capacityTemp<0) {
	    //				System.out.println("Warning: [Constraint 7] A plan was refused, not respecting capacity constraint");
	    				return false;
	    			}
	    			action = nextActionA.get(action);
	    		}
    		}

    	}
    	//Constraint 6
    	if ((nextActionA.size()+nextActionV.size())!= (actions.size()+vehicles.size())) {
    		System.out.println("Error: [Constraint 6] A plan was refused, sizes don't match");
    		return false;}
    	
		return true;
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
    
    /*resets the global CSP variable to the current plan*/
    private void resetCSPVariables() {
    	this.nextActionA = new HashMap<CentralizedAction, CentralizedAction>(this.currentPlan.getNextActionA());
        this.nextActionV = new HashMap<Vehicle, CentralizedAction>(this.currentPlan.getNextActionV());
        this.time = new HashMap<CentralizedAction, Integer>(this.currentPlan.getTime());
        this.vehicle = new HashMap<CentralizedAction, Vehicle>(this.currentPlan.getVehicle());
    	
    }
    /*set current plant*/
    private void setCurrentPlan(CentralizedPlan bestPlan) {
    	this.currentPlan = new CentralizedPlan(bestPlan.getNextActionA(),
    						bestPlan.getNextActionV(), bestPlan.getTime(), 
    						bestPlan.getVehicle(), this.vehicles);
    	
    }
}
