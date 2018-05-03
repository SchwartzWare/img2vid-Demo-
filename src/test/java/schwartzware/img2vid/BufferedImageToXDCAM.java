package schwartzware.img2vid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
public class BufferedImageToXDCAM {
	public static void main(String[] args) {
		Path BIN = Paths.get("./ffmpeg");
		// Path VIDEO_MP4 = Paths.get("/path/to/video.mp4");
		// Path OUTPUT_MP4 = Paths.get("/path/to/output.mp4");

		Path tempDir;
		tempDir = Paths.get("./");
		Path output = tempDir.resolve("test.mxf");

		FrameProducer producer = new FrameProducer() {

			public List<Stream> produceStreams() {
				return Arrays.asList(
						new Stream().setId(0).setType(Stream.Type.VIDEO).setTimebase(1000L).setWidth(1920)
								.setHeight(1080),
						new Stream().setId(1).setType(Stream.Type.AUDIO).setTimebase(1000L).setSampleRate(sampleRate)
								.setChannels(1),
						new Stream().setId(2).setType(Stream.Type.AUDIO).setTimebase(1000L).setSampleRate(sampleRate)
								.setChannels(1));
			}

			private long frameCounter = 0;
			private long audio1FrameCounter = 0;
			private long audio2FrameCounter = 0;
			int fps = 25;
			int sampleRate = 48000;
			int frames = 50;
			int nSamples = (int) (sampleRate);
			int[] samples = new int[nSamples];

			public Frame produce() {
				if (frameCounter > frames) {
					System.out.println("Finished");
					return null;
				}

				System.out.println("video:" + frameCounter);
				System.out.println("audio1:" + audio1FrameCounter);
				System.out.println("audio2:" + audio2FrameCounter);

				if (audio2FrameCounter == frameCounter) {
					System.out.println("Creating video frame " + frameCounter);

					BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR);
					Graphics2D graphics = image.createGraphics();
					graphics.setPaint(new Color(frameCounter * 1.0f / frames, 0, 0));
					graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

					Frame videoFrame = new Frame().setStreamId(0).setPts(frameCounter * 1000 / fps).setImage(image);
					frameCounter++;
					return videoFrame;
				}

				if (audio1FrameCounter < frameCounter) {
					System.out.println("Creating audio 1 frame " + audio1FrameCounter);

					Frame audioFrame = new Frame().setStreamId(1).setPts(audio1FrameCounter * 1000 / fps)
							.setSamples(samples);
					audio1FrameCounter++;
					return audioFrame;
				}

				System.out.println("Creating audio 2 frame " + audio2FrameCounter);

				Frame audioFrame = new Frame().setStreamId(2).setPts(audio2FrameCounter * 1000 / fps)
						.setSamples(samples);
				audio2FrameCounter++;

				return audioFrame;
			}
		};

		FFmpegResult result = FFmpeg.atPath(BIN).addInput(FrameInput.withProducer(producer).addArguments("-f", "lavfi"))
				.addOutput(UrlOutput.toPath(output).setCodec(StreamType.VIDEO, "mpeg2video")
						.setFrameSize("1920", "1080").setFrameRate("25").addArguments("-b:v", "50000k")
						.addArguments("-minrate", "50000k").addArguments("-maxrate", "50000k")
						.addArguments("-flags", "ilme").addArguments("-top", "1").addArguments("-pix_fmt", "yuv422p")
						.addArguments("-profile:v", "0").addArguments("-level:v", "2")

						.setCodec(StreamType.AUDIO, "pcm_s24le").addArguments("-ar", "48000"))
				.addArguments("-map", "0:0").addArguments("-map", "0:1").addArguments("-map", "0:2")
				.setOverwriteOutput(true).setProgressListener(new ProgressListener() {

					@Override
					public void onProgress(FFmpegProgress progress) {
						// System.out.println(progress.getTime());
					}
				}).execute();

		// .addArguments("-shortest", "")
		// .addArguments("-map", "0:0").addArguments("-map", "1:0").addArguments("-map",
		// "1:0")
	}
}
