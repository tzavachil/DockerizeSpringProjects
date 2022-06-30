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
		String analyzedProjectName = urlComponents[urlComponents.length-1].replaceAll(".git", "");
		this.getGitRepo(analyzedProjectName);
		String results = analyzeService.start(System.getProperty("user.dir") + "\\Git Projects\\" + analyzedProjectName) + " " + url;
		this.dockerCommands(analyzedProjectName);
		try {
			FileUtils.deleteDirectory(gitProjectsDirectory);
		} catch (IOException e) {
			System.out.println("Failed to delete \"Git Projects\" directory");
		}
		return results;
	}
	
	private void dockerCommands(String analyzedProjectName) {
		if(isWindows()) {
			try {
				System.out.println("Give Username:");
				String username = this.testTakeInput();
				System.out.println("Give Password:");
				String password = this.testTakeInput();
				String imageName = username + "/" + analyzedProjectName.toLowerCase();
				Process proc = Runtime.getRuntime().exec("cmd /c \"docker build -t " + imageName + " . && "	//docker build
						+ "docker login -u " + username + " -p " + password + " &&"	//docker login
						+ "docker push " + imageName + " &&"
						+ "docker logout\"");	//docker push
				this.printProcessRun(proc);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				System.out.println("Give Username:");
				String username = this.testTakeInput();
				System.out.println("Give Password:");
				String password = this.testTakeInput();
				String imageName = username + "/" + analyzedProjectName.toLowerCase();
				ProcessBuilder pbuilder1 = new ProcessBuilder("bash", "-c", "docker build -t " + imageName + " .; "	//docker build
						+ "docker login -u " + username + " -p " + password + "; "	//docker login
						+ "docker push " + imageName + "; "
						+ "docker logout");	//docker push													//build maven project
			    Process p1 = pbuilder1.start();
				this.printProcessRun(p1);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private String testTakeInput() {
		
        
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String s = br.readLine();
	        
	        return s;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return null;
	}
	
	private void getGitRepo(String analyzedProjectName) {
		if (isWindows()) {
			try {
				//to change dir and then clone 
			    Process proc = Runtime.getRuntime().exec("cmd /c \"cd \"Git Projects\" && " //to change dir
			    		+ "git clone " + gitURL() + " && "									//clone git repository from url
			    		+ "cd \"" + analyzedProjectName + "\" && "							//change dir
			    		+ "mvn clean install\"");											//build maven project
			    //If project's build fails at the tests
			    if(this.printProcessRun(proc)) {
			    	System.out.println("[INFO] BUILD FAILURE\n Trying without the tests");
			    	proc = Runtime.getRuntime().exec("cmd /c \"cd \"Git Projects\" && " 	//to change dir
				    		+ "cd \"" + analyzedProjectName + "\" && "						//change dir
				    		+ "mvn clean install -DskipTests\"");							//build maven project skipping tests
			    	this.printProcessRun(proc);
			    }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
		    try {
				ProcessBuilder pbuilder1 = new ProcessBuilder("bash", "-c", "cd \"Git Projects\"; "	//to change dir
						+ "git clone "+ gitURL() + "; "												//clone git repository form url
						+ "cd \"" + analyzedProjectName + "\"; "									//change dir
						+ "mvn clean install");														//build maven project
			    Process p1 = pbuilder1.start();
			    //If project's build fails at the tests
			    if(this.printProcessRun(p1)) {
			    	System.out.println("[INFO] BUILD FAILURE\n Trying without the tests");
			    	pbuilder1 = new ProcessBuilder("bash", "-c", "cd \"Git Projects\"; "			//to change dir
							+ "cd \"" + analyzedProjectName + "\"; "								//change dir
							+ "mvn clean install -DskipTests");													//build maven project skipping tests
			    	p1 = pbuilder1.start();
			    	this.printProcessRun(p1);
			    }
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	private boolean printProcessRun(Process proc) throws IOException {
	    boolean errorFlag = false;
	    BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	    String inputLine;
	    while ((inputLine = inputReader.readLine()) != null) {
	    	if(!errorFlag && inputLine.contains("[INFO] BUILD FAILURE"))
	    		errorFlag = true;
	    	System.out.println(inputLine);
	    }
	    BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	    String errorLine;
	    while ((errorLine = errorReader.readLine()) != null) {
	    	System.out.println(errorLine);
	    }
	    
	    return errorFlag;
	}
	
	private String gitURL() {
		return this.gitURL;
	}
	
	public boolean isWindows() {
	    return System.getProperty("os.name").toLowerCase().contains("win");
	}
	
}
