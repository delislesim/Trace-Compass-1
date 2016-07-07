package org.eclipse.tracecompass.tmf.attributetree.core.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AttributeTree {
	
	private static AttributeTree INSTANCE = null;
	
	//private static AbstractAttributeNode invisibleRoot = null;
	private static Map<File, AbstractAttributeNode> fLoadedTrees = new HashMap<>(); // File = Attribute tree file, AbstractAttributeNode = Invisible root
	private static Stack<Pair<AbstractAttributeNode, String>> fQueryNodeStack = new Stack<>();
	//private static File currentFile;
	
	private AttributeTree() {
	}
 
	public static AttributeTree getInstance()
	{
		if (INSTANCE == null) {
			INSTANCE = new AttributeTree();
		}		
		return INSTANCE;
	}
	
	public AbstractAttributeNode getNodeFromPath(File attributeTreeFile, @NonNull String path) {
		checkLoadedTrees(attributeTreeFile);
		AbstractAttributeNode currentNode = fLoadedTrees.get(attributeTreeFile);
		for(String nodeName : splitPath(path)) {
			currentNode = searchNode(currentNode, nodeName);
		}
		return currentNode;
	}
	
	public AbstractAttributeNode getRoot(File attributeTreeFile) {
		checkLoadedTrees(attributeTreeFile);
		return fLoadedTrees.get(attributeTreeFile);
	}
	
	public boolean saveAttributeTree(File attributeTreeFile) {
		checkLoadedTrees(attributeTreeFile); 
		// TODO If attributeTreeFile doesn't exist it crashes
		Document xmlFile = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			xmlFile = dBuilder.newDocument();
		} catch (ParserConfigurationException exception) {
			return false;
		}
		
		Element rootElement = fLoadedTrees.get(attributeTreeFile).createElement(fLoadedTrees.get(attributeTreeFile), xmlFile);
		xmlFile.appendChild(rootElement);
		try {
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(xmlFile);
			
			StreamResult savedFileResult = new StreamResult(attributeTreeFile);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, savedFileResult);
		} catch (TransformerException exception) {
			return false;
		}
		return true;
	}
	
	public boolean createNewAttributeTree(File newAttributeTreeFile) {
		try {
			newAttributeTreeFile.createNewFile();
		} catch (IOException e) {
			return false;
		}
		
		fLoadedTrees.put(newAttributeTreeFile, new ConstantAttributeNode(null, "root"));
		saveAttributeTree(newAttributeTreeFile);
		
		return true;		
	}
	
	/**
	 * Check if the tree associated with the file is loaded. If not load it.
	 * 
	 * @param treeFile File to check if the tree is loaded
	 */
	private void checkLoadedTrees(File attributeTreeFile) {
		if(!fLoadedTrees.containsKey(attributeTreeFile)) {
			loadXmlTree(attributeTreeFile);
		}
	}
	
	private static void loadXmlTree(File xmlFile) {
		Document xmlTree = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			xmlTree = dBuilder.parse(xmlFile);
			xmlTree.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException | IOException e) {
		}
		
//		if(xmlTree == null) {
//			invisibleRoot = new ConstantAttributeNode(null, "root");
//			return;
//		}
		
		NodeList nodeList = xmlTree.getElementsByTagName("root");
		Node xmlRootNode = nodeList.item(0); // Only one root
		AbstractAttributeNode root = new ConstantAttributeNode(null, ((Element)xmlRootNode).getAttribute("name"));
		fLoadedTrees.put(xmlFile, root);
		getTreeFromXml(xmlRootNode, fLoadedTrees.get(xmlFile));
		addQueryToTree(fLoadedTrees.get(xmlFile));
	}
	
	private static void getTreeFromXml(Node parentNode, AbstractAttributeNode parentAttribute) {
		NodeList childrenNodes = parentNode.getChildNodes();
		for(int i = 0; i < childrenNodes.getLength(); i++) {
			Node childNode = childrenNodes.item(i);
			if(childNode.getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			Element childElement = (Element) childNode;
			AbstractAttributeNode parent = null;
			if (childElement.getAttribute("type").equals(ConstantAttributeNode.class.getSimpleName())) {
				parent = new ConstantAttributeNode(parentAttribute, childElement.getAttribute("name"));
			} else if (childElement.getAttribute("type").equals(VariableAttributeNode.class.getSimpleName())) {
				parent = new VariableAttributeNode(parentAttribute, childElement.getAttribute("name"));
				
				String xpathQuery = childElement.getAttribute("query");
				if(!xpathQuery.equals("")) {
					fQueryNodeStack.push(new Pair<AbstractAttributeNode, String>(parent, xpathQuery));
				}
			} else if (childElement.getAttribute("type").equals(AttributeValueNode.class.getSimpleName())) {
				parent = new AttributeValueNode(parentAttribute, childElement.getAttribute("name"));
			}
			getTreeFromXml(childNode, parent);
		}
	}
	
	private static void addQueryToTree(AbstractAttributeNode root) {
		while (!fQueryNodeStack.empty()) {
			Pair<AbstractAttributeNode, String> queryPair = fQueryNodeStack.pop();
			VariableAttributeNode node = (VariableAttributeNode) queryPair.getFirst();
			String path = queryPair.getSecond();
			
			AbstractAttributeNode currentNode = root;
			for(String nodeName : splitPath(path)) {
				currentNode = searchNode(currentNode, nodeName);
			}
			
			node.setIsQuery(true);
			node.setQueryPath(new AttributeTreePath(currentNode));
		}
	}
	
	private static AbstractAttributeNode searchNode(AbstractAttributeNode parent, String nodeName) {
		if(!parent.hasChildren()) {
			return parent;
		}
		
		for(AbstractAttributeNode child : parent.getChildren()) {
			if(child.getName().replace(" ", "").equals(nodeName)) {
				return child;
			}
		}
		
		return null;
	}
	
	private static List<String> splitPath(@NonNull String path) {
		String[] splitedPath = path.split("/");
		List<String> nodeInPath = new ArrayList<>();
		for (int i = 2; i < splitedPath.length; i++) { // Skip root + empty string
			nodeInPath.add(splitedPath[i]);
		}
		
		return nodeInPath;
	}
}
