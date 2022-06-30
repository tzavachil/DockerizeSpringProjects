package gr.achilleas.ProjectAnalyzer.Analyze;

public class Import {
		
	private boolean success;
	private String message;
	private String url;
	
	public Import() {
		this.success = false;
		this.message = "";
		this.url = "";
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
	
	public void print() {
		System.out.println("Success = " + this.success);
		System.out.println("Message = " + this.message);
		System.out.println("Url = " + this.url);
	}
	
}
