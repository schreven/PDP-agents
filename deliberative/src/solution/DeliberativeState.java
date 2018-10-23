package solution;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import logist.task.Task;
import logist.task.TaskSet;

public class DeliberativeState{

	final private City city;	
	final private TaskSet tasksLeft;
	final private TaskSet tasksCarried;
	
	final private DeliberativeState parentState;
	private double cost;
	private double travelCost;
	//only used for A*Search
	private double heuristicCost;
	private int capacity;
	
	public DeliberativeState(City city, TaskSet tasksLeft, TaskSet tasksCarried, DeliberativeState parentState) {
		this.city = city;
		this.tasksLeft = tasksLeft;
		this.tasksCarried = tasksCarried;
		this.parentState = parentState;
		
		this.cost = 0.0;
		this.travelCost = 0.0;
		this.heuristicCost = 0.0;
	}
	
	/*Formally, only the city, the tasksLeft, and the tasksCarried define a state.
	 * Therefore s1.equals(s2) will be true if this is the case. The costs and the parent are not considered.
	 */
	@Override
	public boolean equals(Object o){
		DeliberativeState s = (DeliberativeState) o;
		if ((city == s.city) && (tasksLeft.equals(s.tasksLeft)) && (tasksCarried.equals(s.tasksCarried))) { 
			return true;
		}
		else {
			return false;
		}
	}
	
  @Override public int hashCode() {
    //simple one-line implementation
    return Objects.hash(city,tasksLeft,tasksCarried);
  }
	
	public City getCity() {
		return city;
	}
	
	public TaskSet getTasksLeft() {
		return tasksLeft;
	}
	
	public TaskSet getTasksCarried() {
		return tasksCarried;
	}
	
	public DeliberativeState getParentState() {
		return parentState;
	}
	
	public double getCost() {
		return cost;
	}
	
	public void setCost(double cost_) {
		cost = cost_;
	}
	
	public double getTravelCost() {
		return travelCost;
	}
	
	public void setTravelCost(double cost_) {
		travelCost = cost_;
	}
	
	public double getHeuristicCost() {
		return heuristicCost;
	}
	
	public void setHeuristicCost(double cost_) {
		heuristicCost = cost_;
	}
	
	public void computeHeuristic(String heuristic) {
		if (heuristic.equals("current-distance")) {
			heuristicCost=0;
			
			ArrayList<City> cities = new ArrayList<City>();
			
			for (Task task: tasksLeft) {
				cities.add(task.pickupCity);
				cities.add(task.deliveryCity);
			}
			for (Task task: tasksCarried) {
				cities.add(task.deliveryCity);
			}
			
			if (!cities.isEmpty()){
				//System.out.println(cities.remove(city1));
				double minDistance = Double.POSITIVE_INFINITY;
				for (City city2: cities) {
					if (minDistance > this.city.distanceTo(city2)) {
						minDistance = this.city.distanceTo(city2);
					}
				}
				this.heuristicCost += minDistance;
			}
			
		}
	  else if (heuristic.equals("all-distances")){
			heuristicCost=0;
			
			ArrayList<City> cities = new ArrayList<City>();
			
			for (Task task: tasksLeft) {
				cities.add(task.pickupCity);
				cities.add(task.deliveryCity);
			}
			for (Task task: tasksCarried) {
				cities.add(task.deliveryCity);
			}
			
			if (!cities.isEmpty()){
				cities.add(this.city);
			
				for (City city1: cities) {
					ArrayList<City> citiesTemp = new ArrayList<City>(cities);
					citiesTemp.remove(city1);
					//System.out.println(cities.remove(city1));
					double minDistance = Double.POSITIVE_INFINITY;
					for (City city2: citiesTemp) {
						if (minDistance > city1.distanceTo(city2)) {
							minDistance = city1.distanceTo(city2);
						}
					}
					this.heuristicCost += minDistance;
				}
			}

		}
	  else if (heuristic.equals("greedy-path")) {
	  	heuristicCost = 0;
			Set<City> cities = new HashSet<City>();
	  	
			for (Task task: tasksLeft) {
				cities.add(task.pickupCity);
				cities.add(task.deliveryCity);
			}
			for (Task task: tasksCarried) {
				cities.add(task.deliveryCity);
			}
	  	
			Queue<City> queue = new LinkedList<City>();
	  	
			queue.add(this.city);
			
			City city1;
			City nextCity;
			
			while (true) {
				
				//if there is just one city left
				if (cities.size()<=1) {
					break;
				}
				
				city1 = queue.remove();
				nextCity=null;
				cities.remove(city1);
				
				double minDistance = Double.POSITIVE_INFINITY;
				for (City city2: cities) {
					if (minDistance > city1.distanceTo(city2)) {
						minDistance = city1.distanceTo(city2);
						nextCity = city2;
					}
				}
				this.heuristicCost += minDistance;
				queue.add(nextCity);
			}
	  	
	  	
	  }
		
	}
	

}
