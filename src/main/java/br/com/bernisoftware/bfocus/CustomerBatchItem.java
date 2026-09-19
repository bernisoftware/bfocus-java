package br.com.bernisoftware.bfocus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Item de {@link CustomersResource#batch(java.util.Collection)}: o {@code external_id} do cliente com os mesmos
 * campos de {@link CustomerUpsert} (parciais do mesmo jeito — veja {@link PatchRequest}). Assim o código que monta o
 * {@code CustomerUpsert} do dia a dia serve também para a carga inicial.
 *
 * <pre>{@code
 * CustomerBatchItem.of("erp-1042", CustomerUpsert.builder().name("Padaria Estrela").build());
 * // no fio: {"external_id":"erp-1042","name":"Padaria Estrela"}
 * }</pre>
 */
public final class CustomerBatchItem extends PatchRequest {
    private final String externalId;

    private CustomerBatchItem(String externalId, Map<String, Object> body) {
        super(body);
        this.externalId = externalId;
    }

    /**
     * Novo item.
     *
     * @param externalId id do cliente no seu sistema (obrigatório; ex.: {@code erp-1042})
     * @param customer campos a gravar
     * @return o item (imutável)
     * @throws IllegalArgumentException id vazio
     */
    public static CustomerBatchItem of(String externalId, CustomerUpsert customer) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(customer, "customer");
        if (externalId.isEmpty()) {
            throw new IllegalArgumentException("O external_id do cliente não pode ser vazio.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_id", externalId);
        body.putAll(customer.body());
        return new CustomerBatchItem(externalId, Collections.unmodifiableMap(body));
    }

    /** @return o {@code external_id} do cliente */
    public String getExternalId() {
        return externalId;
    }
}
