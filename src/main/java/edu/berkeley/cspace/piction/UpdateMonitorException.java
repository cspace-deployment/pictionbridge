package edu.berkeley.cspace.piction;

public class UpdateMonitorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UpdateMonitorException() {
		super();
	}

	public UpdateMonitorException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpdateMonitorException(String message) {
		super(message);
	}

	public UpdateMonitorException(Throwable cause) {
		super(cause);
	}
}
