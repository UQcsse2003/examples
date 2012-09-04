package robots.strategy;

import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

public class AlgernonStrategy {
	
	private AlgernonReborn algernonReborn;
	
	public String closestRobotName;
	public RTheta closestRobotPos;
	public RTheta closestRobotVel;
	public double closestRobotEnergy;
	public int lastSeen;
	/** Where we want the tank body to be pointed. */
	public double desiredHeading;
	/** Where we want our gun to be pointed. */
	public RTheta targetVector;
	/** How powerfully to fire the shot if we do so this turn. */
	public double bulletPower;
	/**
	 * True when we want to be in reverse gear
	 */
	public boolean reverse;
	/**
	 * How long before we change direction
	 */
	public int turnsToFlip;
	/**
	 * For when we can't see a robot.
	 */
	static RTheta AT_INFINITY = new RTheta(Double.POSITIVE_INFINITY, 0d);
	/** Longest acceptable time to impact */
	static double MAX_SHOOT = 30d;
	/** Preferred stand-off distance. */
	static double PREF_DIST = 100d;
	/** If the target is this far away, drop power to 2. */
	static double TWO_POWER_DIST = 150d;
	/** If the target is this far away, drop power to 1. */
	static double ONE_POWER_DIST = 300d;
	/** How soon we forget a closest robot if we haven't seen it. */
	static int FORGET_ROBOT_COUNT = 3;

	public AlgernonStrategy(AlgernonReborn algernonReborn) {
		this.algernonReborn = algernonReborn;
		
		this.closestRobotName = null;
		this.closestRobotPos = AT_INFINITY;
		this.closestRobotVel = AT_INFINITY;
		this.closestRobotEnergy = Double.POSITIVE_INFINITY;
		this.lastSeen = 0;
		this.desiredHeading = 0d;
		this.targetVector = AT_INFINITY;
		this.bulletPower = 3d;
		this.reverse = false;
		this.turnsToFlip = 10;
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
		algernonReborn.setAhead(reverse ? -1000 : 1000);			
	}

	/** 
	 * Decide if we need to change direction entirely
	 */
	protected void updateFlip() {
		turnsToFlip--;
		if (turnsToFlip <= 0 || algernonReborn.strategy.willHitWall()) {
			reverse = !reverse;
			turnsToFlip = 10 + (int)(40 * Math.random());
			updateDesiredHeading();
		}
	}

