package schwartzware.img2vid;

import com.github.kokorin.jaffree.ffmpeg.*;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BouncingBallOnlyAudio {
	private final Path ffmpegBin;
	private final long sampleRate = 48000;
	private final long duration = 30_000;

	private static final Logger LOGGER = LoggerFactory.getLogger(BouncingBallOnlyAudio.class);

	public BouncingBallOnlyAudio(String ffmpegBin) {
		this.ffmpegBin = Paths.get(ffmpegBin);
	}

	public void execute() {
		Path output = Paths.get("bouncing_ball.wav");

		FrameProducer frameProducer = new FrameProducer() {
			private long nextAudioTimecode = 0;

			@Override
			public List<Stream> produceStreams() {
				return Arrays.asList(new Stream().setType(Stream.Type.AUDIO).setId(0)
						.setTimebase(1000L).setSampleRate(sampleRate).setChannels(1));
			}

			@Override
			public Frame produce() {
				if (nextAudioTimecode > duration) {
					LOGGER.info("Finished");
					return null;
				}

				int nSamples = (int) (sampleRate);
				int[] samples = new int[nSamples];

				Frame audioFrame = new Frame().setStreamId(0).setPts(nextAudioTimecode).setSamples(samples)
						.setImage(null);

				nextAudioTimecode += 1000;
				return audioFrame;
			}
		};

		FFmpegResult result = FFmpeg.atPath(ffmpegBin).addInput(FrameInput.withProducer(frameProducer))
				.setOverwriteOutput(true).addOutput(UrlOutput.toPath(output)).execute();

		if (result != null) {
			LOGGER.info("Finished successfully: " + result);
		}
	}

	public static void main(String[] args) {
		new BouncingBallOnlyAudio("./ffmpeg").execute();
	}

}