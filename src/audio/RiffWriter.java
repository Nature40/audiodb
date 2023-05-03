package audio;

import java.io.IOException;
import java.io.OutputStream;

public class RiffWriter {

	private final OutputStream out;
	private final int samplesLen;
	private final int sampleRate;
	private final int channels;
	private int samplesWritten = 0;

	public RiffWriter(int samples, int sampleRate, int channels, OutputStream out) {
		this.samplesLen = samples;
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.out = out;
	}

	public void writeHeader() throws IOException {
		final int bytesPerSample = 2;
		final int headerSize = 44;
		final int dataSize = samplesLen * bytesPerSample;
		final int riffFileSize = headerSize + dataSize;
		final int fmtChunkSize = 16;
		final int bitsPerSample = bytesPerSample * 8;
		final int avgBytesPerSec = (sampleRate * bitsPerSample * channels) / 8;
		final int blockAlign = (bitsPerSample * channels) / 8;

		writeInt(Riff.RIFF_MARK);
		writeInt(riffFileSize);
		writeInt(Riff.WAVE_MARK);

		writeInt(Riff.fmt_CHUNK_MARKER);
		writeInt(fmtChunkSize);
		writeShort(Riff.PCM_WAVE_TYPE);
		writeShort(channels);
		writeInt(sampleRate);
		writeInt(avgBytesPerSec);
		writeShort(blockAlign);
		writeShort(bitsPerSample);

		writeInt(Riff.data_CHUNK_MARKER);
		writeInt(dataSize);
	}
	
	public void writeSamples(short[] samples, int dataLen) throws IOException {
		int len = samplesWritten + dataLen <= samplesLen ? dataLen : (samplesLen - samplesWritten >= 0 ? samplesLen - samplesWritten : 0);
		for (int i = 0; i < len; i++) {
			writeShort(samples[i]);
		}
		samplesWritten += len;
	}
	
	public boolean hasWrittenAllSamples() {
		return samplesWritten >= samplesLen;
	}
	
	private void writeInt(int v) throws IOException {
		out.write((v >>>  0) & 0xFF);
		out.write((v >>>  8) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>> 24) & 0xFF);
	}
	
	private void writeShort(int v) throws IOException {
		out.write((v >>> 0) & 0xFF);
		out.write((v >>> 8) & 0xFF);
	}
}
