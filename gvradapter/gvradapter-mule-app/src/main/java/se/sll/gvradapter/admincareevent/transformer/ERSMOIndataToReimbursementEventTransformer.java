package se.sll.gvradapter.admincareevent.transformer;

import java.util.ArrayList;
import java.util.List;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import riv.followup.processdevelopment.reimbursement.v1.CVType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.GenderType;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.Diagnoser.Diagnos;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.Tillståndslista.Tillstånd;
import se.sll.ersmo.xml.indata.Yrkeskategorier.Yrkeskategori;
import se.sll.ersmo.xml.indata.Åtgärder.Åtgärd;

public class ERSMOIndataToReimbursementEventTransformer extends AbstractTransformer {

	public ERSMOIndataToReimbursementEventTransformer() {
		registerSourceType(DataTypeFactory.create(ERSMOIndata.class));
		setReturnDataType(DataTypeFactory.create(List.class));
		setName("ERSMOIndataToReimbursementEventTransformer");
	}

	@Override
	protected Object doTransform(Object src, String enc)
			throws TransformerException {
		ERSMOIndata ersmoIndata = (ERSMOIndata) src;
		List<CareEventType> responseList = new ArrayList<CareEventType>();
		for (Ersättningshändelse currentErsh : ersmoIndata.getErsättningshändelse()) {
			ObjectFactory of = new ObjectFactory();
			CareEventType currentEvent = of.createCareEventType();
			currentEvent.setId(currentErsh.getID());

			// Source System
			currentEvent.setSourceSystem(of.createSourceType());
			currentEvent.getSourceSystem().setOrganization("1.2.752.97.??");
			currentEvent.getSourceSystem().setName(ersmoIndata.getKälla());
			
			// Patient
			if (currentErsh.getPatient() != null) {
				currentEvent.setPatient(of.createPatientType());
				currentEvent.getPatient().setId(of.createPersonIdType());
				currentEvent.getPatient().getId().setId(currentErsh.getPatient().getID());
				if (currentErsh.getPatient().getID().startsWith("99")) {
					currentEvent.getPatient().getId().setType("1.2.752.97.3.1.3");
				} else {
					currentEvent.getPatient().getId().setType("1.2.752.129.2.1.3.1");
				} // TODO: Fixa logiken och lägg till reservpersoner
				if (currentErsh.getPatient().getFödelsedatum() != null) {
					currentEvent.getPatient().setBirthDate(currentErsh.getPatient().getFödelsedatum().toXMLFormat());
				}
				if (currentErsh.getPatient().getKön() != null) {
					if (currentErsh.getPatient().getKön().equals("M")) {
						currentEvent.getPatient().setGender(GenderType.M);
					} else if (currentErsh.getPatient().getKön().equals("K")) {
						currentEvent.getPatient().setGender(GenderType.F);
					} else if (currentErsh.getPatient().getKön().equals("X")) {
						currentEvent.getPatient().setGender(GenderType.U);
					}
				}
				currentEvent.getPatient().setResidency(of.createPatientTypeResidency());
				if (currentErsh.getPatient().getLkf() != null && currentErsh.getPatient().getLkf().length() >= 6) {
					currentEvent.getPatient().getResidency().setRegion(currentErsh.getPatient().getLkf().substring(0, 2));
					currentEvent.getPatient().getResidency().setMunicipality(currentErsh.getPatient().getLkf().substring(2, 4));
					currentEvent.getPatient().getResidency().setParish(currentErsh.getPatient().getLkf().substring(4, 6));
				}
			}
			
			// Contract
			//currentEvent.setContract(of.createCVType());
			//currentEvent.getContract().setOriginalText(currentErsh.getStartverksamhet());

			// Care Unit HSA-id
            currentEvent.setCareUnit(of.createCareUnitType());
			currentEvent.getCareUnit().setCareUnitHsaId("1.2.752.97.??:" + currentErsh.getStartverksamhet());

			// Last updated time
			currentEvent.setLastUpdatedTime("???");

			// Deleted
			currentEvent.setDeleted(currentErsh.isMakulerad());

			// Date Period
            currentEvent.setDatePeriod(of.createSplitDatePeriodType());
            currentEvent.getDatePeriod().setStartDate(currentErsh.getStartdatum().toXMLFormat());
            if (currentErsh.getSlutdatum() != null) {
                currentEvent.getDatePeriod().setEndDate(currentErsh.getSlutdatum().toXMLFormat());
            }

			if (currentErsh.getHändelseklass() != null) {
				if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
					// Emergency
					if (currentErsh.getHändelseklass().getVårdkontakt().getAkut() != null) {
						currentEvent.setEmergency(currentErsh.getHändelseklass().getVårdkontakt().getAkut().equals("J") ? true : false);
					}

					// Event Type
					currentEvent.setEventType(of.createEventTypeType());
					if (currentErsh.getHändelseklass().getVårdkontakt().getHändelseform() != null) {
						currentEvent.getEventType().setMainType(of.createCVType());
						currentEvent.getEventType().getMainType().setCodeSystem("no.oid");
						currentEvent.getEventType().getMainType().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getHändelseform().toString());

						currentEvent.getEventType().setSubType(of.createCVType());
						currentEvent.getEventType().getSubType().setCodeSystem("no.oid");
						currentEvent.getEventType().getSubType().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getTyp() + "+" + currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp());
					}

					// Fee Category
					currentEvent.setFeeCategory(of.createCVType());
					currentEvent.getFeeCategory().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass());
					//currentEvent.getFeeCategory().setCode(currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass());
					//currentEvent.getFeeCategory().setCodeSystem("????");

