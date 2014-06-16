/**
 *  Copyright (c) 2013 SLL <http://sll.se/>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.sll.reimbursementadapter.admincareevent.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sll.reimbursementadapter.admincareevent.model.*;
import se.sll.reimbursementadapter.parser.CodeServerCode;
import se.sll.reimbursementadapter.parser.CodeServiceEntry;
import se.sll.reimbursementadapter.parser.CodeServiceXMLParser;
import se.sll.reimbursementadapter.parser.CodeServiceXMLParser.CodeServiceEntryCallback;
import se.sll.reimbursementadapter.parser.SimpleXMLElementParser;
import se.sll.reimbursementadapter.parser.SimpleXMLElementParser.ElementMatcherCallback;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.parser.TermState;

/**
 * Builds HSA Mapping index. <p>
 * 
 * Uses XML parsing utilities to traverse input files.
 * 
 * @author Peter
 */
public class CodeServerMEKCacheBuilder {
    // attribute and element names.
    private static final String AVTAL = "AVTAL";
    private static final String STYP = "STYP";
    private static final String TILL_DATUM = "TillDatum";
    private static final String FROM_DATUM = "FromDatum";
    private static final String KUND = "KUND";
    private static final String HSA_ID = "HSAId";
    private static final String KOMBIKAKOD = "Kombikakod";
    private static final String ABBREVIATION = "abbreviation";
    private static final String UPPDRAGSTYP = "UPPDRAGSTYP";
    private static final String NO_COMMISSION_ID = "0000";
    private static final String SAMVERKS = "SAMVERKS";
    private static final String MOTTAGNINGSTYP = "AVDTYP";
    private static final String SHORTNAME = "shortname";

    private static final Logger LOG = LoggerFactory.getLogger(CodeServerMEKCacheBuilder.class);

    private String mekFile;
    private String facilityFile;
    private String commissionFile;
    private String commissionTypeFile;
    
    private Date newerThan = CodeServiceXMLParser.ONE_YEAR_BACK;

    /**
     * Input file for mapping (MEK) data (mandatory).
     * 
     * @param mekFile the input XML file name.
     * @return the builder.
     */
    public CodeServerMEKCacheBuilder withMekFile(String mekFile) {
        this.mekFile = mekFile;
        return this;
    }

    /**
     * Input file for facility (AVD) data (mandatory).
     * 
     * @param facilityFile the input XML file name.
     * @return the builder.
     */
    public CodeServerMEKCacheBuilder withFacilityFile(String facilityFile) {
        this.facilityFile = facilityFile;
        return this;
    }

    /**
     * Input file for commission (SAMVERKS) mapping data (mandatory).
     * 
     * @param commissionFile the input XML file name.
     * @return the builder.
     */
    public CodeServerMEKCacheBuilder withCommissionFile(String commissionFile) {
        this.commissionFile = commissionFile;
        return this;
    }

    /**
     * Input file for commission type (UPPDRAGSTYP) data (mandatory).
     * 
     * @param commissionTypeFile the input XML file name.
     * @return the builder.
     */
    public CodeServerMEKCacheBuilder withCommissionTypeFile(String commissionTypeFile) {
        this.commissionTypeFile = commissionTypeFile;
        return this;
    }
   

    /**
     * Indicates how to filter out old data items, default setting is to keep one year old data, i.e.
     * expiration date is less than one year back in time.
     * 
     * @param newerThan the date that data must be newer than to be stored in index, otherwise it's ignored.
     * @return the builder.
     */
    public CodeServerMEKCacheBuilder newerThan(Date newerThan) {
        this.newerThan = newerThan;
        return this;
    }

    /**
     * Builds the index.
     * 
     * @return a map with Kombika as keys and {@link FacilityState} as value objects.
     */
    public Map<String, TermItem<FacilityState>> build() {

        LOG.info("build hsaMappingIndex from: {}", mekFile);
        final HashMap<String, TermItem<FacilityState>> avdIndex = createFacilityIndex();
        LOG.info("hsaMappingIndex size: {}", avdIndex.size());

        return avdIndex;
    }
    
    
    /**
     * Assumes list is empty or contains one item.
     * 
     * @param list the list.
     * @return null or first item.
     */
    protected static <T> T singleton(List<T> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        if (list.size() > 1) {
            LOG.warn("Expected singleton list " + list + " has more than one entry (unsupported and unexpected " +
                    "behavior as result)");
        }
        return list.get(0);
    }

