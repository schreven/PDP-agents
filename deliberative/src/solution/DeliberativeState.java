package solution;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Objects;

import logist.task.Task;
import logist.task.TaskSet;

public class DeliberativeState{

	final private City city;	
	final private TaskSet tasksLeft;
	final private TaskSet tasksCarried;
	
	final private DeliberativeState parentState;
	private double cost;
	//only used for A*Search
	private double travelCost;
	private double heuristicCost;
	
	public DeliberativeState(City city, TaskSet tasksLeft, TaskSet tasksCarried, DeliberativeState parentState) {
		this.city = city;
		this.tasksLeft = tasksLeft;
		this.tasksCarried = tasksCarried;
		this.parentState = parentState;
		
		this.cost = 0.0;
		this.travelCost = 0.0;
		this.heuristicCost = 0.0;
	}
	
	//Only the city, the list of tasks left and the list of tasks carried define a state
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
	

}
