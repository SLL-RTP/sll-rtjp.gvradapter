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

/**
 * An enum that contains the names of all the code systems used in the application.
 * Is used in {@link se.sll.reimbursementadapter.gvr.transform.OIDList} for fetching
 * OID:s and names for the respective code system.
 */
public enum CodeSystem {

    // Format code systems according to V-TIM
    KV_KÖN,
    KV_KONTAKTTYP,
    KV_LÄN,
    KV_KOMMUN,
    KV_FÖRSAMLING,

    // Clinical codes
    ICD10_SE,
    KVÅ,
    ATC,

    // Patient identifiers
    PERSONNUMMER ,
    RESERVNUMMER_SLL,
    SAMORDNINGSNUMMER,

    HYBRID_GUID_IDENTIFIER,

    // Code systems from the local SLL Code Server
    SLL_CS_UPPDRAGSTYP,
    SLL_CS_TILLSTÅND,
    SLL_CS_VDG
}
