import javax.vecmath.*;

public class CollisionInfo {
	// Position of the collision in world coordinates. 
	Vector2f position;
	// Normal on the surface of the first object at the collision position.
	Vector2f normal;
	// Depth of overlap (positive).
	float depth;
}