	/** 
	 * Rough guess as to whether we might hit a wall if we keep going the way
	 * we're going.
	 */
	protected boolean willHitWall() {
		
		double fieldW = algernonReborn.getBattleFieldWidth();
		double fieldH = algernonReborn.getBattleFieldHeight();
		
		double backoff = 40; // Just a bit bigger than our robot;
		
		RTheta projectedPos = myProjectedPos();
		double oldx = algernonReborn.getX();
		double oldy = algernonReborn.getY();						
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
	 * Takes a guess at where our robot will be in a little while. We don't care
	 * too much about accuracy because we only use this to know when to reverse to
	 * avoid a wall. (So we don't work it out based on speed, acceleration, etc, but
	 * just use a number chosen from running a battle against the empty robot and 
	 * seeing if we ran into the wall too much).
	 * @return
	 */
	protected RTheta myProjectedPos() {
		RTheta pos = algernonReborn.getPos();
		
		double heading = algernonReborn.getHeadingRadians();
		RTheta move = new RTheta(reverse ? -40d : 40d, heading);
		
		return pos.plus(move);		
	}

	/** 
	 * Recalculate our desired heading
	 */
	protected void updateDesiredHeading() {
		
		desiredHeading = closestRobotPos.getTheta() + (Math.PI / 2);
		
		if (closestRobotPos.getR() > AlgernonStrategy.PREF_DIST) {
			// turn a smidgen in
			desiredHeading += reverse ? 0.5d : -0.5d;			
		} else {
			// turn a smidgen out
			desiredHeading += reverse ? -0.5d : 0.5d;						
		}
		
		
		while (desiredHeading > Math.PI) {
			desiredHeading = desiredHeading - RTheta.TWO_PI;
		}
		while (desiredHeading < -Math.PI) {
			desiredHeading = desiredHeading + RTheta.TWO_PI;
		}
		
		//System.out.println("Closest theta " + closestRobotPos.theta + " desired heading " + desiredHeading);
		
	}

	/**
	 * Turn towards our desired heading 
	 */
	protected void updateHeading() { 
		double h = algernonReborn.getHeadingRadians();
		double dh = desiredHeading - h;
		
		if (dh > Math.PI) {
			dh -= RTheta.TWO_PI;
		}
		if (dh < -Math.PI) {
			dh += RTheta.TWO_PI;
		}
		
		//System.out.println("h " + h + ", desired " + desiredHeading + ", dh " + dh);
		if (dh > Rules.MAX_TURN_RATE_RADIANS) {
			dh = Rules.MAX_TURN_RATE_RADIANS;
		}
		if (dh < -Rules.MAX_TURN_RATE_RADIANS) {
			dh = -Rules.MAX_TURN_RATE_RADIANS;
		}		
		
		//System.out.println("turning " + dh);
		algernonReborn.setTurnRightRadians(dh);		
	}

	/**
	 * Forgets the closest robot (means we won't fire and will look for another
	 * robot).
	 */
	void forgetRobot() {
		closestRobotName = null;
		closestRobotPos = AlgernonStrategy.AT_INFINITY;
		closestRobotVel = AlgernonStrategy.AT_INFINITY;	
		closestRobotEnergy = Double.POSITIVE_INFINITY;
	}

	/**
	 * Choose a bullet power.
	 */
	protected void updateBulletPower() {
		if (closestRobotPos.getR() > AlgernonStrategy.ONE_POWER_DIST  || closestRobotEnergy < 6d) {
			bulletPower = 1d;
		} else if (closestRobotPos.getR() > AlgernonStrategy.TWO_POWER_DIST || closestRobotEnergy < 9d) {
			bulletPower = 2d;
		} else {
			bulletPower = 3d;	
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
	 * Fire if the gun is pointed in the direction of a viable firing solution.
	 * (ie, aimed at where the target will be, and it'll hit the robot soon 
	 * enough that we don't think "Bah, it'll have turned by then.")
	 */
	protected void conditionallyFire() {
		// If we have a viable firing solution
		if (targetVector.getR() > 0 && targetVector.getR() < AlgernonStrategy.MAX_SHOOT) {
			double g = algernonReborn.getGunHeadingRadians();
			double d = targetVector.getTheta() - g;			
			while (d > Math.PI) {
				d -= RTheta.TWO_PI;
			} 
			while (d < -Math.PI) {
				d += RTheta.TWO_PI;
			}			
			
			if (Math.abs(d) < 0.1) {
				algernonReborn.setFire(bulletPower);
			}
		}
	}


	/**
	 * Moves the gun to point at our firing solution if we have a viable one.
	 */
	protected void updateGun() {
		if (targetVector != AT_INFINITY) {
			double g = algernonReborn.getGunHeadingRadians();
			double d = targetVector.getTheta() - g;
			
			if (d > Math.PI) {
				d -= RTheta.TWO_PI;
			} else if (d < -Math.PI) {
				d += RTheta.TWO_PI;
			}			
			algernonReborn.setTurnGunRightRadians(d);			
		}
	}

	/**
	 * Moves the radar to either track the closest robot, or look for one.
	 */
	protected void updateRadar() {
		if (closestRobotName != null) {
				
			double rh = algernonReborn.getRadarHeadingRadians();
			double d = closestRobotPos.getTheta() - rh;
			
			if (d < -Math.PI) {
				d += RTheta.TWO_PI;
			} else if (d > Math.PI) {
				d -= RTheta.TWO_PI;
			}			
			algernonReborn.setTurnRadarRightRadians(d);
		} else {
			algernonReborn.setTurnRadarRightRadians(Math.PI);
		}
	}
	
	public void doTurnActions() {
		updateTarget();
		updateGun();
		updateRadar();
		updateHeading();
		updateVelocity();
		conditionallyFire();
		algernonReborn.execute();
	}


	public void onHitWall(HitWallEvent evt) {
		RTheta p = myProjectedPos();
		System.out.printf("Bump! projected pos was (%f, %f) and willhit %b%n", p.x(), p.y(), willHitWall());
	}


	public void onRobotDeath(RobotDeathEvent evt) {
		if (evt.getName().equals(closestRobotName)) {
			forgetRobot();	
		}
	}

	public void onScannedRobot(ScannedRobotEvent evt) {		
		// Update the closest robot.
		RTheta posVec = new RTheta(evt.getDistance(), evt.getBearingRadians() + algernonReborn.getHeadingRadians());
		RTheta velVec = new RTheta(evt.getVelocity(), evt.getHeadingRadians());
		String name = evt.getName();
		
		// If this is news on our closest robot, or is a new closest robot ... 
		if (name.equals(closestRobotName) || posVec.getR() < closestRobotPos.getR()) {
			closestRobotName = name; 
			closestRobotPos = posVec;
			closestRobotVel = velVec;
			closestRobotEnergy = evt.getEnergy();
			lastSeen = 0;
			
			updateDesiredHeading();			
		}		
		
		//System.out.println("Closest robot is " + closestRobotName + " at " + closestRobotPos);
	}

	public void onStatus(StatusEvent evt) {
		// Called every turn
		lastSeen++;
		if (lastSeen > AlgernonStrategy.FORGET_ROBOT_COUNT) {
			// One that got away.
			forgetRobot();
		}
		// Is it time to change direction?
		updateFlip();
	}
}