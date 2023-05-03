package audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;


import org.tinylog.Logger;



public class Riff {
	

	static final int RIFF_MARK = 1179011410;
	static final int WAVE_MARK = 1163280727;
	static final int fmt_CHUNK_MARKER = 544501094;
	private static final int LIST_CHUNK_MARKER = 1414744396;
	static final int data_CHUNK_MARKER = 1635017060;
	private static final int INFO_LIST_TYPE = 1330007625;
	private static final int ICMT_SUBCHUNK_MARKER = 1414349641;
	private static final int IART_SUBCHUNK_MARKER = 1414676809;
	
	static final short PCM_WAVE_TYPE = 1;
	
	public short type = -1;
	public short channels = -1;
	public int sample_rate = -1;
	public int avg_bytes_per_sec = -1;
	public short bits_per_sample = -1;
	public long samples = -1;
	public short block_align;
	public String comments = null;
	public String artist = null;


	
	public static String convertIntToForcc(int n) {
		int a = (byte) n;
		if(a < 32 || a > 126) {
			return "";
		}
		int b = (byte) (n >> 8);
		if(b < 32 || b > 126) {
			return new String(new char[] {(char) a});
		}
		int c = (byte) (n >> 16);
		if(c < 32 || c > 126) {
			return new String(new char[] {(char) a, (char) b});
		}
		int d = (byte) (n >> 24);
		if(d < 32 || d > 126) {
			return new String(new char[] {(char) a, (char) b, (char) c});
		}
		return new String(new char[] {(char) a, (char) b, (char) c, (char) d});		
	}
	
	private String getString(ByteBuffer byteBuffer, int maxSize) {
		byte[] data = new byte[maxSize];
		byteBuffer.get(data);
		int textSize = maxSize;
		for (int i = 0; i < maxSize; i++) {
			if(data[i] == 0) {
				textSize = i;
				break;
			}
		}
		String text = new String(data, 0, textSize, StandardCharsets.US_ASCII);
		return text;
	}

	public Riff(File file) throws FileNotFoundException, IOException {
		int bufferSize = 4096;		
		long fileSize = -1;
		ByteBuffer byteBuffer = null;
		try(FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			fileSize = fileChannel.size();
			if(fileSize < 12) {
				Logger.warn("file too small for RIFF header");
				return;
			}
			if(fileSize < bufferSize) {
				bufferSize = (int) fileSize;
			}
			byteBuffer = ByteBuffer.allocate(bufferSize);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			fileChannel.read(byteBuffer);
		}
		byteBuffer.flip();
		int riffMarker = byteBuffer.getInt();
		if(riffMarker != RIFF_MARK) {
			Logger.info(riffMarker);
			throw new RuntimeException("no RIFF");
		}
		int riffFileSize = byteBuffer.getInt();
		//Logger.info("file size: " + riffFileSize);
		if((fileSize - 8) != riffFileSize) {
			Logger.warn("file size not same as in RIFF size: " + fileSize + "  RIFF " + riffFileSize + "  in " + file);
		}
		
		int riffTypeMarker = byteBuffer.getInt();
		if(riffTypeMarker != WAVE_MARK) {
			Logger.info(riffTypeMarker);
			throw new RuntimeException("no WAVE");
		}
		long chunkPosition = byteBuffer.position();
		while(chunkPosition + 8 <= bufferSize) {
			byteBuffer.position((int) chunkPosition);
			//Logger.info("chunk position: " + chunkPosition);
			int chunkMarker = byteBuffer.getInt();
			int chunkSize = byteBuffer.getInt();
			
			//Logger.info("chunk size: " + chunkSize + "   chunk    |" + convertIntToForcc(chunkMarker) + "|  " + chunkMarker);
			long nextChunkPosition = chunkPosition + 8 + chunkSize;
			
			if(chunkMarker == data_CHUNK_MARKER && chunkSize >= 0 && type == PCM_WAVE_TYPE && this.block_align > 0) {				
				long sampleEndPos = nextChunkPosition;
				if(sampleEndPos > fileSize) {
					sampleEndPos = fileSize;
				}				
				long sampleBytes = sampleEndPos - chunkPosition - 8;
				this.samples = sampleBytes / this.block_align;
				//Logger.info("fileSize " + fileSize + "  chunkSize " + chunkSize + "  block_align " + block_align + "  sampleBytes " + sampleBytes + "  samples " + samples);
			}
			
			if(nextChunkPosition > bufferSize) {
				break;
			}
			switch(chunkMarker) {
			case fmt_CHUNK_MARKER:
				//Logger.info("chunk " + "fmt");
				this.type = byteBuffer.getShort();
				this.channels = byteBuffer.getShort();
				this.sample_rate = byteBuffer.getInt();
				this.avg_bytes_per_sec =  byteBuffer.getInt();
				this.block_align = byteBuffer.getShort();
				//Logger.info("fmt_block_align " + fmt_block_align);
				if(type == PCM_WAVE_TYPE) {
					this.bits_per_sample = byteBuffer.getShort();
				}
				break;
			case LIST_CHUNK_MARKER:
				int listType = byteBuffer.getInt();
				//Logger.info("chunk " + "LIST   type    |" + convertIntToForcc(listType) + "|  " + listType);
				if(listType == INFO_LIST_TYPE) {
					long subChunkPosition = byteBuffer.position();
					while(subChunkPosition + 8 <= nextChunkPosition) {
						byteBuffer.position((int) subChunkPosition);
						int subChunkMarker = byteBuffer.getInt();
						int subChunkSize = byteBuffer.getInt();
						//Logger.info("subChunk size: " + subChunkSize + "   subChunk    |" + convertIntToForcc(subChunkMarker) + "|  " + subChunkMarker);
						long nextSubChunkPosition = subChunkPosition + 8 + subChunkSize;
						if(nextSubChunkPosition > nextChunkPosition) {
							break;
						}
						switch(subChunkMarker) {
						case ICMT_SUBCHUNK_MARKER: {
							this.comments = getString(byteBuffer, subChunkSize);
							//Logger.info("ICMT |" + comments + "|");
							break;
						}
						case IART_SUBCHUNK_MARKER: {
							this.artist = getString(byteBuffer, subChunkSize);
							//Logger.info("IART |" + artist + "|");
							break;
						}
						default:
							Logger.warn("unknown subChunk marker: " + subChunkMarker);
						}
						subChunkPosition = nextSubChunkPosition;
					}
				} else {
					Logger.warn("unknown LIST type: " + listType);
				}

				break;
			case data_CHUNK_MARKER:
				//Logger.info("chunk " + "data");
				break;
			default:
				Logger.warn("unknown chunk marker: " + chunkMarker);
			}
			chunkPosition = nextChunkPosition;			
		}
		/*if(waveMarker != WAVE_MARK) {
			Logger.info(waveMarker);
			throw new RuntimeException("no WAVE");
		}
		Logger.info(chunkMarker);
		Logger.info(chunkSize);*/
	}

}
