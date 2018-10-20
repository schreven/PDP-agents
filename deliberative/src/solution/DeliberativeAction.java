package solution;

import logist.task.Task;

public class DeliberativeAction {
	
	private final boolean action;  // 0: pickup, 1:drop
	private final Task task;
	
	public DeliberativeAction(boolean action, Task task) {
		this.action = action;
		this.task = task;
	}
	
	public boolean getAction() {
		return action;
	}
	
	public Task getTask() {
		return task;
	}

}
