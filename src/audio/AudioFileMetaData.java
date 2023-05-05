package audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import audio.task.Task_audio_create_yaml;

public class AudioFileMetaData {

	public int sample_rate = -1;
	public int avg_bytes_per_sec = -1;
	public short bits_per_sample = -1;
	public long samples = -1;
	public String comments = null;
	public String artist = null;
	
	public static AudioFileMetaData createFromAudioFile(File audioFile) throws FileNotFoundException, IOException {
		String filename = audioFile.getName();
		if(Task_audio_create_yaml.isQoa(filename)) {
			return new QoaMeta(audioFile);
		} else {
			return new Riff(audioFile);
		}
	}
}
