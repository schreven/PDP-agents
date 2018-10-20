package solution;

import logist.topology.Topology.City;

import java.util.ArrayList;

import logist.task.Task;
import logist.task.TaskSet;

public class DeliberativeState {

	final private City currentCity;	
	final private TaskSet tasksLeft;
	final private TaskSet tasksCarried;
	
	public DeliberativeState(City currentCity, TaskSet tasksLeft, TaskSet tasksCarried) {
		this.currentCity = currentCity;
		this.tasksLeft = tasksLeft;
		this.tasksCarried = tasksCarried;
	}
	
	public City getCurrentCity() {
		return currentCity;
	}
	
	public TaskSet getTasksLeft() {
		return tasksLeft;
	}
	
	public TaskSet getTasksCarried() {
		return tasksCarried;
	}
}
