package gr.achilleas.ProjectAnalyzer.Analyze;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

	public String print() {
		
		String result = "";
		String path = "D:\\Programming\\Eclipse 2021 Projects\\smartclide-TD-Principal-main - Copy";
		
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
				if(dockerFile.exists() && !dockerFile.isDirectory()) {
					//System.out.println("Dockerfile found");
					result += "Dockerfile found\n";
				}
				else {
					//System.out.println("Dockerfile doesn't exist");
					result += "Dockerfile doesn't exist\n";
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
		return result;
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
				 //hasRestController = this.hasRestController(tempFile);
				 hasRestController = this.hasTextAndRegex(tempFile,"@RestController","^\\s*(public class " + tempFile.getName().replace(".java","") + ")\\s*\\{");
				 hasPostMapping = this.hasTextAndRegex(tempFile, "@PostMapping", "^\\s*(public )(.*)");
				 hasGetMapping = this.hasTextAndRegex(tempFile, "@GetMapping", "^\\s*(public )(.*)");
				 System.out.println(hasRestController);
				 System.out.println(hasPostMapping);
				 System.out.println(hasGetMapping);
				 if (hasRestController && (hasPostMapping || hasGetMapping)) break;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return hasRestController && (hasPostMapping || hasGetMapping);
	}
	
	private boolean hasTextAndRegex(File file, String text, String regex) {
		boolean flag = false;
		
		boolean foundRegexLine = false;
		
		try(Scanner scanner = new Scanner(file)) {

		    //now read the file line by line...
			String currLine;
		    while (scanner.hasNextLine()) {
		        currLine = scanner.nextLine();
		        if(flag) {
		        	foundRegexLine = currLine.toLowerCase().matches(regex);
		        	if(foundRegexLine) break;
		        }
		    	if(currLine.contains(text) && currLine.matches("^\\s*" + text) && !flag) {	
		    		flag = true;
		    	}
		    		
		    }
		} catch(FileNotFoundException e) { 
		    e.printStackTrace();
		}
		
		return flag && foundRegexLine;
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
