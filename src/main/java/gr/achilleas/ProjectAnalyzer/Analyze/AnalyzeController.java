package gr.achilleas.ProjectAnalyzer.Analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private String username;
	private String password;
	private Report myReportClass;
	private boolean done;
	private String pomXmlPath;
	
	@GetMapping("/service/{username}/{password}")
	public Report analyze(@RequestParam(value = "url") String url, @PathVariable("username") String username, @PathVariable("password") String password) {
		this.myReportClass = new Report();
		this.gitURL = url;
		this.username = username;
		this.password = password;
		String[] urlComponents = url.split("/");
		this.gitProjectsDirectory = new File(System.getProperty("user.dir") + "\\Git Projects");
		this.gitProjectsDirectory.mkdir();
		String analyzedProjectName = urlComponents[urlComponents.length-1].replaceAll(".git", "");
		this.getGitRepo(analyzedProjectName);
		if(this.findFile(new File(System.getProperty("user.dir") + "\\Git Projects\\" + analyzedProjectName), "pom.xml")) {
			String results = analyzeService.start(this.pomXmlPath, this.myReportClass);
			this.myReportClass.updateMessage(results);
			if(analyzeService.pushOnDocker()) {
				this.dockerCommands(analyzedProjectName);
			}
			else {
				System.out.println("Project doesn't fill the requirements");
			}
			try {
				FileUtils.deleteDirectory(gitProjectsDirectory);
			} catch (IOException e) {
				System.out.println("Failed to delete \"Git Projects\" directory");
			}
		}
		else
			this.myReportClass.updateMessage("pom.xml file doesn't exists");
		
		
		return this.myReportClass;
	}
	
	//Searching for pom.xml inside directory "starting path"
	private boolean findFile(File startingPath, String name) {
		boolean found = false;
		
		File file = startingPath;
		File[] list = file.listFiles();
        if(list!=null)
        for (File fil : list)
        {
            if (fil.isDirectory())
            {
            	found = this.findFile(fil, name);
            }
            else if (name.equalsIgnoreCase(fil.getName()))
            {
                found = true;
                this.pomXmlPath = fil.getParentFile().getPath();
            }
            if(found) break;
        }
		
		return found;
	}
	
	private void dockerCommands(String analyzedProjectName) {
		if(isWindows()) {
			try {
				String imageName = this.username + "/" + analyzedProjectName.toLowerCase();
				Process proc = Runtime.getRuntime().exec("cmd /c \"docker build -t " + imageName + " . && "	//docker build
						+ "docker login -u " + this.username + " -p " + this.password + " &&"				//docker login
						+ "docker push " + imageName + " &&"												//docker push
						+ "docker logout\"");																//docker logout
				if(this.printProcessRun(proc, "Login Succeeded"))
					this.myReportClass.updateMessage("Login Succeeded");
				if(this.done) {
					this.myReportClass.setSuccess(true);
					this.myReportClass.setUrl("docker.io/" + imageName);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				String imageName = this.username + "/" + analyzedProjectName.toLowerCase();
				ProcessBuilder pbuilder1 = new ProcessBuilder("bash", "-c", "docker build -t " + imageName + " .; "	//docker build
						+ "docker login -u " + this.username + " -p " + this.password + "; "						//docker login
						+ "docker push " + imageName + "; "															//docker push
						+ "docker logout");																			//docker logout
			    Process p1 = pbuilder1.start();
				if(this.printProcessRun(p1, "Login Succeeded"))
					this.myReportClass.updateMessage("Login Succeeded");
				if(this.done) {
					this.myReportClass.setSuccess(true);
					this.myReportClass.setUrl("docker.io/" + imageName);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
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
			    if(this.printProcessRun(proc, "[INFO] BUILD FAILURE")) {
			    	this.myReportClass.updateMessage("Build failure, trying without tests");
			    	proc = Runtime.getRuntime().exec("cmd /c \"cd \"Git Projects\" && " 	//to change dir
				    		+ "cd \"" + analyzedProjectName + "\" && "						//change dir
				    		+ "mvn clean install -DskipTests\"");							//build maven project skipping tests
			    	//If project's build fails even without tests
			    	if(this.printProcessRun(proc, "[INFO] BUILD FAILURE")) {
			    		this.myReportClass.updateMessage("Build failed!");
			    	}
			    }
			    else
			    	this.myReportClass.updateMessage("Build success");
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
			    if(this.printProcessRun(p1, "[INFO] BUILD FAILURE")) {
			    	this.myReportClass.updateMessage("Build failure, trying without tests");
			    	pbuilder1 = new ProcessBuilder("bash", "-c", "cd \"Git Projects\"; "			//to change dir
							+ "cd \"" + analyzedProjectName + "\"; "								//change dir
							+ "mvn clean install -DskipTests");													//build maven project skipping tests
			    	p1 = pbuilder1.start();
			    	//If project's build fails even without tests
			    	if(this.printProcessRun(p1, "[INFO] BUILD FAILURE")) {
			    		this.myReportClass.updateMessage("Build failed!");
			    	}
			    }
			    else
			    	this.myReportClass.updateMessage("Build success");
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	//Printing process input and error streams
	//Returning true if input stream contains textContained 
	private boolean printProcessRun(Process proc, String textContained) throws IOException {
	    this.done = false;
		boolean errorFlag = false;
	    BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	    String inputLine;
	    while ((inputLine = inputReader.readLine()) != null) {
	    	if(!errorFlag && inputLine.contains(textContained))
	    		errorFlag = true;
	    	System.out.println(inputLine);
	    }
	    BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	    String errorLine;
	    while ((errorLine = errorReader.readLine()) != null) {
	    	System.out.println(errorLine);
	    	if(!this.done)
	    		this.done = errorLine.matches(".*naming.*done.*");
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
