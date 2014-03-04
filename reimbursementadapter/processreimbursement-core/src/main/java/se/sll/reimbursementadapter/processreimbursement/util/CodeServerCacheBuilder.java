package se.sll.reimbursementadapter.processreimbursement.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sll.reimbursementadapter.parser.CodeServerCode;
import se.sll.reimbursementadapter.parser.CodeServiceEntry;
import se.sll.reimbursementadapter.parser.CodeServiceXMLParser;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CodeServerCacheBuilder {

    private static final String OMRKODNY = "OMRKODNY";
    private static final String BASOMR_OMRKOD = "BASOMRomrkod";
    private static final String SHORTNAME = "shortname";

    private String geoAreaFile;
    private Date newerThan = CodeServiceXMLParser.ONE_YEAR_BACK;
    private static final Logger log = LoggerFactory.getLogger(CodeServerCacheBuilder.class);

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
    @SuppressWarnings("unused")
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

        log.info("build CodeServerCache from: {}", geoAreaFile);
        final HashMap<String, TermItem<GeographicalAreaState>> avdIndex = createGeographicalAreaIndex();
        log.info("hsaMappingIndex size: {}", avdIndex.size());
        return avdIndex;
    }

    protected HashMap<String, TermItem<GeographicalAreaState>> createGeographicalAreaIndex() {
        final HashMap<String, TermItem<GeographicalAreaState>> index = new HashMap<>();
        File test = new File(this.geoAreaFile);
        System.out.printf("Current geoAreaFile: %1$s \\n", test.getAbsolutePath());
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

        parser.extractAttribute(SHORTNAME);
        parser.extractCodeSystem(OMRKODNY);
        parser.setNewerThan(newerThan);

        parser.parse();

        return index;
    }

}
