package audio;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sound.sampled.UnsupportedAudioFileException;


import org.tinylog.Logger;

import audio.server.api.AudioHandler;
import jakarta.servlet.http.HttpServletResponse;
import util.Timer;
import util.collections.vec.Vec;

public class AudioCache {

	private static class Entry {
		public final String infile;
		public final int osr;

		public volatile File outFile = null;
		public final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
		public final ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();

		public Entry(String infile, int osr) {
			this.infile = infile;
			this.osr = osr;
		}
	}

	private final int capacity;

	private Vec<Entry> vec;

	public AudioCache(int capacity) {
		this.capacity = capacity;
		this.vec = new Vec<Entry>(capacity);
	}

	public synchronized Entry getEntry(String infile, float overwrite_sampling_rate) {
		int osr = Float.floatToIntBits(overwrite_sampling_rate);
		int index = vec.findIndexOf(e -> infile.equals(e.infile) && osr == e.osr);
		Entry entry;
		if(index < 0) {
			Logger.info("insert " + infile + "   " + overwrite_sampling_rate);
			entry = new Entry(infile, osr);
		} else {
			//Logger.info("get chache pos " + index);
			entry = vec.remove(index);
		}
		entry.lifecycleLock.readLock().lock();
		vec.add(entry);
		return entry;
	}

	public synchronized Entry tryRemoveFirstUnlocked() {
		int len = vec.size();
		if(capacity < len) {
			for(int i = 0; i < len; i++) {
				Entry entry = vec.get(i);
				if(entry.lifecycleLock.writeLock().tryLock()) {
					Logger.info("remove pos " + i + "   of " + vec.size());
					vec.remove(i);
					return entry;
				} else {
					Logger.info("skip locked remove pos " + i);
				}
			}
		}
		return null;
	}

	public void run(File infile, float overwrite_sampling_rate, String rangeText, HttpServletResponse response) throws IOException {
		boolean changed = false;
		Entry entry = getEntry(infile.toString(), overwrite_sampling_rate);
		try {
			if(entry.outFile == null) {
				entry.dataLock.writeLock().lock();
				try {
					if(entry.outFile == null) {
						File tempFile = File.createTempFile("audio_", ".wav");
						entry.outFile = tempFile;
						changed = true;
						tempFile.deleteOnExit();
						try {	
							Logger.info("create overwrite sampling rate " + tempFile);	
							AudioHandler.createOverwriteSamplingRate(infile, tempFile, (float) overwrite_sampling_rate);
						} catch (UnsupportedAudioFileException e) {
							throw new RuntimeException(e);
						}
					}
				} finally {
					entry.dataLock.writeLock().unlock();
				}
			}

			entry.dataLock.readLock().lock();
			try {
				AudioHandler.sendFile(entry.outFile, rangeText, response, "audio/wave");
			} finally {
				entry.dataLock.readLock().unlock();
			}

		} finally {
			entry.lifecycleLock.readLock().unlock();

			if(changed) {
				entry = tryRemoveFirstUnlocked();
				while(entry != null) {
					try {
						File outFile = entry.outFile;
						entry.outFile = null;
						outFile.delete();
					} finally {
						entry.lifecycleLock.writeLock().unlock();					
						entry = tryRemoveFirstUnlocked();
					}
				}
			}
		}		
	}

	public File runDecode(File infile) throws IOException {
		boolean changed = false;
		Entry entry = getEntry(infile.toString(), Float.NaN);
		try {
			if(entry.outFile == null) {
				entry.dataLock.writeLock().lock();
				try {
					if(entry.outFile == null) {
						File tempFile = File.createTempFile("audio_", ".wav");
						entry.outFile = tempFile;
						changed = true;
						tempFile.deleteOnExit();
						AudioHandler.decodeQoa(infile, tempFile, -1);
						/*Logger.info("decode to " + tempFile);	
						for(int r = 0; r < 5; r++) {
							Timer.start("qoa decode");
							for(int i = 0; i < 100; i++) {
								AudioHandler.decodeQoa(infile, tempFile, -1);
							}
							Logger.info(Timer.stop("qoa decode"));
						}*/
					}
				} finally {
					entry.dataLock.writeLock().unlock();
				}
			}
			return entry.outFile;
		} finally {
			entry.lifecycleLock.readLock().unlock();

			if(changed) {
				entry = tryRemoveFirstUnlocked();
				while(entry != null) {
					try {
						File outFile = entry.outFile;
						entry.outFile = null;
						outFile.delete();
					} finally {
						entry.lifecycleLock.writeLock().unlock();					
						entry = tryRemoveFirstUnlocked();
					}
				}
			}
		}
	}
}
