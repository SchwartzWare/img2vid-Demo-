package schwartzware.img2vid;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameInput;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

public class ImgToXDCAM_HD422 {
	protected FFmpeg ffmpeg = null;

	protected BufferedImage inputImg = null;
	protected int duration = 0;
	protected Dimension frameSize = new Dimension(1920, 1080);

	protected ArrayList<SimpleFFmpegProgressListener> progressListeners = new ArrayList<>();

	public ImgToXDCAM_HD422(FFmpeg ffmpeg, BufferedImage img, int durationInSeconds) {
		this.ffmpeg = ffmpeg;

		BufferedImage tempImg = convertBufferedImageToType(img, BufferedImage.TYPE_3BYTE_BGR);
		int imgX = (int) (tempImg.getWidth() / 2 - frameSize.getWidth() / 2);
		int imgY = (int) (tempImg.getHeight() / 2 - frameSize.getHeight() / 2);
		int imgWidth = (int) (frameSize.getWidth());
		int imgHeight = (int) (frameSize.getHeight());
		this.inputImg = cropBufferedImage(tempImg, imgX, imgY, imgWidth, imgHeight);

		this.duration = durationInSeconds;
	}

	public void addProgressListener(SimpleFFmpegProgressListener l) {
		progressListeners.add(l);
	}

	public void removeProgressListener(SimpleFFmpegProgressListener l) {
		progressListeners.removeAll(Arrays.asList(l));
	}

	public void start(Path outputFile) {
		ExtendedProgressListener progressListener = null;
		ImgToXDCAM_HD422FrameProducer producer = null;
		try {
			producer = new ImgToXDCAM_HD422FrameProducer(inputImg, duration);
			progressListener = new ExtendedProgressListener(producer) {
				public void onProgress(int currentFrame, float progress) {
					for (SimpleFFmpegProgressListener l : progressListeners) {
						l.onProgress(currentFrame, progress);
					}
				}
			};

			ffmpeg.addInput(FrameInput.withProducer(producer))
					.addOutput(UrlOutput.toPath(outputFile).setCodec(StreamType.VIDEO, "mpeg2video")
							.setFrameSize("1920", "1080").setFrameRate("25").addArguments("-b:v", "50000k")
							.addArguments("-minrate", "50000k").addArguments("-maxrate", "50000k")
							.addArguments("-flags", "ilme").addArguments("-top", "1")
							.addArguments("-pix_fmt", "yuv422p").addArguments("-profile:v", "0")
							.addArguments("-level:v", "2").setCodec(StreamType.AUDIO, "pcm_s24le")
							.addArguments("-ar", "48000"))
					.addArguments("-map", "0:0").addArguments("-map", "0:1").addArguments("-map", "0:1")
					.addArguments("-map", "0:1").addArguments("-map", "0:1").addArguments("-map", "0:1")
					.addArguments("-map", "0:1").addArguments("-map", "0:1").addArguments("-map", "0:1")
					.setOverwriteOutput(true).setProgressListener(progressListener).execute();
		} catch (Exception e) {
			e.printStackTrace();
			for (SimpleFFmpegProgressListener l : progressListeners) {
				l.onError(e.getMessage());
			}
		}
		if (progressListener != null && producer != null) {
			if (progressListener.getLastFrame() != producer.getFramesCount()) {
				for (SimpleFFmpegProgressListener l : progressListeners) {
					l.onError("Not all requested frames where rendered!");
				}
			}
		}
	}

	public static BufferedImage cropBufferedImage(BufferedImage inputImg, int fromX, int fromY, int width, int height) {
		if (inputImg.getWidth() < width || inputImg.getHeight() < height) {
			BufferedImage tempImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			int x = tempImg.getWidth() / 2 - inputImg.getWidth() / 2;
			int y = tempImg.getHeight() / 2 - inputImg.getHeight() / 2;
			tempImg.getGraphics().drawImage(inputImg, x, y, null);
			return tempImg;
		}
		return inputImg.getSubimage(fromX, fromY, width, height);
	}

