package util;

import static util.AssumptionCheck.throwFalse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.tinylog.Logger;

import ch.randelshofer.fastdoubleparser.FastDoubleParser;

public abstract class AbstractTable {

	public static class ColumnReader {
		public static final int MISSING_COLUMN = Integer.MAX_VALUE;
		public final int rowIndex;
		public ColumnReader(int rowIndex) {
			throwFalse(rowIndex >= 0);
			this.rowIndex = rowIndex;
		}
	}
	public static class ColumnReaderString extends ColumnReader {
		public ColumnReaderString(int rowIndex) {
			super(rowIndex);
		}
		public String get(String[] row) {
			return row[rowIndex];
		}
		public ColumnReaderString then(UnaryOperator<String> func) {
			ColumnReaderString outher = this;
			return new ColumnReaderString(rowIndex) {				
				@Override
				public String get(String[] row) {
					return func.apply(outher.get(row));
				}				
			};
		}
	}
	public static class ColumnReaderStringMissing extends ColumnReaderString {
		private String missing;
		public ColumnReaderStringMissing(String missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public String get(String[] row) {
			return missing;
		}		
	}
	public static class ColumnReaderFloat extends ColumnReader {
		public ColumnReaderFloat(int rowIndex) {
			super(rowIndex);
		}
		public float get(String[] row, boolean warnIfEmpty) {			
			try {
				String textValue = row[rowIndex];
				if(textValue.isEmpty()) {
					if(warnIfEmpty) {
						Logger.warn("empty");
					}
					return Float.NaN;
				}
				//return Float.parseFloat(row[rowIndex]);
				return (float) FastDoubleParser.parseDouble(row[rowIndex]);
			} catch(NumberFormatException e) {
				if(row[rowIndex].toLowerCase().equals("na")||row[rowIndex].toLowerCase().equals("null")||row[rowIndex].toLowerCase().equals("nan")) {
					return Float.NaN;
				} else {
					Logger.warn(row[rowIndex]+" not parsed");
					e.printStackTrace();
					return Float.NaN;
				}
			}
		}
		public ColumnReaderFloat then(UnaryOperator<Float> func) {
			ColumnReaderFloat outher = this;
			return new ColumnReaderFloat(rowIndex) {				
				@Override
				public float get(String[] row, boolean warnIfEmpty) {	
					return func.apply(outher.get(row, warnIfEmpty));
				}				
			};
		}
	}
	public static class ColumnReaderFloatMissing extends ColumnReaderFloat {
		private float missing;
		public ColumnReaderFloatMissing(float missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public float get(String[] row, boolean warnIfEmpty) {
			return missing;
		}		
	}
	public static class ColumnReaderDoubleMissing extends ColumnReaderDouble {
		private double missing;
		public ColumnReaderDoubleMissing(double missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public double get(String[] row, boolean warnIfEmpty) {
			return missing;
		}		
	}
	public static class ColumnReaderDouble extends ColumnReader {
		public ColumnReaderDouble(int rowIndex) {
			super(rowIndex);
		}
		public double get(String[] row, boolean warnIfEmpty) {			
			try {
				String textValue = row[rowIndex];
				if(textValue.isEmpty()) {
					if(warnIfEmpty) {
						Logger.warn("empty");
					}
					return Double.NaN;
				}
				//return Double.parseDouble(row[rowIndex]);
				return FastDoubleParser.parseDouble(row[rowIndex]);
			} catch(NumberFormatException e) {
				if(row[rowIndex].toLowerCase().equals("na")||row[rowIndex].toLowerCase().equals("null")||row[rowIndex].toLowerCase().equals("nan")) {
					return Double.NaN;
				} else {
					Logger.warn(row[rowIndex]+" not parsed");
					e.printStackTrace();
					return Double.NaN;
				}
			}
		}
		public ColumnReaderDouble then(UnaryOperator<Double> func) {
			ColumnReaderDouble outher = this;
			return new ColumnReaderDouble(rowIndex) {				
				@Override
				public double get(String[] row, boolean warnIfEmpty) {	
					return func.apply(outher.get(row, warnIfEmpty));
				}				
			};
		}
	}
	public static class ColumnReaderInt extends ColumnReader {
		public ColumnReaderInt(int rowIndex) {
			super(rowIndex);
		}
		public int get(String[] row) {
			return Integer.parseInt(row[rowIndex]);
		}
	}
	public static class ColumnReaderIntFunc extends ColumnReader {
		private final IntegerParser parser;
		public ColumnReaderIntFunc(int rowIndex, IntegerParser parser) {
			super(rowIndex);
			this.parser = parser;
		}
		public int get(String[] row) {
			return parser.parse(row[rowIndex]);
		}
		public interface IntegerParser {
			int parse(String text);
		}
	}
	public static abstract class ColumnReaderBoolean extends ColumnReader {
		public ColumnReaderBoolean(int rowIndex) {
			super(rowIndex);
		}
		public abstract boolean get(String[] row);
	}
	public static class ColumnReaderBooleanMissing extends ColumnReaderBoolean {
		private final boolean missing;
		public ColumnReaderBooleanMissing(boolean missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public boolean get(String[] row) {
			return missing;
		}		
	}
	public static class ColumnReaderBooleanYN extends ColumnReaderBoolean {
		private final boolean missing;
		public ColumnReaderBooleanYN(int rowIndex, boolean missing) {
			super(rowIndex);
			this.missing = missing;
		}
		@Override
		public boolean get(String[] row) {
			String text = row[rowIndex];
			if(text.isEmpty()) {
				return missing;
			}
			if(text.length()!=1) {
				text = text.trim();
				if(text.length()!=1) {
					Logger.warn("boolean not parsed "+text);
					return missing;
				}
			}
			char c = text.toUpperCase().charAt(0);
			if(c=='Y') {
				return true;
			}
			if(c=='N') {
				return false;
			}
			Logger.warn("boolean not parsed "+text);
			return missing;
		}		
	}
	
