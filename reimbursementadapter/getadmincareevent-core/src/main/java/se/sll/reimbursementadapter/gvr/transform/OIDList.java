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
package se.sll.reimbursementadapter.gvr.transform;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a list of mappings for all the types of code systems to their respective OID:s and CV Names.
 */
public class OIDList {

    private static Map<CodeSystem, String> oidMap = new HashMap<>();
    private static Map<CodeSystem, String> nameMap = new HashMap<>();

    static {
        oidMap.put(CodeSystem.PERSONNUMMER, "1.2.752.129.2.1.3.1");
        nameMap.put(CodeSystem.PERSONNUMMER, "Personnummer - vid användning inom vård och omsorg");

        oidMap.put(CodeSystem.SAMORDNINGSNUMMER, "1.2.752.129.2.1.3.3");
        nameMap.put(CodeSystem.SAMORDNINGSNUMMER, "Samordningsnummer - vid användning inom vård och omsorg");

        oidMap.put(CodeSystem.RESERVNUMMER_SLL, "1.2.752.129.2.1.3.2");
        nameMap.put(CodeSystem.RESERVNUMMER_SLL, "Reservnummer enligt V-TIM");

        oidMap.put(CodeSystem.HYBRID_GUID_IDENTIFIER,"1.2.752.129.2.1.2.1");
        nameMap.put(CodeSystem.HYBRID_GUID_IDENTIFIER, "Hybrididentifierare bestående av HSA-id för org. där id är unikt + id");

        oidMap.put(CodeSystem.ICD10_SE, "1.2.752.116.1.1.1.1.3");
        nameMap.put(CodeSystem.ICD10_SE, "Internationell statistisk klassifikation av sjukdomar och relaterade hälsoproblem, systematisk förteckning (ICD-10-SE)");

        oidMap.put(CodeSystem.KVÅ, "1.2.752.116.1.3.2.1.4");
        nameMap.put(CodeSystem.KVÅ, "Klassifikation av vårdåtgärder (KVÅ)");

        oidMap.put(CodeSystem.ATC, "1.2.752.129.2.2.3.1.1");
        nameMap.put(CodeSystem.ATC, "Anatomical Therapeutic Chemical classification system (ATC)");

        oidMap.put(CodeSystem.KV_KÖN, "1.2.752.129.2.2.1.1");
        nameMap.put(CodeSystem.KV_KÖN, "kv_kön Anger adminstrativt kön");

        oidMap.put(CodeSystem.KV_KONTAKTTYP, "1.2.752.129.2.2.2.25");
        nameMap.put(CodeSystem.KV_KONTAKTTYP, "kv_kontakttyp, anger vilken typ kontakten är");

        oidMap.put(CodeSystem.KV_LÄN, "1.2.752.129.2.2.1.18");
        nameMap.put(CodeSystem.KV_LÄN, "kv_län, länskod enligt SCB");

        oidMap.put(CodeSystem.KV_KOMMUN, "1.2.752.129.2.2.1.17");
        nameMap.put(CodeSystem.KV_KOMMUN, "kv_kommun, kommunkod enligt SCB");

        oidMap.put(CodeSystem.KV_FÖRSAMLING, "1.2.752.129.2.2.1.16");
        nameMap.put(CodeSystem.KV_FÖRSAMLING, "KV_FÖRSAMLING - Församlingskod enligt SCB");

        oidMap.put(CodeSystem.SLL_CS_UPPDRAGSTYP, "SLL.CS.UPPDRAGSTYP");
        nameMap.put(CodeSystem.SLL_CS_UPPDRAGSTYP, "SLL Code Server definition from the 'UPPDRAGSTYP' table.");

        oidMap.put(CodeSystem.SLL_CS_TILLSTÅND, "SLL.CS.TILLSTAND");
        nameMap.put(CodeSystem.SLL_CS_TILLSTÅND, "SLL Code Server definition from the 'TILLSTAND' table");

        oidMap.put(CodeSystem.SLL_CS_VDG, "SLL.CS.VDG");
        nameMap.put(CodeSystem.SLL_CS_VDG, "SLL Code Server definition from the 'VDG' table");
    }

    /**
     * Gets the OID for a specific incoming {@link CodeSystem}
     *
     * @param cs The Code System to map from.
     * @return The mapped OID for the code system.
     */
    public static String getOid(CodeSystem cs) {
        return oidMap.get(cs);
    }

    /**
     * Gets the Name for a specific incoming {@link CodeSystem}
     *
     * @param cs The Code System to map from.
     * @return The mapped OID for the code system.
     */
    public static String getName(CodeSystem cs) {
        return nameMap.get(cs);
    }
}
