package se.sll.reimbursementadapter.gvr.transform;

/**
 * Contains a list of mappings for all the types of code systems to their respective OID:s and CV Names.
 * The OID can be found in OID_[name], and the text can be found in OID_[name]_TEXT.
 */
public class OIDList {

    // Kodverk
    protected static final String OID_TEMPORARY_PATIENT_ID = "1.2.752.97.3.1.3";
    protected static final String OID_COORDINATION_ID = "1.2.752.129.2.1.3.3";
    protected static final String OID_PATIENT_IDENTIFIER = "1.2.752.129.2.1.3.1";
    protected static final String OID_HYBRID_GUID_IDENTIFIER = "1.2.752.129.2.1.2.1";
    protected static final String OID_ICD10_SE = "1.2.752.116.1.1.1.1.3";
    protected static final String OID_ICD10_SE_TEXT = "Internationell statistisk klassifikation av sjukdomar och relaterade hälsoproblem, systematisk förteckning (ICD-10-SE)";
    protected static final String OID_KVÅ = "1.2.752.116.1.3.2.1.4";
    protected static final String OID_ATC = "1.2.752.129.2.2.3.1.1";
    // Formella kodverk enligt V-TIM 2.0.
    protected static final String OID_KV_KÖN = "1.2.752.129.2.2.1.1";
    protected static final String OID_KV_KONTAKTTYP = "1.2.752.129.2.2.2.25";
    protected static final String OID_KV_KONTAKTTYP_TEXT = "KV kontakttyp";
    protected static final String OID_KV_LÄN = "1.2.752.129.2.2.1.18";
    protected static final String OID_KV_LÄN_TEXT = "KV_LÄN - Länskod enligt SCB";
    protected static final String OID_KV_KOMMUN = "1.2.752.129.2.2.1.17";
    protected static final String OID_KV_KOMMUN_TEXT = "KV_KOMMUN - Kommunkod enligt SCB";
    protected static final String OID_KV_FÖRSAMLING = "1.2.752.129.2.2.1.16";
    protected static final String OID_KV_FÖRSAMLING_TEXT = "KV_FÖRSAMLING - Församlingskod enligt SCB";
    // Egna kodverk skapade från Codeserver.
    protected static final String OID_SLL_CS_UPPDRAGSTYP = "SLL.CS.UPPDRAGSTYP";
    protected static final String OID_SLL_CS_UPPDRADSTYP_TEXT = "SLL Code Server definition from the 'UPPDRAGSTYP' table.";
    protected static final String OID_SLL_CS_TILLSTAND = "SLL.CS.TILLSTAND";
    protected static final String OID_SLL_CS_TILLSTAND_TEXT = "SLL Code Server definition from the 'UPPDRAGSTYP' table";
    protected static final String OID_SLL_CS_VDG = "SLL.CS.VDG";
    protected static final String OID_SLL_CS_VDG_TEXT = "SLL Code Server definition from the 'VDG' table";

}
