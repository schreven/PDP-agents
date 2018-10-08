package solution;

import logist.topology.Topology.City;

public class ReactiveAction {
	
	private final boolean action;		// 1: pickup, 0: move
	private final City dest;			// destination city
	
	private double reward;				// variable to store possible reward
	
	public ReactiveAction(boolean action, City dest) {
		this.action = action;
		this.dest = dest;
		this.reward = 0.0;
	}
	
	public boolean getAction() {
		return action;
	}
	
	public City getDest() {
		return dest;
	}
	
	public double getReward() {
		return reward;
	}
	
	public void setReward(double r) {
		reward = r;
	}



}
