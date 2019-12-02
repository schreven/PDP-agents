package solution;

import logist.task.Task;
import logist.topology.Topology.City;

public class AuctionAction {
	private final Task task;
	private final boolean pickup;  // true: pickup, false:drop
	private final City city;
	
	public AuctionAction(Task task, boolean pickup) {
		this.task = task;
		this.pickup = pickup;
		if (pickup==true) {
		this.city = task.pickupCity;
		}
		else {
			this.city = task.deliveryCity;
		}
	}
	
	public boolean isPickup() {
		return pickup;
	}
	
	public Task getTask() {
		return task;
	}
	
	public City getCity() {
		return city;
	}
}