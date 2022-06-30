package gr.achilleas.ProjectAnalyzer.Analyze;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Service
public class AnalyzeService {

	private ArrayList<String> postMappingMethodsList = new ArrayList<>();
	private ArrayList<String> getMappingMethodsList = new ArrayList<>();
	private String path;
	
	public String start(String projectPath) {
		
		String result = "";
		this.path = projectPath;
		System.out.println(path);
		
		File pomFile = new File(path + "\\pom.xml");
		File dockerFile = new File(path + "\\Dockerfile");
		
		// Checks if there is the pom.xml file in the project's root directory
		if(pomFile.exists() && !pomFile.isDirectory()) {
			
			//System.out.println("pom.xml file found");
			result += "pom.xml file found\n";
			
			// Checks if pom.xml file has dependencies of a Spring Project 
			boolean temp = this.checkDependencies(pomFile);
			if(temp) {
				//System.out.println("pom.xml has Spring's dependencies");
				result += "pom.xml has Spring's dependencies\n";
				if(this.isRestApi(path)) { 
					//System.out.println("Spring project is a Rest API");
					result += "Spring project is a Rest API\n";
				}
				// Checks if there is the Docker file in the project's root directory
				if(false){//dockerFile.exists() && !dockerFile.isDirectory()) {
					//System.out.println("Dockerfile found");
					result += "Dockerfile found\n";
				}
				else {
					//System.out.println("Dockerfile doesn't exist");
					result += "Dockerfile doesn't exist\n";
					String javaVersion = this.findNode(pomFile,"java.version", "properties");
					String name = this.findNode(pomFile, "name", "project");
					String version = this.findNode(pomFile, "version", "project");
					this.createDockerFile(javaVersion, name, version);
				}
			}
			else {
				//System.out.println("pom.xml hasn't Spring's dependencies");
				result += "pom.xml hasn't Spring's dependencies\n";
			}		
		}
		else {
			//System.out.println("pom.xml file doesn't exist");
			result += "pom.xml file doesn't exist\n";
		}
		
		System.out.println(result);
		System.out.println("Post Mapping Methods List");
		this.printPostList();
		System.out.println("Get Mapping Methods List");
		this.printGetList();
		//Emptying Lists
		this.postMappingMethodsList.clear();
		this.getMappingMethodsList.clear();
		return result;
	}
	
