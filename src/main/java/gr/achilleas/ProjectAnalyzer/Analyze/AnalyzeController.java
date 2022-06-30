package gr.achilleas.ProjectAnalyzer.Analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path= "api/analyze")
public class AnalyzeController {
	
	@Autowired
	private AnalyzeService analyzeService;
	
	private String gitURL;
	private File gitProjectsDirectory;
	
	@GetMapping("/service")
	public String analyze(@RequestParam String url) {
		this.gitURL = url;
		String[] urlComponents = url.split("/");
		this.gitProjectsDirectory = new File(System.getProperty("user.dir") + "\\Git Projects");
		this.gitProjectsDirectory.mkdir();
		this.getGitRepo();
		String results = analyzeService.start(System.getProperty("user.dir") + "\\Git Projects\\" + urlComponents[urlComponents.length-1].replaceAll(".git", "")) + " " + url;
		try {
			FileUtils.deleteDirectory(gitProjectsDirectory);
		} catch (IOException e) {
			System.out.println("Failed to delete \"Git Projects\" directory");
		}
		return results;
	}
	
	private void getGitRepo() {
		if ( isWindows() ) {
			try {
			    Process proc = Runtime.getRuntime().exec("cmd /c \"cd \"Git Projects\" && git clone " + gitURL() + " \"");  //to change dir and then clone \"cd temp && git clone " + gitURL() + " \""
			    BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			    String errorLine;
			    while ((errorLine = errorReader.readLine()) != null) {
			    	System.out.println("~ " + errorLine);
			    }
			    BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			    String inputLine;
			    while ((inputLine = inputReader.readLine()) != null) {
			    	System.out.println("! " + inputLine);
			    }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
		    try {
				ProcessBuilder pbuilder1 = new ProcessBuilder("bash", "-c", "cd \"Git Projects\"; git clone "+ gitURL());  //to change dir and then clone "cd tmp; git clone"+gitURL()
			    Process p1 = pbuilder1.start();
			    BufferedReader errorReader = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
			    String errorLine;
			    while ((errorLine = errorReader.readLine()) != null) {
			        System.out.println("~ " + errorLine);
			    }
			    BufferedReader inputReader = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			    String inputLine;
			    while((inputLine = inputReader.readLine()) != null) {
			    	System.out.println("! " + inputLine);
			    }
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	private String gitURL() {
		return this.gitURL;
	}
	
	public boolean isWindows() {
	    return System.getProperty("os.name").toLowerCase().contains("win");
	}
	
}
