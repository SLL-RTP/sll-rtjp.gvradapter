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
package se.sll.reimbursementadapter.processreimbursement.util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sll.reimbursementadapter.parser.CodeServerCode;
import se.sll.reimbursementadapter.parser.CodeServiceEntry;
import se.sll.reimbursementadapter.parser.CodeServiceXMLParser;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;

/**
 * Builder class that is responsible for creating the code server index for processreimbursement.
 */
public class CodeServerCacheBuilder {

    /** Code system name for the OMRKODNY system. */
    private static final String OMRKODNY = "OMRKODNY";
    /** Reference name for the link between BASOMR and OMRKOD. */
    private static final String BASOMR_OMRKOD = "BASOMRomrkod";
    /** Name of the attribute SHORTNAME. */
    private static final String SHORTNAME = "shortname";

    /** The configured file with codes for Geographical Areas. */
    private String geoAreaFile;
    /** The configured Date for which the returned codes should be newer than. Default is CodeServiceXMLParser.ONE_YEAR_BACK. */
    private Date newerThan = CodeServiceXMLParser.BREAK_POINT;
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(CodeServerCacheBuilder.class);

    /**
     * Input file for mapping BASOMRNY (Basområde). Mandatory.
     *
     * @param geoAreaFile the input XML file name.
     * @return the builder.
     */
    public CodeServerCacheBuilder withGeographicalAreaFile(String geoAreaFile) {
        this.geoAreaFile = geoAreaFile;
        return this;
    }

    /**
     * Indicates how to filter out old data items, default setting is to keep one year old data, i.e.
     * expiration date is less than one year back in time.
     *
     * @param newerThan the date that data must be newer than to be stored in index, otherwise it's ignored.
     * @return the builder.
     */
    public CodeServerCacheBuilder newerThan(Date newerThan) {
        this.newerThan = newerThan;
        return this;
    }

    /**
     * Builds the index.
     *
     * @return a map with Basområden as keys and {@link GeographicalAreaState} as value objects.
     */
    public Map<String, TermItem<GeographicalAreaState>> build() {

        LOG.info("build CodeServerCache from: {}", geoAreaFile);
        final HashMap<String, TermItem<GeographicalAreaState>> avdIndex = createGeographicalAreaIndex();
        LOG.info("hsaMappingIndex size: {}", avdIndex.size());
        return avdIndex;
    }

    /**
     * Builds the Geographical Area Index (Basområde) from the configured input file.
     *
     * @return A Map keyed with the Geographical Area code and the mapped TermItems.
     */
    protected HashMap<String, TermItem<GeographicalAreaState>> createGeographicalAreaIndex() {
        File testFile = new File(this.geoAreaFile);
        LOG.debug("Using geoAreaFile: " + testFile.getAbsolutePath());
        final HashMap<String, TermItem<GeographicalAreaState>> index = new HashMap<>();

        // Define a Code Server parser implementation for reading the file.
        final CodeServiceXMLParser parser = new CodeServiceXMLParser(this.geoAreaFile, new CodeServiceXMLParser.CodeServiceEntryCallback() {
            @Override
            public void onCodeServiceEntry(CodeServiceEntry codeServiceEntry) {
                TermItem<GeographicalAreaState> commissionType = index.get(codeServiceEntry.getId());
                if (commissionType == null) {
                    commissionType = new TermItem<>();
                    commissionType.setId(codeServiceEntry.getId());
                    index.put(codeServiceEntry.getId(), commissionType);
                }
                GeographicalAreaState state = new GeographicalAreaState();
                state.setName(codeServiceEntry.getAttribute(SHORTNAME));
                state.setValidFrom(codeServiceEntry.getValidFrom());
                state.setValidTo(codeServiceEntry.getValidTo());
                for (CodeServerCode codeServerCode : codeServiceEntry.getCodes(OMRKODNY)) {
                    if (codeServerCode.getReferenceId().equals(BASOMR_OMRKOD)) {
                        state.setMedicalServiceArea(codeServerCode.getValue());
                    }
                }

                commissionType.addState(state);
            }
        });

        // Define which attributes and codeSystems to parse (the rest will be silently filtered away)
        parser.extractAttribute(SHORTNAME);
        parser.extractCodeSystem(OMRKODNY);
        parser.setNewerThan(newerThan);

        parser.parse();

        return index;
    }

}
