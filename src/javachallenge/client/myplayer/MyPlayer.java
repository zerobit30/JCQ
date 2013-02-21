package javachallenge.client.myplayer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

import javachallenge.client.Agent;
import javachallenge.client.Player;
import javachallenge.client.World;
import javachallenge.common.BlockType;
import javachallenge.common.Direction;
import javachallenge.common.Point;



public class MyPlayer extends Player {
	int cycle = 0;
	Map<Point,Integer> fires = new HashMap<Point,Integer>() ;
        Set<Point> handledFires = new HashSet<Point>();
        Map<Integer,Point> fireTarget = new HashMap<>();
        Point[] blockTarget = new Point[6];
        BlockType[][] map;
        
        
        private enum SearchMode{
            EXPLORED_ONLY,
            UNEXPLORED_AND_NEIBOUR,
            EXPLORED_UNEXPLORED,
            SET_REACHABILITY
        }
        
	public MyPlayer(World world) {
		super(world);
	}
        
        private void log(String msg){
            System.out.println(msg);
        }
        
        private void logn(String msg){
            System.out.print(msg);
        }
        
        private void init() {
            System.out.println("Map height: "+getWorld().getMapHeight());
            System.out.println("Map width: "+getWorld().getMapWidth());
            System.out.println("Spawn location: "+getWorld().getSpawnLocation());
            map = new BlockType[getWorld().getMapHeight()][getWorld().getMapWidth()];
            map[getWorld().getSpawnLocation().getX()][getWorld().getSpawnLocation().getY()] = BlockType.GROUND; //aya?
            
            blockTarget[0]=new Point(getWorld().getMapHeight()/2,getWorld().getMapWidth()/2); //alaki
            
            blockTarget[1]=new Point(getWorld().getMapHeight()/2,getWorld().getMapWidth()/2);
            blockTarget[2]=new Point(0,getWorld().getMapWidth()-1);
            //blockTarget[3]=new Point(0,0);
            blockTarget[3]=new Point(getWorld().getMapHeight()/2+5,getWorld().getMapWidth()/2+5); //alaki
            blockTarget[4]=new Point(getWorld().getMapHeight()-1,0);
            blockTarget[5]=new Point(getWorld().getMapHeight()-1,getWorld().getMapWidth()-1);
            
        }
        
        private boolean unexplored(Point p){
            return map[p.getX()][p.getY()] == null;
        }
        
        private boolean unexplored(int y,int x){
            return map[y][x] == null;
        }
        
        private boolean outOfMap(Point p){
            return p.getX()<0 || p.getY()<0 ||
                    p.getX()>=getWorld().getMapHeight() ||
                    p.getY()>=getWorld().getMapWidth();
        }
        
        private boolean isGround(Point p){
            return map[p.getX()][p.getY()] == BlockType.GROUND;
        }
        
        private boolean willPostponeSpawn(Point from,Point p){
            if (p.equals(getWorld().getSpawnLocation()) && isNeibour(from,p))
                if (cycle%3 == 0 && cycle<18)
                    return true;
            return false;
        }
        
        private boolean isNeibour(Point a,Point b){
            return getDirectionToNeib(a, b) != null;
        }
        
        private Direction getDirectionToNeib(Point a,Point b){
            for (Direction dir : Direction.values())
                if (b.equals(a.applyDirection(dir)))
                    return dir;
            return null;
        }
        
        
        
        private void updateMap(){
            for (Agent agent : getAgents()){
                if (fires.containsKey(agent.getLocation())){
                    log("agent "+agent.getId()+" is in "+agent.getLocation()+", so no fire now!");
                    fires.remove(agent.getLocation());
                    handledFires.add(agent.getLocation());
                }
                for (Direction dir : Direction.values()){
                    Point nei = agent.getLocation().applyDirection(dir);
                    if (agent.getAdjBlockType(dir) == BlockType.OUT_OF_MAP)
                        continue;
                    if (agent.hasFire(dir) && !handledFires.contains(nei)){
                        log("found fire in "+nei+", agent"+agent.getId()+" is in charge!");
                        fires.put(nei, agent.getId());
                    }
                    map[nei.getX()][nei.getY()] = agent.getAdjBlockType(dir);
                }
            }
            
            //hala inja ghesmathayi ke moshakashe gheire ghabele dasrese ro ba dfs/bfs hazf mikonim
            findShortestPath(getWorld().getSpawnLocation(), null, SearchMode.SET_REACHABILITY);
            
        }
        
