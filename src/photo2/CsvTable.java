package photo2;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

public class CsvTable implements Closeable {

	private CsvReader csv;
	private CloseableIterator<CsvRow> it;
	private HashMap<String, Integer> headerMap;
	
	public static class CsvCell {
		public static CsvCell DEFAULT = new CsvCell();
		private CsvCell() {
		}
		public String get(CsvRow row) {
			return null;			
		}
	}
	
	public static class CsvCellPos extends CsvCell {
		private final int pos;
		public CsvCellPos(int pos) {
			this.pos = pos;
		}
		public String get(CsvRow row) {
			return pos < row.getFieldCount() ? row.getField(pos) : null;			
		}
	}

	public CsvTable(Path path) throws IOException {
		this.csv = CsvReader.builder().commentStrategy(CommentStrategy.SKIP).build(path, Charset.forName("UTF-8"));
		this.it = csv.iterator();
		this.headerMap = new HashMap<String, Integer>();
		if(it.hasNext()) {
			CsvRow header = it.next();
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
	
	public void forEach(Consumer<CsvRow> action) {
		it.forEachRemaining(action);
	}
	
	@FunctionalInterface
	public interface CsvRowConsumer {
		void accept(CsvRow csvRow, int pos) throws Exception;
	}
	
	public void forEach(CsvRowConsumer consumer) throws Exception {
		CloseableIterator<CsvRow> itt = this.it;
		int pos = 0;
		while(itt.hasNext()) {
			CsvRow csvRow = itt.next();
			if(!csvRow.isComment() && !csvRow.isEmpty()) {
				consumer.accept(csvRow, pos);
				pos++;
			}
		}
	}
	
	@FunctionalInterface
	public interface CsvRowConsumerThrowable<E extends Exception> {
		void accept(CsvRow csvRow, int pos) throws E;
	}
	
	public <E extends Exception> void forEachThrowable(CsvRowConsumerThrowable<E> consumer) throws E {
		CloseableIterator<CsvRow> itt = this.it;
		int pos = 0;
		while(itt.hasNext()) {
			CsvRow csvRow = itt.next();
			if(!csvRow.isComment() && !csvRow.isEmpty()) {
				consumer.accept(csvRow, pos);
				pos++;
			}
		}
	}

	@Override
	public void close() throws IOException {
		CloseableIterator<CsvRow> i = it;
		if(i != null) {
			i.close();
		}
		it = null;
		CsvReader c = csv;
		if(c != null) {
			c.close();
		}		
		csv = null;
	}
}
