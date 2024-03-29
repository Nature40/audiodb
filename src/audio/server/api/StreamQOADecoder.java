package audio.server.api;

import java.io.IOException;
import java.io.InputStream;

import qoa.QOADecoder;

class StreamQOADecoder extends QOADecoder {

	private final InputStream in;

	private final byte[] buf = new byte[8192];
	private int pos = 0;
	private int len = 0;

	public StreamQOADecoder(InputStream in) throws IOException {
		this.in = in;
	}

	@Override
	protected int readByte() {
		/*if(len == -1) {
			return -1;
		}*/
		if(pos < len) {
			return buf[pos++] & 0xff;
		} else {
			try {
				len = in.read(buf);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(len == -1) {
				return -1;
			}
			pos = 1;
			return buf[0] & 0xff;
		}
		/*try {
			return in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
	}

	@Override
	protected void seekToByte(int position) {
		throw new RuntimeException("seek not supported");
		/*try {
			in.getChannel().position(position);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/			
	}

}