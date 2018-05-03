package schwartzware.img2vid;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameInput;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

public class GenerateSilentMP3 {
	private final Path ffmpegBin;
	private final long sampleRate = 44100;
	private final long duration = 40000;

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSilentMP3.class);

	public GenerateSilentMP3(String ffmpegBin) {
		this.ffmpegBin = Paths.get(ffmpegBin);
	}

	public void execute() {
		Path output = Paths.get("silence.mp3");

		FrameProducer frameProducer = new FrameProducer() {
			private long nextAudioTimecode = 0;

			@Override
			public List<Stream> produceStreams() {
				return Arrays.asList(new Stream().setType(Stream.Type.AUDIO).setId(0).setTimebase(1000L)
						.setSampleRate(sampleRate).setChannels(1));
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
				.setOverwriteOutput(true).addOutput(UrlOutput.toPath(output).addArguments("-b:a", "320k")).execute();

		if (result != null) {
			LOGGER.info("Finished successfully: " + result);
		}
	}

	public static void main(String[] args) {
		new GenerateSilentMP3("./ffmpeg").execute();
	}

}