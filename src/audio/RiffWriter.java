package audio;

import java.io.IOException;

import util.BufferedDataOutputStreamLE;

public class RiffWriter {

	private final BufferedDataOutputStreamLE out;
	private final int samplesLen;
	private final int sampleRate;
	private final int channels;
	private int samplesWritten = 0;

	public RiffWriter(int samples, int sampleRate, int channels, BufferedDataOutputStreamLE out) {
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

		out.writeInt(Riff.RIFF_MARK);
		out.writeInt(riffFileSize);
		out.writeInt(Riff.WAVE_MARK);

		out.writeInt(Riff.fmt_CHUNK_MARKER);
		out.writeInt(fmtChunkSize);
		out.writeShort(Riff.PCM_WAVE_TYPE);
		out.writeShort(channels);
		out.writeInt(sampleRate);
		out.writeInt(avgBytesPerSec);
		out.writeShort(blockAlign);
		out.writeShort(bitsPerSample);

		out.writeInt(Riff.data_CHUNK_MARKER);
		out.writeInt(dataSize);
	}
	
	public void writeSamples(short[] samples, int dataLen) throws IOException {
		int len = samplesWritten + dataLen <= samplesLen ? dataLen : (samplesLen - samplesWritten >= 0 ? samplesLen - samplesWritten : 0);
		for (int i = 0; i < len; i++) {
			out.writeShort(samples[i]);
		}
		samplesWritten += len;
	}
	
	public boolean hasWrittenAllSamples() {
		return samplesWritten >= samplesLen;
	}
}
