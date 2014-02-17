import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.media.j3d.*;
import javax.vecmath.*;

public class Circle extends PhysicsObject {
	private static final long serialVersionUID = -1217683300913032033L;

	public float radius;
	public Color3f topColor;
	public Color3f bottomColor;

  public int visitor = 0;
	public int home = 0;
	
	public Circle(float mass, float positionX, float positionY, float velocityX, float velocityY, float orientation, float angularVelocity, float radius, Color3f color1, Color3f color2) {
		super(mass, positionX, positionY, velocityX, velocityY, orientation, angularVelocity);
		
		if (radius <= 0)
			throw new IllegalArgumentException();
		
		momentOfInertia = mass * radius * radius / 2;
		centerOfMass.x = 0f;
		// Using the parallel axis theorem
		momentOfInertia += mass * centerOfMass.lengthSquared();
		this.radius = radius;
		topColor = color1;
		bottomColor = color2;
		TG.addChild(createShape(radius, 20));
	}
	
	public Circle(float mass, Tuple2f position, Tuple2f velocity, float orientation, float angularVelocity, float radius, Color3f color1, Color3f color2) {
		this(mass, position.x, position.y, velocity.x, velocity.y, orientation, angularVelocity, radius, color1, color2);
	}
	
	private Node createShape(float radius, int samples) {
		samples += samples % 2;
		
		TriangleFanArray topGeometry = new TriangleFanArray(samples / 2 + 2, GeometryArray.COORDINATES, new int[] {samples / 2 + 2});
		Point3f[] vertices = new Point3f[samples / 2 + 2];
		vertices[0] = new Point3f();
		for (int i = 0; i <= samples / 2; i++)
			vertices[i+1] = new Point3f(radius * (float)Math.cos(2 * Math.PI * i / samples), radius * (float)Math.sin(2 * Math.PI * i / samples), 0);
		topGeometry.setCoordinates(0, vertices);

		TriangleFanArray bottomGeometry = new TriangleFanArray(samples / 2 + 2, GeometryArray.COORDINATES, new int[] {samples / 2 + 2});
		for (int i = samples / 2; i <= samples; i++)
			vertices[i - samples / 2 + 1] = new Point3f(radius * (float)Math.cos(2 * Math.PI * i / samples), radius * (float)Math.sin(2 * Math.PI * i / samples), 0);
		bottomGeometry.setCoordinates(0, vertices);
		
		PointArray centerOfMassGeometry = new PointArray(1, GeometryArray.COORDINATES);
		centerOfMassGeometry.setCoordinate(0, new Point3f(centerOfMass.x, centerOfMass.y, 0));
		
		BranchGroup root = new BranchGroup();
		if (topColor == null)
			topColor = new Color3f(Color.getHSBColor((float)Math.random(), 1, 1));
		Appearance appearance = new Appearance();
		appearance.setColoringAttributes(new ColoringAttributes(topColor, ColoringAttributes.FASTEST));
		PolygonAttributes polyAttr = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0);
		appearance.setPolygonAttributes(polyAttr);
		root.addChild(new Shape3D(topGeometry, appearance));
		
		if (bottomColor == null)
			bottomColor = new Color3f(Color.getHSBColor((float)Math.random(), 1, 1));
		appearance = new Appearance();
		appearance.setColoringAttributes(new ColoringAttributes(bottomColor, ColoringAttributes.FASTEST));
		appearance.setPolygonAttributes(polyAttr);
		root.addChild(new Shape3D(bottomGeometry, appearance));
		
		appearance = new Appearance();
		appearance.setPointAttributes(new PointAttributes(4, true));
		root.addChild(new Shape3D(centerOfMassGeometry, appearance));
	
		return root;
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		TG.addChild(createShape(radius, 20));
	}
}
