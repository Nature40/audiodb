package audio.server.api;

import java.io.IOException;
import java.io.RandomAccessFile;

import qoa.QOADecoder;

class RafQOADecoder extends QOADecoder {

	private final RandomAccessFile raf;

	private final byte[] buf = new byte[8192];
	private int pos = 0;
	private int len = 0;

	public RafQOADecoder(RandomAccessFile raf) throws IOException {
		this.raf = raf;
	}

	@Override
	protected int readByte() {
		if(pos < len) {
			return buf[pos++] & 0xff;
		} else {
			try {
				len = raf.read(buf, 0, buf.length);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(len == -1) {
				return -1;
			}
			pos = 1;
			return buf[0] & 0xff;
		}
	}

	@Override
	protected void seekToByte(int position) {
		pos = 0;
		len = 0;
		try {
			raf.seek(position);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
}