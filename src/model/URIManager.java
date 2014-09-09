package model;

import org.restlet.data.Reference;

// Class created for returning all the required URIs (mostly URLs) for the declared resources
public class URIManager {
	// Base URL that can be modified for the resources
	private static String base = "http://ltw1313.web.cs.unibo.it/RifBib";

	public static void setBase(String URI) {
		URIManager.base = URI;
	}

	public static String getAuthorURI(String givenName, String familyName, String suffix) {
		if (givenName.equals("") && familyName.equals("") && suffix.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "author");
		ref.addQueryParameter("givenName", givenName.toLowerCase().trim());
		ref.addQueryParameter("familyName", familyName.toLowerCase().trim());
		ref.addQueryParameter("suffix", suffix.toLowerCase().trim());
		return ref.toString();
	}

	public static String getPersonURI(String givenName, String familyName, String suffix) {
		if (givenName.equals("") && familyName.equals("") && suffix.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "person");
		ref.addQueryParameter("givenName", givenName.toLowerCase().trim());
		ref.addQueryParameter("familyName", familyName.toLowerCase().trim());
		ref.addQueryParameter("suffix", suffix.toLowerCase().trim());
		return ref.toString();
	}

	public static String getCollaborationURI(String name) {
		if (name.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "collaboration");
		ref.addQueryParameter("name", name.toLowerCase().trim());
		return ref.toString();
	}

	public static String getAgentURI(String name) {
		if (name.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "agent");
		ref.addQueryParameter("name", name.toLowerCase().trim());
		return ref.toString();
	}

	public static String getEditorURI(String givenName, String familyName, String suffix) {
		if (givenName.equals("") && familyName.equals("") && suffix.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "editor");
		ref.addQueryParameter("given_name", givenName.toLowerCase().trim());
		ref.addQueryParameter("family_name", familyName.toLowerCase().trim());
		ref.addQueryParameter("suffix", suffix.toLowerCase().trim());
		return ref.toString();
	}

	public static String getConferenceProceedingsURI(String title) {
		if (title.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "conference_proceedings");
		ref.addQueryParameter("name", title.toLowerCase().trim());
		return ref.toString();
	}

	public static String getConferenceEvent(String name) {
		if (name.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "conference_event");
		ref.addQueryParameter("name", name.toLowerCase().trim());
		return ref.toString();
	}

	public static String getJournalURI(String issn) {
		if (issn.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "journal");
		ref.addQueryParameter("issn", issn.toLowerCase().trim());
		return ref.toString();
	}

	public static String getJournalVolumeURI(String volume, String issn) {
		if (volume.equals("") && issn.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "journal_volume");
		ref.addQueryParameter("journal_issn", issn.toLowerCase().trim());
		ref.addQueryParameter("volume", volume.toLowerCase().trim());
		return ref.toString();
	}

	public static String getJournalIssueURI(String issue, String volume, String issn) {
		if (issue.equals("") && volume.equals("") && issn.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "journal_issue");
		ref.addQueryParameter("journal_issn", issn.toLowerCase().trim());
		ref.addQueryParameter("volume", volume.toLowerCase().trim());
		ref.addQueryParameter("issue", issue.toLowerCase().trim());
		return ref.toString();
	}

	public static String getPublisherURI(String name) {
		if (name.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "publisher");
		ref.addQueryParameter("name", name.toLowerCase().trim());
		return ref.toString();
	}

	public static String getBookSeriesURI(String title) {
		if (title.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "book_series");
		ref.addQueryParameter("title", title.toLowerCase().trim());
		return ref.toString();
	}

	public static String getWorkURI(String title) {
		if (title.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "frbr_work");
		ref.addQueryParameter("title", title.toLowerCase().trim());
		return ref.toString();
	}

	public static String getExpressionURI(String title) {
		if (title.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "frbr_expression");
		ref.addQueryParameter("title", title.toLowerCase().trim());
		return ref.toString();
	}

	public static String getManifestationURI(String title) {
		if (title.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "frbr_manifestation");
		ref.addQueryParameter("title", title.toLowerCase().trim());
		return ref.toString();
	}

	public static String getLocationURI(String name) {
		if (name.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "location");
		ref.addQueryParameter("name", name.toLowerCase().trim());
		return ref.toString();
	}

	// URI returned for resources that do not have any recognizable pattern
	public static String getNoPatternURI(String text) {
		if (text.equals(""))
			return null;
		Reference ref = new Reference(base);
		ref.addQueryParameter("type", "no_pattern");
		ref.addQueryParameter("text", text.toLowerCase().trim());
		return ref.toString();
	}
}
