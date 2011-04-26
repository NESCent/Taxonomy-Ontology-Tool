package org.phenoscape.VTO.lib;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.DefaultOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class OWLUtils {
	
	private OWLOntologyManager owlOntManager;
	private OWLDataFactory dataFactory;
	private OWLReasonerFactory projectReasonerFactory; 
	private OWLOntology taxonomy;
	private DefaultPrefixManager oboPM = new DefaultPrefixManager(OWLNamespaces.OBO.toString());
	private String baseIRI = null;

	static final Logger logger = Logger.getLogger(OWLUtils.class.getName());

	
	
	private final OWLClass cladeClass;
	private final OWLClass dbXrefClass;
	
	private final OWLAnnotationProperty hasRankProperty;
	private final OWLAnnotationProperty isExtinctProperty;
	private final OWLObjectProperty subCladeOfProperty;
	private final OWLAnnotationProperty hasDbxRefProperty;
	private final OWLAnnotationProperty hasURIProperty;
	private String localPath;
	
	/** 
	 * Constructor for use by OWLStore - does not set baseIRI
	 * @throws OWLOntologyCreationException 
	 */	
	OWLUtils() throws OWLOntologyCreationException{
		oboPM.setPrefix("TTO:", "http://purl.org/obo/owl/TTO");
		oboPM.setPrefix("OBOINOWL:", OWLNamespaces.OBOINOWL.toString());
		owlOntManager = OWLManager.createOWLOntologyManager();
		dataFactory = owlOntManager.getOWLDataFactory();	
		projectReasonerFactory = new StructuralReasonerFactory();
		taxonomy = owlOntManager.createOntology();
		hasRankProperty = dataFactory.getOWLAnnotationProperty(":has_rank",oboPM);
		subCladeOfProperty = dataFactory.getOWLObjectProperty(IRI.create("http://vocab.phenoscape.org/subclade_of"));
		OWLAnnotation rankLabel = dataFactory.getOWLAnnotation(
				dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				dataFactory.getOWLLiteral("has taxonomic rank", "en")); 
		Set<OWLAxiom> addAxioms = new HashSet<OWLAxiom>();
		addAxioms.add(dataFactory.getOWLAnnotationAssertionAxiom(hasRankProperty.getIRI(), rankLabel));
		isExtinctProperty = dataFactory.getOWLAnnotationProperty(":is_extinct",oboPM);
		OWLAnnotation extinctLabel = dataFactory.getOWLAnnotation(
				dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				dataFactory.getOWLLiteral("is extinct", "en")); 
		addAxioms.add(dataFactory.getOWLAnnotationAssertionAxiom(hasRankProperty.getIRI(), extinctLabel));
		cladeClass = dataFactory.getOWLClass(IRI.create("http://vocab.phenoscape.org/clade"));
		dbXrefClass = dataFactory.getOWLClass(":DbXref", oboPM);
		hasDbxRefProperty = dataFactory.getOWLAnnotationProperty(":hasDbXref",oboPM);
		addAxioms.add(dataFactory.getOWLDeclarationAxiom(cladeClass));
		hasURIProperty = dataFactory.getOWLAnnotationProperty(":hasURI",oboPM);
		addAxioms.add(dataFactory.getOWLDeclarationAxiom(dbXrefClass));
		addAxioms.add(dataFactory.getOWLAnnotationAssertionAxiom(hasRankProperty.getIRI(), rankLabel));
		OWLAnnotation hasDbXrefLabel = dataFactory.getOWLAnnotation(
				dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				dataFactory.getOWLLiteral("has dbxref", "en")); 
		addAxioms.add(dataFactory.getOWLAnnotationAssertionAxiom(hasDbxRefProperty.getIRI(),hasDbXrefLabel));
		owlOntManager.addAxioms(taxonomy,addAxioms);
		
		// this is temporary
	}
	
	/** 
	 * Constructor for use by OWLStore
	 * @param ontIRI
	 * @param path
	 * @throws OWLOntologyCreationException 
	 */	
	OWLUtils(String ontIRI, String path) throws OWLOntologyCreationException{
		this();
		baseIRI = ontIRI;
		localPath = path;
	}
	
	protected OWLObjectProperty lookupProperty(String name){
		return null;
	}
	
	public boolean isEmpty(){
		logger.info("Count is " + taxonomy.getIndividualsInSignature().size());
		return (taxonomy.getIndividualsInSignature().size() == 0);  //need to verify this
	}
	
	public OWLNamedIndividual makeTerm(String id, String label){
		OWLNamedIndividual ni = dataFactory.getOWLNamedIndividual(id,oboPM);
		OWLAnnotation labelAnnotation = dataFactory.getOWLAnnotation(
				dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				dataFactory.getOWLLiteral(label, "en")); 
		OWLAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(ni.getIRI(), labelAnnotation);
		owlOntManager.applyChange(new AddAxiom(taxonomy,ax));
		OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(cladeClass, ni);
		owlOntManager.addAxiom(taxonomy,classAssertion);
		return ni;
	}
	
	public SynonymI makeSynonym(String synString){
		return null;
	}
	
	public OWLIndividual lookupTermByName(String name){
		OWLAnnotationProperty label = dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		for (OWLIndividual taxon : taxonomy.getIndividualsInSignature()) {
			if (!taxon.isAnonymous()){
				// Get the annotations for the taxon s that use rdfs:label
				for (OWLAnnotation annotation : taxon.asOWLNamedIndividual().getAnnotations(taxonomy, label)) {
					if (annotation.getValue() instanceof OWLLiteral) {
						OWLLiteral val = (OWLLiteral) annotation.getValue();
						if (val.getLiteral().equals(name)) {
							return taxon;
						}
					}
				}
			}
		}
		return null;
	}
	
	protected void saveOntology() throws OWLOntologyStorageException{
		File outFile = new File(localPath);
		owlOntManager.saveOntology(taxonomy,IRI.create(outFile.toURI()));
	}

	public void attachParent(OWLIndividual childTerm, OWLIndividual parentTerm) {
		OWLObjectPropertyAssertionAxiom cladeAssertion = dataFactory.getOWLObjectPropertyAssertionAxiom(subCladeOfProperty, childTerm, parentTerm);
		owlOntManager.addAxiom(taxonomy,cladeAssertion);
		
	}
	
	public void addDbXref(OWLIndividual term, String xrefid){
		OWLAnonymousIndividual i = dataFactory.getOWLAnonymousIndividual(xrefid);
		OWLAnnotation dbXrefAnnotation = dataFactory.getOWLAnnotation(
				dataFactory.getOWLAnnotationProperty(hasURIProperty.getIRI()),
				dataFactory.getOWLLiteral(xrefid,OWL2Datatype.XSD_ANY_URI)); 
		OWLAnnotationAssertionAxiom dbxAnnotationAssertion = dataFactory.getOWLAnnotationAssertionAxiom(i, dbXrefAnnotation);
		owlOntManager.applyChange(new AddAxiom(taxonomy,dbxAnnotationAssertion));
		
	}

}