					// Involved Professions
					currentEvent.setInvolvedProfessions(of.createCareEventTypeInvolvedProfessions());
					if (currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori().size() > 0) {
						for (Yrkeskategori kategori : currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori()) {
							CVType currentProfession = of.createCVType();
							currentProfession.setCodeSystem("NO.OID: " + kategori.getKlass()); // TODO: Konvertera till OID?
							currentProfession.setCode(kategori.getKod());
							currentEvent.getInvolvedProfessions().getProfession().add(currentProfession);
						}
					}

					// Diagnoses
					currentEvent.setDiagnoses(of.createCareEventTypeDiagnoses());
					if (currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos().size() > 0) {
						for (Diagnos diagnos : currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos()) {
							CVType currentDiagnosis = of.createCVType();
							if (diagnos.getKlass().equals("008")) {
								currentDiagnosis.setCodeSystem("1.2.752.116.1.1.1.1.3");
								currentDiagnosis.setCodeSystemName("Internationell statistisk klassifikation av sjukdomar och relaterade hälsoproblem, systematisk förteckning (ICD-10-SE)");
							} else {
								currentDiagnosis.setCodeSystem("NO.OID: " + diagnos.getKlass());
							}
							currentDiagnosis.setCode(diagnos.getKod());
							currentEvent.getDiagnoses().getDiagnosis().add(currentDiagnosis);
						}
					}

					// Conditions
					currentEvent.setConditions(of.createCareEventTypeConditions());
					if (currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd().size() > 0) {
						for (Tillstånd tillstånd : currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd()) {
							CVType currentCondition = of.createCVType();
							currentCondition.setCodeSystem("NO.OID: " + tillstånd.getKlass()); // TODO: Konvertera till OID?
							currentCondition.setCode(tillstånd.getKod());
							currentEvent.getConditions().getCondition().add(currentCondition);
						}
					}

					// Activities
					currentEvent.setActivities(of.createCareEventTypeActivities());
					if (currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd().size() > 0) {
						for (Åtgärd åtgärd : currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd()) {
							CVType currentActivity = of.createCVType();
							if (åtgärd.getKlass().equals("007")) {
								currentActivity.setCodeSystem("1.2.752.116.1.3.2.1.4");
								currentActivity.setCodeSystemName("Klassifikation av vårdåtgärder (KVÅ)");
							} else if (åtgärd.getKlass().equals("020")) {
								currentActivity.setCodeSystem("1.2.752.129.2.2.3.1.1");
								currentActivity.setCodeSystemName("Anatomical Therapeutic Chemical classification system (ATC)");
							} else {
								currentActivity.setCodeSystem("no.oid: " + åtgärd.getKlass());
							}
							currentActivity.setCode(åtgärd.getKod());
							currentEvent.getActivities().getActivity().add(currentActivity);
						}
					}

					// Referred From
					currentEvent.setReferredFrom("1.2.752.97.??:" + currentErsh.getHändelseklass().getVårdkontakt().getRemissFöre().getKod());

					// Stay Before
					currentEvent.setStayBefore(of.createCVType());
					currentEvent.getStayBefore().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre().getKod());
					currentEvent.getStayBefore().setCodeSystem("NO.OID: " + currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre().getKod());

					// Stay After
					currentEvent.setStayAfter(of.createCVType());
					currentEvent.getStayAfter().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod());
					currentEvent.getStayAfter().setCodeSystem("NO.OID: " + currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod());

					// Deceased
					//currentEvent.setDeceased(true); TODO: Mapping??
				}
			}
			responseList.add(currentEvent);
		}

		return responseList;
	}

}

