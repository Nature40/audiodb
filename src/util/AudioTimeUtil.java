package util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AudioTimeUtil {
	
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
		c[1] = (char) ('0'+ ((y%1000)/100)  );
		c[2] = (char) ('0'+ ((y%100)/10)  );
		c[3] = (char) ('0'+ (y%10)  );
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

}
