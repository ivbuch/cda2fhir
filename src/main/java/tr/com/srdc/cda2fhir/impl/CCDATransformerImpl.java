package tr.com.srdc.cda2fhir.impl;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.IdDt;
import org.openhealthtools.mdht.uml.cda.Section;
import org.openhealthtools.mdht.uml.cda.SubstanceAdministration;
import org.openhealthtools.mdht.uml.cda.consol.*;
import tr.com.srdc.cda2fhir.CCDATransformer;
import tr.com.srdc.cda2fhir.ResourceTransformer;

import java.util.UUID;

/**
 * Created by mustafa on 8/3/2016.
 */
public class CCDATransformerImpl implements CCDATransformer {

    private int counter;
    private IdGeneratorEnum idGenerator;
    private ResourceTransformer resTransformer;
    private ResourceReferenceDt patientRef;

    public CCDATransformerImpl() {
        this.counter = 0;
        // The default resource id pattern is UUID
        this.idGenerator = IdGeneratorEnum.UUID;
        resTransformer = new ResourceTransformerImpl(this);
    }

    public CCDATransformerImpl(IdGeneratorEnum idGen) {
        this();
        // Override the default resource id pattern
        this.idGenerator = idGen;
    }

    @Override
    public void setIdGenerator(IdGeneratorEnum idGen) {
        this.idGenerator = idGen;
    }

    @Override
    public synchronized String getUniqueId() {
        switch (this.idGenerator) {
            case COUNTER:
                return Integer.toString(++counter);
            case UUID:
            default:
                return UUID.randomUUID().toString();
        }
    }

    @Override
    public ResourceReferenceDt getPatientRef() {
        return patientRef;
    }

