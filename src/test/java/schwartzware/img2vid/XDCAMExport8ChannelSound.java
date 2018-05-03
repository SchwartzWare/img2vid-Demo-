package schwartzware.img2vid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameInput;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

/**
 * Hello world!
 *
 */
public class XDCAMExport8ChannelSound {
	public static void main(String[] args) {
		// TODO: Add other platforms
		Path BIN = Paths.get("./ffmpeg/windows");
		// Path VIDEO_MP4 = Paths.get("/path/to/video.mp4");
		// Path OUTPUT_MP4 = Paths.get("/path/to/output.mp4");

		Path tempDir;
		tempDir = Paths.get("./");
		Path output = tempDir.resolve("test.mxf");

		BufferedImage img = null;
		try {
			img = ImageIO.read(new File("./test.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		final BufferedImage loadedImg = convertToType(img, BufferedImage.TYPE_3BYTE_BGR);

		FrameProducer producer = new FrameProducer() {

			public List<Stream> produceStreams() {
				return Arrays.asList(
						new Stream().setId(0).setType(Stream.Type.VIDEO).setTimebase(1000L).setWidth(1920)
								.setHeight(1080),
						new Stream().setId(1).setType(Stream.Type.AUDIO).setTimebase(1000L).setSampleRate(sampleRate)
								.setChannels(1));
			}

			private long frameCounter = 0;
			int fps = 25;
			int sampleRate = 48000;
			int frames = 50;
			int nSamples = (int) (sampleRate);
			int[] samples = new int[nSamples];
			int audioStreams = 1;
			int[] counterOfRenderedAudioStreamFrames = new int[audioStreams];
			int nextAudioStreamToRender = 0;

			// In XDCAM every channel is a seperate Stream

			public Frame produce() {
				if (frameCounter > frames) {
					System.out.println("Finished");
					return null;
				}

				if (counterOfRenderedAudioStreamFrames[audioStreams - 1] == frameCounter) {
					System.out.println("Creating video frame " + frameCounter);

					Frame videoFrame = new Frame().setStreamId(0).setPts(frameCounter * 1000 / fps).setImage(loadedImg);
					frameCounter++;
					nextAudioStreamToRender = 0;
					return videoFrame;
				}

				System.out.println("Creating audio " + nextAudioStreamToRender + " frame "
						+ counterOfRenderedAudioStreamFrames[nextAudioStreamToRender]);
				Frame audioFrame = new Frame().setStreamId(nextAudioStreamToRender + 1)
						.setPts(counterOfRenderedAudioStreamFrames[nextAudioStreamToRender] * 1000 / fps).setSamples(samples);
				counterOfRenderedAudioStreamFrames[nextAudioStreamToRender]++;
				nextAudioStreamToRender++;
				return audioFrame;
			}
		};

		FFmpegResult result = FFmpeg.atPath(BIN).addInput(FrameInput.withProducer(producer))
				.addOutput(UrlOutput.toPath(output).setCodec(StreamType.VIDEO, "mpeg2video")
						.setFrameSize("1920", "1080").setFrameRate("25").addArguments("-b:v", "50000k")
						.addArguments("-minrate", "50000k").addArguments("-maxrate", "50000k")
						.addArguments("-flags", "ilme").addArguments("-top", "1").addArguments("-pix_fmt", "yuv422p")
						.addArguments("-profile:v", "0").addArguments("-level:v", "2")
						.setCodec(StreamType.AUDIO, "pcm_s24le").addArguments("-ar", "48000"))
				.addArguments("-map", "0:0").addArguments("-map", "0:1").addArguments("-map", "0:1")
				.addArguments("-map", "0:1").addArguments("-map", "0:1").addArguments("-map", "0:1")
				.addArguments("-map", "0:1").addArguments("-map", "0:1").addArguments("-map", "0:1")
				.setOverwriteOutput(true).setProgressListener(new ProgressListener() {

					@Override
					public void onProgress(FFmpegProgress progress) {
						System.out.println(progress.getFrame());
					}
				}).execute();

		// .addArguments("-shortest", "")
		// .addArguments("-map", "0:0").addArguments("-map", "1:0").addArguments("-map",
		// "1:0")
	}

	public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
		BufferedImage image;

		// if the source image is already the target type, return the source image

		if (sourceImage.getType() == targetType)
			image = sourceImage;

		// otherwise create a new image of the target type and draw the new
		// image

		else {
			image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
			image.getGraphics().drawImage(sourceImage, 0, 0, null);
		}

		return image;
	}
}
