package javachallenge.client.myplayer;

import java.lang.reflect.Array;
import java.util.ArrayList;
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
	Map<Point,Integer> fires = new TreeMap<Point,Integer>() ;
        Set<Point> handledFires = new TreeSet<Point>();
        Map<Integer,Point> fireTarget = new TreeMap<>();
        BlockType[][] map;
        
        
        private enum SearchMode{
            EXPLORED_ONLY,
            UNEXPLORED_AND_NEIBOUR
        }
        
	public MyPlayer(World world) {
		super(world);
	}
        
        private void init() {
            System.out.println("Map height: "+getWorld().getMapHeight());
            System.out.println("Map width: "+getWorld().getMapWidth());
            System.out.println("Spawn location: "+getWorld().getSpawnLocation());
            map = new BlockType[getWorld().getMapHeight()][getWorld().getMapWidth()];
        }
        
        private boolean unexplored(Point p){
            return map[p.getY()][p.getX()] == null;
        }
        
        private boolean outOfMap(Point p){
            return p.getY()<0 || p.getX()<0 ||
                    p.getY()>=getWorld().getMapHeight() ||
                    p.getX()>=getWorld().getMapWidth();
        }
        
        private boolean isGround(Point p){
            return map[p.getY()][p.getX()] == BlockType.GROUND;
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
                    fires.remove(agent.getLocation());
                    handledFires.add(agent.getLocation());
                }
                for (Direction dir : Direction.values()){
                    Point nei = agent.getLocation().applyDirection(dir);
                    if (agent.getAdjBlockType(dir) == BlockType.OUT_OF_MAP)
                        continue;
                    if (agent.hasFire(dir) && !handledFires.contains(nei))
                        fires.put(nei, agent.getId());
                    map[nei.getY()][nei.getX()] = agent.getAdjBlockType(dir);
                }
            }
            
            //hala inja ghesmathayi ke moshakashe gheire ghabele dasrese ro ba dfs/bfs hazf mikonim
            
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
            par[from.getY()][from.getX()] = from;
            q.add(from);
            while (!q.isEmpty()){
                Point p = q.poll();
                if (p.equals(to))
                    break;
                for (Direction dir : Direction.values()){
                    Point nei = p.applyDirection(dir);
                    if (!outOfMap(nei) && par[nei.getY()][nei.getX()] == null && !willPostponeSpawn(from,nei))
                        if ((mode == SearchMode.EXPLORED_ONLY && 
                                !unexplored(nei) && isGround(nei))||
                            (mode == SearchMode.UNEXPLORED_AND_NEIBOUR &&
                                (unexplored(nei) || (p == from && isGround(nei))))
                           ){
                            par[nei.getY()][nei.getX()] = p;
                            q.add(nei);
                        }
                }
            }
            
            Point temp = to;
            while (par[temp.getY()][temp.getX()] != null && par[temp.getY()][temp.getX()] != from )
                temp = par[temp.getY()][temp.getX()];
            if (par[temp.getY()][temp.getX()] == null)
                return null;
            return getDirectionToNeib(from,temp);
            
        }

	public void step() {
                if (cycle == 0)
                    init();
                if (cycle<18)
                    for (int id: getAgentIds())
                        System.out.println(id);
		cycle++;
                
                
                
                updateMap();
                handleFireTargets();
                
                
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