    /**
     * Builds the Facility (AVD) index that links in all the relevant connections from the other indexes.
     * This method is responsible for creating the Commission (Samverks) and the HSA (MEK) mapping index.
     * All the configuration is taken from class variables set in the building (pattern, not code) process.
     *
     * @return a Hashmap where each key represents a Facility Id (Kombika), and the value is a TermItem of
     * type FacilityState.
     */
    protected HashMap<String, TermItem<FacilityState>> createFacilityIndex() {
        LOG.info("build commissionIndex from: {}", commissionFile);
        // Create underlying indexes to this one that will be linked in.
        final HashMap<String, TermItemCommission<CommissionState>> samverksIndex = createCommissionIndex();
        final Map<String, List<TermItem<HSAMappingState>>> hsaIndex = createHSAIndex();
        
        LOG.info("commissionIndex size: {}", samverksIndex.size());

        // Create a index structure
        final HashMap<String, TermItem<FacilityState>> index = new HashMap<>();

        // Create a CodeServiceXMLParser impl. that populates the index from the facilityFile.
        final CodeServiceXMLParser parser = new CodeServiceXMLParser(this.facilityFile, new CodeServiceEntryCallback() {
            @Override
            public void onCodeServiceEntry(CodeServiceEntry codeServiceEntry) {
                // Get all SAMVERKS links from the current codeServiceEntry (facility).
                final List<CodeServerCode> codes = codeServiceEntry.getCodes(SAMVERKS);
                if (codes != null) {
                    // Filter out non-existing SAMVERKS associations.
                    if (codes.size() == 1 && NO_COMMISSION_ID.equals(codes.get(0).getValue())) {
                        return;
                    }
                    // See if the Facility id already exists in the index. If not, add a new TermItem.
                    TermItem<FacilityState> avd = index.get(codeServiceEntry.getId());
                    if (avd == null) {
                        avd = new TermItem<>();
                        avd.setId(codeServiceEntry.getId());
                        index.put(codeServiceEntry.getId(), avd);
                    }

                    // Create a new FacilityState and populate the base values from the current codeServiceEntry.
                    final FacilityState state = new FacilityState();
                    state.setName(codeServiceEntry.getAttribute(SHORTNAME));
                    state.setValidFrom(codeServiceEntry.getValidFrom());
                    state.setValidTo(codeServiceEntry.getValidTo());
                    // If the HSA mapping does not already exist, create and populate it from the hsaIndex.
                    if (hsaIndex.get(codeServiceEntry.getId()) != null && hsaIndex.get(codeServiceEntry.getId()).size() > 0) {
                    	state.setHSAMapping(hsaIndex.get(codeServiceEntry.getId()).get(0));
                    }
                    // Loop over every each previously fetched samverks code in the facility.
                    for (final CodeServerCode code : codes) {
                        // If the connection doesn't exist, fetch from the samverksIndex and populate it.
                        final TermItemCommission<CommissionState> samverks = samverksIndex.get(code.getValue());
                        if (samverks != null) {
                            state.getCommissions().add(samverks);

                            samverks.putBackRef(avd);
                        }
                    }

                    // Set the base parameter customer code.
                    List<CodeServerCode> customerCodes = codeServiceEntry.getCodes(KUND);
                    if (customerCodes != null) {
                        for (CodeServerCode customerCode : customerCodes) {
                            state.setCustomerCode(customerCode.getValue());
                        }
                    }

                    // Set the base parameter care unit type.
                    List<CodeServerCode> careUnitTypeCodes = codeServiceEntry.getCodes(MOTTAGNINGSTYP);
                    if (careUnitTypeCodes != null) {
                        for (CodeServerCode careUnitTypeCode : careUnitTypeCodes) {
                            state.setCareUnitType(careUnitTypeCode.getValue());
                        }
                    }
                    avd.addState(state);
                }
            }
        });

        // Set the attributes and code systems that will be extracted in the parser.
        parser.extractAttribute(SHORTNAME);
        parser.extractCodeSystem(SAMVERKS);
        parser.extractCodeSystem(KUND);
        parser.extractCodeSystem(MOTTAGNINGSTYP);
        parser.setNewerThan(newerThan);

        // Execure the parsing of the source XML.
        parser.parse();
        return index;
    }

    /**
     * Creates an index for mapping between Facility Id:s (Kombika) to the national HSA-id format.
     * The index is created by parsing the MEK XML File, mekFile.
     *
     * @return A Map where the key is a string with the Facility Id and with the value of the corresponding
     * HSAMappingState that holds the actual mapping information to HSA.
     */
    protected Map<String, List<TermItem<HSAMappingState>>> createHSAIndex() {
        LOG.info("build HSA index from: {}", mekFile);

        // Create the parser and point it to the mekFile.
        final SimpleXMLElementParser elementParser = new SimpleXMLElementParser(this.mekFile);
        final Map<String, List<TermItem<HSAMappingState>>> map = new HashMap<>();

        // Create a map with the mappings from the attriture names in the XML to the index used in the parser below.
        final Map<String, Integer> elements = new HashMap<>();
        elements.put(KOMBIKAKOD, 1);
        elements.put(HSA_ID, 2);
        elements.put(FROM_DATUM, 3);
        elements.put(TILL_DATUM, 4);


        // Parse the XML with a new ElementMatcherCallback set to the  <mappning> element in the XML file.
        elementParser.parse("mappning", elements, new ElementMatcherCallback() {
            private TermItem<HSAMappingState> mapping = null;
            private HSAMappingState state = null;
            @Override
            public void match(int element, String data) {
                switch (element) {
                case 1:
                    mapping.setId(data);
                    break;
                case 2:
                    state.setHsaId(data);
                    break;
                case 3:
                    state.setValidFrom(TermState.toDate(data));
                    break;
                case 4:
                    state.setValidTo(TermState.toDate(data));
                    break;
                }
            }

            @Override
            public void end() {
                if (state.isNewerThan(newerThan)) {
                    List<TermItem<HSAMappingState>> list = map.get(mapping.getId());
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(mapping.getId(), list);
                    }
                    mapping.addState(state);
                    list.add(mapping);
                }
            }

            @Override
            public void begin() {
                state = new HSAMappingState();
                mapping = new TermItem<>();
            }
        });

