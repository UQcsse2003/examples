package robots.strategy;

import robocode.HitWallEvent;
import robocode.Robot;
import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

/**
 * From-memory recreation of Will Billingsley's first robot written a decade ago.
 * 
 * <p>Also written to be used as a refactoring demo.</p>
 * 
 * <p><em>PS. No, this isn't the second robot to use for benchmarking.</em></p>
 * 
 * <p>One of the first things I wrote in Java was a Robocode robot -- competing with
 * a few people at work while learning Java. I couldn't find the original file, but
 * could remember the fairly simple algorithm, so this is a reimplementation of how
 * it worked.</p>
 * 
 * <p>(At the time I had a naming scheme for these things that the first version was
 * always "Algernon" the second "Bertie", and so on. But I never wrote Bertie.
 * As this is a recreation of Algernon as well as I can remember, it's "Algernon 
 * Reborn".)
 * </p>
 * 
 * <p>Movement:
 * Find the closest robot.  Circle-strafe, reversing randomly (or when about to hit a
 * wall) to make it harder for the target to hit us.  Nudge our heading angle so that
 * we wiggle in (or out) to a comfortable shooting distance.<p>
 * 
 * <p>To target a robot:
 * Simple linear shot predictor (uses a bit of trig to calculate the angle and time
 * to impact for a bullet if the target continues on its current vector).  Don't care
 * about anything fancy like walls or circular tracking -- we'll only be firing when
 * closish anyway, and half the time if two robots try to circle-strafe each other
 * they end up going straight.
 * </p>
 * 
 * <p>Gun control:
 * Keep turning to our target vector.
 * </p>
 * 
 * <p>Radar control:
 * Train relentlessly on the closest robot. If we haven't seen it in 3 turns, forget it
 * and keep spinning the radar looking for someone.
 * </p>
 * 
 * <p>Bullet power:
 * Drop the power if it's further away (get a faster bullet). Also drop the power to
 * avoid over-killing the robot by too much.</p>
 * 
 * <p>Firing:
 * Only fire if the time-to-impact is small enough. Otherwise the target will almost
 * certainly have turned out of the way by the time it gets there.</p>
 * 
 * @author William Billingsley
 */
public class AlgernonReborn extends AdvancedRobot {
	
	@Override
	public void run() {
		// Make all component turns relative to the global origin not local origins.
		this.setAdjustGunForRobotTurn(true);
		this.setAdjustRadarForGunTurn(true);
		this.setAdjustRadarForRobotTurn(true);
		
		while (true) {
			strategy.doTurnActions();			
		}		
	}


	@Override
	public void onStatus(StatusEvent evt) {
		strategy.onStatus(evt);
	}
	
	
	protected AlgernonStrategy strategy = new AlgernonStrategy(this);


	@Override
	public void onScannedRobot(ScannedRobotEvent evt) {
		strategy.onScannedRobot(evt);
	}
	
	@Override
	public void onRobotDeath(RobotDeathEvent evt) {
		strategy.onRobotDeath(evt);
	}
	
	@Override
	public void onHitWall(HitWallEvent evt) {
		strategy.onHitWall(evt);
	}
	
	/** Where are we. */
	RTheta getPos() {
		return RTheta.fromCartesian(this.getX(), this.getY());
	}

}


