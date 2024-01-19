package audio.server.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.tinylog.Logger;

import qoa.QOAEncoder;

public class RafQOAEncoder extends QOAEncoder implements Closeable {

	private RandomAccessFile raf;
	private byte[] buf = new byte[8192];
	private int pos;

	public RafQOAEncoder(RandomAccessFile raf) {
		this.raf = raf; 
	}

	@Override
	protected boolean writeLong(long v) {
		if(buf.length < pos + 8) {
			try {
				flushAlways();
			} catch (IOException e) {
				Logger.warn(e);
				return false;
			}
		}
		buf[pos++] = (byte) (v >>> 56);
		buf[pos++] = (byte) (v >>> 48);
		buf[pos++] = (byte) (v >>> 40);
		buf[pos++] = (byte) (v >>> 32);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
		return true;		
	}

	public void flush() throws IOException {
		if(pos > 0) {
			raf.write(buf, 0, pos);
			pos = 0;
		}
	}

	public void flushAlways() throws IOException {
		raf.write(buf, 0, pos);
		pos = 0;
	}

	@Override
	public void close() throws IOException {
		if(pos > 0) {
			raf.write(buf, 0, pos);
		}
		raf = null;
		buf = null;
		pos = 0;		
	}
}