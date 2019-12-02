/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
	
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitSpace;
	
  public RabbitsGrassSimulationSpace(int xSize, int ySize){
    grassSpace = new Object2DGrid(xSize, ySize);
    rabbitSpace = new Object2DGrid(xSize, ySize);
    for(int i = 0; i < xSize; i++){
      for(int j = 0; j < ySize; j++){
        grassSpace.putObjectAt(i,j,new Integer(0));
      }
    }
  }
  
  public void spreadGrass(int grassToSpread){
    // Randomly place money in moneySpace
    for(int i = 0; i < grassToSpread; i++){

      // Choose coordinates
      int x = (int)(Math.random()*(grassSpace.getSizeX()));
      int y = (int)(Math.random()*(grassSpace.getSizeY()));


      // Get the value of the object at those coordinates
      int currentValue = getGrassAt(x, y);
      // Replace the Integer object with another one with the new value
      grassSpace.putObjectAt(x,y,new Integer(Math.min(currentValue + 1,32)));
    }
  }

  public int getGrassAt(int x, int y){
    int i;
    if(grassSpace.getObjectAt(x,y)!= null){
      i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
    }
    else{
      i = 0;
    }
    return i;
  }
  
  public Object2DGrid getCurrentGrassSpace(){
    return grassSpace;
  }
  
  public Object2DGrid getCurrentRabbitSpace(){
    return rabbitSpace;
  }
  
  
  

  public boolean isCellOccupied(int x, int y){
    boolean retVal = false;
    if(rabbitSpace.getObjectAt(x, y)!=null) retVal = true;
    return retVal;
  }

  public boolean addRabbit(RabbitsGrassSimulationAgent rabbit){
    boolean retVal = false;
    int count = 0;
    int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

    while((retVal==false) && (count < countLimit)){
      int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
      int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
      if(isCellOccupied(x,y) == false){
        rabbitSpace.putObjectAt(x,y,rabbit);
        rabbit.setXY(x,y);
        rabbit.setRabbitsGraceSimulationSpace(this);
        retVal = true;
      }
      count++;
    }

    return retVal;
  }
  
  public void removeRabbitAt(int x, int y){
    rabbitSpace.putObjectAt(x, y, null);
  }
  
  public int eatGrassAt(int x, int y){
    int grass = getGrassAt(x, y);
    grassSpace.putObjectAt(x, y, new Integer(0));
    return grass;
  }
  
  public boolean moveRabbitAt(int x, int y, int newX, int newY){
    boolean retVal = false;
    if(!isCellOccupied(newX, newY)){
      RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)rabbitSpace.getObjectAt(x, y);
      removeRabbitAt(x,y);
      cda.setXY(newX, newY);
      rabbitSpace.putObjectAt(newX, newY, cda);
      retVal = true;
    }
    return retVal;
  }
  
  public int getTotalGrass(){
    int totalGrass = 0;
    for(int i = 0; i < grassSpace.getSizeX(); i++){
      for(int j = 0; j < grassSpace.getSizeY(); j++){
        totalGrass += getGrassAt(i,j);
      }
    }
    System.out.println("Total amount of grass is: " + totalGrass);
    return totalGrass;
  }
  

}
