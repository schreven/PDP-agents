import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		
	
  // Default Values
  private static final int NUMRABBITS = 10;
  private static final int WORLDXSIZE = 20;
  private static final int WORLDYSIZE = 20;
  private static final int BIRTHTHRESHOLD = 25;
  private static final int GRASSGROWTHRATE = 20;
  private static final int INITIALGRASS = 200;
  private static final int RABBITINITIALENERGY = 15;
  private static final int BIRTHENERGYCOST = 10;

  private int worldXSize = WORLDXSIZE;
  private int worldYSize = WORLDYSIZE;
  private int numRabbits = NUMRABBITS;
  private int birthThreshold = BIRTHTHRESHOLD;
  private int grassGrowthRate = GRASSGROWTHRATE;
  private int initialGrass = INITIALGRASS;
  private int rabbitInitialEnergy = RABBITINITIALENERGY;
  private int birthEnergyCost = BIRTHENERGYCOST;
  

	
  private Schedule schedule;
  
  private RabbitsGrassSimulationSpace rgsSpace;
  
  private DisplaySurface displaySurf;
  
  private OpenSequenceGraph amountOfGrassAndRabbitsInSpace;
  
 // private OpenSequenceGraph amountOfRabbitsInSpace;
  
  private ArrayList rabbitList;
 
  
  class rabbitsInSpace implements DataSource, Sequence {

    public Object execute() {
      return new Double(getSValue());
    }

    public double getSValue() {
      return (double) countLivingRabbits();
    }
  }
  
  
  class grassInSpace implements DataSource, Sequence {

    public Object execute() {
      return new Double(getSValue());
    }

    public double getSValue() {
      return (double)rgsSpace.getTotalGrass()/20;
    }
  }


  public static void main(String[] args) {
    SimInit init = new SimInit();
    RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
    init.loadModel(model, "", false);
			
		System.out.println("Rabbit skeleton");
			
	}
  
	public void setup() {
		System.out.println("Running setup");
		// TODO Auto-generated method stub
		rgsSpace = null;
    rabbitList = new ArrayList();
    schedule = new Schedule(1);	
    
    if (displaySurf != null){
      displaySurf.dispose();
    }
    displaySurf = null;
    
    if (amountOfGrassAndRabbitsInSpace != null){
    	amountOfGrassAndRabbitsInSpace.dispose();
    }
    amountOfGrassAndRabbitsInSpace = null;

    //create displays
    displaySurf = new DisplaySurface(this, "Rabbits Grass Simluation Model Window 1");
    amountOfGrassAndRabbitsInSpace = new OpenSequenceGraph("Amount Of Grass And Rabbits In Space",this);

    //register displays
    registerDisplaySurface("Rabbits Grass Simluation Model Window 1", displaySurf);
    this.registerMediaProducer("Plot", amountOfGrassAndRabbitsInSpace);
	}

	public void begin() {
			
	    buildModel();
	    buildSchedule();
	    buildDisplay();
	    
	    displaySurf.display();
	    amountOfGrassAndRabbitsInSpace.display();
	}
	
	public void buildModel(){
		System.out.println("Running BuildModel");
		rgsSpace = new RabbitsGrassSimulationSpace(worldXSize,worldYSize);
		rgsSpace.spreadGrass(initialGrass);
		
    for(int i = 0; i < numRabbits; i++){
      addNewRabbit();
    }
  }


  public void buildSchedule(){
  	System.out.println("Running BuildSchedule");
  	
    class RabbitsGrassSimulationStep extends BasicAction {
      public void execute() {
      	rgsSpace.spreadGrass(grassGrowthRate);
        SimUtilities.shuffle(rabbitList);
        for(int i =0; i < rabbitList.size(); i++){
          RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
          rgsa.step();
        }
        handleRabbitsDeathOrBirth();
        
        displaySurf.updateDisplay();
      }
    }

    schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());
    
    class RabbitsGrassSimulationUpdateGrassAndRabbitsInSpace extends BasicAction {
      public void execute(){
        amountOfGrassAndRabbitsInSpace.step();
      }
    }

    schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassAndRabbitsInSpace());
  }

  public void buildDisplay(){
  	System.out.println("Running BuildDisplay");
  	
    ColorMap map = new ColorMap();

    for(int i = 1; i<32; i++){
      map.mapColor(i, new Color(0, (int)(255-6*i), 0));
    }
    map.mapColor(0, Color.white);

    Value2DDisplay displayGrass =
        new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);
    
    Object2DDisplay displayRabbits = new Object2DDisplay(rgsSpace.getCurrentRabbitSpace());
    displayRabbits.setObjectList(rabbitList);
    
    displaySurf.addDisplayableProbeable(displayGrass, "Grass");
    displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");
    
    amountOfGrassAndRabbitsInSpace.addSequence("Grass In Space", new grassInSpace());
    amountOfGrassAndRabbitsInSpace.addSequence("Rabbits In Space", new rabbitsInSpace());
  }
  
  private void addNewRabbit(){
    RabbitsGrassSimulationAgent r = new RabbitsGrassSimulationAgent(rabbitInitialEnergy);
    rabbitList.add(r);
    rgsSpace.addRabbit(r);
  }
  
  private void handleRabbitsDeathOrBirth(){
    for(int i = (rabbitList.size() - 1); i >= 0 ; i--){
      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
      if(rgsa.getEnergy() < 1){
        rgsSpace.removeRabbitAt(rgsa.getX(), rgsa.getY());
        rabbitList.remove(i);
      }
      else if (rgsa.getEnergy()>=birthThreshold) {
      	rgsa.setEnergy(rgsa.getEnergy()-birthEnergyCost);
      	addNewRabbit();
      	
      }
      else {
      	//do nothing
      }
    }
  }
  
  private int countLivingRabbits(){
    int livingAgents = 0;
    for(int i = 0; i < rabbitList.size(); i++){
      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
      if(rgsa.getEnergy() > 0) livingAgents++;
    }
    System.out.println("Number of living agents is: " + livingAgents);

    return livingAgents;
  }

  public String[] getInitParam(){
    String[] initParams = {"WorldXSize", "WorldYSize", "NumRabbits", "BirthThreshold", "GrassGrowthRate",
    		"InitialGrass", "RabbitInitialEnergy", "BirthEnergyCost"};
    return initParams;
  }

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Schedule getSchedule() {
		// TODO Auto-generated method stub
	   return schedule;
	}
	
  public int getWorldXSize(){
    return worldXSize;
  }

  public void setWorldXSize(int wxs){
    worldXSize = wxs;
  }
  
  public int getWorldYSize(){
    return worldYSize;
  }

  public void setWorldYSize(int wys){
    worldYSize = wys;
  }
  
  public int getNumRabbits(){
    return numRabbits;
  }

  public void setNumRabbits(int nr){
    numRabbits = nr;
  }
  
  public int getBirthThreshold(){
    return birthThreshold;
  }

  public void setBirthThreshold(int bt){
    birthThreshold = bt;
  }
  
  public int getGrassGrowthRate(){
    return grassGrowthRate;
  }

  public void setGrassGrowthRate(int ggt){
    grassGrowthRate = ggt;
  }
  
  public int getInitialGrass(){
    return initialGrass;
  }

  public void setInitialGrass(int ig){
    initialGrass = ig;
  }
  
  public int getRabbitInitialEnergy(){
    return rabbitInitialEnergy;
  }

  public void setRabbitInitialEnergy(int rie){
    rabbitInitialEnergy = rie;
  }
  
  public int getBirthEnergyCost(){
    return birthEnergyCost;
  }

  public void setBirthEnergyCost(int bec){
    birthEnergyCost = bec;
  }
}
