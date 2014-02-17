import java.io.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class PhysicsObject implements Serializable {
	private static final long serialVersionUID = -6803318416353762022L;

	public float mass;
	public float momentOfInertia;
	// Position of the object's local coordinate system origin (usually geometric center) in world coordinates.
	public Vector2f position;
	public Vector2f velocity;
	public Vector2f forceAccumulator;
	public float orientation;
	public float angularVelocity;
	// Center of mass relative the the geometric center at zero orientation in local coordinates.
	public Vector2f centerOfMass;
	transient public BranchGroup BG;
	transient protected TransformGroup TG;
	transient protected Transform3D T3D;
	
	public PhysicsObject(float mass, float positionX, float positionY, float velocityX, float velocityY, float orientation, float angularVelocity) {
		if (mass <= 0)
			throw new IllegalArgumentException();
		
		this.mass = mass;
		position = new Vector2f(positionX, positionY);
		velocity = new Vector2f(velocityX, velocityY);
		forceAccumulator = new Vector2f();
		this.orientation = orientation;
		this.angularVelocity = angularVelocity;
		centerOfMass = new Vector2f();
		BG = new BranchGroup();
		BG.setCapability(BranchGroup.ALLOW_DETACH);
		TG = new TransformGroup();
		TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		BG.addChild(TG);
		T3D = new Transform3D();
		updateTransformGroup();

		// momentOfInertia and centerOfMass must be set in subclasses.
		// Visible components must be added to TG in subclasses.
	}
	
	public PhysicsObject(float mass, Tuple2f position, Tuple2f velocity, float orientation, float angularVelocity) {
		this(mass, position.x, position.y, velocity.x, velocity.y, orientation, angularVelocity);
	}
	
	public void updateState(float duration) {
		Vector2f globalCenterOfMass = getGlobalCenterOfMass();
		// The force accumulator vector (net force) now becomes
		// the acceleration vector.
		forceAccumulator.scale(1 / mass);
		globalCenterOfMass.scaleAdd(duration, velocity, globalCenterOfMass);
		globalCenterOfMass.scaleAdd(duration * duration / 2, forceAccumulator, globalCenterOfMass);
		velocity.scaleAdd(duration, forceAccumulator, velocity);
		orientation += angularVelocity * duration;
		updatePositionFromGlobalCenterOfMass(globalCenterOfMass);
		clearCaches();
	}
	
	public Vector2f getGlobalCenterOfMass() {
		Transform3D t3D = new Transform3D();
		t3D.rotZ(orientation);
		Vector3f v3f = new Vector3f(centerOfMass.x, centerOfMass.y, 0);
		t3D.transform(v3f);
		return new Vector2f(v3f.x + position.x, v3f.y + position.y);
	}
	
	private void updatePositionFromGlobalCenterOfMass(Tuple2f GCoM) {
		Transform3D t3D = new Transform3D();
		t3D.rotZ(orientation);
		Vector3f v3f = new Vector3f(-centerOfMass.x, -centerOfMass.y, 0);
		t3D.transform(v3f);
		position.x = v3f.x + GCoM.x;
		position.y = v3f.y + GCoM.y;
	}

	public void updateTransformGroup() {
		T3D.rotZ(orientation);
		T3D.setTranslation(new Vector3f(position.x, position.y, 0));
		TG.setTransform(T3D);
	}
	
	public void clearCaches() {
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		BG = new BranchGroup();
		BG.setCapability(BranchGroup.ALLOW_DETACH);
		TG = new TransformGroup();
		TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		BG.addChild(TG);
		T3D = new Transform3D();
	}
}