	public static interface ReaderConstructor<T> {
		T create(int a);
	}
	/**
	 * header names in csv file
	 */
	public String[] names;
	/**
	 * header name -> column position
	 */
	public Map<String, Integer> nameMap;
	public void updateNames(String[] columnNames) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
	
		for(int i=0;i<columnNames.length;i++) {
			if(map.containsKey(columnNames[i])) {
				int nameNumber = 2;
				String name2 = columnNames[i] + nameNumber;
				while(map.containsKey(name2)) {
					nameNumber++;
					name2 = columnNames[i] + nameNumber;
				}
				Logger.warn("dublicate name: '"+columnNames[i] + "' replaced with '" + name2 + "'");
				columnNames[i] = name2;
				map.put(columnNames[i], i);
			} else {
				map.put(columnNames[i], i);
			}
		}
	
		this.names = columnNames;
		this.nameMap = map;
	}
	/**
	 * get column position of one header name
	 * @param name
	 * @return if name not found -1
	 */
	public int getColumnIndex(String name) {
		return getColumnIndex(name, true);
	}
	/**
	 * get column position of one header name
	 * @param name
	 * @return if name not found -1
	 */
	public int getColumnIndex(String name, boolean warn) {
		Integer index = nameMap.get(name);
		if(index==null) {			
			if(warn) {
				Logger.error("name not found in table: "+name);
			}
			return -1;
		}
		return index;
	}
	public boolean containsColumn(String name) {
		return nameMap.containsKey(name);
	}
	public String getName(ColumnReader cr) {
		return names[cr.rowIndex];
	}
	public ColumnReaderString createColumnReader(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderString(columnIndex);
	}
	public ColumnReaderString createColumnReader(String name, String missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderStringMissing(missing);
		}
		return new ColumnReaderString(columnIndex);
	}
	public ColumnReaderFloat createColumnReaderFloat(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderFloat(columnIndex);
	}
	/**
	 * Creates reader of column or producer of value "missing" if columns does not exist.
	 * @param name
	 * @param missing
	 * @return
	 */
	public ColumnReaderFloat createColumnReaderFloat(String name, float missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderFloatMissing(missing);
		}
		return new ColumnReaderFloat(columnIndex);
	}
	public ColumnReaderDouble createColumnReaderDouble(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderDouble(columnIndex);
	}
	public ColumnReaderDouble createColumnReaderDouble(String name, double missing) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return new ColumnReaderDoubleMissing(missing);
		}
		return new ColumnReaderDouble(columnIndex);
	}
	public ColumnReaderInt createColumnReaderInt(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderInt(columnIndex);
	}
	public ColumnReaderBoolean createColumnReaderBooleanYN(String name, boolean missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderBooleanMissing(missing);
		}
		return new ColumnReaderBooleanYN(columnIndex, missing);
	}

	public <T> T getColumnReader(String name, ReaderConstructor<T> readerConstructor) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return readerConstructor.create(columnIndex);
	}
}
