
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

class RenderPanel extends JPanel {

	public byte[] pixels;
	public List<Surface> surfaces;
	public List<Light> lights;
	public Camera cam;

	public double ambientIntensity = 0.7;

	private int width, height;
	private BufferedImage canvas;
	private static final double epsilon = 0.001;

	public RenderPanel(int width, int height, List<Surface> surfaces, Camera cam) {
		this.width = width;
		this.height = height;
		this.surfaces = surfaces;
		canvas = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		pixels = ((DataBufferByte) canvas.getRaster().getDataBuffer()).getData();
		this.cam = cam;
		lights = new ArrayList<>();
	}

	public void render() {
		for (int i = 0; i < pixels.length / 3; i++) {
			double u = i % width - width / 2 + 0.5;
			double v = height / 2 - i / width + 0.5;
			Ray ray = computeRay(cam.e, cam.w.multiply(-cam.focalLength).add(cam.u.multiply(u)).add(cam.v.multiply(v)));
			Surface first = null;
			HitRecord firstSurface = new HitRecord(Double.MAX_VALUE);
			for (Surface s : surfaces) {
				HitRecord record = new HitRecord(0);
				if (s.hit(ray, 0, Double.MAX_VALUE, record))
					if (record.t < firstSurface.t) {
						first = s;
						firstSurface = record;
					}
			}
			if (firstSurface.t != Double.MAX_VALUE) {
				int[] temp = new int[3];
				for (Light light : lights) {
					Vector3D l = light.pos.subtract(firstSurface.p);
					Vector3D h = cam.e.subtract(firstSurface.p).add(l).normalize();
					l = l.normalize();
					Ray shadowRay = computeRay(firstSurface.p, l);
					double shadow = 1;
					for (Surface s : surfaces)
						if (s.hit(shadowRay, epsilon, Double.MAX_VALUE, new HitRecord(0)))
							shadow = 0;
					for (int j = 0; j < 3; j++)
						temp[j] += signedByteToInt(first.specularBGR[j]) * light.intensity * shadow * Math.pow(Math.max(0, firstSurface.normal.dot(h)), first.phong) +
						           signedByteToInt(first.diffuseBGR[j]) * light.intensity * shadow * Math.max(0, firstSurface.normal.dot(l)) +
						           signedByteToInt(first.ambientBGR[j]) * ambientIntensity;
					for (int j = 0; j < 3; j++)
						pixels[i * 3 + j] = (byte) Math.min(255, temp[j]);
				}
			}
		}
		repaint();
	}

	public void addLight(Light light) {
		lights.add(light);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(canvas, null, null);
	}

	private Ray computeRay(Vector3D e, Vector3D s) {
		return new Ray(e, s);
	}

	private int signedByteToInt(byte b) {
		return (((b >> 7) & 1) == 1) ? 256 + b : b;
	}

}