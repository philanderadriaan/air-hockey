import javax.swing.JOptionPane;
import javax.vecmath.Vector2f;

public class CollisionHandler {
	private static final float COEFFICIENT_OF_RESTITUTION = 0.9f;
	
	public static void checkAndResolveCollision(PhysicsObject a, PhysicsObject b) {
		resolveCollision(a, b, getCollisionInfo(a, b));
	}
	
	public static void resolveCollision(PhysicsObject a, PhysicsObject b, CollisionInfo ci) {
 		if (ci == null)
			return;

		// Vector from the center of mass of object a to the collision point
		Vector2f r_ap = new Vector2f();
		r_ap.scaleAdd(-1, a.getGlobalCenterOfMass(), ci.position);
		// Vector from the center of mass of object b to the collision point
		Vector2f r_bp = new Vector2f();
		r_bp.scaleAdd(-1, b.getGlobalCenterOfMass(), ci.position);
		// Velocity of object a at the point of collision
		Vector2f v_ap1 = new Vector2f();
		v_ap1.x = a.velocity.x - a.angularVelocity * r_ap.y;
		v_ap1.y = a.velocity.y + a.angularVelocity * r_ap.x;
		// Velocity of object b at the point of collision
		Vector2f v_bp1 = new Vector2f();
		v_bp1.x = b.velocity.x - b.angularVelocity * r_bp.y;
		v_bp1.y = b.velocity.y + b.angularVelocity * r_bp.x;
		// The collision impulse
		Vector2f v_ab1 = new Vector2f();
		v_ab1.scaleAdd(-1, v_bp1, v_ap1);
		float tmpA = r_ap.x * ci.normal.y - r_ap.y * ci.normal.x;
		float tmpB = r_bp.x * ci.normal.y - r_bp.y * ci.normal.x;
		float j = -(1 + COEFFICIENT_OF_RESTITUTION) * v_ab1.dot(ci.normal) / (1 / a.mass + 1 / b.mass + tmpA * tmpA / a.momentOfInertia + tmpB * tmpB / b.momentOfInertia);
		// Update object a's velocity
		a.velocity.scaleAdd(j / a.mass, ci.normal, a.velocity);
		// Update object b's velocity
		b.velocity.scaleAdd(-j / b.mass, ci.normal, b.velocity);
		// Update object a's angular velocity
		a.angularVelocity += j * (r_ap.x * ci.normal.y - r_ap.y * ci.normal.x) / a.momentOfInertia;
		// Update object b's angular velocity
		b.angularVelocity -= j * (r_bp.x * ci.normal.y - r_bp.y * ci.normal.x) / b.momentOfInertia;
		// Remove object overlap
		a.position.scaleAdd(-ci.depth / (a.mass * (1 / a.mass + 1 / b.mass)), ci.normal, a.position);
		b.position.scaleAdd(ci.depth / (b.mass * (1 / a.mass + 1 / b.mass)), ci.normal, b.position);
		
		a.clearCaches();
		b.clearCaches();
	}
	
	public static CollisionInfo getCollisionInfo(PhysicsObject a, PhysicsObject b) {
		if (a == b)
			return null;
		
		CollisionInfo ci = null;
		if (a instanceof HalfSpace) {
			if (b instanceof Circle)
        ci = getCollision((HalfSpace) a, (Circle) b);
		} else if (a instanceof Circle) {
			if (b instanceof Circle)
				ci = getCollision((Circle)a, (Circle)b);
		}
		return ci;
	}

	private static CollisionInfo getCollision(HalfSpace a, Circle b) {
		float distance = a.normal.dot(b.position) - a.intercept - b.radius;
		if (distance < 0) { // && a.normal.dot(b.velocity) < 0) {
		  if (b.position.x > 0 && b.position.y < 5 && b.position.y > -5) {
			  b.position = new Vector2f(5, 0);
			  b.velocity = new Vector2f(0, 0);
			  b.visitor++;
			  if (b.visitor == 7){
				  JOptionPane.showMessageDialog( null, "SCORE: "+ b.visitor+" - " + b.home + "\nGame Over! Start a new game?");
				  b.visitor = 0;
				  b.home = 0;
			  } else {			    
				  JOptionPane.showMessageDialog( null, "SCORE: "+ b.visitor+" - " + b.home);
			  }
			 
			  return null;
		  }
			CollisionInfo ci = new CollisionInfo();
			ci.normal = a.normal;
			ci.depth = -distance;
			ci.position = new Vector2f();
			ci.position.scaleAdd(-(b.radius - ci.depth), ci.normal, b.position);
			return ci;
		}
		 
		return null;
	}
	
	private static CollisionInfo getCollision(Circle a, Circle b) {
		Vector2f n = new Vector2f();
		n.scaleAdd(-1, a.position, b.position);
		float distance = n.length() - a.radius - b.radius;
		if (distance < 0) {
			CollisionInfo ci = new CollisionInfo();
			n.normalize();
			ci.normal = n;
			ci.depth = -distance;
			ci.position = new Vector2f();
			ci.position.scaleAdd(a.radius - ci.depth / 2, ci.normal, a.position);
			return ci;
		}
		return null;
	}
}