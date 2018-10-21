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
	private double cost;
	
	public DeliberativeState(City city, TaskSet tasksLeft, TaskSet tasksCarried, double cost) {
		this.city = city;
		this.tasksLeft = tasksLeft;
		this.tasksCarried = tasksCarried;
		this.cost = cost;
	}
	
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
	
	public double getCost() {
		return cost;
	}
	
	public void setCost(double cost_) {
		cost = cost_;
	}
	

}
