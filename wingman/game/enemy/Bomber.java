package wingman.game.enemy;

import java.awt.Point;

import wingman.GameWorld;
import wingman.game.Ship;
import wingman.modifiers.motions.SimpleFiringMotion;
import wingman.modifiers.weapons.SpreadBomb;

public class Bomber extends Ship {
	public Bomber(int location){
		this(location, 30, 6);
	}
	
	public Bomber(int location, int speed, int interval){
		super(location, new Point(0,speed), 100, GameWorld.sprites.get("enemy3"));
		this.weapon = new SpreadBomb();
		
		motion = new SimpleFiringMotion(interval);
	}

}
