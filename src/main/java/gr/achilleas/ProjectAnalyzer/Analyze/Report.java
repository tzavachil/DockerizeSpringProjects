package gr.achilleas.ProjectAnalyzer.Analyze;

public class Report {
		
	private boolean success;
	private String message;
	private String url;
	private String methods;
	
	public Report() {
		this.success = false;
		this.message = "";
		this.url = "";
		this.methods = "";
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public void updateMessage(String message) {
		this.message += message + ", ";
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getMethods() {
		return this.methods;
	}
	
	public void updateMethods(String methods) {
		this.methods = methods;
	}
	
	public void print() {
		System.out.println("Success = " + this.success);
		System.out.println("Message = " + this.message);
		System.out.println("Url = " + this.url);
		System.out.println("Methdos = " + this.methods);
	}
	
}
