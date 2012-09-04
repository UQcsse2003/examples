package robots.mockpeer;

import robocode.HitWallEvent;
import robocode.Robot;
import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.Rules;
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
			doTurnActions();			
		}		
	}


	public void doTurnActions() {
		this.updateTarget();
		this.updateGun();
		this.updateRadar();
		this.updateHeading();
		this.updateVelocity();
		this.conditionallyFire();
		this.execute();
	}

	
	@Override
	public void onStatus(StatusEvent evt) {
		// Called every turn
		lastSeen++;
		if (lastSeen > FORGET_ROBOT_COUNT) {
			// One that got away.
			forgetRobot();
		}
		// Is it time to change direction?
		updateFlip();
	}
	
	
	/**
	 * For when we can't see a robot.
	 */
	private static RTheta AT_INFINITY = new RTheta(Double.POSITIVE_INFINITY, 0d);
	
	/** Does what it says on the tin. */
	static double TWO_PI = Math.PI * 2;
	
	/** Longest acceptable time to impact */
	private static double MAX_SHOOT = 30d;
	
	/** Preferred stand-off distance. */
	private static double PREF_DIST = 100d;
	
	/** If the target is this far away, drop power to 2. */
	private static double TWO_POWER_DIST = 150d;
	
	/** If the target is this far away, drop power to 1. */
	private static double ONE_POWER_DIST = 300d;
	
	/** How soon we forget a closest robot if we haven't seen it. */
	private static int FORGET_ROBOT_COUNT = 3;
	
	/*
	 * These all relate to our curent target.
	 */
	private String closestRobotName = null;
	private RTheta closestRobotPos = AT_INFINITY;
	private RTheta closestRobotVel = AT_INFINITY;
	private double closestRobotEnergy = Double.POSITIVE_INFINITY;
	private int lastSeen = 0;
	
	/** Where we want the tank body to be pointed. */
	protected double desiredHeading = 0d;

	/** Where we want our gun to be pointed. */
	private RTheta targetVector = AT_INFINITY;
	
	/** How powerfully to fire the shot if we do so this turn. */
	protected double bulletPower = 3d;
	
	/**
	 * True when we want to be in reverse gear
	 */
	protected boolean reverse = false;
	
	/**
	 * How long before we change direction
	 */
	protected int turnsToFlip = 10;
	
		
	@Override
	public void onScannedRobot(ScannedRobotEvent evt) {		
		// Update the closest robot.
		RTheta posVec = new RTheta(evt.getDistance(), evt.getBearingRadians() + this.getHeadingRadians());
		RTheta velVec = new RTheta(evt.getVelocity(), evt.getHeadingRadians());
		String name = evt.getName();
		
		// If this is news on our closest robot, or is a new closest robot ... 
		if (name.equals(closestRobotName) || posVec.getR() < closestRobotPos.getR()) {
			closestRobotName = name; 
			closestRobotPos = posVec;
			closestRobotVel = velVec;
			closestRobotEnergy = evt.getEnergy();
			lastSeen = 0;
			
			this.updateDesiredHeading();			
		}		
		
		//System.out.println("Closest robot is " + closestRobotName + " at " + closestRobotPos);
	}
	
	/**
	 * Moves the radar to either track the closest robot, or look for one.
	 */
	protected void updateRadar() {
		if (closestRobotName != null) {
				
			double rh = this.getRadarHeadingRadians();
			double d = closestRobotPos.getTheta() - rh;
			
			if (d < -Math.PI) {
				d += TWO_PI;
			} else if (d > Math.PI) {
				d -= TWO_PI;
			}			
			this.setTurnRadarRightRadians(d);
		} else {
			this.setTurnRadarRightRadians(Math.PI);
		}
	}
	
	/**
	 * Moves the gun to point at our firing solution if we have a viable one.
	 */
	protected void updateGun() {
		if (targetVector != AT_INFINITY) {
			double g = this.getGunHeadingRadians();
			double d = targetVector.getTheta() - g;
			
			if (d > Math.PI) {
				d -= TWO_PI;
			} else if (d < -Math.PI) {
				d += TWO_PI;
			}			
			this.setTurnGunRightRadians(d);			
		}
	}
	
	/**
	 * Fire if the gun is pointed in the direction of a viable firing solution.
	 * (ie, aimed at where the target will be, and it'll hit the robot soon 
	 * enough that we don't think "Bah, it'll have turned by then.")
	 */
	protected void conditionallyFire() {
		// If we have a viable firing solution
		if (targetVector.getR() > 0 && targetVector.getR() < MAX_SHOOT) {
			double g = this.getGunHeadingRadians();
			double d = targetVector.getTheta() - g;			
			while (d > Math.PI) {
				d -= TWO_PI;
			} 
			while (d < -Math.PI) {
				d += TWO_PI;
			}			
			
			if (Math.abs(d) < 0.1) {
				this.setFire(bulletPower);
			}
		}
	}
	
	/**
	 * Update our bullet power and firing solution.
	 */
	protected void updateTarget() {
		updateBulletPower();
		targetVector = RTheta.firingSolution(closestRobotPos, closestRobotVel, Rules.getBulletSpeed(bulletPower));
	}
	
	/**
	 * Choose a bullet power.
	 */
	protected void updateBulletPower() {
		if (closestRobotPos.getR() > ONE_POWER_DIST  || closestRobotEnergy < 6d) {
			bulletPower = 1d;
		} else if (closestRobotPos.getR() > TWO_POWER_DIST || closestRobotEnergy < 9d) {
			bulletPower = 2d;
		} else {
			bulletPower = 3d;	
		}
	}
	
	@Override
	public void onRobotDeath(RobotDeathEvent evt) {
		if (evt.getName().equals(closestRobotName)) {
			forgetRobot();	
		}
	}
	
	/**
	 * Forgets the closest robot (means we won't fire and will look for another
	 * robot).
	 */
	private void forgetRobot() {
		closestRobotName = null;
		closestRobotPos = AT_INFINITY;
		closestRobotVel = AT_INFINITY;	
		closestRobotEnergy = Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Turn towards our desired heading 
	 */
	protected void updateHeading() { 
		double h = this.getHeadingRadians();
		double dh = desiredHeading - h;
		
		if (dh > Math.PI) {
			dh -= TWO_PI;
		}
		if (dh < -Math.PI) {
			dh += TWO_PI;
		}
		
		//System.out.println("h " + h + ", desired " + desiredHeading + ", dh " + dh);
		if (dh > Rules.MAX_TURN_RATE_RADIANS) {
			dh = Rules.MAX_TURN_RATE_RADIANS;
		}
		if (dh < -Rules.MAX_TURN_RATE_RADIANS) {
			dh = -Rules.MAX_TURN_RATE_RADIANS;
		}		
		
		//System.out.println("turning " + dh);
		this.setTurnRightRadians(dh);		
	}
	
	
	/** 
	 * Recalculate our desired heading
	 */
	protected void updateDesiredHeading() {
		
		desiredHeading = closestRobotPos.getTheta() + (Math.PI / 2);
		
		if (closestRobotPos.getR() > PREF_DIST) {
			// turn a smidgen in
			desiredHeading += reverse ? 0.5d : -0.5d;			
		} else {
			// turn a smidgen out
			desiredHeading += reverse ? -0.5d : 0.5d;						
		}
		
		
		while (desiredHeading > Math.PI) {
			desiredHeading = desiredHeading - TWO_PI;
		}
		while (desiredHeading < -Math.PI) {
			desiredHeading = desiredHeading + TWO_PI;
		}
		
		//System.out.println("Closest theta " + closestRobotPos.theta + " desired heading " + desiredHeading);
		
	}
	
	@Override
	public void onHitWall(HitWallEvent evt) {
		RTheta p = myProjectedPos();
		System.out.printf("Bump! projected pos was (%f, %f) and willhit %b%n", p.x(), p.y(), willHitWall());
	}
	
	/**
	 * Takes a guess at where our robot will be in a little while. We don't care
	 * too much about accuracy because we only use this to know when to reverse to
	 * avoid a wall. (So we don't work it out based on speed, acceleration, etc, but
	 * just use a number chosen from running a battle against the empty robot and 
	 * seeing if we ran into the wall too much).
	 * @return
	 */
	protected RTheta myProjectedPos() {
		RTheta pos = RTheta.fromCartesian(this.getX(), this.getY());
		
		double heading = this.getHeadingRadians();
		RTheta move = new RTheta(reverse ? -40d : 40d, heading);
		
		return pos.plus(move);		
	}
	
	/** Where are we. */
	private RTheta getPos() {
		return RTheta.fromCartesian(this.getX(), this.getY());
	}
	
	/** 
	 * Rough guess as to whether we might hit a wall if we keep going the way
	 * we're going.
	 */
	protected boolean willHitWall() {
		
		double fieldW = this.getBattleFieldWidth();
		double fieldH = this.getBattleFieldHeight();
		
		double backoff = 40; // Just a bit bigger than our robot;
		
		RTheta projectedPos = myProjectedPos();
		double oldx = this.getX();
		double oldy = this.getY();						
		double x = projectedPos.x();
		double y = projectedPos.y();
		boolean willhit = (
				(x < backoff && x < oldx) || 
				(x > fieldW - backoff && x > oldx) || 
				(y < backoff && y < oldy) || 
				(y > fieldH - backoff && y > oldy)
		);
		
		//if (willhit) {
			//RTheta pos = getPos();
			//System.out.printf("time %d p(%f, %f) h %f rev %b p'(%f, %f) will hit %b%n", this.getTime(), pos.x(), pos.y(), getHeadingRadians(), reverse, x, y, willhit);
		//}
		return willhit; 
	}
	
	/** 
	 * Decide if we need to change direction entirely
	 */
	protected void updateFlip() {
		turnsToFlip--;
		if (turnsToFlip <= 0 || willHitWall()) {
			reverse = !reverse;
			turnsToFlip = 10 + (int)(40 * Math.random());
			updateDesiredHeading();
		}
	}
	
	/**
	 * Decide how fast to go. 
	 */
	protected void updateVelocity() {
		/* 
		 * Just go fast. As this gets updated every turn it should effectively mean
		 * we're going at full ahead or full reverse (the number is well in excess
		 * of max velocity).
		 */
		this.setAhead(reverse ? -1000 : 1000);			
	}

}


