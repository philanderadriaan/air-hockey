import javax.vecmath.*;

public class HalfSpace extends PhysicsObject {
	public Vector2f normal;
	// Right-hand side of the plane equation A * x + B * y = C
	public float intercept;
	
	public HalfSpace(float positionX, float positionY, float normalX, float normalY) {
		super(Float.POSITIVE_INFINITY, positionX, positionY, 0, 0, 0, 0);
		normal = new Vector2f(normalX, normalY);
		normal.normalize();
		intercept = normal.dot(position);
		momentOfInertia = Float.POSITIVE_INFINITY;
	}

	public HalfSpace(Tuple2f position, Tuple2f normal) {
		this(position.x, position.y, normal.x, normal.y);
	}
}
