Necessary parameters:
[1] path of namespaces file
[2] path of input folder containing all the XML files and subfolders to be processed
[3] ABSOLUTE path of output file that will be used to write the RDF statements (strongly advised that the folder is to be empty) (ATTENTION: the path MUST be absolute)
[4] 'jena-log4j.properties' file for log configuration
[5] base URL for the declared resources.

Here is an example of a command line invocation:
java -Xmx1024m -jar rif_bib.jar namespaces.xml <path_to_folder_containing_xml_articles> "C:\output\output.rdf" jena-log4j.properties "http://www.example.org/rdf"

_________________________________________

Parametri necessari:
[1] il path del file contenente i namespace necessari
[2] path della cartella contenente tutte le sottocartelle e tutti i file XML da analizzare
[3] path ASSOLUTO del file di output (è consigliato che la cartella in cui il file è presente sia vuota) (ATTENZIONE: il path deve essere assoluto)
[4] path del file 'jena-log4j.properties' per la configurazione di Jena
[5] URI/URL di base per le risorse dichiarate