	private void createDockerFile(String javaVersion, String name, String version) {
		String dockerFileText = "";
		
		switch(javaVersion) {
			case "1.8":
				dockerFileText += "FROM openjdk:8-jdk-alpine\n";
				break;
			case "11":
				dockerFileText += "FROM adoptopenjdk:11-jre-hotspot\n";
				break;
			default :
				System.exit(0);
				break;
		}
		dockerFileText += "ADD target/" + name + "-" + version + ".jar app.jar\n";
		Path jarFolderPath = Paths.get(path + "\\jars");
		if(Files.exists(jarFolderPath)) {
			try {
				List<String> jars = this.findFiles(jarFolderPath, "jar");
				for(String s : jars) {
					String[] tempList = s.split(Pattern.quote("\\"));
					dockerFileText += "ADD jars/" + tempList[tempList.length-1] + " " + tempList[tempList.length-1] + "\n";
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		dockerFileText += "ENDPOINT [\"java\", \"-jar\", \"/app.jar\"]";
		
		System.out.println("DockerFile Text : \n" + dockerFileText);
		
		this.storeDockerFile(dockerFileText, name);
	}
	
	private void storeDockerFile(String text, String analyzedProjectName) {
		String rootFolder = System.getProperty("user.dir");
		
		// Create folder to store DockerFiles
		File dockerFilesFolder = new File(rootFolder + "/DockerFiles");
		if(!Files.exists(Paths.get(rootFolder + "/DockerFiles"))) {
			dockerFilesFolder.mkdir();
		}
		// Create subfolder specifically for this dockerfile
		File dockerFileSubfolder = new File(rootFolder + "/DockerFiles/" + analyzedProjectName);
		if(!Files.exists(Paths.get(rootFolder + "/DockerFiles/" + analyzedProjectName))){
			dockerFileSubfolder.mkdir();
		}
		
		// Create and store the dockerfile
		try {
			File dockerFile = new File(rootFolder + "/DockerFiles/" + analyzedProjectName + "/Dockerfile");
			if(dockerFile.createNewFile()) {
				FileWriter writer = new FileWriter(rootFolder + "/DockerFiles/" + analyzedProjectName + "/Dockerfile");
				writer.write(text);
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private String findNode(File pomFile, String nodeId, String parentId) {
		String nodeContent = null;
		
		try {
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document doc = dbBuilder.parse(pomFile);
			doc.getDocumentElement().normalize();
			NodeList dep = doc.getElementsByTagName(nodeId);
			for(int i=0; i<dep.getLength(); i++) {
				if(dep.item(i).getParentNode().getNodeName().equals(parentId)) {
					nodeContent = dep.item(i).getTextContent();
				}
			}

		} catch (ParserConfigurationException|SAXException|IOException e) {
			e.printStackTrace();
		} 
		
		return nodeContent;
	}
	
	private boolean isRestApi(String path) {
		boolean hasRestController = false;
		boolean hasPostMapping = false;
		boolean hasGetMapping = false;
 		
		Path rootPath = Paths.get(path + "\\src");
		try {
			List<String> files = this.findFiles(rootPath, "java");
			File tempFile;
			for(String filePath : files) {
				 tempFile = new File(filePath);
				 hasRestController = this.hasTextAndRegex(tempFile,"@RestController","^\\s*(public class " + tempFile.getName().replace(".java","") + ")\\s*\\{", null);
				 hasPostMapping = this.hasTextAndRegex(tempFile, "@PostMapping", "^\\s*[^//](public )?(.*)\\s(.*)\\((.*)", this.postMappingMethodsList);
				 hasGetMapping = this.hasTextAndRegex(tempFile, "@GetMapping", "^\\s*[^//](public )?(.*)\\s(.*)\\((.*)", this.getMappingMethodsList);
				 if (hasRestController && (hasPostMapping || hasGetMapping)) break;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return hasRestController && (hasPostMapping || hasGetMapping);
	}
	
	/* Search in a file if there is a certain text and then if there is a line
	 * that match the regular expression. If the ArrayList list isn't null it stores methods's
	 * names of type text into list */
	private boolean hasTextAndRegex(File file, String text, String regex, ArrayList<String> list) {
		boolean returnable = false;
		boolean flag = false;
		boolean foundRegexLine = false;
		
		try(Scanner scanner = new Scanner(file)) {

		    //now read the file line by line...
			String currLine;
		    while (scanner.hasNextLine()) {
		        currLine = scanner.nextLine();
		        if(flag) {
		        	foundRegexLine = currLine.toLowerCase().matches(regex);
		        	if(foundRegexLine) {
		        		returnable = true;
		        		if(list != null) {
		        			if(!currLine.matches("(.*)\\ *\\((.*)\\)(.*)")) {
		        				String line = currLine;
		        				while(!currLine.matches("(.*)\\)") && !currLine.matches("(.*)\\)\\s*\\{")) {
		        					currLine = scanner.nextLine();
		        					line += currLine.replaceAll("\\t", "");
		        				}
		        				currLine = line;		        				
		        			}
		        			this.storeMethodsNames(currLine, list);
			        		foundRegexLine = false;
			        		flag = false;		        				
		        		}
		        		else break;
		        	}
		        }
		    	if(currLine.contains(text) && currLine.matches("^\\s*" + text + "(.*)")) {	
		    		flag = true;
		    	}
		    }
		} catch(FileNotFoundException e) { 
		    e.printStackTrace();
		}
		
		return returnable;
	}
	
	private void storeMethodsNames(String line, ArrayList<String> list) {
		Pattern pattern = Pattern.compile("[^ ]*\\ *\\((.*)\\)");
		Matcher matcher = pattern.matcher(line);
		if(matcher.find()) list.add(matcher.group());
	}
	
	private void printPostList() {
		for(String s : this.postMappingMethodsList)
			System.out.println(s);
	}
	
	private void printGetList() {
		for(String s : this.getMappingMethodsList)
			System.out.println(s);
	}
	
	//Find files with a specified file extension
	private List<String> findFiles(Path path, String fileExtension) throws IOException{ 
		
		List<String> result;
		
		try(Stream<Path> walk = Files.walk(path)){
			result = walk.filter(p -> !Files.isDirectory(p))
					.map(p -> p.toString().toLowerCase())
					.filter(f -> f.endsWith(fileExtension))
					.collect(Collectors.toList());
		}
		
		return result;
	}
	
	private boolean checkDependencies(File pomFile) {
		
		boolean flag = false;
		
		try {
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document doc = dbBuilder.parse(pomFile);
			doc.getDocumentElement().normalize();
			NodeList dep = doc.getElementsByTagName("dependency");
			
			ArrayList<String> groupIds = new ArrayList<>();
			
			for(int i=0; i<dep.getLength(); i++) {
				
				Node tempNode = dep.item(i);
				if(tempNode.getNodeType() == Node.ELEMENT_NODE) {
				
					Element depGroupIdElement = (Element) tempNode;
					groupIds.add(depGroupIdElement.getElementsByTagName("groupId").item(0).getTextContent());
					
				}
				
			}
			
			flag = groupIds.contains("org.springframework.boot");

		} catch (ParserConfigurationException|SAXException|IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return flag;
	}
	
}
