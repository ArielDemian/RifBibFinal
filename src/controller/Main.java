package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import model.MyNamespaceContext;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.atlas.logging.Log;
import org.w3c.dom.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import model.*;
import patternRecognizers.*;

public class Main {
	// Maximum number of statements allowed in a model before it is dumped to
	// file
	public static final int maxStatementsInModel = 550000;

	public static void main(String[] args) {
		// Check for the right arguments passed
		if (args.length != 5) {
			System.out.println("ERROR: The application requires exactly 5 parameters: [1] path of namespaces file, [2] path of input folder containing all the XML files and subfolders to be processed, [3] path of output file that will be used to write the RDF statements (strongly advised that the folder is to be empty), [4] 'jena-log4j.properties' file for log configuration, [5] base URL for the declared resources.");
			System.exit(0);
		}
		File namespacesFile = new File(args[0]);
		if (!namespacesFile.exists() || !namespacesFile.isFile()) {
			System.out.println("ERROR: The first parameter [1] must be the location in the file system of the XML file containing the necessary namespaces.");
			System.exit(0);
		}
		File inputFolder = new File(args[1]);
		if (!inputFolder.exists() || !inputFolder.isDirectory()) {
			System.out.println("ERROR: The second parameter [2] must be a folder containing the XML files to be processed.");
			System.exit(0);
		}
		File log4j = new File(args[3]);
		if (!log4j.exists() || !log4j.isFile()) {
			System.out.println("ERROR: The forth parameter [4] must be the 'jena-log4j.properties' file for log configuration.");
			System.exit(0);
		}
		String[] schemes = { "http", "https", "ftp" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		if (!urlValidator.isValid(args[4])) {
			System.out.println("ERRORE: The fifth parameter [5] must be a valid URL for the declared resources.");
			System.exit(0);
		}
		// Setup of the required resources
		Log.setLog4j(args[3]);
		URIManager.setBase(args[4]);
		XPath xPath = XPathFactory.newInstance().newXPath();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document document = null;
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = dbf.newDocumentBuilder();
			document = documentBuilder.parse(namespacesFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<PatternRecognizer> prs = new ArrayList<PatternRecognizer>();
		Main.addPatternRecognizers(prs);
		MyNamespaceContext nsc = new MyNamespaceContext(document, false);
		if (!nsc.hasAllRequiredNamespaces()) {
			System.out.println("ERROR: The namespaces file \"" + namespacesFile.getName() + "\" does not contain all the required namespaces.");
			System.out.println("The required namespaces are: " + MyNamespaceContext.getNecessaryNamespaces());
			System.exit(0);
		}
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(nsc.getMap());
		setResources(model);
		dbf.setNamespaceAware(false);
		try {
			documentBuilder = dbf.newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int fileNameCount = 1, fileCount = 0, bibRefCount = 0, bibRefProcessed = 0;
		// List all files in folder and subfolder
		List<File> files = new LinkedList<File>();
		listFiles(inputFolder, files);
		System.out.println("Processing of files commencing!");
		Map<String, Integer> bibRefsAnalysedByPatternRecognizers = new HashMap<String, Integer>();
		for (PatternRecognizer pr : prs) {
			bibRefsAnalysedByPatternRecognizers.put(pr.getClass().getSimpleName(), 0);
		}
		for (File fEntry : files) {
			boolean hasBibRefs = false;
			if (fEntry.length() > 0 && fEntry.getPath().toLowerCase().endsWith(".xml")) {
				fileCount++;
				NodeList result = null;
				Resource referenceList = null;
				String doi = null;
				Model tempModel = ModelFactory.createDefaultModel();
				try {
					document = documentBuilder.parse(fEntry);
					MyNamespaceContext mynsc = new MyNamespaceContext(document, false);
					xPath.setNamespaceContext(mynsc);
					// Find doi of document containing the bib. refs.
					XPathExpression xpe = xPath.compile("/full-text-retrieval-response/coredata/*[name()='prism:doi']");
					NodeList doiNodes = (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
					if (doiNodes == null || doiNodes.getLength() == 0) {
						System.out.println("ERROR: There was an error while retrieving the DOI for the file " + fEntry.getName() + ". It will be skipped.");
						continue;
					} else
						doi = doiNodes.item(0).getTextContent().trim();
					// Add all the statements related to the document to the Jena
					// model
					Resource r = tempModel.createResource(model.getNsPrefixURI("doi") + doi, frbrWork);
					Resource expression = tempModel.createResource(model.getNsPrefixURI("doi") + doi + "#Expression");
					r.addProperty(frbrRealization, expression);
					Resource bibliography = tempModel.createResource(r.getURI() + "#Bibliography", docoBibliography);
					bibliography.addProperty(frbrPartOf, expression);
					referenceList = tempModel.createResource(r.getURI() + "#ReferenceList", biroReferenceList);
					referenceList.addProperty(frbrPartOf, bibliography);
					xpe = xPath.compile("/full-text-retrieval-response/coredata/*[name()='dc:title']");
					NodeList titleNodes = (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
					String title = null;
					if (titleNodes == null || titleNodes.getLength() == 0) {
						System.out.println("ERROR: There was an error while retrieving the title of the document " + fEntry.getPath() + ". The document will have no title.");
						continue;
					} else {
						title = titleNodes.item(0).getTextContent().trim();
						expression.addLiteral(DC.title, title);
					}
					// Find all the bib. refs. in the document
					xpe = xPath.compile("//*[name()='ce:bib-reference']");
					result = (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
					if (result == null || result.getLength() == 0)
						continue;
					if (!verifyUniqueIDInsideFile(result)) {
						System.out.println("ERROR: The file " + fEntry.getPath() + " has duplicate ids for the bibliographic references or one of the ids is absent. It will be skipped.");
						continue;
					}
					bibRefCount += result.getLength();
					// The single bib. ref. is sent to each PatternRecognizer in the
					// order
					// they were added to the list
					for (int i = 0; i < result.getLength(); i++) {
						for (PatternRecognizer pr : prs) {
							try {
								if (pr.getData(result.item(i), referenceList, doi, nsc)) {
									String name = pr.getClass().getSimpleName();
									bibRefsAnalysedByPatternRecognizers.put(name, bibRefsAnalysedByPatternRecognizers.get(name) + 1);
									bibRefProcessed++;
									hasBibRefs = true;
									break;
								}
							} catch (Exception e) {
								System.out.println("ERROR: There was an error while processing a bib. ref. in the file " + fEntry.getPath() + ". It will be skipped.");
								e.printStackTrace();
								continue;
							}
						}
					}
				} catch (Exception e) {
					// If there is a grave error while processing the file, it is
					// simply
					// skipped
					System.out.println("ERROR: There was an error while processing the file " + fEntry.getPath() + ". It will be skipped.");
					e.printStackTrace();
					continue;
				}
				if (hasBibRefs)
					model.add(tempModel);
			}
			// If there are more statements in the model than the constant value
			// then
			// the model is written to a file and all statements are removed
			if (model.size() > maxStatementsInModel) {
				writeFile(args[2], model, fileNameCount);
				fileNameCount++;
				System.out.println(model.size() + " statements written to file.");
				model.removeAll();
				System.gc();
			}
		}
		writeFile(args[2], model, fileNameCount);
		System.out.println("Files processed: " + fileCount);
		System.out.println("Bib. ref. found: " + bibRefCount);
		System.out.println("Bib. ref. processed: " + bibRefProcessed);
		for (Map.Entry<String, Integer> e : bibRefsAnalysedByPatternRecognizers.entrySet()) {
			System.out.println("Bib. refs. analysed by " + e.getKey() + ": " + e.getValue());
		}
	}

	// Writes Jena model statements to file
	public static void writeFile(String outputFilePath, Model m, int fileNameCount) {
		FileOutputStream fop = null;
		String output = null;
		File a = new File(outputFilePath);
		String outputFileName = a.getName();
		if (outputFileName.contains(".")) {
			String[] fileNameSplit = outputFileName.split("\\.");
			output = "";
			int l = fileNameSplit.length;
			for (int i = 0; i < l; i++) {
				output += fileNameSplit[i] + (i == l - 2 ? fileNameCount : "") + (i == l - 1 ? "" : ".");
			}
		} else {
			output = outputFileName + fileNameCount;
		}
		output = a.getParent() + File.separatorChar + output;
		File outputFile = new File(output);
		try {
			if (!outputFile.exists()) {
				if (!outputFile.createNewFile())
					throw new IOException();
			}
			fop = new FileOutputStream(outputFile, false);
			m.write(fop, "TURTLE");
			fop.close();
		} catch (Exception e) {
			System.out.println("ERROR: There was an error in writing or creating the file " + outputFile.getPath());
			e.printStackTrace();
			System.exit(0);
		}
	}

	// Verifies if every single bib. ref. has a unique ID among all other bib.
	// refs. inside the file
	public static boolean verifyUniqueIDInsideFile(NodeList nl) {
		if (nl == null || nl.getLength() == 0)
			return false;
		List<String> ls = new LinkedList<String>();
		for (int i = 0; i < nl.getLength(); i++) {
			for (String s : ls) {
				if (nl.item(i).getAttributes().getNamedItem("id") == null || nl.item(i).getAttributes().getNamedItem("id").getNodeValue().equals(""))
					return false;
				if (s.equals(nl.item(i).getAttributes().getNamedItem("id").getNodeValue())) {
					return false;
				}
			}
			ls.add(nl.item(i).getAttributes().getNamedItem("id").getNodeValue());
		}
		return true;
	}

	// Fills the list with all the files in the folder and all the subfolders
	public static void listFiles(File dir, List<File> files) {
		if (dir == null || !dir.isDirectory()) {
			return;
		}
		for (File f : dir.listFiles()) {
			if (f.isFile())
				files.add(f);
			else if (f.isDirectory())
				listFiles(f, files);
		}
	}

	// Adds all the needed PatternRecognizers to the list, the order in which
	// they
	// are added is the order in which they are invoked
	public static void addPatternRecognizers(List<PatternRecognizer> prs) {
		PatternRecognizer pr = null;
		pr = new StructuredBibRefRecognizer();
		prs.add(pr);
		pr = new ThesisPatternRecognizer();
		prs.add(pr);
		pr = new ConferenceBibRefRecognizer();
		prs.add(pr);
		pr = new TitleAndAuthorsPatternRecognizer();
		prs.add(pr);
		pr = new URLPatternRecognizer();
		prs.add(pr);
		pr = new NoPatternRecognizer();
		prs.add(pr);
	}

	// Sets all the required Jena Properties and Resources for the model
	// statements
	public static void setResources(Model m) {
		frbrWork = m.createProperty(m.getNsPrefixURI("frbr"), "Work");
		docoBibliography = m.createResource(m.getNsPrefixURI("doco") + "Bibliography");
		frbrRealization = m.createProperty(m.getNsPrefixURI("frbr") + "realization");
		frbrPartOf = m.createProperty(m.getNsPrefixURI("frbr") + "partOf");
		biroReferenceList = m.createResource(m.getNsPrefixURI("biro") + "ReferenceList");
		coElementOf = m.createProperty(m.getNsPrefixURI("co") + "elementOf");
		proRoleInTime = m.createResource(m.getNsPrefixURI("pro") + "RoleInTime");
		proWithRole = m.createProperty(m.getNsPrefixURI("pro") + "withRole");
		proRelatesToDocument = m.createProperty(m.getNsPrefixURI("pro") + "relatesToDocument");
		proHoldsRoleInTime = m.createProperty(m.getNsPrefixURI("pro") + "holdsRoleInTime");
		fabioJournalArticle = m.createResource(m.getNsPrefixURI("fabio") + "JournalArticle");
		fabioPrintObject = m.createResource(m.getNsPrefixURI("fabio") + "PrintObject");
		prismStartingPage = m.createProperty(m.getNsPrefixURI("prism") + "startingPage");
		prismEndingPage = m.createProperty(m.getNsPrefixURI("prism") + "endingPage");
		fabioJournalIssue = m.createResource(m.getNsPrefixURI("fabio") + "JournalIssue");
		fabioHasSequenceIdentifier = m.createProperty(m.getNsPrefixURI("fabio") + "hasSequenceIdentifier");
		prismPublicationDate = m.createProperty(m.getNsPrefixURI("prism") + "publicationDate");
		fabioJournalVolume = m.createResource(m.getNsPrefixURI("fabio") + "JournalVolume");
		biroReferences = m.createProperty(m.getNsPrefixURI("biro") + "references");
		biroBibliographicReference = m.createResource(m.getNsPrefixURI("biro") + "BibliographicReference");
		coHasItem = m.createProperty(m.getNsPrefixURI("co") + "item");
		coItem = m.createResource(m.getNsPrefixURI("co") + "Item");
		coItemContent = m.createProperty(m.getNsPrefixURI("co") + "itemContent");
		foafFamilyName = m.createProperty(m.getNsPrefixURI("foaf") + "familyName");
		proContributor = m.createResource(m.getNsPrefixURI("pro") + "contributor");
		prismSubtitle = m.createProperty(m.getNsPrefixURI("prism") + "subtitle");
		proEditor = m.createResource(m.getNsPrefixURI("pro") + "editor");
		swcConferenceEvent = m.createResource(m.getNsPrefixURI("pro") + "ConferenceEvent");
		swcRelatedToEvent = m.createProperty(m.getNsPrefixURI("swc") + "relatedToEvent");
		prismISSN = m.createProperty(m.getNsPrefixURI("prism") + "issn");
		fabioJournal = m.createResource(m.getNsPrefixURI("fabio") + "Journal");
		prismHasEdition = m.createProperty(m.getNsPrefixURI("prism") + "edition");
		fabioBook = m.createResource(m.getNsPrefixURI("fabio") + "Book");
		frbrEmbodiment = m.createProperty(m.getNsPrefixURI("frbr") + "embodiment");
		proPublisher = m.createResource(m.getNsPrefixURI("pro") + "publisher");
		prismISBN = m.createProperty(m.getNsPrefixURI("prism") + "isbn");
		fabioBookSeries = m.createResource(m.getNsPrefixURI("fabio") + "BookSeries");
		fabioWebContent = m.createResource(m.getNsPrefixURI("fabio") + "WebContent");
		fabioWebManifestation = m.createResource(m.getNsPrefixURI("fabio") + "WebManifestation");
		prismURL = m.createProperty(m.getNsPrefixURI("prism") + "url");
		prismDOI = m.createProperty(m.getNsPrefixURI("prism") + "doi");
		proAuthor = m.createProperty(m.getNsPrefixURI("pro") + "author");
		proSeriesEditor = m.createResource(m.getNsPrefixURI("pro") + "series-editor");
		geoSpatialThing = m.createResource(m.getNsPrefixURI("geo") + "SpatialThing");
		fabioHasPublicationYear = m.createProperty(m.getNsPrefixURI("fabio") + "hasPublicationYear");
		swcHasLocation = m.createProperty(m.getNsPrefixURI("swc") + "hasLocation");
		frbrExpression = m.createResource(m.getNsPrefixURI("frbr") + "Expression");
		frbrManifestation = m.createResource(m.getNsPrefixURI("frbr") + "Manifestation");
		fabioPhDThesis = m.createResource(m.getNsPrefixURI("fabio") + "DoctoralThesis");
		fabioArticle = m.createResource(m.getNsPrefixURI("fabio") + "Article");
		fabioMastersThesis = m.createResource(m.getNsPrefixURI("fabio") + "MastersThesis");
		fabioBachelorThesis = m.createResource(m.getNsPrefixURI("fabio") + "BachelorsThesis");
		geoLocation = m.createProperty(m.getNsPrefixURI("geo") + "location");
		fabioConferenceProceedings = m.createResource(m.getNsPrefixURI("fabio") + "ConferenceProceedings");
		fabioConferencePaper = m.createResource(m.getNsPrefixURI("fabio") + "ConferencePaper");
	}

	// All the required Jena Resources and Properties
	public static Resource fabioConferencePaper, fabioConferenceProceedings, fabioBachelorThesis, fabioMastersThesis, fabioArticle, fabioPhDThesis, frbrExpression, frbrManifestation, geoSpatialThing, proSeriesEditor, fabioWebManifestation, fabioWebContent, fabioBookSeries, proPublisher, fabioBook, fabioJournal, biroBibliographicReference, fabioJournalVolume, fabioJournalIssue, frbrWork, docoBibliography, biroReferenceList, proEditor, swcConferenceEvent, proContributor, coItem, proRoleInTime, fabioJournalArticle, fabioPrintObject;
	public static Property geoLocation, swcHasLocation, fabioHasPublicationYear, prismDOI, prismURL, prismISBN, frbrEmbodiment, prismHasEdition, prismISSN, swcRelatedToEvent, prismSubtitle, foafFamilyName, coItemContent, coHasItem, biroReferences, prismPublicationDate, fabioHasSequenceIdentifier, prismEndingPage, prismStartingPage, proHoldsRoleInTime, proRelatesToDocument, proAuthor, proWithRole, coElementOf, frbrRealization, frbrPartOf;
}