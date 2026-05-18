import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Models {
    private Models() {}

    public record Transaction(
        @JsonProperty("tx_id") String txId,
        int seq,
        String type,
        double amount,
        String account,
        @JsonProperty("from") String fromAcct,
        @JsonProperty("to") String toAcct
    ) {}

    public record SnapshotResult(
        Map<String, Double> balances,
        @JsonProperty("applied_tx_ids") List<String> appliedTxIds,
        @JsonProperty("rejected_tx_ids") List<String> rejectedTxIds
    ) {}

    public record InputData(
        @JsonProperty("initial_balances") Map<String, Double> initialBalances,
        List<Transaction> transactions,
        SnapshotResult expected
    ) {}
}
