
import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	
  private int x;
  private int y;
  private int vX;
  private int vY;
  private int energy;
  private RabbitsGrassSimulationSpace rgsSpace;

  



  public RabbitsGrassSimulationAgent(int energy_){
    x = -1;
    y = -1;
    vX = 0;
    vY = 0;
    energy = energy_;
  }
  
	public void draw(SimGraphics G) {
		if (energy>=10) {
			G.drawOval(new Color(102, 51, 0));
		}
		else if (energy >=5) {
			G.drawOval(new Color(128, 64, 0));
		}
		else {
			G.drawOval(new Color(153, 77, 0));
		}
		// TODO Auto-generated method stub
		
	}
  
  
	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}
  
  public void setXY(int newX, int newY){
    x = newX;
    y = newY;
  }
  
  private void setVxVy(){
    vX = 0;
    vY = 0;
    int hor_ver = (int)Math.floor(Math.random() * 2);
    if (hor_ver == 0) {
    	while(vX==0) {
    		vX = (int)Math.floor(Math.random() * 3) - 1;
    	}
    }
    else if (hor_ver == 1){
    	while(vY ==0) {
    		vY = (int)Math.floor(Math.random() * 3) - 1;	
    	}
    	
    }  
  }
  
  public void setRabbitsGraceSimulationSpace(RabbitsGrassSimulationSpace rgss) {
  	rgsSpace = rgss;
  }
  
  public int getEnergy() {
  	return energy;
  }
  
  public void setEnergy(int energy_) {
  	energy = energy_;
  }
  
  public void step(){
    setVxVy();
    int newX = x + vX;
    int newY = y + vY;

    Object2DGrid grid = rgsSpace.getCurrentRabbitSpace();
    newX = (newX + grid.getSizeX()) % grid.getSizeX();
    newY = (newY + grid.getSizeY()) % grid.getSizeY();
  	
    if(tryMove(newX, newY)){
    	energy += rgsSpace.eatGrassAt(x, y);
    }
    else{
      setVxVy();
    }
    
    energy--;
  }
  
  private boolean tryMove(int newX, int newY){
    return rgsSpace.moveRabbitAt(x, y, newX, newY);
  }
  
}
