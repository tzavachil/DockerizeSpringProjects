package gr.achilleas.ProjectAnalyzer.Analyze;

public class MethodStructure {
	
	private String name;
	private String path;
	
	public MethodStructure(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}
	
	public String printData() {
		String text = this.name + ":" + this.path;
		System.out.println(text);
		return text;
	}
	
}
