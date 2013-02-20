package javachallenge.client.myplayer;

import java.util.ArrayList;
import java.util.Random;

import javachallenge.client.Agent;
import javachallenge.client.Player;
import javachallenge.client.World;
import javachallenge.common.Direction;
import javachallenge.common.Point;

public class MyPlayer extends Player {
	int cycle = 0;
	ArrayList<Point> putoutFires = new ArrayList<Point>();

	public MyPlayer(World world) {
		super(world);
	}

	public void step() {
		Random rnd = new Random();
		cycle++;
		
		for (int id: getAgentIds()) {
			Agent agent = this.getAgentById(id);
			
			agent.doMove(Direction.values()[rnd.nextInt(6)]);
			
			for (Direction d: Direction.values()) {
				Point point = agent.getLocation().applyDirection(d);
				
				if (!putoutFires.contains(point) && agent.hasFire(d)) {
					agent.doMove(d);
					putoutFires.add(point);
				}
			}
		}
	}
}
