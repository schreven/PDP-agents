package solution;

import logist.task.Task;

public class DeliberativeAction {
	
	private final boolean pickup;  // true: pickup, false:drop
	private final Task task;
	
	public DeliberativeAction(boolean pickup, Task task) {
		this.pickup = pickup;
		this.task = task;
	}
	
	public boolean isPickup() {
		return pickup;
	}
	
	public Task getTask() {
		return task;
	}

}
