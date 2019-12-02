package solution;

import logist.task.Task;

public class CentralizedAction {
	private final Task task;
	private final boolean pickup;  // true: pickup, false:drop
	
	public CentralizedAction(Task task, boolean pickup) {
		this.task = task;
		this.pickup = pickup;
	}
	
	public boolean isPickup() {
		return pickup;
	}
	
	public Task getTask() {
		return task;
	}
}
