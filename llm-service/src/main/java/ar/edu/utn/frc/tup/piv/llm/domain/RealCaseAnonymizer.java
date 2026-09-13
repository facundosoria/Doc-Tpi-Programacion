package ar.edu.utn.frc.tup.piv.llm.domain;
import com.fasterxml.jackson.databind.JsonNode; import com.fasterxml.jackson.databind.node.*; import java.util.*;
public final class RealCaseAnonymizer { private RealCaseAnonymizer(){} private static final Set<String> FORBIDDEN=Set.of("studentid","student_id","userid","user_id","attemptid","attempt_id","email","studentname","student_name");
 public static JsonNode anonymize(JsonNode source){return scrub(source.deepCopy());}
 private static JsonNode scrub(JsonNode n){if(n instanceof ObjectNode o){var keys=new ArrayList<String>();o.fieldNames().forEachRemaining(keys::add);for(String k:keys)if(FORBIDDEN.contains(k.toLowerCase()))o.remove(k);else o.set(k,scrub(o.get(k)));return o;}if(n instanceof ArrayNode a){for(int i=0;i<a.size();i++)a.set(i,scrub(a.get(i)));return a;}return n.isTextual()?new TextNode(TranscriptSanitizer.anonymize(n.asText())):n;}
}
