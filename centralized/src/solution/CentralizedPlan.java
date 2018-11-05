package solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import logist.plan.Plan;

public class CentralizedPlan {
	
    final private Map<CentralizedAction, CentralizedAction> nextActionA;
    final private Map<Vehicle, CentralizedAction> nextActionV;
    final private Map<CentralizedAction, Integer> time;
    final private Map<CentralizedAction, Vehicle> vehicle;
    final ArrayList<Plan> plans;
    final Double cost;
    
	public CentralizedPlan(Map<CentralizedAction, CentralizedAction> nextActionA,
							Map<Vehicle, CentralizedAction> nextActionV,
							Map<CentralizedAction, Integer> time,
							Map<CentralizedAction, Vehicle> vehicle) {
		
		this.nextActionA = nextActionA;
		this.nextActionV = nextActionV;
		this.time = time;
		this.vehicle = vehicle;
		this.plans = computePlan();
		this.cost = computeCost();
	}
	
	
	private ArrayList<Plan> computePlan(){
		ArrayList<Plan> plans = new ArrayList<Plan>();
		CentralizedAction actionTemp;
		
		for (Vehicle vehicleTemp : this.nextActionV.keySet()) {
			City currentCity = vehicleTemp.getCurrentCity();
			Plan plan = new Plan(currentCity);
			
			actionTemp = nextActionV.get(vehicleTemp);
			
			//assuming first task is pickup
			// move: current city => pickup location
			for (City city : currentCity.pathTo(actionTemp.getTask().pickupCity))
				plan.appendMove(city);
			//pickup task
			plan.appendPickup(actionTemp.getTask());
			// update current city
			currentCity = actionTemp.getTask().pickupCity;

			
			
			while(nextActionA.get(actionTemp)!=null){
				actionTemp = nextActionA.get(actionTemp);
				//picking up a task
				if (actionTemp.isPickup()) {
					// move: current city => pickup location
					for (City city : currentCity.pathTo(actionTemp.getTask().pickupCity))
						plan.appendMove(city);
					//pickup task
					plan.appendPickup(actionTemp.getTask());
					// update current city
					currentCity = actionTemp.getTask().pickupCity;
				}
				//delivering a task
				else{
					// move: current city => delivery location
					for (City city : currentCity.pathTo(actionTemp.getTask().deliveryCity))
						plan.appendMove(city);
					//deliver task
					plan.appendDelivery(actionTemp.getTask());
					// update current city
					currentCity = actionTemp.getTask().deliveryCity;
				}
			
			}
			plans.add(plan);
		
		}
		return plans;	
	}
	
	private Double computeCost() {
		Double cost = 0.;
		Integer id = 0;
		
		//compute and add cost of each vehicle
		for (Vehicle vehicleTemp : this.nextActionV.keySet()) {
			cost += vehicleTemp.costPerKm()*this.plans.get(id).totalDistance();
		}
		return cost;	
	}
	
	public boolean verifyConstraints(List<Vehicle> vehicles, Set<CentralizedAction> actions){
		/* Based on the CSP Variables, verify if all the constraints are met */
		Integer capacityTemp;
		CentralizedAction action;
		
    	for (CentralizedAction actionTemp : nextActionA.keySet()) {
    		// Constraint 1
    		if (actionTemp == nextActionA.get(actionTemp)) return false;
    		// Constraint 3
    		else if (nextActionA.get(actionTemp)!=null && 
    				time.get(nextActionA.get(actionTemp)) != time.get(actionTemp) +1 ) {
    			return false;}   	
    		//Contraint 5
    		else if (nextActionA.get(actionTemp)!=null && 
    				vehicle.get(nextActionA.get(actionTemp))!= vehicle.get(actionTemp)) {
    			return false;}
    	}
    	
    	for (Vehicle vehicleTemp : nextActionV.keySet()) {
    		//Contraint 2
    		if (time.get(nextActionV.get(vehicleTemp))!=1) return false;
    		//Constraint 4
    		else if (vehicle.get(nextActionV.get(vehicleTemp))!=vehicleTemp) return false;
    		//Constraint 7
    		action = nextActionV.get(vehicleTemp);
    		capacityTemp = vehicleTemp.capacity();
    		while (action!=null) {
    			//substract to available capacity if pickup
    			if (action.isPickup()) capacityTemp -= action.getTask().weight;
    			//add to available capacity if delivery
    			else capacityTemp += action.getTask().weight;
    			if (capacityTemp<0) return false;
    		}

    	}
    	//Contraint 6
    	if ((nextActionA.size()+nextActionV.size())!= (actions.size()+vehicles.size())) {
    		return false;}
    	
		return true;
	}
    
}


