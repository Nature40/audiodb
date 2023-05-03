package audio.server.api;

import java.io.IOException;
import java.io.InputStream;

import qoa.QOADecoder;

class StreamQOADecoder extends QOADecoder {

	private final InputStream in;

	public StreamQOADecoder(InputStream in) {
		this.in = in;
	}

	@Override
	protected int readByte() {
		try {
			return in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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