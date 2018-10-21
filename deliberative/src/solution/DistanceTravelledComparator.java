package solution;

import java.util.Comparator;

public class DistanceTravelledComparator implements Comparator<DeliberativeState>{
	@Override
	public int compare(DeliberativeState state1, DeliberativeState state2) {
		if(state1.getCost()<=state2.getCost()){
			return -1;
		}
		if (state1.getCost()>state2.getCost()){
			return 1;
		}
		else {
			//should never come here
			return 0;
		}	
	}

}
