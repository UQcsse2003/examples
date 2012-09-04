package robots.mockpeer;

/**
 * A vector in (r, theta) format.
 * 
 * <p>Lots of time lost to forgetting Robocode has 0 pointing "north"</p>
 *
 */
public class RTheta {
	
	private double r;
	private double theta;
	
	/** The tolerance with which we'll consider a double value equal */
	public static double EPSILON = 0.001d;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof RTheta) {
			RTheta other = (RTheta)o;
			double thetaDiff = getTheta() - other.getTheta();
			while (thetaDiff > Math.PI) {
				thetaDiff -= AlgernonReborn.TWO_PI;
			}
			while (thetaDiff < -Math.PI) {
				thetaDiff += AlgernonReborn.TWO_PI;
			}				
			return (Math.abs(getR() - other.getR()) < EPSILON) && (Math.abs(thetaDiff) < EPSILON);
		} else {
			return false;
		}

		
	}
	
	public static RTheta fromCartesian(double x, double y) {
		return new RTheta(
				Math.sqrt(Math.pow(x, 2) + Math.pow(y,  2)),
				- Math.atan2(y, x) + Math.PI/2  // have to cope with Robocode's 0=North
			);
	}
	
	public RTheta(double r, double theta) {
		this.setR(r);
		this.setTheta(theta);		
		
		while (this.getTheta() > Math.PI) {
			this.setTheta(this.getTheta() - AlgernonReborn.TWO_PI);
		}
		while (this.getTheta() < -Math.PI) {
			this.setTheta(this.getTheta() + AlgernonReborn.TWO_PI);
		}
	}	
	
	public String toString() {
		return "(" + getR() + ", " + getTheta() +")";
	}
	
	public RTheta plus(RTheta b) {			
		double x = this.x() + b.x();
		double y = this.y() + b.y();
		
		return fromCartesian(x,y);
	}
	
	public double x() {
		// Handle's Robocode's coordinate system
		return this.getR() * Math.cos(- this.getTheta() + Math.PI/2);
	}
	public double y() {
		// Handle's Robocode's coordinate system
		return this.getR() * Math.sin(- this.getTheta() + Math.PI/2);
	}

	public double getR() {
		return r;
	}

	public void setR(double r) {
		this.r = r;
	}

	public double getTheta() {
		return theta;
	}

	public void setTheta(double theta) {
		this.theta = theta;
	}

	/**
	 * Determines a firing solution in local coordinates (ie, assuming robot is straight 
	 * ahead). This simplifies the trigonometry to a situation roughly like:
	 * <pre>
	 *   |
	 *   | alpha
	 *   |\
	 *   | \ t . ve
	 * d |  \ collision
	 *   |  /
	 *   | / t . vb
	 *   |/
	 *  beta
	 * </pre>
	 *   
	 * @param d direction to target
	 * @param alpha angle of target's heading vector (relative to their position vector from us)
	 * @param ve velocity of target
	 * @param vb velocity of bullet
	 * @return an RTheta containing (firing angle relative to position vector, time to impact)
	 */
	public static RTheta resolveRelative(double d, double alpha, double ve, double vb) {
		
		//System.out.printf("Testing d %f, alpha %f, ve %f, vb%f%n", d, alpha, ve, vb);		
		
		/*
		 * From the law of sines we have
		 * t . vb . sin beta = t . ve sin alpha
		 * as sin alpha = sin (pi - alpha)
		 * 
		 * so
		 * beta = asin ( ve sin alpha / vb )
		 * 
		 * But this has two solutions, as sin beta and sin (pi - beta) are the same
		 */
		double beta1 = Math.asin(ve * Math.sin(alpha) / vb);
		double beta2 = Math.PI - beta1;
		
		/*
		 * We also have 
		 * t . vb . cos beta - t . ve . cos alpha = d
		 * 
		 * Solving for t
		 * t = d / (vb cos beta - ve cos alpha)
		 */
		double t1 = d / (vb * Math.cos(beta1) - ve * Math.cos(alpha));
		double t2 = d / (vb * Math.cos(beta2) - ve * Math.cos(alpha));
		
		//System.out.printf("Solution 1 angle %f closing at %f%n", beta1, t1);
		//System.out.printf("Solution 2 angle %f closing at %f%n", beta2, t2);
	
		/*
		 *  If there is a viable solution, one of these will be positive and the other negative
		 */
		if (t1 > 0) {
			return new RTheta(t1, beta1);
		} else {
			return new RTheta(t2, beta2);
		}
	}

	/**
	 * Gets a firing solution on the closest robot. Note that it's possible the 
	 * solution will have an impact time of Double.POSITIVE_INFINITY (ie, no chance
	 * sunshine).
	 * @param position
	 * @param velocity
	 * @param vb
	 * @return
	 */
	public static RTheta firingSolution(RTheta position, RTheta velocity, double vb) {
		
		double d = Math.abs(position.getR());
		double localOriginAngle = (position.getR() > 0) ? position.getTheta() : - position.getTheta(); 
		
		double alpha = velocity.getTheta() - localOriginAngle;
		double ve = velocity.getR();
		
		RTheta relativeSolution = resolveRelative(d, alpha, ve, vb);
		RTheta globalSolution = new RTheta(relativeSolution.getR(), relativeSolution.getTheta() + localOriginAngle);
		return globalSolution;
	}
	
}