        return map;
    }

    /**
     * Builds the Commission (Samverks) index that links in all the relevant connections from the lower indexes in the tree.
     * This method is responsible for creating the Commission Type (Samverkstyp) index.
     * All the configuration is taken from class variables set in the building (pattern, not code) process.
     *
     * @return a Hashmap where each key represents a Commission id (Samverks-id), and the value is a TermItem of
     * type CommissionState.
     */
    protected HashMap<String, TermItemCommission<CommissionState>> createCommissionIndex() {

        final HashMap<String, TermItemCommission<CommissionState>> index = new HashMap<>();

        LOG.info("build commissionTypeIndex from: {}", commissionTypeFile);
        final HashMap<String, TermItem<CommissionTypeState>> uppdragstypIndex = createCommissionTypeIndex();
        LOG.info("commissionTypeIndex size: {}", uppdragstypIndex.size());

        final CodeServiceXMLParser parser = new CodeServiceXMLParser(this.commissionFile, new CodeServiceEntryCallback() {
            @Override
            public void onCodeServiceEntry(CodeServiceEntry codeServiceEntry) {
                final CodeServerCode uCode = singleton(codeServiceEntry.getCodes(UPPDRAGSTYP));
                final TermItem<CommissionTypeState> uppdragstyp = (uCode == null) ? null : uppdragstypIndex.get(uCode.getValue());
                if (uppdragstyp == null) {
                    LOG.trace("No such commission: {}", uCode);
                    return;
                }

                TermItemCommission<CommissionState> commission = index.get(codeServiceEntry.getId());
                if (commission == null) {
                    commission = new TermItemCommission<>();
                    commission.setId(codeServiceEntry.getId());
                    index.put(codeServiceEntry.getId(), commission);
                }
                final CommissionState state = new CommissionState();
                CodeServerCode contractCode = singleton(codeServiceEntry.getCodes(AVTAL));
                if (contractCode != null) {
                    state.setContractCode(contractCode.getValue());
                }
                state.setName(codeServiceEntry.getAttribute(ABBREVIATION));
                state.setCommissionType(uppdragstyp);
                CodeServerCode assignmentCode = singleton(codeServiceEntry.getCodes(STYP));
                if (assignmentCode != null) {
                    state.setAssignmentType(assignmentCode.getValue());
                }
                state.setValidFrom(codeServiceEntry.getValidFrom());
                state.setValidTo(codeServiceEntry.getValidTo());
                
                commission.addState(state);
            }
        });

        parser.extractAttribute(ABBREVIATION);
        parser.extractCodeSystem(UPPDRAGSTYP);
        parser.extractCodeSystem(STYP);
        parser.extractCodeSystem(AVTAL);
        
        parser.setNewerThan(newerThan);
    
        parser.parse();

        return index;
    }

    /**
     * Builds the Commission Type (Samverkstyp) index. This index is the bottom node, and will not link in any other trees.
     * All the configuration is taken from class variables set in the building (pattern, not code) process.
     *
     * @return a Hashmap where each key represents a Commission Type id (Samverkstypsid), and the value is a TermItem of
     * type CommissionTypeState.
     */
    protected HashMap<String, TermItem<CommissionTypeState>> createCommissionTypeIndex() {
        final HashMap<String, TermItem<CommissionTypeState>> index = new HashMap<>();
        final CodeServiceXMLParser parser = new CodeServiceXMLParser(this.commissionTypeFile, new CodeServiceEntryCallback() {
            @Override
            public void onCodeServiceEntry(CodeServiceEntry codeServiceEntry) {
                TermItem<CommissionTypeState> commissionType = index.get(codeServiceEntry.getId());
                if (commissionType == null) {
                    commissionType = new TermItem<>();
                    commissionType.setId(codeServiceEntry.getId());
                    index.put(codeServiceEntry.getId(), commissionType);
                }
                final CommissionTypeState state = new CommissionTypeState();
                state.setName(codeServiceEntry.getAttribute(SHORTNAME));
                state.setValidFrom(codeServiceEntry.getValidFrom());
                state.setValidTo(codeServiceEntry.getValidTo());
                commissionType.addState(state);
            }
        });

        parser.extractAttribute(SHORTNAME);
        parser.setNewerThan(newerThan);

        parser.parse();

        return index;
    }

}
