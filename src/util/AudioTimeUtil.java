package util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.LongConsumer;

import org.json.JSONWriter;

public class AudioTimeUtil {
	
	public static final DateTimeFormatter DATE_SPACE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final LocalDateTime UNIX_EPOCH = LocalDateTime.of(1970,1,1,0,0);

	public static LocalDateTime ofAudiotime(long timestamp) {
		LocalDateTime datetime = UNIX_EPOCH.plusSeconds(timestamp);	
		return datetime;
	}

	public static long toAudiotime(LocalDateTime localDateTime) {
		return Duration.between(UNIX_EPOCH, localDateTime).toSeconds();
	}

	public static String toTextMinutes(LocalDateTime localDateTime) {
		char[] c = new char[16];

		LocalDate localDate = localDateTime.toLocalDate();
		int y = localDate.getYear();
		c[0] = (char) ('0'+  y/1000);
		c[1] = (char) ('0'+ ((y%1000)/100));
		c[2] = (char) ('0'+ ((y%100)/10));
		c[3] = (char) ('0'+ (y%10));
		c[4] = (char) ('-');
		int m = localDate.getMonthValue();
		c[5] = (char) ('0'+(m/10));
		c[6] = (char) ('0'+(m%10));
		c[7] = (char) ('-');
		int d = localDate.getDayOfMonth();
		c[8] = (char) ('0'+(d/10));
		c[9] = (char) ('0'+(d%10));
		c[10] = (char) ('T');
		LocalTime localTime = localDateTime.toLocalTime();
		int h = localTime.getHour();
		c[11] = (char) ('0'+(h/10));
		c[12] = (char) ('0'+(h%10));
		c[13] = (char) (':');
		int mo = localTime.getMinute();
		c[14] = (char) ('0'+(mo/10));
		c[15] = (char) ('0'+(mo%10));		

		return new String(c);		
	}



	public static long toAudiotimeStart(String startText) {
		if(startText == null) {
			return Long.MIN_VALUE;
		}
		startText = startText.trim();
		switch(startText.length()) {
		case 0: {
			return Long.MIN_VALUE;
		}
		case 1: {
			if(startText.charAt(0)=='*') {
				return Long.MIN_VALUE;
			} else {
				throw new RuntimeException("unknown timestamp " + startText);
			}
		}
		case 4: {
			return toAudiotime(LocalDateTime.parse(startText+"-01-01T00:00:00"));
		}
		case 7: {
			return toAudiotime(LocalDateTime.parse(startText+"-01T00:00:00"));
		}
		case 10: {
			return toAudiotime(LocalDateTime.parse(startText+"T00:00:00"));
		}
		case 13: {
			return toAudiotime(LocalDateTime.parse(startText+":00:00"));
		}
		case 16: {
			return toAudiotime(LocalDateTime.parse(startText+":00"));
		}
		case 19: {
			return toAudiotime(LocalDateTime.parse(startText));
		}
		default:
			throw new RuntimeException("unknown timestamp "+startText);
		}		
	}

	public static long toAudiotimeEnd(String endText) {
		if(endText == null) {
			return Long.MAX_VALUE;
		}
		endText = endText.trim();
		switch(endText.length()) {
		case 0: {
			return Long.MAX_VALUE;
		}
		case 1: {
			if(endText.charAt(0)=='*') {
				return Long.MAX_VALUE;
			} else {
				throw new RuntimeException("unknown timestamp " + endText);
			}
		}
		case 4: {
			return toAudiotime(LocalDateTime.parse(endText+"-12-31T23:59:59"));
		}
		case 7: {			
			try {
				return toAudiotime(LocalDateTime.parse(endText+"-31T23:59:59"));
			} catch (DateTimeParseException e) {
				try {
					return toAudiotime(LocalDateTime.parse(endText+"-30T23:59:59"));
				} catch (DateTimeParseException e1) {
					try {
						return toAudiotime(LocalDateTime.parse(endText+"-29T23:59:59"));
					} catch (DateTimeParseException e2) {
						return toAudiotime(LocalDateTime.parse(endText+"-28T23:59:59"));
					}
				}
			}
		}
		case 10: {
			return toAudiotime(LocalDateTime.parse(endText+"T23:59:59"));
		}
		case 13: {
			return toAudiotime(LocalDateTime.parse(endText+":59:59"));
		}
		case 16: {
			return toAudiotime(LocalDateTime.parse(endText+":59"));
		}
		case 19: {
			return toAudiotime(LocalDateTime.parse(endText));
		}
		default:
			throw new RuntimeException("unknown timestamp "+endText);
		}		
	}

	public static String toString(long timestamp) {
		if(timestamp == Long.MIN_VALUE) {
			return "-inf";
		}
		if(timestamp == Long.MAX_VALUE) {
			return "+inf";
		}
		return ofAudiotime(timestamp).toString();
	}

	public static LongConsumer timestampDateTimeWriter(JSONWriter json) {
		return timestamp -> writeTimestampDateTime(json, timestamp);
	}

	public static void writeTimestampDateTime(JSONWriter json, long timestamp) {
		json.object();
		writePropsTimestampDateTime(json, timestamp);			
		json.endObject();
	}

	public static void writePropsTimestampDateTime(JSONWriter json, long timestamp) {
		json.key("timestamp");
		json.value(timestamp);
		if(timestamp > 0) {
			LocalDateTime dateTime = AudioTimeUtil.ofAudiotime(timestamp);
			json.key("date");
			json.value(dateTime.toLocalDate());
			json.key("time");
			json.value(dateTime.toLocalTime());
		}				
	}

	public static LongConsumer timestampDateWriter(JSONWriter json) {
		return timestamp -> writeTimestampDate(json, timestamp);
	}

	public static void writeTimestampDate(JSONWriter json, long timestamp) {
		json.object();
		writePropsTimestampDate(json, timestamp);			
		json.endObject();
	}

	public static void writePropsTimestampDate(JSONWriter json, long timestamp) {
		json.key("timestamp");
		json.value(timestamp);
		if(timestamp > 0) {
			LocalDateTime dateTime = AudioTimeUtil.ofAudiotime(timestamp);
			json.key("date");
			json.value(dateTime.toLocalDate());
			json.key("year");
			json.value(dateTime.getYear());
			json.key("month");
			json.value(dateTime.getMonthValue());
			json.key("day");
			json.value(dateTime.getDayOfMonth());
		}				
	}
}
