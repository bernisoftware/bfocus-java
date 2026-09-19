package br.com.bernisoftware.bfocus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Item de {@link PeopleResource#batch(java.util.Collection)}: o cliente, o {@code external_id} da pessoa e os mesmos
 * campos de {@link PersonUpsert}. No fio vira {@code {"customer_external_id": …, "person": {"external_id": …, …}}}.
 *
 * <pre>{@code
 * PersonBatchItem.of("erp-1042", "app-77", PersonUpsert.builder().name("Paula Reis").email("paula@padaria.example").build());
 * }</pre>
 */
public final class PersonBatchItem extends PatchRequest {
    private final String customerExternalId;
    private final String externalId;

    private PersonBatchItem(String customerExternalId, String externalId, Map<String, Object> body) {
        super(body);
        this.customerExternalId = customerExternalId;
        this.externalId = externalId;
    }

    /**
     * Novo item.
     *
     * @param customerExternalId id do cliente (empresa) da pessoa no seu sistema
     * @param externalId id da pessoa no seu sistema (o {@code user.externalId} do widget)
     * @param person campos a gravar
     * @return o item (imutável)
     * @throws IllegalArgumentException id vazio
     */
    public static PersonBatchItem of(String customerExternalId, String externalId, PersonUpsert person) {
        Objects.requireNonNull(customerExternalId, "customerExternalId");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(person, "person");
        if (customerExternalId.isEmpty()) {
            throw new IllegalArgumentException("O customer_external_id da pessoa não pode ser vazio.");
        }
        if (externalId.isEmpty()) {
            throw new IllegalArgumentException("O external_id da pessoa não pode ser vazio.");
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("external_id", externalId);
        fields.putAll(person.body());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer_external_id", customerExternalId);
        body.put("person", Collections.unmodifiableMap(fields));
        return new PersonBatchItem(customerExternalId, externalId, Collections.unmodifiableMap(body));
    }

    /** @return o {@code external_id} do cliente da pessoa */
    public String getCustomerExternalId() {
        return customerExternalId;
    }

    /** @return o {@code external_id} da pessoa */
    public String getExternalId() {
        return externalId;
    }
}