        private void printMap(){
            log("================================");
            for (int i=0;i<getWorld().getMapHeight();i++){
                for (int j=0;j<getWorld().getMapWidth();j++){
                    String c = "?";
                    if (map[i][j]==BlockType.GROUND)
                        c = ".";
                    else if (map[i][j]==BlockType.RIVER || map[i][j]==BlockType.WALL)
                        c = "X";
                    for (Agent agent : getAgents())
                        if (agent.getLocation().equals(new Point(i,j)))
                            c = "O";
                    logn(c);
                }
                log("");            
            }
            log("================================");
        }
        
        private void handleFireTargets(){
            for (Agent agent : getAgents()){
                Point target = fireTarget.get(agent.getId());
                if (target != null && !fires.containsKey(target))
                    fireTarget.remove(agent.getId());
            }
            for (Point fire : fires.keySet()){
               if (fireTarget.get(fires.get(fire)) == null)
                   fireTarget.put(fires.get(fire), fire);
            }
        }
        
        private Direction findShortestPath(Point from,Point to,SearchMode mode){
            Point par[][] = new Point[getWorld().getMapHeight()][getWorld().getMapWidth()];
            Queue<Point> q= new ArrayBlockingQueue<Point>(getWorld().getMapHeight()*getWorld().getMapWidth());
            par[from.getX()][from.getY()] = from;
            q.add(from);
            while (!q.isEmpty()){
                Point p = q.poll();
                if (p.equals(to))
                    break;
                for (Direction dir : Direction.values()){
                    Point nei = p.applyDirection(dir);
                    if (!outOfMap(nei) && par[nei.getX()][nei.getY()] == null && !willPostponeSpawn(from,nei))
                        if ((mode == SearchMode.EXPLORED_ONLY && 
                                !unexplored(nei) && isGround(nei))||
                            (mode == SearchMode.UNEXPLORED_AND_NEIBOUR &&
                                (unexplored(nei) || (p == from && isGround(nei))))||
                            ((mode == SearchMode.EXPLORED_UNEXPLORED || mode == SearchMode.SET_REACHABILITY) &&
                                (unexplored(nei) || isGround(nei)))
                           ){
                            par[nei.getX()][nei.getY()] = p;
                            q.add(nei);
                        }
                }
            }
            
            if (mode == SearchMode.SET_REACHABILITY){
                for (int i=0;i<getWorld().getMapHeight();i++)
                    for (int j=0;j<getWorld().getMapWidth();j++)
                        if (unexplored(i,j) && par[i][j] == null)
                            map[i][j] = BlockType.WALL;
                return null;
            }
            
            Point temp = to;
            while (par[temp.getX()][temp.getY()] != null && par[temp.getX()][temp.getY()] != from )
                temp = par[temp.getX()][temp.getY()];
            if (par[temp.getX()][temp.getY()] == null)
                return null;
            return getDirectionToNeib(from,temp);
            
        }
        
        private void updateBlockTargets(){
            
            while (!unexplored(blockTarget[1])){ //center
                
            }
            while (!unexplored(blockTarget[2])){ //top right
                
            }
            while (!unexplored(blockTarget[3])){ //top left
                
            }
            while (!unexplored(blockTarget[4])){ //down left
                
            }
            while (!unexplored(blockTarget[5])){ //down right
                
            }
        }
        
        private void move(Agent agent,Direction dir){
            //tedade null ha ro ham beshmar!
            agent.doMove(dir);
        }
        
        private void setMoves(){
            for (Agent agent : getAgents()){
                if (fireTarget.get(agent.getId()) != null)
                    move(agent,findShortestPath(agent.getLocation(), fireTarget.get(agent.getId()), SearchMode.EXPLORED_ONLY));
                else{
                    System.out.println(agent.getId());
                    Direction dir = findShortestPath(agent.getLocation(), blockTarget[agent.getId()], SearchMode.UNEXPLORED_AND_NEIBOUR);
                    if (dir == null)
                        dir = findShortestPath(agent.getLocation(), blockTarget[agent.getId()], SearchMode.EXPLORED_UNEXPLORED);
                    move(agent,dir);
                }   
            }
        }
        

	public void step() {
                if (cycle == 0)
                    init();
		cycle++;
                
                
                
                
                updateMap();
                if (cycle%1==0)
                    printMap();
                handleFireTargets();
                updateBlockTargets();
                setMoves();
                
                
                
                /*Random rand = new Random();
                for (Agent aget: getAgents())
                    aget.doMove(Direction.values()[rand.nextInt(6)]);
		*/
                
		/*for (int id: getAgentIds()) {
			Agent agent = this.getAgentById(id);
			
			agent.doMove(Direction.values()[rnd.nextInt(6)]);
			
			for (Direction d: Direction.values()) {
				Point point = agent.getLocation().applyDirection(d);
				
				if (!putoutFires.contains(point) && agent.hasFire(d)) {
					agent.doMove(d);
					putoutFires.add(point);
				}
			}
		}*/
	}
}
