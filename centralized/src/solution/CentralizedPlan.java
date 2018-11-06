package solution;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;
//import java.util.Objects;
//import java.util.Set;

import logist.simulation.Vehicle;
//import logist.task.Task;
//import logist.task.TaskSet;
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
		
		this.nextActionA = new HashMap<CentralizedAction, CentralizedAction>(nextActionA);
		this.nextActionV = new HashMap<Vehicle, CentralizedAction>(nextActionV);
		this.time = new HashMap<CentralizedAction, Integer>(time);
		this.vehicle = new HashMap<CentralizedAction, Vehicle>(vehicle);
		this.plans = computePlan();
		this.cost = computeCost();
	}

	
	
	private ArrayList<Plan> computePlan(){
		ArrayList<Plan> plans = new ArrayList<Plan>();
		CentralizedAction actionTemp;
		
		for (Vehicle vehicleTemp : this.nextActionV.keySet()) {
			
			City currentCity = vehicleTemp.getCurrentCity();
			Plan plan = new Plan(currentCity);
			
			if (nextActionV.get(vehicleTemp)==null) {
				plans.add(plan);
				continue; //directly skip vehicles without tasks
			}
			
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
	
	public Map<CentralizedAction, CentralizedAction> getNextActionA() {
		return this.nextActionA;
	}
	
	public Map<Vehicle, CentralizedAction> getNextActionV() {
		return this.nextActionV;
	}
	
	public Map<CentralizedAction, Integer> getTime(){
		return this.time;
	}
	
	public Map<CentralizedAction, Vehicle> getVehicle(){
		return this.vehicle;
	}
	
	public Double getCost(){
		return this.cost;
	}
	
	public ArrayList<Plan> getPlan(){
		return this.plans;
	}
	
    
}