	public static BufferedImage convertBufferedImageToType(BufferedImage sourceImage, int targetType) {
		if (sourceImage.getType() == targetType) {
			return sourceImage;
		}

		BufferedImage image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
		image.getGraphics().drawImage(sourceImage, 0, 0, null);
		return image;
	}

	private class ImgToXDCAM_HD422FrameProducer implements FrameProducer {
		private BufferedImage inputImg = null;

		private int duration = 2;
		private int fps = 25;
		private int audioSampleRate = 48000;
		private int frames = duration * fps;
		private int[] samples = new int[audioSampleRate];

		private long frameCounter = 0;

		private int audioStreams = 1; // Generating only one stream and copying it with FFmpeg to get 8-channel audio
		private int[] counterOfRenderedAudioStreamFrames = new int[audioStreams];
		private int nextAudioStreamToRender = 0;

		public ImgToXDCAM_HD422FrameProducer(BufferedImage img, int duration) throws Exception {
			this.setDuration(duration);
			this.inputImg = img;
		}

		public ImgToXDCAM_HD422FrameProducer setDuration(int duration) throws Exception {
			if (duration < 1) {
				throw new Exception("Duration must be >= 1");
			}

			this.duration = duration;
			this.frames = duration * fps;

			return this;
		}

		public int getFramesCount() {
			return this.frames;
		}

		@Override
		public List<Stream> produceStreams() {
			frameCounter = 0;
			nextAudioStreamToRender = 0;
			counterOfRenderedAudioStreamFrames = new int[audioStreams];

			return Arrays.asList(
					new Stream().setId(0).setType(Stream.Type.VIDEO).setTimebase(1000L).setWidth(1920).setHeight(1080),
					new Stream().setId(1).setType(Stream.Type.AUDIO).setTimebase(1000L).setSampleRate(audioSampleRate)
							.setChannels(1));
		}

		@Override
		public Frame produce() {
			if (frameCounter > frames) {
				// System.out.println("Finished");
				return null;
			}

			if (counterOfRenderedAudioStreamFrames[audioStreams - 1] == frameCounter) {
				// System.out.println("Creating video frame " + frameCounter);

				Frame videoFrame = new Frame().setStreamId(0).setPts(frameCounter * 1000 / fps).setImage(this.inputImg);
				frameCounter++;
				nextAudioStreamToRender = 0;
				return videoFrame;
			}

			// System.out.println("Creating audio " + nextAudioStreamToRender + " frame "
			// + counterOfRenderedAudioStreamFrames[nextAudioStreamToRender]);
			Frame audioFrame = new Frame().setStreamId(nextAudioStreamToRender + 1).setSamples(samples)
					.setPts(counterOfRenderedAudioStreamFrames[nextAudioStreamToRender] * 1000 / fps);

			counterOfRenderedAudioStreamFrames[nextAudioStreamToRender]++;
			nextAudioStreamToRender++;
			return audioFrame;
		}

	}

	public interface SimpleFFmpegProgressListener {
		public void onProgress(int currentFrame, float progress);

		public void onError(String msg);
	}

	private abstract class ExtendedProgressListener implements ProgressListener, SimpleFFmpegProgressListener {
		private int lastFrame = -1;
		private ImgToXDCAM_HD422FrameProducer producer = null;

		public ExtendedProgressListener(ImgToXDCAM_HD422FrameProducer producer) {
			this.producer = producer;
		}

		public int getLastFrame() {
			return this.lastFrame;
		}

		@Override
		public void onProgress(FFmpegProgress progress) {
			lastFrame = (int) progress.getFrame();
			float progressValue = (float) (progress.getFrame() + 1) / (float) producer.getFramesCount();
			this.onProgress((int) progress.getFrame(), progressValue);
		}

		public void onProgress(int currentFrame, float progress) {
		}

		public void onError(String msg) {
		}
	}
}
