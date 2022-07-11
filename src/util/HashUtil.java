package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

public class HashUtil {
	public static final long XXH64_SEED = 0xe951bd6bc02a275fl;
	public static final XXHashFactory XXH64_FACTORY = XXHashFactory.fastestInstance();
	
	public static StreamingXXHash64 createHashInstance() {
		StreamingXXHash64 xx = XXH64_FACTORY.newStreamingHash64(XXH64_SEED);
		return xx;
	}
	
	/**
	 * 
	 * @param file
	 * @return hash, throws if error
	 */
	public static long getFileHashLong(File file) {
		try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {				
			StreamingXXHash64 xx = HashUtil.createHashInstance();
			final int BUF_SIZE = 1024*1024;
			byte[] buf = new byte[BUF_SIZE];
			int readCount = raf.read(buf, 0, BUF_SIZE);
			while(readCount >= 0) {
				if(readCount > 0) {
					xx.update(buf, 0, readCount);
				}
				readCount = raf.read(buf, 0, BUF_SIZE);
			}
			long hash = xx.getValue();
			return hash;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param file
	 * @return hash, throws if error
	 */
	public static String getFileHashString(File file) {
		long hash = getFileHashLong(file);
		String xxh64 = Long.toHexString(hash);
		return xxh64;
	}
	
	
}
