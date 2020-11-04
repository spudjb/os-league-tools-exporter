package spudjb.osleague.exporter;

public class ExportException extends Exception
{
	public ExportException(String message) {
		super(message);
	}

	public ExportException(String message, Throwable t) {
		super(message, t);
	}
}
