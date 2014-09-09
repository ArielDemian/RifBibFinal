package patternRecognizers;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

import org.apache.commons.validator.routines.UrlValidator;
import org.w3c.dom.Node;

import model.MyNode;
import model.PatternRecognizer;
import model.URIManager;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;

// PatternRecognizer for structured bib. refs.
public class StructuredBibRefRecognizer implements PatternRecognizer {
	public boolean getData(Node n, Resource referenceList, String doi, NamespaceContext nsc) {
		// All the required information for the bib. ref. is declared as
		// statements
		Model m = ModelFactory.createDefaultModel();
		Model model = referenceList.getModel();
		if (n == null || n.hasChildNodes() == false)
			return false;
		if (n.getAttributes().getNamedItem("id") == null) {
			return false;
		}
		String id = n.getAttributes().getNamedItem("id").getNodeValue();
		Resource bibRef = m.createResource(nsc.getNamespaceURI("doi") + doi + "#" + id, Main.biroBibliographicReference);
		Resource coItem = m.createResource(bibRef.getURI() + "-Item", Main.coItem);
		coItem.addProperty(Main.coItemContent, bibRef);
		bibRef.addLiteral(DC.identifier, id);
		Resource book = null, work = null, expression = null, manifestation = null, journal = null, journalIssue = null, journalVolume = null, bookSeries = null;
		String[] workTitle = getWorkTitle(n);
		if (workTitle[0] == null) {
			if (workTitle[1] == null)
				return false;
			else {
				work = m.createResource(URIManager.getWorkURI(workTitle[1]), Main.frbrWork);
				expression = m.createResource(URIManager.getExpressionURI(workTitle[1]), Main.fabioBook);
				manifestation = m.createResource(URIManager.getManifestationURI(workTitle[1]), Main.fabioPrintObject);
			}
		} else {
			if (workTitle[1] != null)
				book = m.createResource(URIManager.getExpressionURI(workTitle[1]), Main.fabioBook);
			work = m.createResource(URIManager.getWorkURI(workTitle[0]), Main.frbrWork);
			expression = m.createResource(URIManager.getExpressionURI(workTitle[0]));
			manifestation = m.createResource(URIManager.getManifestationURI(workTitle[0]));
		}
		expression.addProperty(Main.frbrEmbodiment, manifestation);
		work.addProperty(Main.frbrRealization, expression);
		bibRef.addProperty(Main.biroReferences, work);
		int numberOfSbReference = 0;
		// The node tree is analysed node by node and the information is declared
		// as
		// statements where required
		for (Node n1 : new MyNode(n)) {
			if (n1.getNodeName().equals("sb:reference")) {
				numberOfSbReference++;
				int numberOfContribution = 0, numberOfHost = 0;
				for (Node n2 : new MyNode(n1)) {
					if (n2.getNodeName().equals("sb:contribution")) {
						numberOfContribution++;
						int numberOfAuthors = 0, numberOfTitle = 0, numberOfTranslatedTitle = 0;
						for (Node n3 : new MyNode(n2)) {
							if (n3.getNodeName().equals("sb:authors")) {
								numberOfAuthors++;
								int numberOfCollaboration = 0, numberOfAuthor = 0;
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:author")) {
										numberOfAuthor++;
										int numberOfGivenName = 0, numberOfSurname = 0, numberOfSuffix = 0;
										String givenName = "", surname = "", suffix = "";
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("ce:given-name")) {
												if (!MyNode.containsOnlyText(n5))
													return false;
												numberOfGivenName++;
												givenName = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("ce:surname")) {
												if (!MyNode.containsOnlyText(n5))
													return false;
												numberOfSurname++;
												surname = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("ce:suffix")) {
												numberOfSuffix++;
												if (!MyNode.containsOnlyText(n5))
													return false;
												suffix = n5.getTextContent().trim();
											}
										}
										if (numberOfSurname != 1 || numberOfGivenName > 1 || numberOfSuffix > 1) {
											return false;
										}
										Resource author = m.createResource(URIManager.getAuthorURI(givenName, surname, suffix), Main.proRoleInTime);
										Resource person = m.createResource(URIManager.getPersonURI(givenName, surname, suffix), FOAF.Person);
										author.addProperty(Main.proWithRole, Main.proAuthor);
										author.addProperty(Main.proRelatesToDocument, work);
										person.addLiteral(FOAF.givenname, givenName + (!suffix.equals("") ? " " + suffix : ""));
										person.addLiteral(Main.foafFamilyName, surname);
										person.addProperty(Main.proHoldsRoleInTime, author);
										work.addProperty(DC.creator, author);
									}
									if (n4.getNodeName().equals("sb:collaboration")) {
										numberOfCollaboration++;
										if (MyNode.containsOnlyText(n4)) {
											Resource agent = m.createResource(URIManager.getAgentURI(n4.getTextContent().trim()), FOAF.Agent);
											Resource collaboration = m.createResource(URIManager.getCollaborationURI(n4.getTextContent().trim()), Main.proRoleInTime);
											collaboration.addProperty(Main.proWithRole, Main.proContributor);
											agent.addLiteral(FOAF.name, n4.getTextContent().trim());
											collaboration.addProperty(Main.proRelatesToDocument, work);
											agent.addProperty(Main.proHoldsRoleInTime, collaboration);
										}
									}
								}
								if (numberOfCollaboration > 1 || numberOfAuthor == 0)
									return false;
							}
							if ((n3.getNodeName().equals("sb:title") && numberOfTitle == numberOfTitle++) || (n3.getNodeName().equals("sb:translated-title") && numberOfTranslatedTitle == numberOfTranslatedTitle++)) {
								int numberOfMaintitle = 0, numberOfSubtitle = 0;
								String title = null, subtitle = null;
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:maintitle")) {
										numberOfMaintitle++;
										title = n4.getTextContent().trim();
									}
									if (n4.getNodeName().equals("sb:subtitle")) {
										numberOfSubtitle++;
										subtitle = n4.getTextContent().trim();
									}
								}
								if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
									return false;
								if (n3.getNodeName().equals("sb:translated-title")) {
									if (title != null)
										expression.addProperty(DC.title, title, "en");
									if (subtitle != null)
										expression.addProperty(Main.prismSubtitle, subtitle, "en");
								} else {
									if (title != null)
										expression.addProperty(DC.title, title);
									if (subtitle != null)
										expression.addProperty(Main.prismSubtitle, subtitle);
								}
							}
						}
						if (numberOfAuthors > 1 || numberOfTitle > 1 || numberOfTranslatedTitle > 1)
							return false;
					}
					if (n2.getNodeName().equals("sb:host")) {
						numberOfHost++;
						int numberOfBook = 0, numberOfIssue = 0, numberOfEditedBook = 0, numberOfEHost = 0, numberOfPages = 0, numberOfDoi = 0;
						for (Node n3 : new MyNode(n2)) {
							if (n3.getNodeName().equals("sb:issue")) {
								numberOfIssue++;
								String journalTitle = "", journalSubtitle = "", journalTranslatedTitle = "", journalTranslatedSubtitle = "", issn = "", volumeNr = "", issueNr = "", issueDate = "", issueTitle = "", issueSubtitle = "", issueTranslatedTitle = "", issueTranslatedSubtitle = "";
								Resource conferenceProceedings = null;
								int numberOfEditors = 0, numberOfConference = 0, numberOfSeries = 0, numberOfIssueNr = 0, numberOfDate = 0, numberOfTitle = 0, numberOfTranslatedTitle = 0;
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:title")) {
										numberOfTitle++;
										int numberOfMaintitle = 0, numberOfSubtitle = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												numberOfMaintitle++;
												issueTitle = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:subtitle")) {
												numberOfSubtitle++;
												issueSubtitle = n5.getTextContent().trim();
											}
										}
										if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
											return false;
									}
									if (n4.getNodeName().equals("sb:translated-title")) {
										numberOfTranslatedTitle++;
										int numberOfMaintitle = 0, numberOfSubtitle = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												numberOfMaintitle++;
												issueTranslatedTitle = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:subtitle")) {
												numberOfSubtitle++;
												issueTranslatedSubtitle = n5.getTextContent().trim();
											}
										}
										if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
											return false;
									}
									if (n4.getNodeName().equals("sb:date")) {
										numberOfDate++;
										if (MyNode.containsOnlyText(n4))
											issueDate = n4.getTextContent().trim();
									}
									if (n4.getNodeName().equals("sb:editors")) {
										numberOfEditors++;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:editor")) {
												int numberOfGivenName = 0, numberOfSurname = 0, numberOfSuffix = 0;
												String givenName = "", surname = "", suffix = "";
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("ce:given-name")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfGivenName++;
														givenName = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("ce:surname")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfSurname++;
														surname = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("ce:suffix")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfSuffix++;
														suffix = n6.getTextContent().trim();
													}
												}
												if (numberOfSurname != 1 || numberOfGivenName > 1 || numberOfSuffix > 1) {
													return false;
												}
												Resource person = m.createResource(URIManager.getPersonURI(givenName, surname, suffix), FOAF.Person);
												Resource editor = m.createResource(URIManager.getEditorURI(givenName, surname, suffix), Main.proRoleInTime);
												editor.addProperty(Main.proWithRole, Main.proEditor);
												editor.addProperty(Main.proRelatesToDocument, work);
												person.addProperty(Main.proHoldsRoleInTime, editor);
											}
										}
									}
									if (n4.getNodeName().equals("sb:conference")) {
										numberOfConference++;
										if (MyNode.containsOnlyText(n4)) {
											conferenceProceedings = m.createResource(URIManager.getConferenceProceedingsURI(n4.getTextContent().trim()), Main.fabioConferenceProceedings);
											conferenceProceedings.addLiteral(DC.title, n4.getTextContent().trim());
										}
									}
									if (n4.getNodeName().equals("sb:series")) {
										numberOfSeries++;
										int numberOfSeriesTitle = 0, numberOfSeriesTranslatedTitle = 0, numberOfIssn = 0, numberOfVolumeNr = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:issn")) {
												numberOfIssn++;
												if (MyNode.containsOnlyText(n5))
													issn = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:volume-nr")) {
												numberOfVolumeNr++;
												if (MyNode.containsOnlyText(n5))
													volumeNr = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:title")) {
												numberOfSeriesTitle++;
												int numberOfMaintitle = 0, numberOfSubtitle = 0;
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("sb:maintitle")) {
														numberOfMaintitle++;
														journalTitle = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("sb:subtitle")) {
														numberOfSubtitle++;
														journalSubtitle = n6.getTextContent().trim();
													}
												}
												if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
													return false;
											}
											if (n5.getNodeName().equals("sb:translated-title")) {
												numberOfSeriesTranslatedTitle++;
												int numberOfMaintitle = 0, numberOfSubtitle = 0;
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("sb:maintitle")) {
														numberOfMaintitle++;
														journalTranslatedTitle = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("sb:subtitle")) {
														numberOfSubtitle++;
														journalTranslatedSubtitle = n6.getTextContent().trim();
													}
												}
												if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
													return false;
											}
										}
										if (numberOfVolumeNr > 1 || numberOfIssn > 1 || numberOfSeriesTitle > 1 || numberOfSeriesTranslatedTitle > 1 || numberOfSeriesTitle + numberOfSeriesTranslatedTitle == 0)
											return false;
									}
									if (n4.getNodeName().equals("sb:issue-nr")) {
										numberOfIssueNr++;
										issueNr = n4.getTextContent().trim();
									}
								}
								if (numberOfEditors > 1 || numberOfConference > 1 || numberOfSeries != 1 || numberOfIssueNr > 1 || numberOfDate != 1 || numberOfTitle > 1 || numberOfTranslatedTitle > 1)
									return false;
								expression.addProperty(RDF.type, Main.fabioJournalArticle);
								if (!journalTitle.equals("")) {
									journal = m.createResource(URIManager.getJournalURI(journalTitle), Main.fabioJournal);
									journal.addLiteral(DC.title, journalTitle);
									if (!journalSubtitle.equals(""))
										journal.addLiteral(Main.prismSubtitle, journalSubtitle);
								}
								if (!journalTranslatedTitle.equals("")) {
									if (journal == null)
										journal = m.createResource(URIManager.getJournalURI(journalTranslatedTitle), Main.fabioJournal);
									journal.addProperty(DC.title, journalTranslatedTitle, "en");
									if (!journalTranslatedSubtitle.equals(""))
										journal.addProperty(Main.prismSubtitle, journalTranslatedSubtitle, "en");
								}
								if (journal == null) {
									continue;
								}
								if (!issn.equals(""))
									journal.addLiteral(Main.prismISSN, issn);
								if (!volumeNr.equals("")) {
									journalVolume = m.createResource(URIManager.getJournalVolumeURI(volumeNr, issn), Main.fabioJournalVolume);
									journalVolume.addLiteral(Main.fabioHasSequenceIdentifier, volumeNr);
									journalVolume.addProperty(Main.frbrPartOf, journal);
									if (!issueNr.equals("")) {
										journalIssue = m.createResource(URIManager.getJournalIssueURI(issueNr, volumeNr, issn), Main.fabioJournalIssue);
										journalIssue.addProperty(Main.frbrPartOf, journalVolume);
										expression.addProperty(Main.frbrPartOf, journalIssue);
										if (conferenceProceedings != null)
											journalIssue.addProperty(Main.frbrPartOf, conferenceProceedings);
										if (!issueTitle.equals(""))
											journalIssue.addLiteral(DC.title, issueTitle);
										if (!issueSubtitle.equals(""))
											journalIssue.addLiteral(Main.prismSubtitle, issueSubtitle);
										if (!issueTranslatedTitle.equals(""))
											journalIssue.addProperty(DC.title, issueTranslatedTitle, "en");
										if (!issueTranslatedSubtitle.equals(""))
											journalIssue.addProperty(Main.prismSubtitle, issueTranslatedSubtitle, "en");
										if (!issueNr.equals(""))
											journalIssue.addLiteral(Main.fabioHasSequenceIdentifier, issueNr);
										if (!issueDate.equals("")) {
											if (issueDate.matches("^\\d{3,4}$")) {
												journalIssue.addProperty(Main.fabioHasPublicationYear, issueDate, XSDDatatype.XSDgYear);
											} else
												journalIssue.addProperty(Main.prismPublicationDate, issueDate, XSDDatatype.XSDdate);
										}
									}
								}
								if (journalIssue == null && conferenceProceedings != null)
									expression.addProperty(Main.frbrPartOf, conferenceProceedings);
							}
							if (n3.getNodeName().equals("sb:book") || n3.getNodeName().equals("sb:edited-book")) {
								boolean editedBook = false;
								if (n3.getNodeName().equals("sb:book"))
									numberOfBook++;
								else {
									numberOfEditedBook++;
									editedBook = true;
								}
								int numberOfEdition = 0, numberOfBookSeries = 0, numberOfDate = 0, numberOfPublisher = 0, numberOfIsbn = 0, numberOfTitle = 0, numberOfTranslatedTitle = 0, numberOfEditors = 0, numberOfConference = 0;
								List<Resource> seriesEditors = new LinkedList<Resource>();
								String title = "", subtitle = "", translatedTitle = "", translatedSubtitle = "";
								for (Node n4 : new MyNode(n3)) {
									if (editedBook && n4.getNodeName().equals("sb:editors")) {
										numberOfEditors++;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:editor")) {
												int numberOfGivenName = 0, numberOfSurname = 0, numberOfSuffix = 0;
												String givenName = "", surname = "", suffix = "";
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("ce:given-name")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfGivenName++;
														givenName = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("ce:surname")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfSurname++;
														surname = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("ce:suffix")) {
														if (!MyNode.containsOnlyText(n6))
															return false;
														numberOfSuffix++;
														suffix = n6.getTextContent().trim();
													}
												}
												if (numberOfSurname != 1 || numberOfGivenName > 1 || numberOfSuffix > 1) {
													return false;
												}
												Resource person = m.createResource(URIManager.getPersonURI(givenName, surname, suffix), FOAF.Person);
												Resource editor = m.createResource(URIManager.getEditorURI(givenName, surname, suffix), Main.proRoleInTime);
												editor.addProperty(Main.proWithRole, Main.proEditor);
												person.addProperty(Main.proHoldsRoleInTime, editor);
												if (book != null)
													editor.addProperty(Main.proRelatesToDocument, book);
												else
													editor.addProperty(Main.proRelatesToDocument, work);
											}
										}
									}
									if (editedBook && n4.getNodeName().equals("sb:conference")) {
										numberOfConference++;
										if (MyNode.containsOnlyText(n4)) {
											Resource conferenceProceedings = m.createResource(URIManager.getConferenceProceedingsURI(n4.getTextContent().trim()), Main.fabioConferenceProceedings);
											conferenceProceedings.addLiteral(DC.title, n4.getTextContent().trim());
											if (book != null)
												book.addProperty(Main.frbrPartOf, conferenceProceedings);
											else
												expression.addProperty(Main.frbrPartOf, conferenceProceedings);
										}
									}
									if (n4.getNodeName().equals("sb:edition")) {
										numberOfEdition++;
										manifestation.addLiteral(Main.prismHasEdition, n4.getTextContent().trim());
									}
									if (n4.getNodeName().equals("sb:book-series")) {
										numberOfBookSeries++;
										int numberOfSeriesEditors = 0, numberOfSeries = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:editors")) {
												numberOfSeriesEditors++;
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("sb:editor")) {
														int numberOfGivenName = 0, numberOfSurname = 0, numberOfSuffix = 0;
														String givenName = "", surname = "", suffix = "";
														for (Node n7 : new MyNode(n6)) {
															if (n7.getNodeName().equals("ce:given-name")) {
																if (!MyNode.containsOnlyText(n7))
																	return false;
																numberOfGivenName++;
																givenName = n7.getTextContent().trim();
															}
															if (n7.getNodeName().equals("ce:surname")) {
																if (!MyNode.containsOnlyText(n7))
																	return false;
																numberOfSurname++;
																surname = n7.getTextContent().trim();
															}
															if (n7.getNodeName().equals("ce:suffix")) {
																if (!MyNode.containsOnlyText(n7))
																	return false;
																numberOfSuffix++;
																suffix = n7.getTextContent().trim();
															}
														}
														if (numberOfSurname != 1 || numberOfGivenName > 1 || numberOfSuffix > 1) {
															return false;
														}
														Resource person = m.createResource(URIManager.getPersonURI(givenName, surname, suffix), FOAF.Person);
														Resource seriesEditor = m.createResource(URIManager.getEditorURI(givenName, surname, suffix), Main.proRoleInTime);
														seriesEditor.addProperty(Main.proWithRole, Main.proSeriesEditor);
														person.addProperty(Main.proHoldsRoleInTime, seriesEditor);
														seriesEditors.add(seriesEditor);
													}
												}
											}
											if (n5.getNodeName().equals("sb:series")) {
												numberOfSeries++;
												String issn = "", volumeNr = "", bookSeriesTitle = "", bookSeriesSubtitle = "", bookSeriesTranslatedTitle = "", bookSeriesTranslatedSubtitle = "";
												int numberOfSeriesTitle = 0, numberOfSeriesTranslatedTitle = 0, numberOfIssn = 0, numberOfVolumeNr = 0;
												for (Node n6 : new MyNode(n5)) {
													if (n6.getNodeName().equals("sb:issn")) {
														numberOfIssn++;
														if (MyNode.containsOnlyText(n6))
															issn = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("sb:volume-nr")) {
														numberOfVolumeNr++;
														if (MyNode.containsOnlyText(n6))
															volumeNr = n6.getTextContent().trim();
													}
													if (n6.getNodeName().equals("sb:title")) {
														numberOfSeriesTitle++;
														int numberOfMaintitle = 0, numberOfSubtitle = 0;
														for (Node n7 : new MyNode(n6)) {
															if (n7.getNodeName().equals("sb:maintitle")) {
																numberOfMaintitle++;
																bookSeriesTitle = n7.getTextContent().trim();
															}
															if (n7.getNodeName().equals("sb:subtitle")) {
																numberOfSubtitle++;
																bookSeriesSubtitle = n7.getTextContent().trim();
															}
														}
														if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
															return false;
													}
													if (n6.getNodeName().equals("sb:translated-title")) {
														numberOfSeriesTranslatedTitle++;
														int numberOfMaintitle = 0, numberOfSubtitle = 0;
														for (Node n7 : new MyNode(n6)) {
															if (n7.getNodeName().equals("sb:maintitle")) {
																numberOfMaintitle++;
																bookSeriesTranslatedTitle = n7.getTextContent().trim();
															}
															if (n7.getNodeName().equals("sb:subtitle")) {
																numberOfSubtitle++;
																bookSeriesTranslatedSubtitle = n7.getTextContent().trim();
															}
														}
														if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
															return false;
													}
												}
												if (numberOfVolumeNr > 1 || numberOfIssn > 1 || numberOfSeriesTitle > 1 || numberOfSeriesTranslatedTitle > 1 || numberOfSeriesTitle + numberOfSeriesTranslatedTitle == 0)
													return false;
												if (!bookSeriesTitle.equals("")) {
													bookSeries = m.createResource(URIManager.getBookSeriesURI(bookSeriesTitle), Main.fabioBookSeries);
													bookSeries.addLiteral(DC.title, bookSeriesTitle);
													if (!bookSeriesSubtitle.equals(""))
														bookSeries.addLiteral(Main.prismSubtitle, bookSeriesSubtitle);
												}
												if (!bookSeriesTranslatedTitle.equals("")) {
													if (bookSeries == null)
														bookSeries = m.createResource(URIManager.getBookSeriesURI(bookSeriesTranslatedTitle), Main.fabioBookSeries);
													bookSeries.addProperty(DC.title, bookSeriesTranslatedTitle, "en");
													if (!bookSeriesTranslatedSubtitle.equals(""))
														bookSeries.addProperty(Main.prismSubtitle, bookSeriesTranslatedSubtitle, "en");
												}
												if (bookSeries == null) {
													continue;
												}
												if (!issn.equals(""))
													bookSeries.addLiteral(Main.prismISSN, issn);
												if (!volumeNr.equals(""))
													bookSeries.addLiteral(Main.fabioHasSequenceIdentifier, volumeNr);
												if (book != null) {
													book.addProperty(Main.frbrPartOf, bookSeries);
												} else
													expression.addProperty(Main.frbrPartOf, bookSeries);
											}
										}
										if (numberOfSeries != 1 || numberOfSeriesEditors > 1)
											return false;
									}
									if (n4.getNodeName().equals("sb:date")) {
										numberOfDate++;
										if (MyNode.containsOnlyText(n4)) {
											if (n4.getTextContent().trim().matches("^\\d{3,4}$")) {
												expression.addProperty(Main.fabioHasPublicationYear, n4.getTextContent().trim(), XSDDatatype.XSDgYear);
											} else {
												expression.addProperty(Main.prismPublicationDate, n4.getTextContent().trim(), XSDDatatype.XSDdate);
											}
										}
									}
									if (n4.getNodeName().equals("sb:publisher")) {
										numberOfPublisher++;
										String publisherName = "", publisherLocation = "";
										int numberOfName = 0, numberOfLocation = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:name")) {
												numberOfName++;
												publisherName = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:location")) {
												numberOfLocation++;
												publisherLocation = n5.getTextContent().trim();
											}
										}
										if (numberOfName != 1 || numberOfLocation > 1)
											return false;
										Resource agent = m.createResource(URIManager.getAgentURI(publisherName), FOAF.Organization);
										Resource publisher = m.createResource(URIManager.getPublisherURI(publisherName), Main.proRoleInTime);
										publisher.addProperty(Main.proWithRole, Main.proPublisher);
										publisher.addProperty(Main.proRelatesToDocument, work);
										agent.addLiteral(FOAF.name, publisherName);
										agent.addProperty(Main.proHoldsRoleInTime, publisher);
										if (!publisherLocation.equals("")) {
											Resource location = m.createResource(URIManager.getLocationURI(publisherLocation), Main.geoSpatialThing);
											location.addLiteral(DC.description, publisherLocation);
											agent.addProperty(Main.geoLocation, location);
										}
									}
									if (n4.getNodeName().equals("sb:isbn")) {
										numberOfIsbn++;
										if (MyNode.containsOnlyText(n4))
											manifestation.addLiteral(Main.prismISBN, n4.getTextContent().trim());
									}
									if (n4.getNodeName().equals("sb:title")) {
										numberOfTitle++;
										int numberOfMaintitle = 0, numberOfSubtitle = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												numberOfMaintitle++;
												title = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:subtitle")) {
												numberOfSubtitle++;
												subtitle = n5.getTextContent().trim();
											}
										}
										if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
											return false;
									}
									if (n4.getNodeName().equals("sb:translated-title")) {
										numberOfTranslatedTitle++;
										int numberOfMaintitle = 0, numberOfSubtitle = 0;
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												numberOfMaintitle++;
												translatedTitle = n5.getTextContent().trim();
											}
											if (n5.getNodeName().equals("sb:subtitle")) {
												numberOfSubtitle++;
												translatedSubtitle = n5.getTextContent().trim();
											}
										}
										if (numberOfMaintitle != 1 || numberOfSubtitle > 1)
											return false;
									}
								}
								if (numberOfBookSeries > 1 || numberOfEdition > 1 || numberOfTitle > 1 || numberOfTranslatedTitle > 1 || numberOfDate == 0 || numberOfIsbn > 1 || numberOfPublisher > 1 || editedBook && numberOfEditors > 1 || editedBook && numberOfConference > 1)
									return false;
								if (book != null) {
									if (!title.equals(""))
										book.addLiteral(DC.title, title);
									if (!subtitle.equals(""))
										book.addLiteral(Main.prismSubtitle, subtitle);
									if (!translatedTitle.equals(""))
										book.addProperty(DC.title, translatedTitle, "en");
									if (!translatedSubtitle.equals(""))
										book.addProperty(Main.prismSubtitle, translatedSubtitle, "en");
									expression.addProperty(RDF.type, Main.fabioArticle);
									expression.addProperty(Main.frbrPartOf, book);
								} else {
									if (!title.equals(""))
										expression.addLiteral(DC.title, title);
									if (!subtitle.equals(""))
										expression.addLiteral(Main.prismSubtitle, subtitle);
									if (!translatedTitle.equals(""))
										expression.addProperty(DC.title, translatedTitle, "en");
									if (!translatedSubtitle.equals(""))
										expression.addProperty(Main.prismSubtitle, translatedSubtitle, "en");
								}
								manifestation.addProperty(RDF.type, Main.fabioPrintObject);
								if (!seriesEditors.isEmpty() && bookSeries != null) {
									for (Resource seriesEditor : seriesEditors)
										seriesEditor.addProperty(Main.proRelatesToDocument, bookSeries);
								}
							}
							if (n3.getNodeName().equals("sb:e-host")) {
								numberOfEHost++;
								int numberOfInterRef = 0, numberOfDate = 0;
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("ce:inter-ref")) {
										numberOfInterRef++;
										if (n4.getAttributes().getNamedItem("xlink:href") != null) {
											String url = n4.getAttributes().getNamedItem("xlink:href").getNodeValue();
											String[] schemes = { "http", "https", "ftp" };
											UrlValidator urlValidator = new UrlValidator(schemes);
											if (urlValidator.isValid(url)) {
												expression.addProperty(RDF.type, Main.fabioWebContent);
												manifestation.addProperty(RDF.type, Main.fabioWebManifestation);
												manifestation.addProperty(Main.prismURL, url, XSDDatatype.XSDanyURI);
											}
										}
									}
									if (n4.getNodeName().equals("sb:date")) {
										numberOfDate++;
										if (MyNode.containsOnlyText(n4))
											if (n4.getTextContent().trim().matches("^\\d{3,4}$"))
												expression.addProperty(Main.fabioHasPublicationYear, n4.getTextContent().trim(), XSDDatatype.XSDgYear);
											else
												expression.addProperty(Main.prismPublicationDate, n4.getTextContent().trim(), XSDDatatype.XSDdate);
									}
								}
								if (numberOfDate > 1 || numberOfInterRef > 1)
									return false;
							}
							if (n3.getNodeName().equals("ce:doi")) {
								numberOfDoi++;
								if (MyNode.containsOnlyText(n3))
									work.addLiteral(Main.prismDOI, n3.getTextContent().trim());
							}
							if (n3.getNodeName().equals("sb:pages")) {
								numberOfPages++;
								int numberOfFirstPage = 0, numberOfLastPage = 0;
								try {
									for (Node n4 : new MyNode(n3)) {
										if (n4.getNodeName().equals("sb:first-page")) {
											numberOfFirstPage++;
											int fpage = Integer.parseInt(n4.getTextContent().trim());
											manifestation.addLiteral(Main.prismStartingPage, fpage);
										}
										if (n4.getNodeName().equals("sb:last-page")) {
											numberOfLastPage++;
											int lpage = Integer.parseInt(n4.getTextContent().trim());
											manifestation.addLiteral(Main.prismEndingPage, lpage);
										}
									}
									if (numberOfFirstPage != 1 || numberOfLastPage > 1)
										return false;
								} catch (NumberFormatException e) {
									return false;
								}
							}
						}
						if (numberOfBook + numberOfIssue + numberOfEditedBook + numberOfEHost != 1 || numberOfDoi > 1 || numberOfEHost + numberOfPages > 1) {
							return false;
						}
					}
					if (n2.getNodeName().equals("sb:comment")) {
						bibRef.addLiteral(RDFS.comment, n2.getTextContent().trim());
					}
				}
				if (numberOfContribution != 1 || numberOfHost != 1)
					return false;
			}
			if (n1.getNodeName().equals("ce:other-ref")) {
				return false;
			}
			if (n1.getNodeName().equals("ce:note"))
				return false;
			if (n1.getNodeName().equals("ce:label")) {
				if (MyNode.containsOnlyText(n1))
					bibRef.addLiteral(RDFS.label, n1.getTextContent().trim());
			}
		}
		if (numberOfSbReference != 1) {
			return false;
		}
		// The information retrieved in a locally declared model is added to the
		// general model containing all the statements
		referenceList.addProperty(Main.coHasItem, coItem);
		model.add(m);
		return true;
	}

	// Returns the title of the work necessary for the declaration of the
	// resources
	private static String[] getWorkTitle(Node n) {
		String title = null, translatedTitle = null, bookTitle = null, bookTranslatedTitle = null;
		for (Node n1 : new MyNode(n)) {
			if (n1.getNodeName().equals("sb:reference")) {
				for (Node n2 : new MyNode(n1)) {
					if (n2.getNodeName().equals("sb:contribution")) {
						for (Node n3 : new MyNode(n2)) {
							if (n3.getNodeName().equals("sb:title")) {
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:maintitle"))
										title = n4.getTextContent().trim();
								}
							}
							if (n3.getNodeName().equals("sb:translated-title")) {
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:maintitle"))
										translatedTitle = n4.getTextContent().trim();
								}
							}
						}
					}
					if (n2.getNodeName().equals("sb:host")) {
						for (Node n3 : new MyNode(n2)) {
							if (n3.getNodeName().equals("sb:book") || n3.getNodeName().equals("sb:edited-book")) {
								for (Node n4 : new MyNode(n3)) {
									if (n4.getNodeName().equals("sb:title")) {
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												bookTitle = n5.getTextContent().trim();
											}
										}
									}
									if (n4.getNodeName().equals("sb:translated-title")) {
										for (Node n5 : new MyNode(n4)) {
											if (n5.getNodeName().equals("sb:maintitle")) {
												bookTranslatedTitle = n5.getTextContent().trim();
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		String[] result = { (title != null ? title : translatedTitle), (bookTitle != null ? bookTitle : bookTranslatedTitle) };
		return result;
	}
}
