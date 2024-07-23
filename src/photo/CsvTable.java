package photo;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;

public class CsvTable implements Closeable {

	private CsvReader<CsvRecord> csv;
	private CloseableIterator<CsvRecord> it;
	private HashMap<String, Integer> headerMap;

	public static class CsvCell {
		public static CsvCell DEFAULT = new CsvCell();
		private CsvCell() {
		}
		public String get(CsvRecord row) {
			return null;			
		}
	}

	public static class CsvCellPos extends CsvCell {
		private final int pos;
		public CsvCellPos(int pos) {
			this.pos = pos;
		}
		public String get(CsvRecord row) {
			return pos < row.getFieldCount() ? row.getField(pos) : null;			
		}
	}

	public CsvTable(Path path) throws IOException {
		CsvReader<CsvRecord> csv = CsvReader.builder()
				.fieldSeparator(',')
				.quoteCharacter('"')
				.commentStrategy(CommentStrategy.SKIP)
				.commentCharacter('#')
				.skipEmptyLines(true)
				.ignoreDifferentFieldCount(true)
				.acceptCharsAfterQuotes(false)
				.detectBomHeader(true)
				.build(new CsvRecordHandler(), path, Charset.forName("UTF-8"));

		this.it = csv.iterator();
		this.headerMap = new HashMap<String, Integer>();
		if(it.hasNext()) {
			CsvRecord header = it.next();
			int headerLen = header.getFieldCount();
			for(int i = 0; i < headerLen; i++) {
				headerMap.putIfAbsent(header.getField(i), i);
			}
		}
	}

	public CsvCell getCell(String name) {
		Integer pos = headerMap.get(name);
		return pos == null ? CsvCell.DEFAULT : new CsvCellPos(pos);
	}

	public void forEach(Consumer<CsvRecord> action) {
		it.forEachRemaining(action);
	}

	@FunctionalInterface
	public interface CsvRecordConsumer {
		void accept(CsvRecord csvRow, int pos) throws Exception;
	}

	public void forEach(CsvRecordConsumer consumer) throws Exception {
		CloseableIterator<CsvRecord> itt = this.it;
		int pos = 0;
		while(itt.hasNext()) {
			CsvRecord csvRow = itt.next();
			consumer.accept(csvRow, pos);
			pos++;
		}
	}

	@FunctionalInterface
	public interface CsvRecordConsumerThrowable<E extends Exception> {
		void accept(CsvRecord csvRow, int pos) throws E;
	}

	public <E extends Exception> void forEachThrowable(CsvRecordConsumerThrowable<E> consumer) throws E {
		CloseableIterator<CsvRecord> itt = this.it;
		int pos = 0;
		while(itt.hasNext()) {
			CsvRecord csvRow = itt.next();
			consumer.accept(csvRow, pos);
			pos++;
		}
	}

	@Override
	public void close() throws IOException {
		CloseableIterator<CsvRecord> i = it;
		if(i != null) {
			i.close();
		}
		it = null;
		CsvReader<CsvRecord> c = csv;
		if(c != null) {
			c.close();
		}		
		csv = null;
	}
}
