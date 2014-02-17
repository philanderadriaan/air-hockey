import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class P2PAirHockey
{
  // Physics updates per second (approximate).
  private static final int UPDATE_RATE = 60;
  // Number of full iterations of the collision detection and resolution system.
  private static final int COLLISION_ITERATIONS = 4;
  // Width of the extent in meters.
  private static final float EXTENT_WIDTH = 20;

  private static int goalCount = 0;
  private static int goalHomeCount = 0;
  // Normal boundaries that cause collisions
  private final HalfSpace[] boundaries;
  // Special boundaries that may result in object migration
  private final HalfSpace leftBoundary;
  // private final HalfSpace rightBoundary;
  private final LinkedList<PhysicsObject> objects;
  private Peer peer;
  private static BranchGroup scene;
  public static TransformGroup extentTransform = new TransformGroup();
  public static TransformGroup circleTransform = new TransformGroup();
  public static TransformGroup fieldTransform = new TransformGroup();
  public static Circle puck = new Circle(1, EXTENT_WIDTH / 5, 0, 0, 0, 0, 0, EXTENT_WIDTH * .04f,
                                         new Color3f(Color.getHSBColor((float) Math.random(),
                                                                       (float) Math.random(),
                                                                       (float) Math.max(Math
                                                                           .random(), 0.5))),
                                         new Color3f(Color.getHSBColor((float) Math.random(),
                                                                       (float) Math.random(),
                                                                       (float) Math.max(Math
                                                                           .random(), 0.5))));
  public static Circle mallet = new Circle(1, 0, 0, 0, 0, 0, 0, EXTENT_WIDTH * .05f,
                                           new Color3f(Color.getHSBColor(0, 0, 1)),
                                           new Color3f(Color.getHSBColor(0, 0, 1)));
  public static Vector2f previous_position = new Vector2f();
  public static Vector2f current_position = new Vector2f();
  public static Vector2f mouse_position = new Vector2f();

  public static void main(String[] args)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        new P2PAirHockey().createAndShowGUI();
      }
    });
  }

  public P2PAirHockey()
  {
    peer = new Peer();
    peer.setup();

    boundaries =
        new HalfSpace[] {new HalfSpace(-EXTENT_WIDTH / 2, -EXTENT_WIDTH / 2, 0, 1),
            new HalfSpace(EXTENT_WIDTH / 2, EXTENT_WIDTH / 2, 0, -1),
            new HalfSpace(EXTENT_WIDTH / 2, -EXTENT_WIDTH / 2, -1, 0)};
    leftBoundary = new HalfSpace(-EXTENT_WIDTH / 2, EXTENT_WIDTH / 2, 1, 0);
    objects = new LinkedList<PhysicsObject>();
    objects.add(mallet);
    if (peer.input == 1)
    {
      objects.add(puck);
    }
  }

  private void createAndShowGUI()
  {
    // Fix for background flickering on some platforms
    System.setProperty("sun.awt.noerasebackground", "true");

    GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
    final Canvas3D canvas3D = new Canvas3D(config);
    canvas3D.setPreferredSize(new Dimension(400, 300));
    SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
    simpleU.getViewingPlatform().setNominalViewingTransform();

    // Add a scaling transform that resizes the virtual world to fit
    // within the standard view frustum.
    BranchGroup trueScene = new BranchGroup();
    TransformGroup worldScaleTG = new TransformGroup();
    Transform3D t3D = new Transform3D();
    t3D.setScale(1 / EXTENT_WIDTH);
    worldScaleTG.setTransform(t3D);
    trueScene.addChild(worldScaleTG);
    scene = new BranchGroup();
    scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
    scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
    worldScaleTG.addChild(scene);

    extentTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    circleTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

    extentTransform.addChild(createTableBorders());
    extentTransform.addChild(createCenterLines());
    fieldTransform.addChild(createField());
    extentTransform.addChild(createGoalLine());
    // Create score board
    extentTransform.addChild(createScoreBoard(goalHomeCount));

    scene.addChild(fieldTransform);

    scene.addChild(extentTransform);

    for (PhysicsObject o : objects)
      scene.addChild(o.BG);

    simpleU.addBranchGraph(trueScene);

    JFrame appFrame = new JFrame("P2P Air Hockey");
    appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas3D.addMouseMotionListener(new MouseAdapter()
    {
      @Override
      public void mouseMoved(MouseEvent e)
      {
        mouse_position.x =
            (float) e.getX() / (float) canvas3D.getWidth() * EXTENT_WIDTH - (EXTENT_WIDTH / 2);
        mouse_position.y =
            -(float) e.getY() / (float) canvas3D.getHeight() * EXTENT_WIDTH +
                (EXTENT_WIDTH / 2);
      }
    });
    appFrame.add(canvas3D);
    appFrame.pack();
    if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
      appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

    new Timer(1000 / UPDATE_RATE, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        canvas3D.stopRenderer();
        tick();
        canvas3D.startRenderer();
      }
    }).start();

    appFrame.setVisible(true);
  }

  private void tick()
  {
    List<PhysicsObject> received = peer.getReceived();
    if (received != null)
      for (PhysicsObject o : received)
      {
        o.updateTransformGroup();
        scene.addChild(o.BG);
        objects.add(o);
      }
    for (PhysicsObject o : objects)
    {
      o.updateState(1f / UPDATE_RATE);
    }

    mallet.position.x = mouse_position.x;
    mallet.position.y = mouse_position.y;

    mallet.position.x = (float) Math.min(mallet.position.x, EXTENT_WIDTH / 2.5);
    mallet.position.x = (float) Math.max(mallet.position.x, 0);

    previous_position.x = current_position.x;
    previous_position.y = current_position.y;
    current_position.x = mallet.position.x;
    current_position.y = mallet.position.y;

    mallet.velocity.x = (current_position.x - previous_position.x) / (1f / UPDATE_RATE);
    mallet.velocity.y = (current_position.y - previous_position.y) / (1f / UPDATE_RATE);

    for (int i = 0; i < COLLISION_ITERATIONS; i++)
    {
      Iterator<PhysicsObject> itr = objects.iterator();
      while (itr.hasNext())
      {
        PhysicsObject o = itr.next();
        for (HalfSpace hs : boundaries)
          CollisionHandler.checkAndResolveCollision(hs, o);
        for (PhysicsObject o2 : objects)
          CollisionHandler.checkAndResolveCollision(o2, o);
        CollisionInfo ci = CollisionHandler.getCollisionInfo(leftBoundary, o);
        if (ci != null)
        {
        }
        if (ci != null)
        {
          int temp = ((Circle) o).visitor;
          ((Circle) o).visitor = ((Circle) o).home;
          ((Circle) o).home = temp;
          o.velocity.x = -o.velocity.x;
          if (peer.sendPhysicsObject(o, peer.getSuccessorID()))
          {
            o.BG.detach();
            itr.remove();
            continue;
          }
          else
          {
            o.position.x = -o.position.x - ci.depth;
            CollisionHandler.resolveCollision(leftBoundary, o, ci);
          }
        }
      }
    }
    mallet.velocity = new Vector2f(0, 0);
    for (PhysicsObject o : objects)
    {
      o.updateTransformGroup();
      // Clear the object's force accumulator.
      o.forceAccumulator.x = o.forceAccumulator.y = 0;
    }
  }

  private static Node createField()
  {
    float[] coordinates =
        {EXTENT_WIDTH / 2, EXTENT_WIDTH / 4, 0, (EXTENT_WIDTH / 2) - 2, EXTENT_WIDTH / 4, 0,
            (EXTENT_WIDTH / 2) - 2, -EXTENT_WIDTH / 4, 0, EXTENT_WIDTH / 2, -EXTENT_WIDTH / 4,
            0, EXTENT_WIDTH / 2, EXTENT_WIDTH / 4, 0};
    LineStripArray geometry = new LineStripArray(5, GeometryArray.COORDINATES, new int[] {4});

    geometry.setCoordinates(0, coordinates);
    Shape3D shape = new Shape3D(geometry);
    Appearance appearance = new Appearance();
    appearance.setColoringAttributes(new ColoringAttributes(1f, 0f, 0f,
                                                            ColoringAttributes.FASTEST));
    shape.setAppearance(appearance);

    return shape;
  }

  private static Node createCenterLines()
  {
    float[] coordinates =
        {0, EXTENT_WIDTH / 2, 0, 0, -EXTENT_WIDTH / 2, 0, (-EXTENT_WIDTH / 2) + 2,
            (-EXTENT_WIDTH / 2), 0, (-EXTENT_WIDTH / 2) + 2, (EXTENT_WIDTH / 2), 0, 0,
            EXTENT_WIDTH / 2, 0};
    LineStripArray geometry = new LineStripArray(5, GeometryArray.COORDINATES, new int[] {5});

    geometry.setCoordinates(0, coordinates);
    Shape3D shape = new Shape3D(geometry);
    Appearance appearance = new Appearance();
    appearance.setColoringAttributes(new ColoringAttributes(1f, 0f, 0f,
                                                            ColoringAttributes.FASTEST));
    shape.setAppearance(appearance);

    return shape;
  }

  private static Node createTableBorders()
  {
    float[] coordinates =
        {(-EXTENT_WIDTH / 2) - 2, (-EXTENT_WIDTH / 2) - 2, 0, (EXTENT_WIDTH / 2) + 2,
            (-EXTENT_WIDTH / 2) - 2, 0, EXTENT_WIDTH / 2, -EXTENT_WIDTH / 2, 0,
            -EXTENT_WIDTH / 2, -EXTENT_WIDTH / 2, 0, (EXTENT_WIDTH / 2) + 2,
            (-EXTENT_WIDTH / 2) - 2, 0, (EXTENT_WIDTH / 2) + 2, (EXTENT_WIDTH / 2) + 2, 0,
            (EXTENT_WIDTH / 2), (EXTENT_WIDTH / 2), 0, (EXTENT_WIDTH / 2),
            (-EXTENT_WIDTH / 2), 0,

            (EXTENT_WIDTH / 2), (EXTENT_WIDTH / 2), 0, (EXTENT_WIDTH / 2) + 2,
            (EXTENT_WIDTH / 2) + 2, 0, (-EXTENT_WIDTH / 2) - 2, (EXTENT_WIDTH / 2) + 2, 0,
            (-EXTENT_WIDTH / 2), (EXTENT_WIDTH / 2), 0, (EXTENT_WIDTH / 2),
            (EXTENT_WIDTH / 2), 0,};
    QuadArray geometry = new QuadArray(36, QuadArray.COORDINATES);

    geometry.setCoordinates(0, coordinates);
    Shape3D shape = new Shape3D(geometry);
    TextureLoader tl = new TextureLoader("wood.jpg", null);
    ImageComponent2D image = tl.getImage();
    int width = image.getWidth();
    int height = image.getHeight();
    Texture2D texture = new Texture2D(Texture.MULTI_LEVEL_MIPMAP, Texture.RGB, width, height);
    int imageLevel = 0;
    texture.setImage(imageLevel, image);
    while (width > 1 || height > 1)
    {
      imageLevel++;
      if (width > 1)
        width /= 2;
      if (height > 1)
        height /= 2;
      texture.setImage(imageLevel, tl.getScaledImage(width, height));
    }

    texture.setMagFilter(Texture2D.NICEST);
    texture.setMinFilter(Texture2D.NICEST);
    Appearance appearance = new Appearance();
    appearance.setTexture(texture);
    shape.setAppearance(appearance);

    return shape;
  }

  private static Node createGoalLine()
  {

    float[] coordinates =
        {(EXTENT_WIDTH / 2), (-EXTENT_WIDTH / 4), 0, (EXTENT_WIDTH / 2) + 2,
            (-EXTENT_WIDTH / 4), 0, (EXTENT_WIDTH / 2) + 2, EXTENT_WIDTH / 4, 0,
            EXTENT_WIDTH / 2, (EXTENT_WIDTH / 4), 0, (EXTENT_WIDTH / 2), (-EXTENT_WIDTH / 4),
            0

        };
    QuadArray geometry = new QuadArray(12, QuadArray.COORDINATES);

    geometry.setCoordinates(0, coordinates);
    Shape3D shape = new Shape3D(geometry);
    Appearance appearance = new Appearance();

    appearance.setColoringAttributes(new ColoringAttributes(0f, 0f, 1f,
                                                            ColoringAttributes.FASTEST));
    shape.setAppearance(appearance);

    return shape;

  }

  private static Node createScoreBoard(int goal1)
  {



    Font3D f3d = new Font3D(new Font("TestFont", Font.PLAIN, 2), new FontExtrusion());
    Text3D text =
        new Text3D(f3d, new String("P2P Air Hockey Table"),
                   new Point3f(-9f, -3.5f, -4.5f));
   
    Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    Color3f blue = new Color3f(.2f, 0.2f, 0.6f);
    Appearance a = new Appearance();
    Material m = new Material(blue, blue, blue, white, 120.0f);
    m.setLightingEnable(true);
    a.setMaterial(m);

    Shape3D shape = new Shape3D();
    shape.setGeometry(text);
    shape.setAppearance(a);

    return shape;

  }
}
