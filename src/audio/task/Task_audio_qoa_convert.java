package audio.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tinylog.Logger;

import audio.RiffExtractor;
import audio.Sample2;
import audio.server.api.RafQOAEncoder;
import qoa.QOABase;
import task.Description;
import task.Descriptor.Param.Type;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Convert audio files to QOA audio format.")
@Param(name = "dst_path", type = Type.STRING, preset = "qoa_output", description = "Root path destination for converted QOA audio files.")
@Role("admin")
public class Task_audio_qoa_convert extends Task {

	private static final boolean ADD_RIFF = true;

	private Path src_root;
	private Path dst_root;

	@Override
	protected void init() {
		src_root = ctx.broker.config().audioConfig.root_data_path;
		Logger.info(this.ctx);
		String dst_path = this.ctx.getParamString("dst_path");
		dst_root = Paths.get(dst_path);
	}

	@Override
	protected void run() throws Exception {		
		this.ctx.broker.sampleManager().forEach(sample -> {
			convert(sample);
		});	
	}

	private void convert(Sample2 sample) {		
		Path src_path = sample.samplePath;
		String src_filename = src_path.getFileName().toString();
		if(Task_audio_create_yaml.isWav(src_filename)) {
			this.setMessage(src_filename);
			String dst_filename = src_filename.substring(0, src_filename.length() - 4) + ".qoa";
			Path rel_src_path = src_root.relativize(src_path);
			Path dst_path = dst_root.resolve(rel_src_path).resolveSibling(dst_filename);
			dst_path.getParent().toFile().mkdirs();
			this.setMessage(dst_filename);
			this.setMessage(dst_path.toString());
			try {
				convert(sample, src_path, dst_path);
			} catch (FileNotFoundException e) {
				setMessage(e.getMessage());
			} catch (IOException e) {
				setMessage(e.getMessage());
			} catch (UnsupportedAudioFileException e) {
				setMessage(e.getMessage());
			}
		}
	}

	private void convert(Sample2 sample, Path src_path, Path dst_path) throws FileNotFoundException, IOException, UnsupportedAudioFileException {
		File src_file = src_path.toFile();
		if(!src_file.exists()) {
			throw new RuntimeException("missing src file: " + src_file);
		}
		File dst_file = dst_path.toFile();
		if(!dst_file.exists()) {
			convert(sample, src_file, dst_file);
		}
	}

	private void convert(Sample2 sample, File src_file, File dst_file) throws FileNotFoundException, IOException, UnsupportedAudioFileException {
		try(AudioInputStream in = AudioSystem.getAudioInputStream(src_file)) {		
			try(RandomAccessFile raf = new RandomAccessFile(dst_file, "rw")) {
				convert(in, raf);
				if(ADD_RIFF) {
					try {
						Logger.info(raf.getFilePointer());
						byte[] riffHeader = RiffExtractor.extract(src_file);
						raf.write(riffHeader);
						Logger.info(raf.getFilePointer());
						Logger.info(riffHeader.length);
					} catch(Exception e) {
						Logger.warn(e);
					}
				}
			}
		}
	}

	private void convert(AudioInputStream in, RandomAccessFile raf) throws IOException {
		AudioFormat audioFormat = in.getFormat();
		int sampleRate = (int) audioFormat.getSampleRate();
		if(sampleRate < 1 || sampleRate > 16777215) {
			throw new RuntimeException("Sample rate not supported.");
		}

		if(audioFormat.getChannels() != 1) {
			throw new RuntimeException("currently for audio only one channel is supported (mono).");
		}

		Encoding audioEncoding = audioFormat.getEncoding();			
		if(audioEncoding != Encoding.PCM_SIGNED) {
			throw new RuntimeException("currently audio in PCM_SIGNED encoding is supported.");
			//Logger.warn("currently audio in PCM_SIGNED encoding is supported.");
		}

		if(audioFormat.getSampleSizeInBits() != 16) {
			throw new RuntimeException("currently for audio only samples of 16 bit are supported: " + audioFormat.getSampleSizeInBits());
			//Logger.warn("currently for audio only samples of 16 bit are supported: " + audioFormat.getSampleSizeInBits());
		}

		if(audioFormat.getFrameSize() != 2) {
			throw new RuntimeException("currently for audio only frame size of 2 bytes is supported (PCM_SIGNED 16 bit mono) : " + audioFormat.getFrameSize());
			//Logger.warn("currently for audio only frame size of 2 bytes is supported (PCM_SIGNED 16 bit mono) : " + audioFormat.getFrameSize());
		}

		int frameLength = (int) in.getFrameLength();

		int shortsWritten = 0;
		try(RafQOAEncoder quaEncoder = new RafQOAEncoder(raf)) {
			if(!quaEncoder.writeHeader(frameLength, 1, sampleRate)) {
				throw new RuntimeException("data write error");
			}

			int bytesMaxLen = QOABase.MAX_FRAME_SAMPLES * 2;
			byte[] bytes = new byte[bytesMaxLen];
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
			if(audioFormat.isBigEndian()) {
				byteBuffer.order(ByteOrder.BIG_ENDIAN);
			} else {
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			}
			ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
			short[] shorts = new short[QOABase.MAX_FRAME_SAMPLES];

			for(;;) {
				int bytesLen = in.read(bytes, 0, bytesMaxLen);
				if(bytesLen <= 0) {
					break;
				}
				if(bytesLen % 2 != 0) {
					throw new RuntimeException("read error");
				}
				int shortsLen = bytesLen / 2;
				shortBuffer.rewind();
				shortBuffer.get(shorts, 0, shortsLen);
				if(!quaEncoder.writeFrame(shorts, shortsLen)) {
					throw new RuntimeException("data write error");
				}
				shortsWritten += shortsLen;
				//Logger.info("written " + shortsLen);
			}
			//streamQOAEncoder.flushAlways();	
		}
		if(frameLength != shortsWritten) {
			throw new RuntimeException("not all audio data written  " +  frameLength + "  " + shortsWritten);
		}

	}
}
