import java.util.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

public class ImagePack {

	public static void main(String[] a) {
		// handle arguments
		List<String> args = new ArrayList<>(Arrays.asList(a));
		int spritew = 8;
		int spriteh = 8;
		for(int x = 0; x < args.size(); x++) {
			if (!args.get(x).startsWith("--sprite=")) { continue; }
			if (args.get(x).length() == "--sprite=".length()) {
				System.err.format("no size for sprite specified!%n");
				System.exit(1);
			}
			String dim = args.get(x).split("=")[1];
			args.remove(x);
			try {
				spriteh = Integer.parseInt(dim);
				if (spriteh < 0 || spriteh > 15) { throw new NumberFormatException(); }
				if (spriteh == 0) {
					spritew = 16;
					spriteh = 16;
				}
			}
			catch(NumberFormatException e) {
				System.err.format("'%s' is not a valid sprite height!%n", dim);
				System.exit(1);
			}
			break;
		}
		Order order = Order.tblr;
		for(int x = 0; x < args.size(); x++) {
			if (!args.get(x).startsWith("--order=")) { continue; }
			if (args.get(x).length() == "--order=".length()) {
				System.err.format("no order string specified!%n");
				System.exit(1);
			}
			String o = args.get(x).split("=")[1].toLowerCase();
			args.remove(x);
			try {
				order = Order.valueOf(o);
			}
			catch(IllegalArgumentException e) {
				System.err.format("'%s' is not a valid order string!%n", o);
			}
			break;
		}
		if (args.size() != 1) {
			System.err.println("usage: imagepack [--sprite=Y] [--order=XXXX] <image>");
			System.exit(1);
		}

		// load image data
		BufferedImage image = null;
		String basename = "";
		try {
			File target = new File(args.get(0));
			image = ImageIO.read(target);
			basename = target.getName().split("\\.")[0];
		}
		catch(IOException e) {
			System.err.println("unable to load image '"+args.get(0)+"'");
			System.exit(1);
		}

		// check image dimensions
		if ((image.getWidth() % spritew) != 0 || (image.getHeight() % spriteh) != 0) {
			System.err.format("A %dx%d image is not evenly divisible into %dx%d sprites!%n",
				image.getWidth(),
				image.getHeight(),
				spritew,
				spriteh
			);
			System.exit(1);
		}

		// unpack image data
		List<Integer> data = new ArrayList<>();
		int outerMax = order.getOuterMax(image, spritew, spriteh);
		int innerMax = order.getInnerMax(image, spritew, spriteh);
		for(int outer = 0; outer < outerMax; outer++) {
			for(int inner = 0; inner < innerMax; inner++) {
				for(int yoff = 0; yoff < spriteh; yoff += 1) {
					for(int xoff = 0; xoff < spritew; xoff += 8) {
						data.add(order.getByte(outer, inner, spritew, spriteh, xoff, yoff, image));
					}
				}
			}
		}

		// format out image data
		System.out.format(": %s # (%d bytes)%n\t", basename, data.size());
		for(int index = 0; index < data.size(); index++) {
			System.out.format((index % 8 == 7) ? "0x%02X%n\t" : "0x%02X ",
				data.get(index)
			);
		}
		System.out.println();

		System.exit(0);	
	}
}

enum Order {
	tblr(true ,true ,true ), // default
	tbrl(true ,true ,false),
	btlr(true ,false,true ),
	btrl(true ,false,false),
	lrtb(false,true ,true ),
	lrbt(false,true ,false),
	rltb(false,false,true ),
	rlbt(false,false,false);

	private final boolean verticalOuter;
	private final boolean ascendingOuter;
	private final boolean ascendingInner;

	private Order(boolean verticalOuter, boolean ascendingOuter, boolean ascendingInner) {
		this.verticalOuter  = verticalOuter;
		this.ascendingOuter = ascendingOuter;
		this.ascendingInner = ascendingInner;
	}

	public int getOuterMax(BufferedImage i, int spritew, int spriteh) {
		return  verticalOuter ? (i.getHeight()/spriteh) : (i.getWidth()/spritew);
	}

	public int getInnerMax(BufferedImage i, int spritew, int spriteh) {
		return !verticalOuter ? (i.getHeight()/spriteh) : (i.getWidth()/spritew);
	}

	public int getByte(int outer, int inner, int spritew, int spriteh, int xoff, int yoff, BufferedImage i) {
		if (!ascendingOuter) { outer = getOuterMax(i, spritew, spriteh) - 1 - outer; }
		if (!ascendingInner) { inner = getInnerMax(i, spritew, spriteh) - 1 - inner; }
		int x = xoff + ( verticalOuter ? (inner*spritew) : (outer*spritew));
		int y = yoff + (!verticalOuter ? (inner*spriteh) : (outer*spriteh));
		int ret = 0;
		for(int index = 0; index < 8; index++) {
			int pixel = i.getRGB(x + index, y);
			ret = ((ret << 1) | (((pixel & 0xFF000000) != 0) ? 1 : 0));
		}
		return ret;
	}
}