    @Override
    public Bundle transformCCD(ContinuityOfCareDocument ccd) {
        if(ccd == null)
            return null;

        // init the global ccd bundle via a call to resource transformer, which handles cda header data (i.e. all except the sections)
        Bundle ccdBundle = resTransformer.tClinicalDocument2Composition(ccd);
        // the first bundle entry is always the composition
        Composition ccdComposition = (Composition)ccdBundle.getEntry().get(0).getResource();
        // init the patient id reference. the patient is always the 2nd bundle entry
        patientRef = new ResourceReferenceDt(ccdBundle.getEntry().get(1).getResource().getId());

        // transform the sections
        for(Section cdaSec: ccd.getSections()) {
            Composition.Section fhirSec = resTransformer.tSection2Section(cdaSec);
            ccdComposition.addSection(fhirSec);
            if(cdaSec instanceof AdvanceDirectivesSection) {

            }
            else if(cdaSec instanceof AllergiesSection) {
            	AllergiesSection allSec = (AllergiesSection) cdaSec;
            	for(AllergyProblemAct probAct : allSec.getAllergyProblemActs()) {
            		Bundle allBundle = resTransformer.tAllergyProblemAct2AllergyIntolerance(probAct);
                    mergeBundles(allBundle, ccdBundle, fhirSec, AllergyIntolerance.class);
            	}
            }
            else if(cdaSec instanceof EncountersSection) {

            }
            else if(cdaSec instanceof FamilyHistorySection) {
                FamilyHistorySection famSec = (FamilyHistorySection) cdaSec;
                for(FamilyHistoryOrganizer fhOrganizer : famSec.getFamilyHistories()) {
                    FamilyMemberHistory fmh = resTransformer.tFamilyHistoryOrganizer2FamilyMemberHistory(fhOrganizer);
                    ResourceReferenceDt ref = fhirSec.addEntry();
                    ref.setReference(fmh.getId());
                    ccdBundle.addEntry(new Bundle.Entry().setResource(fmh));
                }
            }
            else if(cdaSec instanceof FunctionalStatusSection) {

            }
            else if(cdaSec instanceof ImmunizationsSection) {
            	ImmunizationsSection immSec = (ImmunizationsSection) cdaSec;
            	for(SubstanceAdministration subAd : immSec.getSubstanceAdministrations()) {
            		Bundle immBundle = resTransformer.tSubstanceAdministration2Immunization(subAd);
                    mergeBundles(immBundle, ccdBundle, fhirSec, Immunization.class);
            	}
            }
            else if(cdaSec instanceof MedicalEquipmentSection) {

            }
            else if(cdaSec instanceof MedicationsSection) {
                MedicationsSection medSec = (MedicationsSection) cdaSec;
                for(MedicationActivity medAct : medSec.getMedicationActivities()) {
                    Bundle medBundle = resTransformer.tMedicationActivity2MedicationStatement(medAct);
                    mergeBundles(medBundle, ccdBundle, fhirSec, MedicationStatement.class);
                }
            }
            else if(cdaSec instanceof PayersSection) {

            }
            else if(cdaSec instanceof PlanOfCareSection) {

            }
            else if(cdaSec instanceof ProblemSection) {
                ProblemSection probSec = (ProblemSection) cdaSec;
                for(ProblemConcernAct pcAct : probSec.getConsolProblemConcerns()) {
                    Bundle conBundle = resTransformer.tProblemConcernAct2Condition(pcAct);
                    mergeBundles(conBundle, ccdBundle, fhirSec, Condition.class);
                }
            }
            else if(cdaSec instanceof ProceduresSection) {
                ProceduresSection procSec = (ProceduresSection) cdaSec;
                for(ProcedureActivityProcedure proc : procSec.getConsolProcedureActivityProcedures()) {
                    Bundle procBundle = resTransformer.tProcedure2Procedure(proc);
                    mergeBundles(procBundle, ccdBundle, fhirSec, ca.uhn.fhir.model.dstu2.resource.Procedure.class);
                }
            }
            else if(cdaSec instanceof ResultsSection) {
            	ResultsSection resultSec = (ResultsSection) cdaSec;
            	for(ResultOrganizer resOrg : resultSec.getResultOrganizers()) {
            		for(ResultObservation resObs : resOrg.getResultObservations()) {
            			// TODO: tResultOrganizer2DiagnosticReport should be used instead of tResultObservation2Observation
            			Bundle resBundle = resTransformer.tResultObservation2Observation(resObs);
                        mergeBundles(resBundle, ccdBundle, fhirSec, Observation.class);
            		}
            	}
            }
            else if(cdaSec instanceof SocialHistorySection) {

            }
            else if(cdaSec instanceof VitalSignsSection) {
            	VitalSignsSection vitalSec = (VitalSignsSection) cdaSec;
            	for(VitalSignsOrganizer vsOrg : vitalSec.getVitalSignsOrganizers())	{
            		for(VitalSignObservation vsObs : vsOrg.getVitalSignObservations()) {
            			Bundle vsBundle = resTransformer.tVitalSignObservation2Observation(vsObs);
                        mergeBundles(vsBundle, ccdBundle, fhirSec, Observation.class);
            		}
            	}
            }
        }

        return ccdBundle;
    }

    /**
     * Copies all the entries from the source bundle to the target bundle, and at the same time adds a reference to the Section.Entry for each instance of the specified class
     * @param sourceBundle
     * @param targetBundle
     * @param fhirSec
     * @param sectionRefCls
     */
    private void mergeBundles(Bundle sourceBundle, Bundle targetBundle, Composition.Section fhirSec, Class<?> sectionRefCls) {
        for(Entry entry : sourceBundle.getEntry()) {
            // Add all the resources returned from the source bundle to the target bundle
            targetBundle.addEntry(new Bundle.Entry().setResource(entry.getResource()));
            // Add a reference to the section for each instance of requested class, e.g. Observation, Procedure ...
            if(sectionRefCls.isInstance(entry.getResource())) {
                ResourceReferenceDt ref = fhirSec.addEntry();
                ref.setReference(entry.getResource().getId());
            }
        }
    }
}
