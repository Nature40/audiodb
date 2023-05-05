package audio;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.tinylog.Logger;

public class RiffExtractor {
	public static byte[] extract(File file) throws IOException {
		int bufferSize = 4096;		
		long fileSize = -1;
		ByteBuffer byteBuffer = null;
		try(FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			fileSize = fileChannel.size();
			if(fileSize < 12) {
				Logger.warn("file too small for RIFF header");
				return null;
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
		if(riffMarker != Riff.RIFF_MARK) {
			Logger.info(riffMarker);
			throw new RuntimeException("no RIFF");
		}
		int riffFileSize = byteBuffer.getInt();
		if((fileSize - 8) != riffFileSize) {
			Logger.warn("file size not same as in RIFF size: " + fileSize + "  RIFF " + riffFileSize + "  in " + file);
		}

		int riffTypeMarker = byteBuffer.getInt();
		if(riffTypeMarker != Riff.WAVE_MARK) {
			Logger.info(riffTypeMarker);
			throw new RuntimeException("no WAVE");
		}
		int chunkPosition = byteBuffer.position();
		int chunkCount = 0;
		while(chunkPosition + 8 <= bufferSize) {
			byteBuffer.position((int) chunkPosition);

			int chunkMarker = byteBuffer.getInt();
			int chunkSize = byteBuffer.getInt();

			int nextChunkPosition = chunkPosition + 8 + chunkSize;

			if(nextChunkPosition > bufferSize) {
				break;
			}
			switch(chunkMarker) {
			case Riff.data_CHUNK_MARKER:
				break;
			}
			chunkPosition = nextChunkPosition;
			chunkCount++;
		}
		Logger.info("chunks read " + chunkCount);
		if(chunkCount < 1) {
			return null;
		}
		byte[] header = new byte[chunkPosition];
		byteBuffer.rewind();
		byteBuffer.get(header);
		return header;
	}
}
