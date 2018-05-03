package schwartzware.img2vid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameInput;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

public class GenerateGIF {

	public static void main(String[] args) {
		Path BIN = Paths.get("./ffmpeg");
		// Path VIDEO_MP4 = Paths.get("/path/to/video.mp4");
		// Path OUTPUT_MP4 = Paths.get("/path/to/output.mp4");

		Path tempDir;
		tempDir = Paths.get("./");
		Path output = tempDir.resolve("test.gif");

		FrameProducer producer = new FrameProducer() {
			private long frameCounter = 0;

			public List<Stream> produceStreams() {
				return Collections.singletonList(
						new Stream().setType(Stream.Type.VIDEO).setTimebase(1000L).setWidth(320).setHeight(240));
			}

			public Frame produce() {
				if (frameCounter > 30) {
					return null;
				}
				System.out.println("Creating frame " + frameCounter);

				BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D graphics = image.createGraphics();
				graphics.setPaint(new Color(frameCounter * 1.0f / 30, 0, 0));
				graphics.fillRect(0, 0, 320, 240);

				Frame videoFrame = new Frame().setStreamId(0).setPts(frameCounter * 1000 / 10).setImage(image);
				frameCounter++;

				return videoFrame;
			}
		};

		FFmpegResult result = FFmpeg.atPath(BIN).addInput(FrameInput.withProducer(producer))
				.addOutput(UrlOutput.toPath(output)).execute();
	}

}
