package solution;

import logist.topology.Topology.City;

public class ReactiveState {
	
	final private City currentCity;	
	final private boolean taskAvailable;
	final private City destinationCity;
	
	public ReactiveState(City currentCity) {
		this.currentCity = currentCity;
		this.taskAvailable = false;
		this.destinationCity = null;
	}
	
	public ReactiveState(City currentCity, City destinationCity) {
		this.currentCity = currentCity;
		this.taskAvailable = true;
		this.destinationCity = destinationCity;
	}
	
	public City getCurrentCity() {
		return currentCity;
	}
	
	public boolean taskAvailible() {
		return taskAvailable;
	}
	
	public City getDestinationCity() {
		return destinationCity;
	}
	
	
}