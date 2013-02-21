package javachallenge.client.myplayer;

import com.sun.media.sound.DirectAudioDeviceProvider;
import com.sun.org.apache.bcel.internal.generic.AALOAD;
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
        Random rand = new Random();
        Point[] lastPos = {new Point(0,0),new Point(0,0),new Point(0,0),new Point(0,0),new Point(0,0),new Point(0,0)};
        int[] lastestNoMoves = new int[6];
        
        
        private enum SearchMode{
            EXPLORED_ONLY,
            UNEXPLORED_AND_NEIBOUR,
            EXPLORED_UNEXPLORED,
            SET_REACHABILITY
        }
        
        private enum Strategy{
            UP_RIGHT,
            UP_LEFT,
            DOWN_LEFT,
            DOWN_RIGHT,
            CENTER,
            FULLFILL
        }
        
        Direction[][] dirToGoOnBorder = {
            {Direction.SOUTHWEST,Direction.SOUTH},
            {Direction.SOUTHEAST,Direction.SOUTH},
            {Direction.NORTH,Direction.NORTHEAST},
            {Direction.SOUTH,Direction.NORTHWEST}
        };
        Direction[][] dirToGoNormal = {
            {Direction.NORTHWEST,Direction.SOUTHEAST},
            {Direction.NORTHEAST,Direction.SOUTHWEST},
            {Direction.NORTHWEST,Direction.SOUTHEAST},
            {Direction.NORTHEAST,Direction.SOUTHWEST}
        };
        int[] currentLine = new int[5];
        Point[] firstBlock =  new Point[5];
        int[] lastBlock = {
            2,
            3,
            0,
            1
        };
        
        
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
            
            
            firstBlock[0] = new Point(0,getWorld().getMapWidth()-1);
            firstBlock[1] = new Point(0,0);
            firstBlock[2] = new Point(getWorld().getMapHeight()-1,0);
            firstBlock[3] = new Point(getWorld().getMapHeight()-1,getWorld().getMapWidth()-1);
            firstBlock[4] = new Point(getWorld().getMapHeight()/2,getWorld().getMapWidth()/2);
            
            for (int i=0;i<5;i++)
                blockTarget[i] = firstBlock[i];
            
           
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
                if (agent.getLocation().equals(lastPos[agent.getId()]))
                    lastestNoMoves[agent.getId()]++;
                else 
                    lastestNoMoves[agent.getId()]/=2;
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
                            c = ""+agent.getId();
                    logn(c);
                }
                log("");            
            }
            log("================================");
        }
        
        private void handleFireTargets(){
            for (Agent agent : getAgents()){
                Point target = fireTarget.get(agent.getId());
                if (target != null && !fires.containsKey(target)){
                    log("removing fire target "+target+" from agent "+agent.getId());
                    fireTarget.remove(agent.getId());
                }
            }
            for (Point fire : fires.keySet()){
               if (fireTarget.get(fires.get(fire)) == null){
                   log("setting new fire target for agent "+fires.get(fire)+", "+fire);
                   fireTarget.put(fires.get(fire), fire);
               }
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
            Point target;
            
            
            //in baraye 4 gooshe
            for (int i=0;i<=3;i++){
                target = blockTarget[i];
                while (target != null && !unexplored(target)){
                    if (target.equals(firstBlock[lastBlock[i]])){
                        target = null;
                        break;
                    }
                    if (outOfMap(target.applyDirection(dirToGoNormal[i][currentLine[i]%2]))){
                        target = target.applyDirection(dirToGoOnBorder[i][currentLine[i]%2]);
                        currentLine[i]++;
                    }else
                        target = target.applyDirection(dirToGoNormal[i][currentLine[i]%2]);
                }
                blockTarget[i] = target;
            }
            
            //in baraye center
            //hala kholase ye joori ...
            target = blockTarget[4];
            if (target != null && !unexplored(target))
                target = null; //alaki felan
            blockTarget[4] = target;
            
            //baraye 5
           target = blockTarget[5];
            if (target != null && !unexplored(target))
                target = null; 
            blockTarget[5] = target;
            
            //hala baraye har bi targeti 
            for (Integer id : getAgentIds()){
                if (blockTarget[id] == null){
                    logn("agent "+id+" has no strategic block taget, fidning closest unexplored block... ");
                    blockTarget[id] = findClosestUnreservedUnexploredBlock(getAgentById(id).getLocation());
                    log(""+blockTarget[id]);
                }else
                    log("agent "+id+ " strategic target: "+blockTarget[id]);
            }
            
            
            //age alanam target nadasht dige random
            for (Integer id : getAgentIds()){
                if (blockTarget[id] == null)
                    blockTarget[id] = new Point(rand.nextInt(getWorld().getMapHeight()), rand.nextInt(getWorld().getMapWidth()));
            }
        }
        
        boolean reserved(Point p){
            for (int i=0;i<6;i++)
                if (p.equals(blockTarget[i]))
                    return true;
            return false;
        }
        
        private Point findClosestUnreservedUnexploredBlock(Point from){
            boolean marked[][] = new boolean[getWorld().getMapHeight()][getWorld().getMapWidth()];
            Queue<Point> q= new ArrayBlockingQueue<Point>(getWorld().getMapHeight()*getWorld().getMapWidth());
            marked[from.getX()][from.getY()] = true;
            q.add(from);
            while (!q.isEmpty()){
                Point p = q.poll();
                if (unexplored(p) && !reserved(p))
                    return p;
                for (Direction dir : Direction.values()){
                    Point nei = p.applyDirection(dir);
                    if (!outOfMap(nei) && !marked[nei.getX()][nei.getY()] && !willPostponeSpawn(from,nei)){
                        marked[nei.getX()][nei.getY()] = true;
                        q.add(nei);
                    }
                }
            }
            return null;
        }
        
        
        private void move(Agent agent,Direction dir){
            //tedade null ha ro ham beshmar!
            agent.doMove(dir);
        }
        
        private void setMoves(){
            for (Agent agent : getAgents()){
                if (fireTarget.get(agent.getId()) != null){
                    log("agent "+agent.getId()+" will go for FIRE on "+fireTarget.get(agent.getId()));
                    move(agent,findShortestPath(agent.getLocation(), fireTarget.get(agent.getId()), SearchMode.EXPLORED_ONLY));
                }else{
                    log("agent "+agent.getId()+" will go for BLOCK on "+blockTarget[agent.getId()]);
                    Direction dir = findShortestPath(agent.getLocation(), blockTarget[agent.getId()], SearchMode.UNEXPLORED_AND_NEIBOUR);
                    if (dir == null){
                        log("no path using only unexplored blocks");
                        dir = findShortestPath(agent.getLocation(), blockTarget[agent.getId()], SearchMode.EXPLORED_UNEXPLORED);
                    }
                    
                    if (lastestNoMoves[agent.getId()]>=10){
                        log ("agent "+agent.getId()+" has stuck for more than 10 cycles, choosing random direction");
                        dir = Direction.values()[rand.nextInt(6)];
                    }
                    
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
