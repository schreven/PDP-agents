package solution;

//the list of imports
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
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
    private Map<Task, Task> nextTaskT;
    private Map<Vehicle, Task> nextTaskV;
    private Map<Task, Integer> time;
    private Map<Task, Vehicle> vehicle;

    
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
        this.time = new HashMap<Task, Integer>();
        this.vehicle = new HashMap<Task, Vehicle>();
        this.nextTaskT = new HashMap<Task, Task>();
        this.nextTaskV = new HashMap<Vehicle, Task>();
    }

    // List of the plan for each vehicle
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis(); 	// save current time to measure computation duration
        double totalCost = 0;							// total cost of a solution
        double oldTotalCost = 0;						// total cost of a previous solution

        /* Use Constraint Satisfaction Problem Optimization to find list of plans*/
        List<Plan> plans = new ArrayList<Plan>();
        List<Plan> oldPlans = new ArrayList<Plan>();
        
        // Initialize variables with non optimized plan.
        // Fails if a task has a weight bigger than any vehicle capacity.
        try {
			SelectInitialSolution(vehicles, tasks);
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
    
    private void SelectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) throws Exception {
    	/* Give all the tasks to the biggest vehicle. Only updates global variables*/
    	
    	int order = 1;			// order in which the task are handled by a vehicle
    	Task prevTask = null;	// previous task carried by a vehicle
    	    	
    	// find biggest vehicle, i.e. vehicle with largest capacity
    	Vehicle bigVehicle = vehicles.get(0);
    	for (Vehicle vehicle : vehicles) {
    		if (vehicle.capacity() > bigVehicle.capacity()) {
    			bigVehicle = vehicle;
    		}    		
    	}
    	
    	// iterate over task set
    	for (Task task : tasks) {  
    		
    		// if the biggest vehicle cannot carry a task, the problem is unsolvable.
    		if (task.weight > bigVehicle.capacity()) {
    			throw new Exception("The task (id: "+task.id+") has a weight too big ! Problem unsolvable.");  	
    		}
 
    		// fill arrays
			this.nextTaskT.put(prevTask, task);  
			this.nextTaskT.put(task, null);
    		this.nextTaskV.put(bigVehicle, task);
    		this.time.put(task, order);
    		this.vehicle.put(task, bigVehicle);
    		
    		order++;
    		prevTask = task;
    	}   
    	
    	if (!VerifyConstraints()) {
    		throw new Exception("Problem solved, but did not meet the constraints");  	// shouldn't go here
    	}
    	
    }
    
    private boolean VerifyConstraints() {
    	/* Based on the CSP Variables, verify if all the constraints are met */
    	
    	// TODO
    	
    	return true;
    }
    
    private void ChooseNeighbours() {
    	/* Find a neighbor to the current solution */
    	
    	// TODO
    	
    }
    
    private List<Plan> ComputePlans(){
    	/* Compute Logist Plan for each vehicle according to the global CSP variables*/
    	
    	List<Plan> plans = new ArrayList<Plan>();
    	
    	// TODO
    	
    	return plans;
    }
    
    private double ComputeCost(List<Plan> plans, List<Vehicle> vehicles) {
    	double cost = 0;
    	int costKm = 0;
    	int index = 0;
    	
    	// iterate over all vehicles and all plans
    	for (Vehicle vehicle : vehicles) {
    		costKm = vehicle.costPerKm();
    		
    		cost += costKm * plans.get(index).totalDistance();
    		index++;
    	}    	
    	
    	return cost;
    }
    
}
