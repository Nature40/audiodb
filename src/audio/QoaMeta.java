package audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.tinylog.Logger;

import qoa.QOABase;

public class QoaMeta extends AudioFileMetaData {

	private static final int FULL_FRAME_LEN = 8 + 8 + 8 + 256 * 8;

	public QoaMeta(File file) throws FileNotFoundException, IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			long fileLen = raf.length();

			if (raf.readByte() != 'q' || raf.readByte() != 'o' || raf.readByte() != 'a' || raf.readByte() != 'f') {
				throw new RuntimeException("no QOA file");
			}

			int totalSamples = raf.readInt();			
			if(totalSamples < 1) {
				throw new RuntimeException("read error");
			}
			this.samples = totalSamples;

			int channels = raf.readUnsignedByte();
			if(channels != 1) {
				throw new RuntimeException("read error");
			}

			int sr0 = raf.readUnsignedByte();
			int sr1 = raf.readUnsignedByte();
			int sr2 = raf.readUnsignedByte();
			this.sample_rate = (sr0 << 16) + (sr1 << 8) + sr2;
			this.bits_per_sample = 16;

			int fullFrames = totalSamples / QOABase.MAX_FRAME_SAMPLES;			
			int fullFramesLen = FULL_FRAME_LEN * fullFrames;
			int paddedFrameSamples = totalSamples % QOABase.MAX_FRAME_SAMPLES;
			int paddedFrameSamplesLen = paddedFrameSamples == 0 ? 0 : 8 + 8 + 8 + ceilDiv(paddedFrameSamples, 20) * 8;
			int quaFileLen = 8 + fullFramesLen + paddedFrameSamplesLen;

			//Logger.info(totalSamples + " totalSamples");
			//Logger.info(fileLen + " fileLen");
			//Logger.info(quaFileLen + " quaFileLen");
			if(fileLen < quaFileLen) {
				throw new RuntimeException("missing end in QOA audio file");
			}
			if(fileLen > quaFileLen) {
				try {
					int extraBytes = (int) (fileLen - quaFileLen);
					//Logger.info("read riff at " + quaFileLen + "  with  " + extraBytes);
					raf.seek(quaFileLen);
					byte[] metaBytes = new byte[extraBytes];
					raf.read(metaBytes);
					raf.close();
					ByteBuffer metaBuffer = ByteBuffer.wrap(metaBytes);
					metaBuffer.order(ByteOrder.LITTLE_ENDIAN);
					AudioFileMetaData riff = new Riff(metaBuffer, extraBytes, extraBytes, file, false);
					Logger.info(riff);
					this.comments = riff.comments;
					this.artist = riff.artist;
				} catch(Exception e) {

				}
			}
		}
	}

	private static int ceilDiv(int x, int y){
		return -Math.floorDiv(-x,y);
	}
}
