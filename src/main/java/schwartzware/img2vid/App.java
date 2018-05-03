package schwartzware.img2vid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;

import schwartzware.img2vid.ImgToXDCAM_HD422.SimpleFFmpegProgressListener;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		String currentOS = OSInfo.getOs().toString().toLowerCase();
		Path ffmpegBinDir = Paths.get("./ffmpeg/" + currentOS);

		File inputFile = new File("./test.png");

		Path outputDir = Paths.get("./");
		final Path outputFile = outputDir.resolve("test.mxf");

		int durationInSeconds = 2;
		BufferedImage img = null;
		try {
			img = ImageIO.read(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		final ImgToXDCAM_HD422 img2vid = new ImgToXDCAM_HD422(FFmpeg.atPath(ffmpegBinDir), img, durationInSeconds);
		img2vid.addProgressListener(new SimpleFFmpegProgressListener() {
			public void onProgress(int currentFrame, float progress) {
				System.out.println("Current Frame: " + currentFrame);
				System.out.println("Current Progress: " + progress);
			}

			public void onError(String msg) {
				System.out.println("Error: " + msg);
			}
		});

		new Thread() {
			@Override
			public void run() {
				img2vid.start(outputFile);
			}
		}.start();
	}
}
